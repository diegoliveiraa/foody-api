package com.foody.api.shared.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.foody.api.shared.message.Messages;
import com.foody.api.shared.probe.ProbeConstants;
import com.foody.api.shared.probe.ProbeController;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@WebMvcTest
@Import(ProbeController.class)
class GlobalExceptionHandlerTest {

	@Autowired
	private MockMvcTester mvc;

	@Test
	void answersAnInvalidBodyWith400ListingEveryInvalidField() {
		String body = "{\"name\":\"\",\"quantity\":0}";

		MvcTestResult result = mvc.post()
			.uri(ProbeConstants.BASE_PATH + "/items")
			.contentType(MediaType.APPLICATION_JSON)
			.content(body)
			.exchange();

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
		assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(Messages.VALIDATION_FAILED.name());
		assertThat(result).bodyJson()
			.extractingPath("$.errors[*].field")
			.asArray()
			.containsExactlyInAnyOrder("name", "quantity");
	}

	@Test
	void answersMalformedJsonWith400MalformedRequest() {
		String body = "{\"name\":";

		MvcTestResult result = mvc.post()
			.uri(ProbeConstants.BASE_PATH + "/items")
			.contentType(MediaType.APPLICATION_JSON)
			.content(body)
			.exchange();

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(Messages.MALFORMED_REQUEST.name());
		assertThat(result).bodyJson().extractingPath("$.detail").isEqualTo(Messages.MALFORMED_REQUEST.text());
	}

	@Test
	void answersADomainNotFoundWith404CarryingTheCatalogCodeAndTheDetail() {
		UUID id = UUID.randomUUID();

		MvcTestResult result = mvc.get().uri(ProbeConstants.BASE_PATH + "/items/{id}", id).exchange();

		assertThat(result).hasStatus(HttpStatus.NOT_FOUND).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
		assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(Messages.RESOURCE_NOT_FOUND.name());
		assertThat(result).bodyJson()
			.extractingPath("$.detail")
			.isEqualTo(Messages.RESOURCE_NOT_FOUND.text() + ErrorConstants.DETAIL_SEPARATOR + id);
	}

	@Test
	void answersAPathParameterOfTheWrongTypeWith400InvalidParameter() {
		String notAUuid = "not-a-uuid";

		MvcTestResult result = mvc.get().uri(ProbeConstants.BASE_PATH + "/items/{id}", notAUuid).exchange();

		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(Messages.INVALID_PARAMETER.name());
	}

	@Test
	void answersAnUnexpectedFailureWith500WithoutLeakingItsMessage() {
		String path = ProbeConstants.BASE_PATH + "/failure";

		MvcTestResult result = mvc.get().uri(path).exchange();

		assertThat(result).hasStatus(HttpStatus.INTERNAL_SERVER_ERROR);
		assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(Messages.INTERNAL_ERROR.name());
		assertThat(result).bodyJson().extractingPath("$.detail").isEqualTo(Messages.INTERNAL_ERROR.text());
		assertThat(result).body().asString().doesNotContain(ProbeConstants.SECRET_DETAIL);
	}

	@Test
	void answersAConcurrentUpdateWith409() {
		String path = ProbeConstants.BASE_PATH + "/concurrent-update";

		MvcTestResult result = mvc.get().uri(path).exchange();

		assertThat(result).hasStatus(HttpStatus.CONFLICT);
		assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(Messages.CONCURRENT_UPDATE.name());
	}

	@Test
	void answersAnUnknownRouteWith404ResourceNotFound() {
		String path = ProbeConstants.BASE_PATH + "/does-not-exist";

		MvcTestResult result = mvc.get().uri(path).exchange();

		assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
		assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(Messages.RESOURCE_NOT_FOUND.name());
		assertThat(result).bodyJson().extractingPath("$.detail").isEqualTo(Messages.RESOURCE_NOT_FOUND.text());
	}

}
