package com.foody.api.catalog.entity;

import com.foody.api.shared.persistence.ActivatableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "categories")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category extends ActivatableEntity {

	@Column(nullable = false)
	private String name;

	@Column(nullable = false)
	private int displayOrder;

	public Category(String name, int displayOrder) {
		this.name = name;
		this.displayOrder = displayOrder;
	}

	public void update(String newName, int newDisplayOrder) {
		this.name = newName;
		this.displayOrder = newDisplayOrder;
	}

}
