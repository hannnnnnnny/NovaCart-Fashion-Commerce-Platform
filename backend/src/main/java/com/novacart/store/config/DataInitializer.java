package com.novacart.store.config;

import com.novacart.store.entity.Category;
import com.novacart.store.entity.Conversation;
import com.novacart.store.entity.Favorite;
import com.novacart.store.entity.Listing;
import com.novacart.store.entity.ListingCondition;
import com.novacart.store.entity.ListingStatus;
import com.novacart.store.entity.Message;
import com.novacart.store.entity.Offer;
import com.novacart.store.entity.OfferStatus;
import com.novacart.store.entity.OrderStatus;
import com.novacart.store.entity.Review;
import com.novacart.store.entity.ReviewRole;
import com.novacart.store.entity.TradeOrder;
import com.novacart.store.entity.User;
import com.novacart.store.entity.UserRole;
import com.novacart.store.repository.CategoryRepository;
import com.novacart.store.repository.ConversationRepository;
import com.novacart.store.repository.FavoriteRepository;
import com.novacart.store.repository.ListingRepository;
import com.novacart.store.repository.MessageRepository;
import com.novacart.store.repository.OfferRepository;
import com.novacart.store.repository.ReviewRepository;
import com.novacart.store.repository.TradeOrderRepository;
import com.novacart.store.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds a coherent second-hand marketplace so every feature page has
 * something to show in a demo: multiple users with reputations, listings
 * across categories and statuses, offer threads in every state, buyer↔
 * seller conversations with unread counts, an order in every lifecycle
 * stage, and two-way reviews that roll up into user ratings.
 *
 * <p>The data is centered on {@code ava@renova.local} (the account demos
 * usually sign in as) so that after login she has populated "buying",
 * "selling", "offers received/sent", "messages", and a reviewed profile.
 *
 * <p>Restricted to the {@code demo}, {@code dev} and {@code test} profiles.
 * It must NOT run in the default/production profile: the seeded accounts use
 * passwords committed to the public repo (e.g. {@code admin@renova.local}),
 * so seeding them into a real deployment would hand anyone admin access.
 */
@Component
@Profile({"demo", "dev", "test"})
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ListingRepository listingRepository;
    private final OfferRepository offerRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final TradeOrderRepository orderRepository;
    private final ReviewRepository reviewRepository;
    private final FavoriteRepository favoriteRepository;
    private final PasswordEncoder passwordEncoder;

    private int orderSeq = 1000;

    public DataInitializer(
            UserRepository userRepository,
            CategoryRepository categoryRepository,
            ListingRepository listingRepository,
            OfferRepository offerRepository,
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            TradeOrderRepository orderRepository,
            ReviewRepository reviewRepository,
            FavoriteRepository favoriteRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.listingRepository = listingRepository;
        this.offerRepository = offerRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.orderRepository = orderRepository;
        this.reviewRepository = reviewRepository;
        this.favoriteRepository = favoriteRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Seeds after the context is fully built, in a single transaction. Spring
     * invokes ApplicationRunner beans through the proxy, so @Transactional here
     * actually applies (unlike on a @PostConstruct method, where init callbacks
     * run before the transactional proxy wraps the bean). One transaction means
     * all seed writes share a persistence context — consistent @Version tracking
     * — and a mid-seed failure rolls the whole graph back instead of leaving a
     * permanently partial one.
     */
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedCategories();
        seedUsers();
        if (listingRepository.count() == 0) {
            seedListingsAndTransactions();
        }
    }

    private void seedCategories() {
        if (categoryRepository.count() > 0) return;
        categoryRepository.saveAll(List.of(
                new Category("Electronics", "electronics", "💻", 1),
                new Category("Fashion", "fashion", "👕", 2),
                new Category("Home & Living", "home", "🏠", 3),
                new Category("Books & Media", "books", "📚", 4),
                new Category("Sports & Outdoors", "sports", "⚽", 5),
                new Category("Toys & Games", "toys", "🎮", 6),
                new Category("Beauty", "beauty", "💄", 7),
                new Category("Collectibles", "collectibles", "🎯", 8),
                new Category("Other", "other", "📦", 99)
        ));
    }

    private void seedUsers() {
        ensureUser("admin@renova.local", "ReNova Admin", "DemoAdmin123!", UserRole.ADMIN,
                "Headquarters", "Keeping the marketplace tidy and safe.");
        ensureUser("ava@renova.local", "Ava Chen", "DemoPassword1!", UserRole.USER,
                "San Francisco, CA", "Downsizing my apartment — gadgets, bikes, and a few good reads. Everything tested before it ships.");
        ensureUser("liam@renova.local", "Liam Park", "DemoPassword1!", UserRole.USER,
                "Brooklyn, NY", "Streetwear and Lego. I photograph everything in daylight, no surprises.");
        ensureUser("nora@renova.local", "Nora Kapoor", "DemoPassword1!", UserRole.USER,
                "Austin, TX", "Home goods and audio. Pet-free, smoke-free home.");
        ensureUser("sam@renova.local", "Sam Reyes", "DemoPassword1!", UserRole.USER,
                "Seattle, WA", "Books, vinyl, and the occasional pair of jeans I can't fit anymore.");
        ensureUser("maya@renova.local", "Maya Flores", "DemoPassword1!", UserRole.USER,
                "Los Angeles, CA", "Vintage finds and camera gear. Fast shipper.");
        ensureUser("theo@renova.local", "Theo Novak", "DemoPassword1!", UserRole.USER,
                "Chicago, IL", "Music gear and electronics. Always open to a fair offer.");
        ensureUser("ivy@renova.local", "Ivy Bennett", "DemoPassword1!", UserRole.USER,
                "Portland, OR", "Plants, ceramics, and cosy knitwear. Local pickup welcome.");
    }

    private User ensureUser(String email, String displayName, String password, UserRole role,
                            String location, String bio) {
        return userRepository.findByEmailIgnoreCase(email).orElseGet(() -> {
            User user = new User();
            user.setEmail(email);
            user.setDisplayName(displayName);
            user.setPasswordHash(passwordEncoder.encode(password));
            user.setLocation(location);
            user.setRole(role);
            user.setBio(bio);
            user.setCreatedAt(Instant.now().minus(daysToHours(280), ChronoUnit.HOURS));
            user.setLastLoginAt(Instant.now().minus(2, ChronoUnit.HOURS));
            return userRepository.save(user);
        });
    }

    // ------------------------------------------------------------------
    // Listings + the whole transaction graph
    // ------------------------------------------------------------------
    private void seedListingsAndTransactions() {
        User ava = user("ava@renova.local");
        User liam = user("liam@renova.local");
        User nora = user("nora@renova.local");
        User sam = user("sam@renova.local");
        User maya = user("maya@renova.local");
        User theo = user("theo@renova.local");
        User ivy = user("ivy@renova.local");

        Category electronics = cat("electronics");
        Category fashion = cat("fashion");
        Category home = cat("home");
        Category books = cat("books");
        Category sports = cat("sports");
        Category collectibles = cat("collectibles");
        Category beauty = cat("beauty");

        // ---- Ava's listings (the demo account: she both buys and sells) ----
        Listing avaIpad = listing(ava, electronics,
                "Apple iPad Air 5th gen, 64GB, Space Gray",
                "Used for note-taking and reading for about 8 months. No scratches on screen, light wear on edges. Original box, charger, and Apple Pencil 2 included.",
                price(380), price(599), ListingCondition.LIKE_NEW, "San Francisco, CA",
                true, price(8), 312, 27,
                img("1561154464-82e9adf32764"), img("1544244015-0df4b3ffc6b0"));

        Listing avaBike = listing(ava, sports,
                "Specialized Sirrus 2.0 hybrid bike, size S",
                "Two years old, garage kept. Recent tune-up at REI, new chain installed last month. Some paint chips on the chainstay.",
                price(320), price(650), ListingCondition.GOOD, "San Francisco, CA",
                true, price(0), 188, 19,
                img("1532298229144-0ec0c57515c7"), img("1485965120184-e220f721d03e"));

        Listing avaBlazer = listing(ava, fashion,
                "Theory wool blazer, charcoal, size 6",
                "Tailored once for a better fit at the shoulders. Dry-cleaned, kept in a garment bag. A workwear staple.",
                price(72), price(225), ListingCondition.GOOD, "San Francisco, CA",
                true, price(7), 96, 8,
                img("1591047139829-d91aecb6caea"));

        Listing avaKindle = listing(ava, electronics,
                "Kindle Paperwhite 11th gen, 8GB",
                "Warm-light display, perfect for late reading. Light scuff on the back, screen is flawless. Includes a fabric cover.",
                price(70), price(160), ListingCondition.GOOD, "San Francisco, CA",
                true, price(5), 134, 11,
                img("1633577418380-1f3c54fd9b62"));

        // ---- Other sellers' listings ----
        Listing liamJacket = listing(liam, fashion,
                "Carhartt WIP Detroit Jacket, Size M",
                "Bought from the Soho store, worn maybe ten times. Aged hamilton brown duck canvas, blanket lining still warm and clean.",
                price(95), price(178), ListingCondition.GOOD, "Brooklyn, NY",
                true, price(12), 221, 16,
                img("1551028719-00167b16eac5"), img("1520975954732-35dd22299614"));

        Listing liamLego = listing(liam, collectibles,
                "Lego Star Wars Razor Crest 75292, 100% complete",
                "Built once, then displayed in a glass case. All minifigs and the original instructions are included. Box has a small crease on the corner.",
                price(210), price(299), ListingCondition.LIKE_NEW, "Brooklyn, NY",
                true, price(15), 274, 31,
                img("1518155317743-a8ff43ea6a5f"));

        Listing liamSneakers = listing(liam, fashion,
                "New Balance 990v5, US 10, grey",
                "Made in USA. Maybe 30 miles on them, soles barely worn. No box.",
                price(85), price(185), ListingCondition.GOOD, "Brooklyn, NY",
                true, price(10), 142, 9,
                img("1539185441755-769473a23570"));

        Listing noraChair = listing(nora, home,
                "IKEA STRANDMON wing chair, dark blue",
                "Comfortable reading chair, in great shape. Pet-free, smoke-free home. Local pickup in Austin preferred but I can arrange shipping at cost.",
                price(180), price(349), ListingCondition.GOOD, "Austin, TX",
                true, price(0), 167, 14,
                img("1555041469-a586c61ea9bc"), img("1493663284031-b7e3aefcae8e"));

        Listing noraHeadphones = listing(nora, electronics,
                "Bose QuietComfort 45 over-ear headphones",
                "Battery still holds full charge. Pads are clean, case included. Selling because I upgraded to AirPods Max.",
                price(125), price(279), ListingCondition.GOOD, "Austin, TX",
                true, price(8), 203, 22,
                img("1505740420928-5e560c06d30e"));

        Listing noraLamp = listing(nora, home,
                "Mid-century brass floor lamp",
                "Warm dimmable light, rewired last year. A small dent on the base, barely visible. Bulb included.",
                price(60), price(140), ListingCondition.GOOD, "Austin, TX",
                true, price(0), 58, 6,
                img("1507473885765-e6ed057f782c"));

        Listing samBook = listing(sam, books,
                "Designing Data-Intensive Applications, hardcover",
                "Read it twice during my first staff-engineer year. Has occasional pencil underlines (erasable). Cover in great shape.",
                price(18), price(55), ListingCondition.GOOD, "Seattle, WA",
                false, price(6), 91, 5,
                img("1532012197267-da84d127e765"));

        Listing samJeans = listing(sam, fashion,
                "Vintage Levi's 501 jeans, W32 L32",
                "Honestly, the best fade I've ever worn in. Some honeycomb behind the knees, no holes, no repairs. Vintage USA red tab.",
                price(65), null, ListingCondition.GOOD, "Seattle, WA",
                true, price(8), 119, 12,
                img("1542272604-787c3835535d"));

        Listing samVinyl = listing(sam, books,
                "Miles Davis — Kind of Blue, original press LP",
                "Columbia 6-eye label. A few light surface marks, plays clean. Sleeve has ring wear.",
                price(45), price(90), ListingCondition.FAIR, "Seattle, WA",
                true, price(6), 64, 7,
                img("1603048588665-791ca8aea617"));

        Listing mayaCamera = listing(maya, electronics,
                "Fujifilm X-T20 with 18-55mm kit lens",
                "My first mirrorless. Shutter count under 6k. Includes two batteries and a charger. Light grip wear.",
                price(420), price(900), ListingCondition.GOOD, "Los Angeles, CA",
                true, price(12), 256, 24,
                img("1516035069371-29a1b244cc32"));

        Listing ivyKnit = listing(ivy, fashion,
                "Hand-knit lambswool sweater, cream, M",
                "Made it myself, wore it one winter. Too warm for Portland now. Zero pilling.",
                price(40), null, ListingCondition.LIKE_NEW, "Portland, OR",
                true, price(7), 47, 5,
                img("1576566588028-4147f3842f27"));

        Listing ivyCeramics = listing(ivy, home,
                "Set of 4 stoneware dinner plates, speckled",
                "Wheel-thrown, food and dishwasher safe. One has a tiny glaze pop on the rim, otherwise perfect.",
                price(38), price(80), ListingCondition.GOOD, "Portland, OR",
                true, price(0), 33, 3,
                img("1578749556568-bc2c40e68b61"));

        Listing mayaSerum = listing(maya, beauty,
                "Sealed skincare bundle (gift, never opened)",
                "Won this in a raffle, not my routine. Two serums and a moisturiser, all sealed and in date.",
                price(35), price(95), ListingCondition.NEW, "Los Angeles, CA",
                true, price(5), 41, 4,
                img("1556228720-195a672e8a03"));

        // ======================= Transaction graph =======================

        // -- 1. COMPLETED, both sides reviewed: Nora bought Sam's book --
        TradeOrder o1 = order(samBook, nora, price(18), price(6),
                OrderStatus.COMPLETED, 26, true,
                "Nora Kapoor", "+1 512 555 0148", "1100 Congress Ave, Austin, TX 78701", null,
                "USPS Media Mail", "9400111699000000000001");
        markSold(samBook, o1.getCompletedAt());
        review(o1, nora, sam, 5, "Exactly as described, shipped next day. Underlines are faint — totally fine. Would buy again.", ReviewRole.BUYER_REVIEWS_SELLER, 24);
        review(o1, sam, nora, 5, "Smooth payment, friendly buyer. Recommended!", ReviewRole.SELLER_REVIEWS_BUYER, 24);

        // -- 2. COMPLETED, only buyer reviewed: Ava bought Liam's Lego --
        TradeOrder o2 = order(liamLego, ava, price(195), price(15),
                OrderStatus.COMPLETED, 19, true,
                "Ava Chen", "+1 415 555 0193", "2030 Union St, San Francisco, CA 94123", "Please double-box if you can!",
                "UPS Ground", "1Z999AA10123456784");
        markSold(liamLego, o2.getCompletedAt());
        review(o2, ava, liam, 5, "Packed like a museum piece, every minifig present. Liam's a pro seller.", ReviewRole.BUYER_REVIEWS_SELLER, 17);

        // -- 3. COMPLETED: Maya bought Ava's blazer → Ava gets reviewed as a seller --
        TradeOrder o3 = order(avaBlazer, maya, price(72), price(7),
                OrderStatus.COMPLETED, 14, true,
                "Maya Flores", "+1 310 555 0172", "845 Sunset Blvd, Los Angeles, CA 90012", null,
                "USPS Priority", "9405511699000000000002");
        markSold(avaBlazer, o3.getCompletedAt());
        review(o3, maya, ava, 5, "Beautiful blazer, even nicer in person. Ava answered all my questions and shipped fast.", ReviewRole.BUYER_REVIEWS_SELLER, 12);
        review(o3, ava, maya, 5, "Lovely buyer, quick to pay. Thank you Maya!", ReviewRole.SELLER_REVIEWS_BUYER, 12);

        // give the other sellers a couple more reviews for believable ratings
        seedStandaloneReputation(liam, ivy, liamSneakers, 5,
                "Sneakers were even cleaner than the photos. Quick shipping.", 40);
        seedStandaloneReputation(nora, theo, noraLamp, 4,
                "Nice lamp, slight delay shipping but kept me posted.", 33);
        seedStandaloneReputation(maya, sam, mayaSerum, 5,
                "Sealed and legit, fast handoff.", 28);
        seedStandaloneReputation(ava, ivy, avaKindle, 5,
                "Kindle works perfectly, generous of Ava to include the cover.", 9);

        // -- 4. SHIPPED (in transit): Theo bought Ava's iPad via an accepted offer --
        offer(avaIpad, theo, price(340),
                "Cash-on-pickup if you're near SoMa, otherwise $340 shipped?", OfferStatus.ACCEPTED, false, null, 7, 6);
        order(avaIpad, theo, price(340), price(8),
                OrderStatus.SHIPPED, 5, true,
                "Theo Novak", "+1 312 555 0119", "55 E Monroe St, Chicago, IL 60603", "No rush, thanks!",
                "FedEx Home", "613290120000123");
        reserve(avaIpad); // sold-pending: reserved until Theo confirms receipt

        // -- 5. PAID (awaiting shipment): Ivy bought Ava's bike --
        order(avaBike, ivy, price(320), price(0),
                OrderStatus.PAID, 2, true,
                "Ivy Bennett", "+1 503 555 0188", "1220 SE Division St, Portland, OR 97202", "Local pickup works too if easier.",
                null, null);
        reserve(avaBike);

        // -- 6. PENDING_PAYMENT: Ava (as buyer) is buying Nora's chair via accepted offer --
        offer(noraChair, ava, price(160),
                "Would $160 work? I can pick up in Austin next week.", OfferStatus.ACCEPTED, false, null, 3, 2);
        order(noraChair, ava, price(160), price(0),
                OrderStatus.PENDING_PAYMENT, 1, false,
                "Ava Chen", "+1 415 555 0193", "2030 Union St, San Francisco, CA 94123", null,
                null, null);
        reserve(noraChair);

        // -- 7. CANCELLED: Liam ordered Maya's camera, then cancelled --
        TradeOrder o7 = order(mayaCamera, liam, price(420), price(12),
                OrderStatus.CANCELLED, 10, false,
                "Liam Park", "+1 718 555 0156", "330 Berry St, Brooklyn, NY 11249", null,
                null, null);
        o7.setCancelledAt(Instant.now().minus(daysToHours(9), ChronoUnit.HOURS));
        o7.setCancelReason("Buyer found a local deal. Listing back up for sale.");
        orderRepository.save(o7);

        // ----- Offer threads in every state (for the offers pages) -----

        // PENDING offer Ava (seller) has yet to answer — shows in her "received"
        offer(avaBike, maya, price(280),
                "Love this bike! Any flex on $280? Can pick up this weekend.", OfferStatus.PENDING, false, null, 1, null);

        // COUNTERED thread: Ivy offered on Liam's jacket, Liam countered
        Offer ivyJacketOffer = offer(liamJacket, ivy, price(80),
                "Would you take $80 shipped to Portland?", OfferStatus.COUNTERED, false, null, 4, 3);
        offer(liamJacket, ivy, price(88),
                "Can't do 80, but I'll meet you at $88 shipped.", OfferStatus.PENDING, true, ivyJacketOffer, 3, null);

        // REJECTED: Theo lowballed Sam's jeans
        offer(samJeans, theo, price(40),
                "$40?", OfferStatus.REJECTED, false, null, 6, 5);

        // WITHDRAWN: Maya offered on Nora's headphones then pulled it
        offer(noraHeadphones, maya, price(95),
                "Open to $95? No worries if not.", OfferStatus.WITHDRAWN, false, null, 8, 7);

        // PENDING offer Ava sent on someone else's listing — shows in her "sent"
        offer(samVinyl, ava, price(38),
                "Would $38 work for the Miles Davis press? Big fan.", OfferStatus.PENDING, false, null, 1, null);

        // --------------------- Conversations + messages ---------------------

        // C1: Theo ↔ Ava about the iPad (Ava has 1 unread as seller)
        Conversation c1 = conversation(avaIpad, theo, ava, 8);
        message(c1, theo, "Hi Ava! Is the Apple Pencil really included?", false, 8, 0);
        message(c1, ava, "Hey Theo — yes, 2nd-gen Pencil and the original box. Both in great shape.", false, 8, 30);
        message(c1, theo, "Perfect. Sent an offer at $340, hope that's fair!", false, 7, 0);
        message(c1, ava, "Accepted! I'll get it boxed up today.", false, 7, 20);
        message(c1, theo, "Just got the shipping notice, thank you! Tracking says Thursday.", true, 5, 0); // unread for Ava

        // C2: Maya ↔ Ava about the bike (Ava has 1 unread as seller)
        Conversation c2 = conversation(avaBike, maya, ava, 1);
        message(c2, maya, "Is the bike still available? And is the frame really a size S?", false, 1, 0);
        message(c2, ava, "Still here! Yes, size S — I'm 5'4\" and it fits me well.", false, 1, 40);
        message(c2, maya, "Great, I put in an offer at $280. Let me know!", true, 1, 90); // unread for Ava

        // C3: Ava (buyer) ↔ Nora about the chair (Ava has 1 unread as buyer)
        Conversation c3 = conversation(noraChair, ava, nora, 3);
        message(c3, ava, "Hi Nora, would you hold the chair if I pick up next week?", false, 3, 0);
        message(c3, nora, "Sure thing — I accepted your $160 offer. Just check out when you're ready.", true, 2, 0); // unread for Ava (buyer)

        // C4: Ivy ↔ Liam about the jacket (counter-offer context)
        Conversation c4 = conversation(liamJacket, ivy, liam, 4);
        message(c4, ivy, "Could you do $80 shipped on the Detroit jacket?", false, 4, 0);
        message(c4, liam, "Appreciate the offer! I countered at $88 — it's barely worn.", false, 3, 0);

        // C5: Nora ↔ Sam about the (now sold) book
        Conversation c5 = conversation(samBook, nora, sam, 24);
        message(c5, nora, "Thanks for the quick ship, the book arrived today!", false, 24, 0);
        message(c5, sam, "Glad it made it safely. Enjoy!", false, 24, 30);

        // ----------------------------- Favorites -----------------------------
        favorite(ava, liamJacket);
        favorite(ava, samVinyl);
        favorite(ava, mayaCamera);
        favorite(ava, ivyKnit);
        favorite(theo, avaBike);
        favorite(maya, avaIpad);
        favorite(nora, ivyCeramics);
        favorite(liam, samJeans);

        // bump a few favorite counts so the listing badges match the seeded favorites
        bumpFavoriteCount(liamJacket, samVinyl, mayaCamera, ivyKnit, avaBike, avaIpad);
    }

    /**
     * Creates a tiny completed order purely to attach one extra review, so a
     * seller's rating looks lived-in. The order is left COMPLETED and its
     * listing marked SOLD.
     */
    private void seedStandaloneReputation(User seller, User buyer, Listing listing,
                                          int rating, String comment, int daysAgo) {
        TradeOrder o = order(listing, buyer, listing.getPrice(),
                listing.getShippingFee() == null ? BigDecimal.ZERO : listing.getShippingFee(),
                OrderStatus.COMPLETED, daysAgo, true,
                buyer.getDisplayName(), "+1 555 555 0100", "Demo address", null,
                "Carrier", "DEMOTRACK" + orderSeq);
        markSold(listing, o.getCompletedAt());
        review(o, buyer, seller, rating, comment, ReviewRole.BUYER_REVIEWS_SELLER, daysAgo - 1);
    }

    // ------------------------------------------------------------------
    // Builders
    // ------------------------------------------------------------------

    private Listing listing(User seller, Category category, String title, String description,
                            BigDecimal price, BigDecimal originalPrice, ListingCondition condition,
                            String location, boolean negotiable, BigDecimal shippingFee,
                            int views, int favs, String... images) {
        Listing l = new Listing();
        l.setSeller(seller);
        l.setCategory(category);
        l.setTitle(title);
        l.setDescription(description);
        l.setPrice(price);
        l.setOriginalPrice(originalPrice);
        l.setCondition(condition);
        l.setLocation(location);
        l.setNegotiable(negotiable);
        l.setShippingFee(shippingFee);
        l.setImageUrls(new ArrayList<>(List.of(images)));
        l.setStatus(ListingStatus.ACTIVE);
        l.setViewCount(views);
        l.setFavoriteCount(favs);
        Instant created = Instant.now().minus(daysToHours(12), ChronoUnit.HOURS);
        l.setCreatedAt(created);
        l.setUpdatedAt(created);
        return listingRepository.save(l);
    }

    private Offer offer(Listing listing, User buyer, BigDecimal amount, String message,
                        OfferStatus status, boolean fromSeller, Offer parent,
                        int createdDaysAgo, Integer respondedDaysAgo) {
        Offer o = new Offer();
        o.setListing(listing);
        o.setBuyer(buyer);
        o.setAmount(amount);
        o.setMessage(message);
        o.setStatus(status);
        o.setFromSeller(fromSeller);
        o.setParentOffer(parent);
        o.setCreatedAt(Instant.now().minus(daysToHours(createdDaysAgo), ChronoUnit.HOURS));
        if (respondedDaysAgo != null) {
            o.setRespondedAt(Instant.now().minus(daysToHours(respondedDaysAgo), ChronoUnit.HOURS));
        }
        return offerRepository.save(o);
    }

    private TradeOrder order(Listing listing, User buyer, BigDecimal agreedPrice, BigDecimal shippingFee,
                             OrderStatus status, int createdDaysAgo, boolean carrierKnown,
                             String shipName, String shipPhone, String shipAddress, String buyerNote,
                             String carrier, String tracking) {
        TradeOrder o = new TradeOrder();
        o.setOrderNumber("RN" + (orderSeq++) + String.format("%04d", (int) (Math.random() * 10000)));
        o.setListing(listing);
        o.setBuyer(buyer);
        o.setSeller(listing.getSeller());
        o.setAgreedPrice(agreedPrice);
        o.setShippingFee(shippingFee);
        o.setTotalAmount(agreedPrice.add(shippingFee));
        o.setShippingName(shipName);
        o.setShippingPhone(shipPhone);
        o.setShippingAddress(shipAddress);
        o.setBuyerNote(buyerNote);
        o.setStatus(status);

        Instant created = Instant.now().minus(daysToHours(createdDaysAgo), ChronoUnit.HOURS);
        o.setCreatedAt(created);

        // Fill the lifecycle timestamps consistent with the status.
        boolean paid = status == OrderStatus.PAID || status == OrderStatus.SHIPPED
                || status == OrderStatus.DELIVERED || status == OrderStatus.COMPLETED;
        boolean shipped = status == OrderStatus.SHIPPED || status == OrderStatus.DELIVERED
                || status == OrderStatus.COMPLETED;
        boolean done = status == OrderStatus.COMPLETED;
        if (paid) o.setPaidAt(created.plus(2, ChronoUnit.HOURS));
        if (shipped) {
            o.setShippedAt(created.plus(daysToHours(1), ChronoUnit.HOURS));
            if (carrierKnown) { o.setCarrier(carrier); o.setTrackingNumber(tracking); }
        }
        if (done) {
            o.setDeliveredAt(created.plus(daysToHours(3), ChronoUnit.HOURS));
            o.setCompletedAt(created.plus(daysToHours(3), ChronoUnit.HOURS).plus(6, ChronoUnit.HOURS));
        }
        return orderRepository.save(o);
    }

    private void review(TradeOrder order, User reviewer, User reviewee, int rating,
                        String comment, ReviewRole role, int daysAgo) {
        Review r = new Review();
        r.setOrder(order);
        r.setReviewer(reviewer);
        r.setReviewee(reviewee);
        r.setRating(rating);
        r.setComment(comment);
        r.setRole(role);
        r.setCreatedAt(Instant.now().minus(daysToHours(daysAgo), ChronoUnit.HOURS));
        reviewRepository.save(r);
        // roll up into the reviewee's rating, same as ReviewService.create
        reviewee.setRatingSum(reviewee.getRatingSum() + rating);
        reviewee.setRatingCount(reviewee.getRatingCount() + 1);
        userRepository.save(reviewee);
    }

    private Conversation conversation(Listing listing, User buyer, User seller, int createdDaysAgo) {
        Conversation c = new Conversation();
        c.setListing(listing);
        c.setBuyer(buyer);
        c.setSeller(seller);
        c.setCreatedAt(Instant.now().minus(daysToHours(createdDaysAgo), ChronoUnit.HOURS));
        c.setLastMessageAt(c.getCreatedAt());
        return conversationRepository.save(c);
    }

    private void message(Conversation c, User sender, String body, boolean unreadForRecipient,
                         int daysAgo, int plusMinutes) {
        Message m = new Message();
        m.setConversation(c);
        m.setSender(sender);
        m.setBody(body);
        Instant when = Instant.now().minus(daysToHours(daysAgo), ChronoUnit.HOURS).plus(plusMinutes, ChronoUnit.MINUTES);
        m.setCreatedAt(when);
        if (!unreadForRecipient) {
            m.setReadAt(when.plus(20, ChronoUnit.MINUTES));
        }
        messageRepository.save(m);

        // keep the conversation's denormalized preview / unread counts in sync
        c.setLastMessagePreview(body.length() > 280 ? body.substring(0, 277) + "..." : body);
        c.setLastMessageAt(when);
        if (unreadForRecipient) {
            boolean senderIsBuyer = sender.getId().equals(c.getBuyer().getId());
            if (senderIsBuyer) {
                c.setSellerUnreadCount(c.getSellerUnreadCount() + 1);
            } else {
                c.setBuyerUnreadCount(c.getBuyerUnreadCount() + 1);
            }
        }
        conversationRepository.save(c);
    }

    private void favorite(User user, Listing listing) {
        favoriteRepository.save(new Favorite(user, listing));
    }

    private void markSold(Listing listing, Instant when) {
        Instant ts = when == null ? Instant.now() : when;
        listing.setStatus(ListingStatus.SOLD);
        listing.setSoldAt(ts);
        listing.setUpdatedAt(ts);
        listingRepository.save(listing);
    }

    private void reserve(Listing listing) {
        listing.setStatus(ListingStatus.RESERVED);
        listing.setUpdatedAt(Instant.now());
        listingRepository.save(listing);
    }

    private void bumpFavoriteCount(Listing... listings) {
        for (Listing l : listings) {
            l.setFavoriteCount(l.getFavoriteCount() + 1);
            listingRepository.save(l);
        }
    }

    // ------------------------------------------------------------------
    // Small helpers
    // ------------------------------------------------------------------

    private User user(String email) {
        return userRepository.findByEmailIgnoreCase(email).orElseThrow();
    }

    private Category cat(String slug) {
        return categoryRepository.findBySlug(slug).orElseThrow();
    }

    private static BigDecimal price(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP);
    }

    private static long daysToHours(int days) {
        return (long) days * 24;
    }

    private static String img(String unsplashId) {
        return "https://images.unsplash.com/photo-" + unsplashId + "?w=900";
    }
}
