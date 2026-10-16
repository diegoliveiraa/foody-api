package com.foody.seed;

import com.foody.api.shared.persistence.BaseEntity;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.springframework.data.jpa.repository.JpaRepository;

public abstract class EntityFactory<E extends BaseEntity> {

	private final JpaRepository<E, UUID> repository;

	protected EntityFactory(JpaRepository<E, UUID> repository) {
		this.repository = repository;
	}

	public abstract E build();

	public E create() {
		return repository.save(build());
	}

	public E create(Consumer<E> customizer) {
		E entity = build();
		customizer.accept(entity);
		return repository.save(entity);
	}

	public List<E> createMany(int count) {
		return repository.saveAll(Stream.generate(this::build).limit(count).toList());
	}

}
