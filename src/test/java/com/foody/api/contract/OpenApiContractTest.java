package com.foody.api.contract;

import static org.assertj.core.api.Assertions.assertThat;

import com.foody.api.TestcontainersConfiguration;
import com.foody.testsupport.openapi.OpenApiConfiguration;
import com.foody.testsupport.openapi.OpenApiConstants;
import com.jayway.jsonpath.JsonPath;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
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

	@Test
	void namesEveryOperationAfterItsControllerAndMethod() {
		String path = OpenApiConstants.DOCS_PATH;

		MvcTestResult result = mvc.get().uri(path).exchange();

		assertThat(result).bodyJson()
			.extractingPath("$.paths['/admin/users'].get.operationId")
			.isEqualTo("adminUserList");
		assertThat(result).bodyJson()
			.extractingPath("$.paths['/catalog/products'].get.operationId")
			.isEqualTo("productList");
		assertThat(result).bodyJson()
			.extractingPath("$.paths['/catalog/categories'].get.operationId")
			.isEqualTo("categoryList");
		assertThat(result).bodyJson().extractingPath("$.paths['/me'].get.operationId").isEqualTo("meGet");
		assertThat(result).bodyJson().extractingPath("$.paths['/menu'].get.operationId").isEqualTo("menuGet");
	}

	@Test
	void givesEveryOperationAUniqueIdWithoutNumericSuffixes() throws IOException {
		String path = OpenApiConstants.DOCS_PATH;

		String contract = mvc.get().uri(path).exchange().getResponse().getContentAsString(StandardCharsets.UTF_8);
		List<String> operationIds = JsonPath.read(contract, OpenApiConstants.OPERATION_IDS_PATH);

		assertThat(operationIds).isNotEmpty()
			.doesNotHaveDuplicates()
			.allMatch(operationId -> operationId.matches(OpenApiConstants.OPERATION_ID_PATTERN));
	}

	@Test
	void declaresBearerAuthenticationOnlyOnProtectedOperations() {
		String path = OpenApiConstants.DOCS_PATH;

		MvcTestResult result = mvc.get().uri(path).exchange();

		assertThat(result).bodyJson()
			.extractingPath("$.components.securitySchemes.bearer.scheme")
			.isEqualTo(OpenApiConstants.BEARER_SCHEME);
		assertThat(result).bodyJson()
			.extractingPath("$.paths['/me'].get.security[0]")
			.asMap()
			.containsOnlyKeys(OpenApiConstants.BEARER_SCHEME);
		assertThat(result).bodyJson().doesNotHavePath("$.paths['/auth/login'].post.security");
	}

	@Test
	void requiresEveryComponentOfTheAuthResponse() {
		String path = OpenApiConstants.DOCS_PATH;

		MvcTestResult result = mvc.get().uri(path).exchange();

		assertThat(result).bodyJson()
			.extractingPath("$.components.schemas.AuthResponse.required")
			.asArray()
			.containsExactlyInAnyOrder("accessToken", "expiresIn");
	}

	@Test
	void requiresEveryComponentOfTheMeResponseAndItsScreens() {
		String path = OpenApiConstants.DOCS_PATH;

		MvcTestResult result = mvc.get().uri(path).exchange();

		assertThat(result).bodyJson()
			.extractingPath("$.components.schemas.MeResponse.required")
			.asArray()
			.containsExactlyInAnyOrder("user", "modules");
		assertThat(result).bodyJson()
			.extractingPath("$.components.schemas.MeScreen.required")
			.asArray()
			.containsExactlyInAnyOrder("key", "label", "actions");
	}

	@Test
	void publishesTheUserAdministrationEndpoints() {
		String path = OpenApiConstants.DOCS_PATH;

		MvcTestResult result = mvc.get().uri(path).exchange();

		assertThat(result).bodyJson().hasPath("$.paths['/admin/users'].get");
		assertThat(result).bodyJson().hasPath("$.paths['/admin/users/{id}/role'].patch");
		assertThat(result).bodyJson().hasPath("$.paths['/admin/users/{id}/disable'].post");
		assertThat(result).bodyJson().hasPath("$.paths['/admin/users/{id}/enable'].post");
	}

	@Test
	void requiresEveryComponentOfTheAdminUserResponseButDisabledAt() {
		String path = OpenApiConstants.DOCS_PATH;

		MvcTestResult result = mvc.get().uri(path).exchange();

		assertThat(result).bodyJson()
			.extractingPath("$.components.schemas.AdminUserResponse.required")
			.asArray()
			.containsExactlyInAnyOrder("id", "name", "email", "role", "status", "createdAt");
		assertThat(result).bodyJson()
			.extractingPath("$.components.schemas.AdminUserResponse.properties.disabledAt.type")
			.asArray()
			.containsExactlyInAnyOrder("string", "null");
	}

	@Test
	void publishesTheCatalogAndMenuEndpoints() {
		String path = OpenApiConstants.DOCS_PATH;

		MvcTestResult result = mvc.get().uri(path).exchange();

		assertThat(result).bodyJson().hasPath("$.paths['/catalog/categories'].get");
		assertThat(result).bodyJson().hasPath("$.paths['/catalog/categories'].post");
		assertThat(result).bodyJson().hasPath("$.paths['/catalog/categories/{id}'].put");
		assertThat(result).bodyJson().hasPath("$.paths['/catalog/categories/{id}'].delete");
		assertThat(result).bodyJson().hasPath("$.paths['/catalog/categories/{id}/disable'].post");
		assertThat(result).bodyJson().hasPath("$.paths['/catalog/categories/{id}/enable'].post");
		assertThat(result).bodyJson().hasPath("$.paths['/catalog/products'].get");
		assertThat(result).bodyJson().hasPath("$.paths['/catalog/products'].post");
		assertThat(result).bodyJson().hasPath("$.paths['/catalog/products/{id}'].put");
		assertThat(result).bodyJson().hasPath("$.paths['/catalog/products/{id}'].delete");
		assertThat(result).bodyJson().hasPath("$.paths['/catalog/products/{id}/disable'].post");
		assertThat(result).bodyJson().hasPath("$.paths['/catalog/products/{id}/enable'].post");
		assertThat(result).bodyJson().hasPath("$.paths['/menu'].get");
		assertThat(result).bodyJson().hasPath("$.paths['/menu/quote'].post");
	}

	@Test
	void requiresEveryCatalogComponentButTheNullableOnes() {
		String path = OpenApiConstants.DOCS_PATH;

		MvcTestResult result = mvc.get().uri(path).exchange();

		assertThat(result).bodyJson()
			.extractingPath("$.components.schemas.CategoryResponse.required")
			.asArray()
			.containsExactlyInAnyOrder("id", "name", "displayOrder", "status", "productCount");
		assertThat(result).bodyJson()
			.extractingPath("$.components.schemas.ProductResponse.required")
			.asArray()
			.containsExactlyInAnyOrder("id", "name", "price", "category", "status", "createdAt");
		assertThat(result).bodyJson()
			.extractingPath("$.components.schemas.ProductRequest.required")
			.asArray()
			.containsExactlyInAnyOrder("name", "price", "categoryId");
		assertThat(result).bodyJson()
			.extractingPath("$.components.schemas.MenuProduct.required")
			.asArray()
			.containsExactlyInAnyOrder("id", "name", "price");
		assertThat(result).bodyJson()
			.extractingPath("$.components.schemas.QuotedItem.required")
			.asArray()
			.containsExactlyInAnyOrder("productId", "quantity", "available");
		assertThat(result).bodyJson()
			.extractingPath("$.components.schemas.QuotedItem.properties.unitPrice.type")
			.asArray()
			.containsExactlyInAnyOrder("number", "null");
		assertThat(result).bodyJson()
			.extractingPath("$.components.schemas.CartQuote.required")
			.asArray()
			.containsExactlyInAnyOrder("items", "total");
	}

	@Test
	void publishesTheOrderEndpointsNamedAfterTheirControllers() {
		String path = OpenApiConstants.DOCS_PATH;

		MvcTestResult result = mvc.get().uri(path).exchange();

		assertThat(result).bodyJson()
			.extractingPath("$.paths['/my-orders'].post.operationId")
			.isEqualTo("myOrderPlace");
		assertThat(result).bodyJson().extractingPath("$.paths['/my-orders'].get.operationId").isEqualTo("myOrderList");
		assertThat(result).bodyJson()
			.extractingPath("$.paths['/my-orders/{id}'].get.operationId")
			.isEqualTo("myOrderDetail");
		assertThat(result).bodyJson().extractingPath("$.paths['/orders'].get.operationId").isEqualTo("orderList");
		assertThat(result).bodyJson()
			.extractingPath("$.paths['/orders/{id}'].get.operationId")
			.isEqualTo("orderDetail");
		assertThat(result).bodyJson()
			.extractingPath("$.paths['/orders'].get.parameters[*].name")
			.asArray()
			.containsExactlyInAnyOrder("status", "from", "to", "search", "page", "pageSize", "sort");
		assertThat(result).bodyJson()
			.extractingPath("$.paths['/orders'].get.parameters[?(@.name == 'status')].schema.type")
			.asArray()
			.containsExactly("array");
	}

	@Test
	void requiresEveryOrderComponentButTheNullableOnes() {
		String path = OpenApiConstants.DOCS_PATH;

		MvcTestResult result = mvc.get().uri(path).exchange();

		assertThat(result).bodyJson()
			.extractingPath("$.components.schemas.PlaceOrderRequest.required")
			.asArray()
			.containsExactlyInAnyOrder("items", "deliveryAddress");
		assertThat(result).bodyJson()
			.extractingPath("$.components.schemas.DeliveryAddressRequest.required")
			.asArray()
			.containsExactlyInAnyOrder("zipCode", "street", "number", "neighborhood", "city", "state");
		assertThat(result).bodyJson()
			.extractingPath("$.components.schemas.CustomerOrderDetailResponse.required")
			.asArray()
			.containsExactlyInAnyOrder("id", "number", "status", "placedAt", "live", "items", "total",
					"deliveryAddress", "history");
		assertThat(result).bodyJson()
			.extractingPath("$.components.schemas.CustomerOrderDetailResponse.properties.cancellation.oneOf[1].type")
			.isEqualTo("null");
		assertThat(result).bodyJson()
			.extractingPath("$.components.schemas.StaffOrderDetailResponse.required")
			.asArray()
			.containsExactlyInAnyOrder("id", "number", "status", "placedAt", "live", "customer", "items", "total",
					"deliveryAddress", "history");
		assertThat(result).bodyJson()
			.extractingPath("$.components.schemas.StaffOrderSummaryResponse.required")
			.asArray()
			.containsExactlyInAnyOrder("id", "number", "status", "placedAt", "total", "live", "customerName", "items");
		assertThat(result).bodyJson()
			.extractingPath("$.components.schemas.CustomerOrderHistoryEntry.required")
			.asArray()
			.containsExactlyInAnyOrder("toStatus", "occurredAt", "actorSide");
		assertThat(result).bodyJson()
			.extractingPath("$.components.schemas.CustomerOrderHistoryEntry.properties.fromStatus.type")
			.asArray()
			.containsExactlyInAnyOrder("string", "null");
		assertThat(result).bodyJson()
			.extractingPath("$.components.schemas.StaffOrderHistoryEntry.required")
			.asArray()
			.containsExactlyInAnyOrder("toStatus", "occurredAt", "actorSide", "actorName");
		assertThat(result).bodyJson()
			.extractingPath("$.components.schemas.OrderCancellationResponse.required")
			.asArray()
			.containsExactlyInAnyOrder("cancelledBy", "reason");
	}

	@Test
	void publishesThePaginationParametersTheApiReads() {
		String path = OpenApiConstants.DOCS_PATH;

		MvcTestResult result = mvc.get().uri(path).exchange();

		assertThat(result).bodyJson()
			.extractingPath("$.paths['/admin/users'].get.parameters[*].name")
			.asArray()
			.containsExactlyInAnyOrder("search", "role", "status", "page", "pageSize", "sort");
		assertThat(result).bodyJson()
			.extractingPath("$.paths['/admin/users'].get.parameters[?(@.name == 'pageSize')].schema.enum[*]")
			.asArray()
			.containsExactly(10, 25, 50, 100);
		assertThat(result).bodyJson()
			.extractingPath("$.paths['/admin/users'].get.parameters[?(@.name == 'page')].schema.minimum")
			.asArray()
			.containsExactly(1);
	}

}
