package com.foody.api.catalog.repository;

import com.foody.api.catalog.entity.Category;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

	@Query("select count(c) > 0 from Category c where lower(c.name) = lower(:name)")
	boolean existsByName(String name);

	@Query("select count(c) > 0 from Category c where lower(c.name) = lower(:name) and c.id <> :id")
	boolean existsByNameOnOtherThan(String name, UUID id);

}
