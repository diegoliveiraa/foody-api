package com.foody.testsupport.persistence;

import com.foody.api.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "sample_entity")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SampleEntity extends BaseEntity {

	@Column(nullable = false)
	private String name;

	public SampleEntity(String name) {
		this.name = name;
	}

	public void rename(String newName) {
		this.name = newName;
	}

}
