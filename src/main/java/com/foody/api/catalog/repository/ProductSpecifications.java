package com.foody.api.catalog.repository;

import com.foody.api.catalog.CatalogConstants;
import com.foody.api.catalog.dto.ProductFilter;
import com.foody.api.catalog.entity.Product;
import com.foody.api.shared.persistence.ActivatableSpecifications;
import com.foody.api.shared.persistence.ActivationStatus;
import com.foody.api.shared.persistence.PersistenceConstants;
import com.foody.api.shared.util.TextSearchUtils;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.domain.PredicateSpecification;
import org.springframework.data.jpa.domain.Specification;

public final class ProductSpecifications {

	private ProductSpecifications() {
	}

	public static Specification<Product> matching(ProductFilter filter) {
		List<PredicateSpecification<Product>> criteria = Stream
			.of(inCategory(filter.categoryId()), withStatus(filter.status()), searching(filter.search()))
			.flatMap(Optional::stream)
			.toList();
		return Specification.where(PredicateSpecification.allOf(criteria));
	}

	private static Optional<PredicateSpecification<Product>> inCategory(@Nullable UUID categoryId) {
		return Optional.ofNullable(categoryId).map(ProductSpecifications::belongsTo);
	}

	private static PredicateSpecification<Product> belongsTo(UUID categoryId) {
		return (from, builder) -> builder
			.equal(from.get(CatalogConstants.CATEGORY_ATTRIBUTE).get(PersistenceConstants.ID_ATTRIBUTE), categoryId);
	}

	private static Optional<PredicateSpecification<Product>> withStatus(@Nullable ActivationStatus status) {
		return Optional.ofNullable(status).map(ActivatableSpecifications::hasStatus);
	}

	private static Optional<PredicateSpecification<Product>> searching(@Nullable String search) {
		return TextSearchUtils.searchTerm(search).map(ProductSpecifications::nameLike);
	}

	private static PredicateSpecification<Product> nameLike(String term) {
		return (from, builder) -> TextSearchUtils.matches(builder, from.get(CatalogConstants.NAME_ATTRIBUTE), term);
	}

}
