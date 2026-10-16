package com.foody.api.contract;

import static org.assertj.core.api.Assertions.assertThat;

import com.foody.api.TestcontainersConfiguration;
import com.foody.testsupport.openapi.OpenApiConfiguration;
import com.foody.testsupport.openapi.OpenApiConstants;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@SpringBootTest
@AutoConfigureMockMvc
@Import({ TestcontainersConfiguration.class, OpenApiConfiguration.class })
class OpenApiContractTest {

	@Autowired
	private MockMvcTester mvc;

	@Test
	void publishesTheApiContractToDocsOpenapiJson() throws IOException {
		MvcTestResult result = mvc.get().uri(OpenApiConstants.DOCS_PATH).exchange();
		String contract = result.getResponse()
			.getContentAsString(StandardCharsets.UTF_8)
			.replace(OpenApiConstants.WINDOWS_LINE_BREAK, OpenApiConstants.LINE_BREAK);
		Files.writeString(OpenApiConstants.CONTRACT_FILE, contract);

		assertThat(result).hasStatus(HttpStatus.OK);
		assertThat(result).bodyJson().extractingPath("$.info.title").isEqualTo(OpenApiConstants.TITLE);
		assertThat(result).bodyJson().extractingPath("$.servers[0].url").isEqualTo(OpenApiConstants.SERVER_URL);
		assertThat(OpenApiConstants.CONTRACT_FILE).hasContent(contract);
	}

}
