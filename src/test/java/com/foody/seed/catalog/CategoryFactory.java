package com.foody.seed.catalog;

import com.foody.api.catalog.CatalogConstants;
import com.foody.api.catalog.entity.Category;
import com.foody.api.catalog.repository.CategoryRepository;
import com.foody.seed.EntityFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import net.datafaker.Faker;

public class CategoryFactory extends EntityFactory<Category> {

	private final Faker faker;

	private final AtomicInteger sequence = new AtomicInteger();

	public CategoryFactory(CategoryRepository repository, Faker faker) {
		super(repository);
		this.faker = faker;
	}

	@Override
	public Category build() {
		int displayOrder = faker.number()
			.numberBetween(CatalogConstants.DISPLAY_ORDER_MIN, CatalogConstants.DISPLAY_ORDER_MAX + 1);
		return build(uniqueName(), displayOrder);
	}

	public Category build(String name, int displayOrder) {
		return new Category(name, displayOrder);
	}

	public Category create(String name, int displayOrder) {
		return save(build(name, displayOrder));
	}

	public Category create(String name, int displayOrder, Consumer<Category> customizer) {
		Category category = build(name, displayOrder);
		customizer.accept(category);
		return save(category);
	}

	private String uniqueName() {
		return faker.food().ingredient().strip() + CatalogSeedConstants.NAME_SEPARATOR + sequence.incrementAndGet();
	}

}
