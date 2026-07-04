package com.novacart.store.flow;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.novacart.store.dto.AuthDtos;
import com.novacart.store.repository.CategoryRepository;
import com.novacart.store.service.AuthService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

/**
 * Listing hardening: removed listings are gone to the public, search input is
 * neutralized against LIKE wildcards, and image URLs are validated.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ListingHardeningTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private AuthService authService;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ObjectMapper objectMapper;

    private String token;
    private long categoryId;

    @BeforeEach
    void setUp() {
        String email = "seller-" + UUID.randomUUID() + "@hard.test";
        authService.signup(new AuthDtos.SignupRequest(email, "Seller", "Password1!", "Auckland"));
        token = authService.login(new AuthDtos.LoginRequest(email, "Password1!")).token();
        categoryId = categoryRepository.findAll().getFirst().getId();
    }

    @Test
    void removed_listing_is_404_to_the_public() throws Exception {
        long id = postListing("Fancy lamp");
        // Public can see it while active.
        mockMvc.perform(get("/api/public/listings/" + id)).andExpect(status().isOk());

        mockMvc.perform(delete("/api/listings/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // After removal it's gone, not a broken RESERVED/SOLD-style page.
        mockMvc.perform(get("/api/public/listings/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void like_wildcards_in_keyword_are_neutralized() throws Exception {
        postListing("Blue ceramic mug");

        // If '%' leaked into the LIKE, "Blue%mug" would become %Blue%mug% and
        // match "Blue ceramic mug". Stripped, it becomes "Bluemug" -> no match.
        MvcResult wildcard = mockMvc.perform(get("/api/public/listings")
                        .param("keyword", "Blue%mug").param("size", "50"))
                .andExpect(status().isOk()).andReturn();
        int wildcardCount = objectMapper.readTree(wildcard.getResponse().getContentAsString())
                .get("data").get("totalElements").asInt();
        org.assertj.core.api.Assertions.assertThat(wildcardCount).isZero();

        // A genuine substring still matches.
        MvcResult real = mockMvc.perform(get("/api/public/listings")
                        .param("keyword", "ceramic").param("size", "50"))
                .andExpect(status().isOk()).andReturn();
        int realCount = objectMapper.readTree(real.getResponse().getContentAsString())
                .get("data").get("totalElements").asInt();
        org.assertj.core.api.Assertions.assertThat(realCount).isGreaterThanOrEqualTo(1);
    }

    @Test
    void rejects_non_url_image_source() throws Exception {
        String json = """
                {"title":"x","description":"d","price":10.00,"categoryId":%d,"condition":"GOOD",
                 "location":"A","negotiable":true,"shippingFee":0.00,
                 "imageUrls":["javascript:alert(1)"]}
                """.formatted(categoryId);
        mockMvc.perform(post("/api/listings").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    private long postListing(String title) throws Exception {
        String json = """
                {"title":"%s","description":"desc","price":25.00,"categoryId":%d,"condition":"GOOD",
                 "location":"Auckland","negotiable":true,"shippingFee":0.00,
                 "imageUrls":["https://example.com/x.jpg"]}
                """.formatted(title, categoryId);
        MvcResult r = mockMvc.perform(post("/api/listings").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(r.getResponse().getContentAsString()).get("data").get("id").asLong();
    }
}
