package com.foody.api.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.awaitility.Awaitility.await;

import com.foody.api.shared.security.Role;
import com.foody.api.user.dto.AdminUserResponse;
import com.foody.api.user.entity.User;
import com.foody.api.user.exception.LastActiveAdminException;
import com.foody.api.user.repository.UserRepository;
import com.foody.seed.user.UserFactory;
import com.foody.testsupport.api.ApiIntegrationTest;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionOperations;

@ApiIntegrationTest
class AdminUserServiceTest {

	private static final String LOCK_WAITS_QUERY = "select count(*) from pg_stat_activity where wait_event_type = 'Lock'";

	private static final Duration LOCK_WAIT_TIMEOUT = Duration.ofSeconds(10);

	@Autowired
	private AdminUserService adminUsers;

	@Autowired
	private UserFactory users;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private TransactionOperations transactions;

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	void cannotDemoteTheLastActiveAdmin() {
		User lastAdmin = users.create(user -> user.changeRole(Role.ADMIN));
		User demotedMeanwhile = users.create(user -> user.changeRole(Role.STAFF));
		disableEveryActiveAdminBut(lastAdmin);

		Throwable thrown = catchThrowable(
				() -> adminUsers.changeRole(demotedMeanwhile.getId(), lastAdmin.getId(), Role.STAFF));

		assertThat(thrown).isInstanceOf(LastActiveAdminException.class);
		assertThat(userRepository.findById(lastAdmin.getId())).map(User::getRole).contains(Role.ADMIN);
	}

	@Test
	void cannotDisableTheLastActiveAdmin() {
		User lastAdmin = users.create(user -> user.changeRole(Role.ADMIN));
		User demotedMeanwhile = users.create(user -> user.changeRole(Role.STAFF));
		disableEveryActiveAdminBut(lastAdmin);

		Throwable thrown = catchThrowable(() -> adminUsers.disable(demotedMeanwhile.getId(), lastAdmin.getId()));

		assertThat(thrown).isInstanceOf(LastActiveAdminException.class);
		assertThat(userRepository.findById(lastAdmin.getId())).map(User::isActive).contains(true);
	}

	@Test
	void anAdminIsDemotedWhileAnotherActiveAdminRemains() {
		User caller = users.create(user -> user.changeRole(Role.ADMIN));
		User target = users.create(user -> user.changeRole(Role.ADMIN));
		disableEveryActiveAdminBut(caller, target);

		AdminUserResponse demoted = adminUsers.changeRole(caller.getId(), target.getId(), Role.STAFF);

		assertThat(demoted.role()).isEqualTo(Role.STAFF);
		assertThat(userRepository.findById(caller.getId())).map(User::getRole).contains(Role.ADMIN);
	}

	@Test
	void twoAdminsDemotingEachOtherAtOnceKeepOneActiveAdmin() {
		User first = users.create(user -> user.changeRole(Role.ADMIN));
		User second = users.create(user -> user.changeRole(Role.ADMIN));
		disableEveryActiveAdminBut(first, second);
		ExecutorService executor = Executors.newSingleThreadExecutor();

		Future<AdminUserResponse> secondDemotesFirst = transactions.execute(status -> {
			adminUsers.changeRole(first.getId(), second.getId(), Role.STAFF);
			Future<AdminUserResponse> concurrent = executor
				.submit(() -> adminUsers.changeRole(second.getId(), first.getId(), Role.STAFF));
			await().atMost(LOCK_WAIT_TIMEOUT).until(() -> concurrent.isDone() || anyTransactionWaitsForALock());
			return concurrent;
		});
		Throwable thrown = catchThrowable(secondDemotesFirst::get);
		executor.shutdown();

		assertThat(thrown).hasCauseInstanceOf(LastActiveAdminException.class);
		assertThat(userRepository.findById(first.getId())).map(User::getRole).contains(Role.ADMIN);
		assertThat(userRepository.findById(second.getId())).map(User::getRole).contains(Role.STAFF);
	}

	private boolean anyTransactionWaitsForALock() {
		return jdbc.queryForObject(LOCK_WAITS_QUERY, Long.class) > 0;
	}

	private void disableEveryActiveAdminBut(User... kept) {
		Set<UUID> keptIds = Stream.of(kept).map(User::getId).collect(Collectors.toSet());
		List<User> otherAdmins = userRepository.findAll()
			.stream()
			.filter(user -> user.getRole() == Role.ADMIN && user.isActive() && !keptIds.contains(user.getId()))
			.toList();
		otherAdmins.forEach(User::disable);
		userRepository.saveAll(otherAdmins);
	}

}
