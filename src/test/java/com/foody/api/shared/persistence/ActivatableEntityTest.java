package com.foody.api.shared.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.foody.api.TestcontainersConfiguration;
import com.foody.testsupport.persistence.SampleActivatableEntity;
import com.foody.testsupport.persistence.SampleActivatableEntityRepository;
import com.foody.testsupport.persistence.SamplePersistenceConfiguration;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

@DataJpaTest(properties = SamplePersistenceConfiguration.FLYWAY_LOCATIONS)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ TestcontainersConfiguration.class, SamplePersistenceConfiguration.class })
class ActivatableEntityTest {

	@Autowired
	private SampleActivatableEntityRepository repository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void startsActive() {
		SampleActivatableEntity entity = new SampleActivatableEntity("supplier");

		SampleActivatableEntity saved = repository.saveAndFlush(entity);

		assertThat(saved.isActive()).isTrue();
		assertThat(saved.getDisabledAt()).isNull();
	}

	@Test
	void stampsDisabledAtWhenDisabledAndStaysVisibleToQueries() {
		SampleActivatableEntity saved = repository.saveAndFlush(new SampleActivatableEntity("supplier"));

		saved.disable();
		repository.saveAndFlush(saved);
		entityManager.clear();

		SampleActivatableEntity reloaded = repository.findById(saved.getId()).orElseThrow();
		assertThat(reloaded.isActive()).isFalse();
		assertThat(reloaded.getDisabledAt()).isCloseTo(Instant.now(), within(1, ChronoUnit.MINUTES));
	}

	@Test
	void reportsTheDisabledAtTheDatabaseKeeps() {
		SampleActivatableEntity saved = repository.saveAndFlush(new SampleActivatableEntity("supplier"));

		saved.disable();
		Instant reported = saved.getDisabledAt();
		repository.saveAndFlush(saved);
		entityManager.clear();

		SampleActivatableEntity reloaded = repository.findById(saved.getId()).orElseThrow();
		assertThat(reloaded.getDisabledAt()).isEqualTo(reported);
	}

	@Test
	void clearsDisabledAtWhenEnabledAgain() {
		SampleActivatableEntity saved = repository.saveAndFlush(new SampleActivatableEntity("supplier"));
		saved.disable();
		repository.saveAndFlush(saved);

		saved.enable();
		repository.saveAndFlush(saved);

		assertThat(saved.isActive()).isTrue();
		assertThat(saved.getDisabledAt()).isNull();
	}

	@Test
	void keepsTheFirstDisabledAtWhenDisabledTwice() {
		SampleActivatableEntity saved = repository.saveAndFlush(new SampleActivatableEntity("supplier"));
		saved.disable();
		Instant firstDisabledAt = saved.getDisabledAt();

		saved.disable();

		assertThat(saved.getDisabledAt()).isEqualTo(firstDisabledAt);
	}

}
