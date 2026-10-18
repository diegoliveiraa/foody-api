package com.foody.api.shared.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.foody.testsupport.api.ApiIntegrationTest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@ApiIntegrationTest
class TrigramSearchIndexTest {

	private static final String USERS_NAME_INDEX = "users_name_unaccent_trgm_idx";

	private static final String DISABLE_SEQUENTIAL_SCANS = "set local enable_seqscan = off";

	private static final String EXPLAIN_USERS_NAME_SEARCH = """
			explain select u.id from users u
			where %s(lower(u.name)) like %s(?) escape '%s'
			""".formatted(PersistenceConstants.UNACCENT_FUNCTION, PersistenceConstants.CONTAINS_PATTERN_FUNCTION,
			PersistenceConstants.LIKE_ESCAPE);

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	@Transactional
	void userNameSearchCanUseTheTrigramIndex() {
		jdbc.execute(DISABLE_SEQUENTIAL_SCANS);

		List<String> plan = jdbc.queryForList(EXPLAIN_USERS_NAME_SEARCH, String.class, "joão");

		assertThat(plan).anyMatch(line -> line.contains(USERS_NAME_INDEX));
	}

}
