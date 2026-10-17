package com.foody.api.shared.probe;

import com.foody.api.shared.message.Messages;
import com.foody.api.shared.pagination.PageResponse;
import com.foody.api.shared.pagination.SortGuard;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@TestComponent
@RestController
@RequestMapping(ProbeConstants.BASE_PATH)
public class ProbeController {

	@PostMapping("/items")
	public ProbeRequest create(@Valid @RequestBody ProbeRequest request) {
		return request;
	}

	@GetMapping("/items/{id}")
	public ProbeRequest find(@PathVariable UUID id) {
		throw new ProbeNotFoundException(id);
	}

	@GetMapping("/failure")
	public ProbeRequest fail() {
		throw new IllegalStateException(ProbeConstants.SECRET_DETAIL);
	}

	@GetMapping("/concurrent-update")
	public ProbeRequest concurrentUpdate() {
		throw new ObjectOptimisticLockingFailureException(ProbeRequest.class, UUID.randomUUID());
	}

	@GetMapping("/page")
	public PageResponse<String> page(Pageable pageable) {
		Pageable allowed = SortGuard.requireAllowed(pageable, ProbeConstants.SORTABLE_PROPERTIES);
		return PageResponse.of(new PageImpl<>(ProbeConstants.ITEMS, allowed, ProbeConstants.TOTAL_ITEMS));
	}

	@GetMapping("/unauthorized")
	public ProbeRequest unauthorized() {
		throw new ProbeUnauthorizedException();
	}

	@GetMapping("/unauthenticated")
	public ProbeRequest unauthenticated() {
		throw new InsufficientAuthenticationException(ProbeConstants.SECRET_DETAIL);
	}

	@GetMapping("/access-denied")
	public ProbeRequest accessDenied() {
		throw new AccessDeniedException(ProbeConstants.SECRET_DETAIL);
	}

	@GetMapping("/too-many-requests")
	public ProbeRequest tooManyRequests() {
		throw new ProbeTooManyRequestsException(ProbeConstants.RETRY_AFTER);
	}

}
