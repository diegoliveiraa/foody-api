package com.foody.api.catalog.entity;

import com.foody.api.shared.persistence.ActivatableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

@Getter
@Entity
@Table(name = "products")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends ActivatableEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(nullable = false)
	private Category category;

	@Column(nullable = false)
	private String name;

	private @Nullable String description;

	@Column(nullable = false)
	private BigDecimal price;

	public Product(Category category, String name, @Nullable String description, BigDecimal price) {
		this.category = category;
		this.name = name;
		this.description = description;
		this.price = price;
	}

	public void update(Category newCategory, String newName, @Nullable String newDescription, BigDecimal newPrice) {
		this.category = newCategory;
		this.name = newName;
		this.description = newDescription;
		this.price = newPrice;
	}

	public boolean isAvailable() {
		return isActive() && category.isActive();
	}

}
