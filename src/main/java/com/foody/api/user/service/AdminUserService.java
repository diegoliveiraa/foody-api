package com.foody.api.user.service;

import com.foody.api.shared.pagination.PageRequests;
import com.foody.api.shared.security.Role;
import com.foody.api.user.UserConstants;
import com.foody.api.user.dto.AdminUserFilter;
import com.foody.api.user.dto.AdminUserResponse;
import com.foody.api.user.entity.User;
import com.foody.api.user.event.UserDisabledEvent;
import com.foody.api.user.exception.CannotChangeOwnAccountException;
import com.foody.api.user.exception.LastActiveAdminException;
import com.foody.api.user.exception.UserNotFoundException;
import com.foody.api.user.repository.UserRepository;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserService {

	private final UserRepository repository;

	private final ApplicationEventPublisher events;

	public AdminUserService(UserRepository repository, ApplicationEventPublisher events) {
		this.repository = repository;
		this.events = events;
	}

	@Transactional(readOnly = true)
	public Page<AdminUserResponse> list(AdminUserFilter filter, Pageable pageable) {
		Pageable sorted = PageRequests.sortedOrDefault(pageable, UserConstants.DEFAULT_SORT);
		return repository.findAllMatching(filter, sorted).map(AdminUserResponse::of);
	}

	@Transactional
	public AdminUserResponse changeRole(UUID callerId, UUID userId, Role role) {
		User user = findOtherThanCaller(callerId, userId);
		if (user.getRole() == role) {
			return AdminUserResponse.of(user);
		}
		requireAnotherActiveAdmin(user);
		user.changeRole(role);
		return AdminUserResponse.of(user);
	}

	@Transactional
	public AdminUserResponse disable(UUID callerId, UUID userId) {
		User user = findOtherThanCaller(callerId, userId);
		if (!user.isActive()) {
			return AdminUserResponse.of(user);
		}
		requireAnotherActiveAdmin(user);
		user.disable();
		events.publishEvent(new UserDisabledEvent(user.getId()));
		return AdminUserResponse.of(user);
	}

	@Transactional
	public AdminUserResponse enable(UUID callerId, UUID userId) {
		User user = findOtherThanCaller(callerId, userId);
		user.enable();
		return AdminUserResponse.of(user);
	}

	private User findOtherThanCaller(UUID callerId, UUID userId) {
		if (callerId.equals(userId)) {
			throw new CannotChangeOwnAccountException();
		}
		return repository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
	}

	private void requireAnotherActiveAdmin(User user) {
		if (user.getRole() != Role.ADMIN || !user.isActive()) {
			return;
		}
		boolean anotherRemains = repository.lockActiveByRole(Role.ADMIN)
			.stream()
			.anyMatch(admin -> !admin.equals(user));
		if (!anotherRemains) {
			throw new LastActiveAdminException();
		}
	}

}
