package com.foody.seed.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.foody.api.catalog.CatalogConstants;
import com.foody.api.catalog.dto.CategoryProductCount;
import com.foody.api.catalog.entity.Category;
import com.foody.api.catalog.entity.Product;
import com.foody.api.catalog.repository.CategoryRepository;
import com.foody.api.catalog.repository.ProductRepository;
import com.foody.seed.SeedProperties;
import com.foody.seed.SeedRunner;
import com.foody.seed.SeedScale;
import com.foody.testsupport.api.ApiIntegrationTest;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.datafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.support.TransactionOperations;

@ApiIntegrationTest
class CatalogSeederTest {

	private static final long RANDOM_SEED = 42;

	private static final List<String> CATEGORY_NAMES = List.of("Lanches", "Pizzas", "Pratos", "Saladas", "Porções",
			"Bebidas", "Sobremesas", "Combos");

	private static final Map<String, BigDecimal> PROTOTYPE_PRICES = Map.of("Smash duplo", new BigDecimal("34.90"),
			"Frango crispy", new BigDecimal("29.90"), "Veggie de grão-de-bico", new BigDecimal("27.90"), "Margherita",
			new BigDecimal("49.90"), "Calabresa", new BigDecimal("52.90"), "Limonada da casa", new BigDecimal("12.00"),
			"Refrigerante lata", new BigDecimal("7.50"), "Brownie com sorvete", new BigDecimal("18.90"));

	private static final BigDecimal HIGHEST_PRICE = new BigDecimal(CatalogConstants.PRICE_MAX);

	@Autowired
	private CategoryRepository categoryRepository;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private CategoryFactory categories;

	@Autowired
	private ProductFactory products;

	@Autowired
	private Faker faker;

	@Autowired
	private TransactionOperations transactions;

	private CatalogSeeder seeder;

	@BeforeEach
	void startFromAnEmptyCatalog() {
		productRepository.deleteAllInBatch();
		categoryRepository.deleteAllInBatch();
		seeder = new CatalogSeeder(categoryRepository, productRepository, categories, products, faker);
	}

	@Test
	void catalogSeederCreatesEightCategoriesAndScaledProducts() {
		SeedScale scale = SeedScale.SMALL;

		seeder.seed(scale);

		List<Category> seededCategories = categoryRepository.findAll(Sort.by(CatalogConstants.DISPLAY_ORDER_ATTRIBUTE));
		List<Product> seededProducts = productRepository.findAll();
		assertThat(seededCategories).extracting(Category::getName).containsExactlyElementsOf(CATEGORY_NAMES);
		assertThat(seededCategories).allMatch(Category::isActive);
		assertThat(seededProducts).hasSize(scale.of(CatalogSeedConstants.PRODUCT_BASE_COUNT));
		assertThat(productRepository.countPerCategory()).hasSize(CATEGORY_NAMES.size())
			.allMatch(count -> count.productCount() > 0)
			.extracting(CategoryProductCount::categoryId)
			.containsExactlyInAnyOrderElementsOf(seededCategories.stream().map(Category::getId).toList());
		assertThat(seededProducts).filteredOn(product -> !product.isActive()).hasSize(5);
		assertThat(seededProducts).extracting(Product::getPrice)
			.allSatisfy(price -> assertThat(price).isPositive().isLessThanOrEqualTo(HIGHEST_PRICE));
	}

	@Test
	void catalogSeederScalesTheProductsWithDistinctReadableNames() {
		SeedScale scale = SeedScale.MEDIUM;

		seeder.seed(scale);

		List<Product> seededProducts = productRepository.findAll();
		assertThat(seededProducts).hasSize(scale.of(CatalogSeedConstants.PRODUCT_BASE_COUNT));
		assertThat(seededProducts).extracting(Product::getName)
			.doesNotHaveDuplicates()
			.allMatch(name -> name.length() <= CatalogConstants.PRODUCT_NAME_MAX_LENGTH)
			.contains("Smash duplo", "Smash duplo especial", "X-salada premium");
		assertThat(seededProducts).filteredOn(product -> !product.isActive()).hasSize(50);
	}

	@Test
	void catalogSeederIsIdempotent() {
		SeedRunner runner = new SeedRunner(List.of(seeder), new SeedProperties(SeedScale.SMALL, RANDOM_SEED),
				transactions);
		boolean seededBefore = seeder.alreadySeeded();
		runner.run(new DefaultApplicationArguments());
		long categoriesAfterFirstRun = categoryRepository.count();
		long productsAfterFirstRun = productRepository.count();

		runner.run(new DefaultApplicationArguments());

		assertThat(seededBefore).isFalse();
		assertThat(seeder.alreadySeeded()).isTrue();
		assertThat(categoryRepository.count()).isEqualTo(categoriesAfterFirstRun);
		assertThat(productRepository.count()).isEqualTo(productsAfterFirstRun);
	}

	@Test
	void catalogSeederKeepsPrototypeProductsActive() {
		SeedScale scale = SeedScale.MEDIUM;

		seeder.seed(scale);

		Map<String, Product> prototypes = productRepository.findAll()
			.stream()
			.filter(product -> PROTOTYPE_PRICES.containsKey(product.getName()))
			.collect(Collectors.toMap(Product::getName, Function.identity()));
		assertThat(prototypes).containsOnlyKeys(PROTOTYPE_PRICES.keySet());
		assertThat(prototypes.values()).allMatch(Product::isActive);
		assertThat(prototypes.values()).allSatisfy(product -> assertThat(product.getPrice())
			.isEqualByComparingTo(PROTOTYPE_PRICES.get(product.getName())));
	}

}
