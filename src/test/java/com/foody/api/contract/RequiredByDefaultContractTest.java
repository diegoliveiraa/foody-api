package com.foody.api.contract;

import static org.assertj.core.api.Assertions.assertThat;

import com.foody.api.TestcontainersConfiguration;
import com.foody.testsupport.openapi.ContractProbeController;
import com.foody.testsupport.openapi.OpenApiConfiguration;
import com.foody.testsupport.openapi.OpenApiConstants;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@SpringBootTest
@AutoConfigureMockMvc
@Import({ TestcontainersConfiguration.class, OpenApiConfiguration.class, ContractProbeController.class })
class RequiredByDefaultContractTest {

	private static final String PROBE_SCHEMA = "$.components.schemas.ContractProbe";

	@Autowired
	private MockMvcTester mvc;

	@Test
	void aComponentWithoutNullableIsRequired() {
		String path = OpenApiConstants.DOCS_PATH;

		MvcTestResult result = mvc.get().uri(path).exchange();

		assertThat(result).bodyJson().extractingPath(PROBE_SCHEMA + ".required").asArray().containsExactly("name");
		assertThat(result).bodyJson()
			.extractingPath("$.components.schemas.ContractProbeDetail.required")
			.asArray()
			.containsExactly("value");
	}

	@Test
	void aNullableComponentIsOptionalAndNullable() {
		String path = OpenApiConstants.DOCS_PATH;

		MvcTestResult result = mvc.get().uri(path).exchange();

		assertThat(result).bodyJson()
			.extractingPath(PROBE_SCHEMA + ".required")
			.asArray()
			.doesNotContain("note", "detail");
		assertThat(result).bodyJson()
			.extractingPath(PROBE_SCHEMA + ".properties.note.type")
			.asArray()
			.containsExactlyInAnyOrder("string", "null");
	}

	@Test
	void aNullableRecordComponentIsAReferenceOrNull() {
		String path = OpenApiConstants.DOCS_PATH;

		MvcTestResult result = mvc.get().uri(path).exchange();

		assertThat(result).bodyJson()
			.extractingPath(PROBE_SCHEMA + ".properties.detail.oneOf[0].$ref")
			.isEqualTo("#/components/schemas/ContractProbeDetail");
		assertThat(result).bodyJson()
			.extractingPath(PROBE_SCHEMA + ".properties.detail.oneOf[1].type")
			.isEqualTo("null");
	}

}
