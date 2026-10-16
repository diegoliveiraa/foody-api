package com.foody.api.shared.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.foody.api.TestcontainersConfiguration;
import com.foody.testsupport.persistence.SampleEntity;
import com.foody.testsupport.persistence.SampleEntityRepository;
import com.foody.testsupport.persistence.SamplePersistenceConfiguration;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest(properties = SamplePersistenceConfiguration.FLYWAY_LOCATIONS)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ TestcontainersConfiguration.class, SamplePersistenceConfiguration.class })
class BaseEntityTest {

	private static final int UUID_VERSION_7 = 7;

	@Autowired
	private SampleEntityRepository repository;

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	void assignsATimeOrderedUuidV7AndCurrentTimestampsWhenPersisted() {
		SampleEntity entity = new SampleEntity("first");

		SampleEntity saved = repository.saveAndFlush(entity);

		assertThat(saved.getId().version()).isEqualTo(UUID_VERSION_7);
		assertThat(saved.getCreatedAt()).isCloseTo(Instant.now(), within(1, ChronoUnit.MINUTES));
		assertThat(saved.getUpdatedAt()).isCloseTo(Instant.now(), within(1, ChronoUnit.MINUTES));
	}

	@Test
	void incrementsTheVersionOnEachUpdate() {
		SampleEntity saved = repository.saveAndFlush(new SampleEntity("first"));
		long initialVersion = saved.getVersion();

		saved.rename("second");
		SampleEntity updated = repository.saveAndFlush(saved);

		assertThat(updated.getVersion()).isEqualTo(initialVersion + 1);
	}

	@Test
	void considersTwoInstancesOfTheSamePersistedRowEqual() {
		SampleEntity saved = repository.saveAndFlush(new SampleEntity("first"));
		entityManager.clear();

		SampleEntity reloaded = repository.findById(saved.getId()).orElseThrow();

		assertThat(reloaded).isNotSameAs(saved).isEqualTo(saved).hasSameHashCodeAs(saved);
	}

	@Test
	void considersTwoUnsavedEntitiesDifferent() {
		SampleEntity first = new SampleEntity("same name");

		SampleEntity second = new SampleEntity("same name");

		assertThat(first).isNotEqualTo(second);
	}

	@Test
	void deletesTheRowForGood() {
		SampleEntity saved = repository.saveAndFlush(new SampleEntity("to delete"));

		repository.delete(saved);
		repository.flush();

		Long rows = jdbc.queryForObject("select count(*) from sample_entity where id = ?", Long.class, saved.getId());
		assertThat(rows).isZero();
	}

}
