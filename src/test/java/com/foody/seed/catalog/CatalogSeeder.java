package com.foody.seed.catalog;

import com.foody.api.catalog.entity.Category;
import com.foody.api.catalog.entity.Product;
import com.foody.api.catalog.repository.CategoryRepository;
import com.foody.api.catalog.repository.ProductRepository;
import com.foody.seed.SeedConstants;
import com.foody.seed.SeedScale;
import com.foody.seed.Seeder;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.datafaker.Faker;
import org.springframework.core.annotation.Order;

@Order(2)
public class CatalogSeeder implements Seeder {

	private final CategoryRepository categoryRepository;

	private final ProductRepository productRepository;

	private final CategoryFactory categories;

	private final ProductFactory products;

	private final Faker faker;

	public CatalogSeeder(CategoryRepository categoryRepository, ProductRepository productRepository,
			CategoryFactory categories, ProductFactory products, Faker faker) {
		this.categoryRepository = categoryRepository;
		this.productRepository = productRepository;
		this.categories = categories;
		this.products = products;
		this.faker = faker;
	}

	@Override
	public boolean alreadySeeded() {
		return categoryRepository.count() > 0;
	}

	@Override
	public void seed(SeedScale scale) {
		Map<SeedCategory, Category> saved = seedCategories();
		List<SeedCategory> sections = List.of(SeedCategory.values());
		List<Product> built = IntStream.range(0, scale.of(CatalogSeedConstants.PRODUCT_BASE_COUNT))
			.mapToObj(index -> buildProduct(index, sections, saved))
			.toList();
		productRepository.saveAll(withSomeDisabled(built));
	}

	private Map<SeedCategory, Category> seedCategories() {
		return Arrays.stream(SeedCategory.values())
			.collect(Collectors.toMap(Function.identity(), this::seedCategory, (first, second) -> first,
					() -> new EnumMap<>(SeedCategory.class)));
	}

	private Category seedCategory(SeedCategory section) {
		int displayOrder = (section.ordinal() + 1) * CatalogSeedConstants.DISPLAY_ORDER_STEP;
		return categoryRepository.save(categories.build(section.label(), displayOrder));
	}

	private Product buildProduct(int index, List<SeedCategory> sections, Map<SeedCategory, Category> saved) {
		SeedCategory section = sections.get(index % sections.size());
		int position = index / sections.size();
		List<SeedProduct> names = section.products();
		SeedProduct seedProduct = names.get(position % names.size());
		int round = position / names.size();
		return products.build(saved.get(section), variantName(seedProduct.name(), round), seedProduct.description(),
				priceOf(section, seedProduct, round));
	}

	private String variantName(String name, int round) {
		if (round == 0) {
			return name;
		}
		int variant = round - 1;
		List<String> variants = CatalogSeedConstants.NAME_VARIANTS;
		String varied = name + CatalogSeedConstants.NAME_SEPARATOR + variants.get(variant % variants.size());
		int cycle = variant / variants.size();
		if (cycle == 0) {
			return varied;
		}
		return varied + CatalogSeedConstants.NAME_SEPARATOR + (cycle + 1);
	}

	private BigDecimal priceOf(SeedCategory section, SeedProduct seedProduct, int round) {
		if (round == 0 && seedProduct.isPrototype()) {
			return seedProduct.prototypePrice();
		}
		int reais = faker.number().numberBetween(section.minPrice(), section.maxPrice());
		return BigDecimal.valueOf(reais).add(CatalogSeedConstants.PRICE_CENTS);
	}

	private List<Product> withSomeDisabled(List<Product> built) {
		Set<String> prototypeNames = SeedCategory.prototypeNames();
		List<Product> eligible = built.stream().filter(product -> !prototypeNames.contains(product.getName())).toList();
		int disabledCount = built.size() * CatalogSeedConstants.DISABLED_PRODUCT_PERCENT / SeedConstants.PERCENT;
		Stream.generate(() -> faker.random().nextInt(eligible.size()))
			.distinct()
			.limit(disabledCount)
			.forEach(index -> eligible.get(index).disable());
		return built;
	}

}
