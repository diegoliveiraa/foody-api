package com.foody.api.catalog.repository;

import com.foody.api.catalog.CatalogConstants;
import com.foody.api.catalog.dto.CategoryProductCount;
import com.foody.api.catalog.dto.ProductFilter;
import com.foody.api.catalog.entity.Category;
import com.foody.api.catalog.entity.Product;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {

	@Override
	@EntityGraph(attributePaths = CatalogConstants.CATEGORY_ATTRIBUTE)
	Page<Product> findAll(Specification<Product> specification, Pageable pageable);

	@Query("select p from Product p join fetch p.category where p.id = :id")
	Optional<Product> findWithCategoryById(UUID id);

	@Query("select p from Product p join fetch p.category where p.id in :ids")
	List<Product> findAllWithCategoryByIdIn(Collection<UUID> ids);

	@Query("""
			select p from Product p join fetch p.category c
			where p.disabledAt is null and c.disabledAt is null
			order by c.displayOrder, c.name, c.id, p.name, p.id
			""")
	List<Product> findAllOnMenu();

	@Query("""
			select new com.foody.api.catalog.dto.CategoryProductCount(p.category.id, count(p))
			from Product p group by p.category.id
			""")
	List<CategoryProductCount> countPerCategory();

	long countByCategory(Category category);

	boolean existsByCategory(Category category);

	default Page<Product> findAllMatching(ProductFilter filter, Pageable pageable) {
		return findAll(ProductSpecifications.matching(filter), pageable);
	}

}
