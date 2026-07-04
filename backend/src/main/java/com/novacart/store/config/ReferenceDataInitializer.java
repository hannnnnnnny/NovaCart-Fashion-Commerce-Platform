package com.novacart.store.config;

import com.novacart.store.entity.Category;
import com.novacart.store.repository.CategoryRepository;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds non-secret reference data (the category taxonomy) in EVERY profile,
 * including production. Unlike {@link DataInitializer} (demo accounts with
 * committed passwords, restricted to demo/dev/test), categories are required
 * for the app to function at all — a fresh production database needs them so
 * sellers can post listings.
 *
 * <p>Idempotent: does nothing if categories already exist. Ordered before the
 * demo seeder so demo listings can reference these categories.
 */
@Component
@Order(0)
public class ReferenceDataInitializer implements ApplicationRunner {

    private final CategoryRepository categoryRepository;

    public ReferenceDataInitializer(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
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
}
