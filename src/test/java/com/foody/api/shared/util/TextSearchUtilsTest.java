package com.foody.api.shared.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.foody.api.TestcontainersConfiguration;
import com.foody.testsupport.persistence.SampleEntity;
import com.foody.testsupport.persistence.SampleEntityRepository;
import com.foody.testsupport.persistence.SamplePersistenceConfiguration;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

@DataJpaTest(properties = SamplePersistenceConfiguration.FLYWAY_LOCATIONS)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ TestcontainersConfiguration.class, SamplePersistenceConfiguration.class })
class TextSearchUtilsTest {

	private static final String NAME_ATTRIBUTE = "name";

	@Autowired
	private SampleEntityRepository repository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void searchTermIgnoresSurroundingSpaces() {
		String search = "  Ana 50% ";

		Optional<String> term = TextSearchUtils.searchTerm(search);

		assertThat(term).contains("Ana 50%");
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = { "", "   " })
	void searchTermIsEmptyForABlankSearch(String search) {
		Optional<String> term = TextSearchUtils.searchTerm(search);

		assertThat(term).isEmpty();
	}

	@Test
	void matchesIgnoringAccentsAndCase() {
		String marker = marker();
		SampleEntity acai = repository.saveAndFlush(new SampleEntity(marker + " Açaí na Tigela"));
		repository.saveAndFlush(new SampleEntity(marker + " Acarajé"));

		List<SampleEntity> found = searching(marker + " ACAI NA TIGELA");

		assertThat(found).containsExactly(acai);
	}

	@Test
	void matchesWildcardsOfTheTermLiterally() {
		String marker = marker();
		SampleEntity percent = repository.saveAndFlush(new SampleEntity(marker + " 100% café"));
		repository.saveAndFlush(new SampleEntity(marker + " 1000 café"));

		List<SampleEntity> found = searching(marker + " 100％");

		assertThat(found).containsExactly(percent);
	}

	private List<SampleEntity> searching(String term) {
		CriteriaBuilder builder = entityManager.getCriteriaBuilder();
		CriteriaQuery<SampleEntity> query = builder.createQuery(SampleEntity.class);
		Root<SampleEntity> root = query.from(SampleEntity.class);
		query.where(TextSearchUtils.matches(builder, root.get(NAME_ATTRIBUTE), term));
		return entityManager.createQuery(query).getResultList();
	}

	private String marker() {
		return "txt" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
	}

}
