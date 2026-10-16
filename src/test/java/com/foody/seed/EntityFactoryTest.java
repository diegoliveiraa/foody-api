package com.foody.seed;

import static org.assertj.core.api.Assertions.assertThat;

import com.foody.api.FoodyApiApplication;
import com.foody.api.TestcontainersConfiguration;
import com.foody.testsupport.persistence.SampleActivatableEntity;
import com.foody.testsupport.persistence.SampleActivatableEntityFactory;
import com.foody.testsupport.persistence.SampleActivatableEntityRepository;
import com.foody.testsupport.persistence.SamplePersistenceConfiguration;
import jakarta.persistence.EntityManager;
import java.util.List;
import net.datafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

@DataJpaTest(properties = SamplePersistenceConfiguration.FLYWAY_LOCATIONS)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = FoodyApiApplication.class)
@Import({ TestcontainersConfiguration.class, SamplePersistenceConfiguration.class })
class EntityFactoryTest {

	@Autowired
	private SampleActivatableEntityRepository repository;

	@Autowired
	private EntityManager entityManager;

	private SampleActivatableEntityFactory factory;

	@BeforeEach
	void createFactory() {
		factory = new SampleActivatableEntityFactory(repository, new Faker(SeedConstants.LOCALE));
	}

	@Test
	void buildsAValidEntityWithFakeDataWithoutSavingIt() {
		SampleActivatableEntity built = factory.build();

		assertThat(built.getName()).isNotBlank();
		assertThat(built.getId()).isNull();
		assertThat(repository.count()).isZero();
	}

	@Test
	void createsAPersistedEntity() {
		SampleActivatableEntity created = factory.create();

		assertThat(created.getId()).isNotNull();
		assertThat(repository.count()).isEqualTo(1);
	}

	@Test
	void createsManyEntitiesInOneCall() {
		int count = 5;

		List<SampleActivatableEntity> created = factory.createMany(count);

		assertThat(created).hasSize(count)
			.extracting(SampleActivatableEntity::getId)
			.doesNotContainNull()
			.doesNotHaveDuplicates();
		assertThat(repository.count()).isEqualTo(count);
	}

	@Test
	void appliesTheCustomizationBeforeSaving() {
		SampleActivatableEntity created = factory.create(SampleActivatableEntity::disable);
		repository.flush();
		entityManager.clear();

		SampleActivatableEntity reloaded = repository.findById(created.getId()).orElseThrow();

		assertThat(reloaded.isActive()).isFalse();
	}

}
