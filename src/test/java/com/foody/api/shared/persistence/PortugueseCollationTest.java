package com.foody.api.shared.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.foody.testsupport.api.ApiIntegrationTest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

@ApiIntegrationTest
class PortugueseCollationTest {

	private static final String PORTUGUESE_COLLATION = "pt-BR-x-icu";

	private static final String PORTUGUESE_ICU_COLLATIONS = """
			select collname from pg_collation
			where collprovider = 'i' and collname like 'pt-%-x-icu'
			""";

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	void portugueseIcuCollationIsAvailable() {
		String query = PORTUGUESE_ICU_COLLATIONS;

		List<String> collations = jdbc.queryForList(query, String.class);

		assertThat(collations).contains(PORTUGUESE_COLLATION);
	}

}
