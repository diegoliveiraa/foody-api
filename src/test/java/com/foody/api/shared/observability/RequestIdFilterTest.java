package com.foody.api.shared.observability;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestIdFilterTest {

	private static final String UUID_PATTERN = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

	private final RequestIdFilter filter = new RequestIdFilter();

	@Test
	void keepsASafeRequestIdTheClientSentForTheWholeRequest() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(ObservabilityConstants.REQUEST_ID_HEADER, "checkout-42");
		MockHttpServletResponse response = new MockHttpServletResponse();
		AtomicReference<String> duringRequest = new AtomicReference<>();

		filter.doFilter(request, response, capturing(duringRequest));

		assertThat(duringRequest.get()).isEqualTo("checkout-42");
		assertThat(response.getHeader(ObservabilityConstants.REQUEST_ID_HEADER)).isEqualTo("checkout-42");
	}

	@Test
	void generatesARequestIdWhenTheClientSentNone() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		AtomicReference<String> duringRequest = new AtomicReference<>();

		filter.doFilter(request, response, capturing(duringRequest));

		assertThat(duringRequest.get()).matches(UUID_PATTERN);
		assertThat(response.getHeader(ObservabilityConstants.REQUEST_ID_HEADER)).isEqualTo(duringRequest.get());
	}

	@Test
	void replacesARequestIdThatIsUnsafeToLog() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(ObservabilityConstants.REQUEST_ID_HEADER, "abc\nFAKE LOG LINE");
		MockHttpServletResponse response = new MockHttpServletResponse();
		AtomicReference<String> duringRequest = new AtomicReference<>();

		filter.doFilter(request, response, capturing(duringRequest));

		assertThat(duringRequest.get()).matches(UUID_PATTERN);
	}

	@Test
	void clearsTheRequestIdOnceTheRequestIsOver() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(ObservabilityConstants.REQUEST_ID_HEADER, "checkout-42");

		filter.doFilter(request, new MockHttpServletResponse(), capturing(new AtomicReference<>()));

		assertThat(MDC.get(ObservabilityConstants.REQUEST_ID_KEY)).isNull();
	}

	private FilterChain capturing(AtomicReference<String> duringRequest) {
		return (request, response) -> duringRequest.set(MDC.get(ObservabilityConstants.REQUEST_ID_KEY));
	}

}
