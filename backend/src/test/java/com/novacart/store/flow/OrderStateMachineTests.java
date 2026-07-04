package com.novacart.store.flow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.novacart.store.config.DataInitializer;
import com.novacart.store.dto.AuthDtos;
import com.novacart.store.entity.ListingStatus;
import com.novacart.store.entity.Offer;
import com.novacart.store.entity.OfferStatus;
import com.novacart.store.repository.CategoryRepository;
import com.novacart.store.repository.ListingRepository;
import com.novacart.store.repository.OfferRepository;
import com.novacart.store.service.AuthService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

/**
 * State-machine correctness: an item can only sell once. Each test drives a
 * scenario the audit found broken (double-accept, offer reuse, accept-on-sold,
 * cancel leaving a live offer, reserved buy-now) and asserts the server now
 * refuses it. These fail against the pre-fix code.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderStateMachineTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private AuthService authService;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ListingRepository listingRepository;
    @Autowired private OfferRepository offerRepository;
    @Autowired private ObjectMapper objectMapper;

    private String sellerToken;
    private String bobToken;
    private String caraToken;
    private long categoryId;

    @BeforeEach
    void setUp() {
        sellerToken = signup("seller");
        bobToken = signup("bob");
        caraToken = signup("cara");
        categoryId = categoryRepository.findAll().getFirst().getId();
    }

    // ---------- C2: a seller cannot accept two offers on one listing ----------
    @Test
    void accepting_one_offer_rejects_the_others() throws Exception {
        long listing = postListing(sellerToken, "320.00");
        long bobOffer = makeOffer(bobToken, listing, "300.00");
        long caraOffer = makeOffer(caraToken, listing, "280.00");

        // Seller accepts Bob's offer.
        mockMvc.perform(post("/api/offers/" + bobOffer + "/accept")
                .header("Authorization", "Bearer " + sellerToken)).andExpect(status().isOk());

        // Cara's offer must have been auto-rejected, so accepting it now fails.
        mockMvc.perform(post("/api/offers/" + caraOffer + "/accept")
                .header("Authorization", "Bearer " + sellerToken)).andExpect(status().is4xxClientError());

        assertThat(offerRepository.findById(caraOffer).map(Offer::getStatus)).contains(OfferStatus.REJECTED);
        assertThat(listingRepository.findById(listing).orElseThrow().getStatus()).isEqualTo(ListingStatus.RESERVED);
    }

    // ---------- C2: an accepted offer can't mint a second order ----------
    @Test
    void accepted_offer_cannot_be_reused_for_a_second_order() throws Exception {
        long listing = postListing(sellerToken, "320.00");
        long offer = makeOffer(bobToken, listing, "300.00");
        mockMvc.perform(post("/api/offers/" + offer + "/accept")
                .header("Authorization", "Bearer " + sellerToken)).andExpect(status().isOk());

        // First checkout succeeds and consumes the offer.
        checkout(bobToken, listing, offer).andExpect(status().isOk());
        assertThat(offerRepository.findById(offer).map(Offer::getStatus)).contains(OfferStatus.CONSUMED);

        // Reusing the same offer for another order must fail.
        checkout(bobToken, listing, offer).andExpect(status().is4xxClientError());
    }

    // ---------- C2: accepting a leftover offer on a SOLD listing ----------
    @Test
    void cannot_accept_offer_on_a_sold_listing() throws Exception {
        long listing = postListing(sellerToken, "100.00");
        // Cara leaves a standing offer but the seller never answers it.
        long caraOffer = makeOffer(caraToken, listing, "90.00");

        // Bob buys it outright and completes the sale.
        MvcResult order = checkout(bobToken, listing, null).andExpect(status().isOk()).andReturn();
        long orderId = dataId(order);
        transition(bobToken, orderId, "pay");
        ship(sellerToken, orderId);
        transition(bobToken, orderId, "confirm-receipt");
        assertThat(listingRepository.findById(listing).orElseThrow().getStatus()).isEqualTo(ListingStatus.SOLD);

        // The seller now trying to accept Cara's stale offer must be refused (409).
        mockMvc.perform(post("/api/offers/" + caraOffer + "/accept")
                        .header("Authorization", "Bearer " + sellerToken))
                .andExpect(status().isConflict());
    }

    // ---------- C2: cancel must not leave a reusable accepted offer ----------
    @Test
    void cancelling_an_order_does_not_leave_a_reusable_offer() throws Exception {
        long listing = postListing(sellerToken, "320.00");
        long offer = makeOffer(bobToken, listing, "300.00");
        mockMvc.perform(post("/api/offers/" + offer + "/accept")
                .header("Authorization", "Bearer " + sellerToken)).andExpect(status().isOk());

        MvcResult order = checkout(bobToken, listing, offer).andExpect(status().isOk()).andReturn();
        long orderId = dataId(order);

        // Buyer cancels while still PENDING_PAYMENT → listing frees up.
        mockMvc.perform(post("/api/orders/" + orderId + "/cancel")
                .header("Authorization", "Bearer " + bobToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"changed my mind\"}"))
                .andExpect(status().isOk());
        assertThat(listingRepository.findById(listing).orElseThrow().getStatus()).isEqualTo(ListingStatus.ACTIVE);

        // The consumed offer cannot be reused to re-order.
        checkout(bobToken, listing, offer).andExpect(status().is4xxClientError());
    }

    // ---------- C3 (serial form): can't buy-now a reserved listing ----------
    @Test
    void second_buyer_cannot_buy_a_reserved_listing() throws Exception {
        long listing = postListing(sellerToken, "50.00");
        checkout(bobToken, listing, null).andExpect(status().isOk()); // Bob reserves it

        // Cara's buy-now must be refused while it's reserved for Bob.
        checkout(caraToken, listing, null).andExpect(status().is4xxClientError());
    }

    // ---------- I1: seller can't re-activate a reserved listing ----------
    @Test
    void seller_cannot_reactivate_a_reserved_listing_via_update() throws Exception {
        long listing = postListing(sellerToken, "50.00");
        checkout(bobToken, listing, null).andExpect(status().isOk()); // now RESERVED

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/listings/" + listing)
                        .header("Authorization", "Bearer " + sellerToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ACTIVE\"}"))
                .andExpect(status().is4xxClientError());
        assertThat(listingRepository.findById(listing).orElseThrow().getStatus()).isEqualTo(ListingStatus.RESERVED);
    }

    // ---------- I5: ownership denial is 403, not 400 ----------
    @Test
    void paying_someone_elses_order_returns_403() throws Exception {
        long listing = postListing(sellerToken, "50.00");
        MvcResult order = checkout(bobToken, listing, null).andExpect(status().isOk()).andReturn();
        long orderId = dataId(order);

        // Cara (not the buyer) tries to pay Bob's order.
        mockMvc.perform(post("/api/orders/" + orderId + "/pay")
                .header("Authorization", "Bearer " + caraToken)).andExpect(status().isForbidden());
    }

    // ---------- BE-C1: seeder must not load in the default profile ----------
    @Test
    void data_initializer_is_restricted_to_local_profiles() {
        Profile profile = DataInitializer.class.getAnnotation(Profile.class);
        assertThat(profile).as("DataInitializer must be @Profile-guarded").isNotNull();
        assertThat(profile.value()).doesNotContain("default").contains("demo");
    }

    // ============================ helpers ============================

    private String signup(String who) {
        String email = who + "-" + UUID.randomUUID() + "@sm.test";
        authService.signup(new AuthDtos.SignupRequest(email, who, "Password1!", "Auckland"));
        return authService.login(new AuthDtos.LoginRequest(email, "Password1!")).token();
    }

    private long postListing(String token, String price) throws Exception {
        String json = """
                {"title":"Item %s","description":"desc","price":%s,"categoryId":%d,
                 "condition":"GOOD","location":"Auckland","negotiable":true,"shippingFee":0.00,
                 "imageUrls":["https://example.com/x.jpg"]}
                """.formatted(UUID.randomUUID(), price, categoryId);
        MvcResult r = mockMvc.perform(post("/api/listings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk()).andReturn();
        return dataId(r);
    }

    private long makeOffer(String token, long listingId, String amount) throws Exception {
        String json = "{\"listingId\":%d,\"amount\":%s,\"message\":\"hi\"}".formatted(listingId, amount);
        MvcResult r = mockMvc.perform(post("/api/offers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk()).andReturn();
        return dataId(r);
    }

    private org.springframework.test.web.servlet.ResultActions checkout(String token, long listingId, Long offerId) throws Exception {
        String offerPart = offerId == null ? "" : ",\"acceptedOfferId\":" + offerId;
        String json = """
                {"listingId":%d%s,"shippingName":"N","shippingPhone":"+64 1","shippingAddress":"A"}
                """.formatted(listingId, offerPart);
        return mockMvc.perform(post("/api/orders")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private void transition(String token, long orderId, String action) throws Exception {
        mockMvc.perform(post("/api/orders/" + orderId + "/" + action)
                .header("Authorization", "Bearer " + token)).andExpect(status().isOk());
    }

    private void ship(String token, long orderId) throws Exception {
        mockMvc.perform(post("/api/orders/" + orderId + "/ship")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"carrier\":\"NZ Post\",\"trackingNumber\":\"NZ1\"}")).andExpect(status().isOk());
    }

    private long dataId(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("data").get("id").asLong();
    }
}
