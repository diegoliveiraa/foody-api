package com.foody.seed.user;

import static org.assertj.core.api.Assertions.assertThat;

import com.foody.api.auth.repository.RefreshTokenRepository;
import com.foody.api.shared.security.Role;
import com.foody.api.user.entity.User;
import com.foody.api.user.repository.UserRepository;
import com.foody.seed.SeedProperties;
import com.foody.seed.SeedRunner;
import com.foody.seed.SeedScale;
import com.foody.testsupport.api.ApiIntegrationTest;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionOperations;

@ApiIntegrationTest
class IdentitySeederTest {

	private static final String ADMIN_PASSWORD = "admin-seed-pass-1";

	private static final String DEFAULT_PASSWORD = "default-seed-pass-1";

	private static final IdentitySeedProperties PROPERTIES = new IdentitySeedProperties(
			new IdentitySeedProperties.Admin("admin.seed@foody.test", ADMIN_PASSWORD),
			new IdentitySeedProperties.Account("staff.seed@foody.test"),
			new IdentitySeedProperties.Account("customer.seed@foody.test"), DEFAULT_PASSWORD);

	private static final long RANDOM_SEED = 42;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@Autowired
	private UserFactory users;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private TransactionOperations transactions;

	private IdentitySeeder seeder;

	@BeforeEach
	void startFromAnEmptyUsersTable() {
		refreshTokenRepository.deleteAllInBatch();
		userRepository.deleteAllInBatch();
		seeder = new IdentitySeeder(userRepository, users, passwordEncoder, PROPERTIES);
	}

	@Test
	void identitySeederCreatesConfiguredAccountsAndCustomers() {
		SeedScale scale = SeedScale.SMALL;

		seeder.seed(scale);

		User admin = userRepository.findByEmail(PROPERTIES.admin().email()).orElseThrow();
		User staff = userRepository.findByEmail(PROPERTIES.staff().email()).orElseThrow();
		User customer = userRepository.findByEmail(PROPERTIES.customer().email()).orElseThrow();
		assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
		assertThat(staff.getRole()).isEqualTo(Role.STAFF);
		assertThat(customer.getRole()).isEqualTo(Role.CUSTOMER);
		assertThat(passwordEncoder.matches(ADMIN_PASSWORD, admin.getPasswordHash())).isTrue();
		assertThat(passwordEncoder.matches(DEFAULT_PASSWORD, staff.getPasswordHash())).isTrue();
		assertThat(passwordEncoder.matches(DEFAULT_PASSWORD, customer.getPasswordHash())).isTrue();
		assertThat(countByRole()).containsExactlyInAnyOrderEntriesOf(Map.of(Role.ADMIN, 1L, Role.STAFF,
				1L + UserSeedConstants.EXTRA_STAFF_COUNT, Role.CUSTOMER, 1L + UserSeedConstants.CUSTOMER_BASE_COUNT));
		assertThat(userRepository.findAll()).allMatch(User::isActive)
			.extracting(User::getEmail)
			.allMatch(email -> email.equals(email.toLowerCase()))
			.doesNotHaveDuplicates();
	}

	@Test
	void identitySeederScalesTheCustomers() {
		SeedScale scale = SeedScale.MEDIUM;

		seeder.seed(scale);

		assertThat(countByRole()).containsEntry(Role.CUSTOMER, 1L + scale.of(UserSeedConstants.CUSTOMER_BASE_COUNT));
	}

	@Test
	void identitySeederIsIdempotent() {
		SeedRunner runner = new SeedRunner(List.of(seeder), new SeedProperties(SeedScale.SMALL, RANDOM_SEED),
				transactions);
		boolean seededBefore = seeder.alreadySeeded();
		runner.run(new DefaultApplicationArguments());
		long usersAfterFirstRun = userRepository.count();

		runner.run(new DefaultApplicationArguments());

		assertThat(seededBefore).isFalse();
		assertThat(seeder.alreadySeeded()).isTrue();
		assertThat(userRepository.count()).isEqualTo(usersAfterFirstRun);
	}

	private Map<Role, Long> countByRole() {
		return userRepository.findAll().stream().collect(Collectors.groupingBy(User::getRole, Collectors.counting()));
	}

}
