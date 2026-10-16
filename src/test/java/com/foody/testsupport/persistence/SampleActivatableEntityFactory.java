package com.foody.testsupport.persistence;

import com.foody.seed.EntityFactory;
import net.datafaker.Faker;

public class SampleActivatableEntityFactory extends EntityFactory<SampleActivatableEntity> {

	private final Faker faker;

	public SampleActivatableEntityFactory(SampleActivatableEntityRepository repository, Faker faker) {
		super(repository);
		this.faker = faker;
	}

	@Override
	public SampleActivatableEntity build() {
		return new SampleActivatableEntity(faker.company().name());
	}

}
