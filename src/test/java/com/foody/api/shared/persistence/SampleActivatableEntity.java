package com.foody.api.shared.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "sample_activatable_entity")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SampleActivatableEntity extends ActivatableEntity {

	@Column(nullable = false)
	private String name;

	public SampleActivatableEntity(String name) {
		this.name = name;
	}

}
