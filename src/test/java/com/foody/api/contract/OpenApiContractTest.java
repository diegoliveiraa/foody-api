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
					"deliveryAddress", "history", "canCancel", "canRequestCancellation", "cancellationReasons");
		assertThat(result).bodyJson()
			.extractingPath("$.components.schemas.CustomerOrderDetailResponse.properties.cancellation.oneOf[1].type")
			.isEqualTo("null");
		assertThat(result).bodyJson()
			.extractingPath("$.components.schemas.StaffOrderDetailResponse.required")
			.asArray()
			.containsExactlyInAnyOrder("id", "number", "status", "placedAt", "live", "customer", "items", "total",
					"deliveryAddress", "history", "allowedTransitions", "canCancel", "cancellationReasons");
		assertThat(result).bodyJson()
			.extractingPath("$.components.schemas.StaffOrderSummaryResponse.required")
			.asArray()
			.containsExactlyInAnyOrder("id", "number", "status", "placedAt", "total", "live", "customerName", "items",
					"hasPendingCancellationRequest", "allowedTransitions", "canCancel", "cancellationReasons");
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
	void publishesTheOrderLifecycleEndpointsNamedAfterTheirControllers() {
		String path = OpenApiConstants.DOCS_PATH;

		MvcTestResult result = mvc.get().uri(path).exchange();

		assertThat(result).bodyJson()
			.extractingPath("$.paths['/orders/{id}/status'].patch.operationId")
			.isEqualTo("orderUpdateStatus");
		assertThat(result).bodyJson()
			.extractingPath("$.paths['/orders/{id}/cancel'].post.operationId")
			.isEqualTo("orderCancel");
		assertThat(result).bodyJson()
			.extractingPath("$.paths['/orders/{id}/cancellation-request/approve'].post.operationId")
			.isEqualTo("orderApproveCancellation");
		assertThat(result).bodyJson()
			.extractingPath("$.paths['/orders/{id}/cancellation-request/decline'].post.operationId")
			.isEqualTo("orderDeclineCancellation");
		assertThat(result).bodyJson()
			.extractingPath("$.paths['/my-orders/{id}/cancel'].post.operationId")
			.isEqualTo("myOrderCancel");
		assertThat(result).bodyJson()
			.extractingPath("$.paths['/my-orders/{id}/cancellation-request'].post.operationId")
			.isEqualTo("myOrderRequestCancellation");
		assertThat(result).bodyJson()
			.extractingPath("$.paths['/orders/{id}/cancellation-request/decline'].post.requestBody.required")
			.isEqualTo(true);
	}

	@Test
	void requiresEveryLifecycleComponentButTheNullableOnes() {
		String path = OpenApiConstants.DOCS_PATH;

		MvcTestResult result = mvc.get().uri(path).exchange();

		assertThat(result).bodyJson()
			.extractingPath("$.components.schemas.UpdateOrderStatusRequest.required")
			.asArray()
			.containsExactly("status");
		assertThat(result).bodyJson()
			.extractingPath("$.components.schemas.CancelOrderRequest.required")
			.asArray()
			.containsExactly("reason");
		assertThat(result).bodyJson()
			.extractingPath("$.components.schemas.CancelOrderRequest.properties.note.type")
			.asArray()
			.containsExactlyInAnyOrder("string", "null");
		assertThat(result).bodyJson().doesNotHavePath("$.components.schemas.CancellationDecisionRequest.required");
		assertThat(result).bodyJson()
			.extractingPath("$.components.schemas.StaffCancellationRequestResponse.required")
			.asArray()
			.containsExactlyInAnyOrder("status", "reason", "requestedAt");
		assertThat(result).bodyJson()
			.extractingPath("$.components.schemas.CustomerCancellationRequestResponse.required")
			.asArray()
			.containsExactlyInAnyOrder("status", "reason", "requestedAt");
		assertThat(result).bodyJson()
			.doesNotHavePath("$.components.schemas.CustomerCancellationRequestResponse.properties.decidedByName");
		assertThat(result).bodyJson()
			.extractingPath(
					"$.components.schemas.StaffOrderDetailResponse.properties.cancellationRequest.oneOf[1].type")
			.isEqualTo("null");
		assertThat(result).bodyJson()
			.extractingPath("$.components.schemas.CustomerOrderSummaryResponse.required")
			.asArray()
			.containsExactlyInAnyOrder("id", "number", "status", "placedAt", "total", "live", "items");
	}

	@Test
	void publishesTheDashboardOverviewNamedAfterItsController() {
		String path = OpenApiConstants.DOCS_PATH;

		MvcTestResult result = mvc.get().uri(path).exchange();

		assertThat(result).bodyJson()
			.extractingPath("$.paths['/dashboard'].get.operationId")
			.isEqualTo("dashboardOverview");
		assertThat(result).bodyJson()
			.extractingPath("$.paths['/dashboard'].get.parameters[*].name")
			.asArray()
			.containsExactly("period");
		assertThat(result).bodyJson()
			.extractingPath("$.paths['/dashboard'].get.parameters[0].required")
			.isEqualTo(false);
		assertThat(result).bodyJson()
			.extractingPath("$.paths['/dashboard'].get.parameters[0].schema.default")
			.isEqualTo("TODAY");
		assertThat(result).bodyJson()
			.extractingPath("$.paths['/dashboard'].get.parameters[0].schema.enum")
			.asArray()
			.containsExactly("TODAY", "LAST_7_DAYS", "LAST_30_DAYS");
		assertThat(result).bodyJson()
			.extractingPath("$.paths['/dashboard'].get.security[0]")
			.asMap()
			.containsOnlyKeys(OpenApiConstants.BEARER_SCHEME);
	}

	@Test
	void requiresEveryDashboardComponentButTheNullableOnes() {
		String path = OpenApiConstants.DOCS_PATH;

		MvcTestResult result = mvc.get().uri(path).exchange();

		assertThat(result).bodyJson()
			.extractingPath("$.components.schemas.DashboardResponse.required")
			.asArray()
			.containsExactlyInAnyOrder("period", "timeZone", "generatedAt", "orders", "revenue", "ordersOverTime",
					"topProducts", "activeByStatus", "averageStageMinutes", "cancellations",
					"pendingCancellationRequests");
		assertThat(result).bodyJson()
			.extractingPath("$.components.schemas.DashboardResponse.properties.averageTicket.type")
			.asArray()
			.containsExactlyInAnyOrder("number", "null");
		assertThat(result).bodyJson()
			.extractingPath("$.components.schemas.DashboardResponse.properties.period.enum")
			.asArray()
			.containsExactly("TODAY", "LAST_7_DAYS", "LAST_30_DAYS");
		assertThat(result).bodyJson()
			.extractingPath("$.components.schemas.DashboardOrders.required")
			.asArray()
			.containsExactlyInAnyOrder("count", "previousCount");
		assertThat(result).bodyJson()
			.extractingPath("$.components.schemas.DashboardOrders.properties.changePercent.type")
			.asArray()
			.containsExactlyInAnyOrder("number", "null");
		assertThat(result).bodyJson()
			.extractingPath("$.components.schemas.DashboardOrdersOverTime.required")
			.asArray()
			.containsExactlyInAnyOrder("granularity", "points");
		assertThat(result).bodyJson()
			.extractingPath("$.components.schemas.DashboardOrdersOverTime.properties.granularity.enum")
			.asArray()
			.containsExactly("HOUR", "DAY");
		assertThat(result).bodyJson()
			.extractingPath("$.components.schemas.DashboardBucket.required")
			.asArray()
			.containsExactlyInAnyOrder("bucketStart", "count");
		assertThat(result).bodyJson()
			.extractingPath("$.components.schemas.DashboardTopProduct.required")
			.asArray()
			.containsExactlyInAnyOrder("productId", "name", "quantity", "revenue");
		assertThat(result).bodyJson()
			.extractingPath("$.components.schemas.DashboardActiveOrders.required")
			.asArray()
			.containsExactlyInAnyOrder("received", "preparing", "outForDelivery");
		assertThat(result).bodyJson().doesNotHavePath("$.components.schemas.DashboardStageMinutes.required");
		assertThat(result).bodyJson()
			.extractingPath("$.components.schemas.DashboardStageMinutes.properties.receivedToPreparing.type")
			.asArray()
			.containsExactlyInAnyOrder("integer", "null");
		assertThat(result).bodyJson()
			.extractingPath("$.components.schemas.DashboardCancellations.required")
			.asArray()
			.containsExactlyInAnyOrder("count", "byReason");
		assertThat(result).bodyJson()
			.extractingPath("$.components.schemas.DashboardCancellations.properties.ratePercent.type")
			.asArray()
			.containsExactlyInAnyOrder("number", "null");
		assertThat(result).bodyJson()
			.extractingPath("$.components.schemas.DashboardCancellationCount.required")
			.asArray()
			.containsExactlyInAnyOrder("reason", "cancelledBy", "count");
		assertThat(result).bodyJson()
			.extractingPath("$.components.schemas.DashboardPendingRequests.required")
			.asArray()
			.containsExactlyInAnyOrder("count", "items");
		assertThat(result).bodyJson()
			.extractingPath("$.components.schemas.DashboardPendingRequest.required")
			.asArray()
			.containsExactlyInAnyOrder("orderId", "number", "requestedAt");
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
