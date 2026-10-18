package com.foody.seed.user;

import static org.assertj.core.api.Assertions.assertThat;

import com.foody.api.auth.repository.RefreshTokenRepository;
import com.foody.api.order.repository.OrderRepository;
import com.foody.api.shared.security.Role;
import com.foody.api.user.entity.User;
import com.foody.api.user.repository.UserRepository;
import com.foody.seed.SeedConstants;
import com.foody.seed.SeedProperties;
import com.foody.seed.SeedRunner;
import com.foody.seed.SeedScale;
import com.foody.testsupport.api.ApiIntegrationTest;
import java.text.Collator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import net.datafaker.Faker;
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

	private static final Set<String> FIXED_EMAILS = Set.of(PROPERTIES.admin().email(), PROPERTIES.staff().email(),
			PROPERTIES.customer().email());

	private static final Set<String> HONORIFICS = Set.of("Sr.", "Sra.", "Srta.", "Dr.", "Dra.", "Jr.", "Neto", "Filho");

	private static final Pattern DERIVED_EMAIL = Pattern.compile("^([a-z]+)\\.([a-z]+)\\.\\d+@(.+)$");

	private static final int FIRST_NAME_GROUP = 1;

	private static final int LAST_NAME_GROUP = 2;

	private static final int DOMAIN_GROUP = 3;

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@Autowired
	private UserFactory users;

	@Autowired
	private Faker faker;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private TransactionOperations transactions;

	private IdentitySeeder seeder;

	@BeforeEach
	void startFromAnEmptyUsersTable() {
		orderRepository.deleteAllInBatch();
		refreshTokenRepository.deleteAllInBatch();
		userRepository.deleteAllInBatch();
		seeder = new IdentitySeeder(userRepository, users, faker, passwordEncoder, PROPERTIES);
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
		assertThat(List.of(admin, staff, customer)).allMatch(User::isActive);
		assertThat(userRepository.findAll()).extracting(User::getEmail)
			.allMatch(email -> email.equals(email.toLowerCase()))
			.doesNotHaveDuplicates();
	}

	@Test
	void seededNamesHaveNoHonorifics() {
		SeedScale scale = SeedScale.SMALL;

		seeder.seed(scale);

		assertThat(userRepository.findAll()).extracting(User::getName)
			.isNotEmpty()
			.allSatisfy(name -> assertThat(wordsOf(name)).hasSizeGreaterThanOrEqualTo(2)
				.doesNotContainAnyElementsOf(HONORIFICS));
	}

	@Test
	void seededEmailsAreDerivedFromTheName() {
		SeedScale scale = SeedScale.SMALL;

		seeder.seed(scale);

		assertThat(userRepository.findAll()).filteredOn(user -> !FIXED_EMAILS.contains(user.getEmail()))
			.hasSize(UserSeedConstants.EXTRA_STAFF_COUNT + scale.of(UserSeedConstants.CUSTOMER_BASE_COUNT))
			.allSatisfy(user -> {
				Matcher email = DERIVED_EMAIL.matcher(user.getEmail());
				assertThat(email.matches()).as(user.getEmail()).isTrue();
				assertThat(UserSeedConstants.EMAIL_DOMAINS).contains(email.group(DOMAIN_GROUP));
				assertThat(splitsInto(user.getName(), email.group(FIRST_NAME_GROUP), email.group(LAST_NAME_GROUP)))
					.as(user.getName() + " -> " + user.getEmail())
					.isTrue();
			});
	}

	@Test
	void identitySeederScalesTheCustomers() {
		SeedScale scale = SeedScale.MEDIUM;

		seeder.seed(scale);

		assertThat(countByRole()).containsEntry(Role.CUSTOMER, 1L + scale.of(UserSeedConstants.CUSTOMER_BASE_COUNT));
	}

	@Test
	void identitySeederDisablesSomeGeneratedCustomersOnly() {
		SeedScale scale = SeedScale.MEDIUM;

		seeder.seed(scale);

		List<User> disabled = userRepository.findAll().stream().filter(user -> !user.isActive()).toList();
		assertThat(disabled).hasSize(25)
			.allMatch(user -> user.getRole() == Role.CUSTOMER)
			.extracting(User::getEmail)
			.doesNotContain(PROPERTIES.admin().email(), PROPERTIES.staff().email(), PROPERTIES.customer().email());
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

	private List<String> wordsOf(String name) {
		return List.of(name.split(UserSeedConstants.NAME_SEPARATOR));
	}

	private boolean splitsInto(String name, String firstName, String lastName) {
		List<String> words = wordsOf(name);
		return IntStream.range(1, words.size())
			.anyMatch(split -> samePrimaryLetters(words.subList(0, split), firstName)
					&& samePrimaryLetters(words.subList(split, words.size()), lastName));
	}

	private boolean samePrimaryLetters(List<String> words, String normalized) {
		Collator collator = Collator.getInstance(SeedConstants.LOCALE);
		collator.setStrength(Collator.PRIMARY);
		return collator.compare(String.join("", words), normalized) == 0;
	}

}
