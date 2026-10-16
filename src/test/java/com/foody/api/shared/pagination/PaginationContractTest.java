package com.foody.api.shared.pagination;

import static org.assertj.core.api.Assertions.assertThat;

import com.foody.api.shared.message.Messages;
import com.foody.api.shared.probe.ProbeConstants;
import com.foody.api.shared.probe.ProbeController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@WebMvcTest
@Import(ProbeController.class)
class PaginationContractTest {

	private static final String PAGE_PATH = ProbeConstants.BASE_PATH + "/page";

	@Autowired
	private MockMvcTester mvc;

	@Test
	void returnsTheDataAndAOneBasedMetaForTheRequestedPage() {
		String query = "?page=2&pageSize=10&sort=name,asc";

		MvcTestResult result = mvc.get().uri(PAGE_PATH + query).exchange();

		assertThat(result).hasStatus(HttpStatus.OK);
		assertThat(result).bodyJson().extractingPath("$.data").asArray().containsExactly("a", "b");
		assertThat(result).bodyJson().extractingPath("$.meta.page").isEqualTo(2);
		assertThat(result).bodyJson().extractingPath("$.meta.pageSize").isEqualTo(10);
		assertThat(result).bodyJson().extractingPath("$.meta.totalItems").isEqualTo(42);
		assertThat(result).bodyJson().extractingPath("$.meta.totalPages").isEqualTo(5);
		assertThat(result).bodyJson().extractingPath("$.meta.hasNext").isEqualTo(true);
		assertThat(result).bodyJson().extractingPath("$.meta.hasPrevious").isEqualTo(true);
	}

	@Test
	void startsAtPageOneWithTenItemsWhenNothingIsRequested() {
		String query = "";

		MvcTestResult result = mvc.get().uri(PAGE_PATH + query).exchange();

		assertThat(result).bodyJson().extractingPath("$.meta.page").isEqualTo(1);
		assertThat(result).bodyJson().extractingPath("$.meta.pageSize").isEqualTo(10);
		assertThat(result).bodyJson().extractingPath("$.meta.hasPrevious").isEqualTo(false);
	}

	@ParameterizedTest
	@ValueSource(ints = { 10, 25, 50, 100 })
	void acceptsEachOfTheOfferedPageSizes(int pageSize) {
		String query = "?pageSize=" + pageSize;

		MvcTestResult result = mvc.get().uri(PAGE_PATH + query).exchange();

		assertThat(result).hasStatus(HttpStatus.OK);
		assertThat(result).bodyJson().extractingPath("$.meta.pageSize").isEqualTo(pageSize);
	}

	@ParameterizedTest
	@ValueSource(strings = { "20", "1000", "0", "abc" })
	void rejectsAPageSizeOutsideTheOfferedOptionsWith400(String pageSize) {
		String query = "?pageSize=" + pageSize;

		MvcTestResult result = mvc.get().uri(PAGE_PATH + query).exchange();

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(Messages.INVALID_PAGE_SIZE.name());
	}

	@Test
	void rejectsSortingByAPropertyOutsideTheAllowlistWith400() {
		String query = "?sort=passwordHash,desc";

		MvcTestResult result = mvc.get().uri(PAGE_PATH + query).exchange();

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(Messages.INVALID_SORT.name());
	}

}
