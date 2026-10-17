package com.foody.seed.catalog;

import com.foody.api.catalog.entity.Category;
import com.foody.api.catalog.entity.Product;
import com.foody.api.catalog.repository.ProductRepository;
import com.foody.api.shared.money.MoneyConstants;
import com.foody.seed.EntityFactory;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.function.Consumer;
import net.datafaker.Faker;
import org.jspecify.annotations.Nullable;

public class ProductFactory extends EntityFactory<Product> {

	private final CategoryFactory categories;

	private final Faker faker;

	public ProductFactory(ProductRepository repository, CategoryFactory categories, Faker faker) {
		super(repository);
		this.categories = categories;
		this.faker = faker;
	}

	@Override
	public Product build() {
		return build(categories.create());
	}

	public Product build(Category category) {
		return build(category, fakeName(), fakePrice());
	}

	public Product build(Category category, String name, BigDecimal price) {
		return build(category, name, fakeDescription(), price);
	}

	public Product build(Category category, String name, @Nullable String description, BigDecimal price) {
		return new Product(category, name, description, price);
	}

	public Product create(Category category) {
		return save(build(category));
	}

	public Product create(Category category, Consumer<Product> customizer) {
		Product product = build(category);
		customizer.accept(product);
		return save(product);
	}

	public Product create(Category category, String name, BigDecimal price) {
		return save(build(category, name, price));
	}

	public Product create(Category category, String name, BigDecimal price, Consumer<Product> customizer) {
		Product product = build(category, name, price);
		customizer.accept(product);
		return save(product);
	}

	private String fakeName() {
		return faker.food().ingredient().strip() + CatalogSeedConstants.PRODUCT_NAME_JOINER
				+ faker.food().spice().strip().toLowerCase(Locale.ROOT);
	}

	private String fakeDescription() {
		return CatalogSeedConstants.DESCRIPTION_PREFIX + faker.food().ingredient().strip().toLowerCase(Locale.ROOT)
				+ CatalogSeedConstants.DESCRIPTION_JOINER + faker.food().spice().strip().toLowerCase(Locale.ROOT);
	}

	private BigDecimal fakePrice() {
		int cents = faker.number()
			.numberBetween(CatalogSeedConstants.FACTORY_MIN_PRICE_CENTS, CatalogSeedConstants.FACTORY_MAX_PRICE_CENTS);
		return BigDecimal.valueOf(cents, MoneyConstants.SCALE);
	}

}
