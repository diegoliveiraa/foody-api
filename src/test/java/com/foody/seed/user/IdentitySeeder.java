package com.foody.seed.user;

import com.foody.api.shared.security.Role;
import com.foody.api.user.entity.User;
import com.foody.api.user.repository.UserRepository;
import com.foody.seed.SeedConstants;
import com.foody.seed.SeedScale;
import com.foody.seed.Seeder;
import java.util.List;
import java.util.stream.Stream;
import net.datafaker.Faker;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;

@Order(1)
public class IdentitySeeder implements Seeder {

	private final UserRepository repository;

	private final UserFactory users;

	private final Faker faker;

	private final PasswordEncoder passwordEncoder;

	private final IdentitySeedProperties properties;

	public IdentitySeeder(UserRepository repository, UserFactory users, Faker faker, PasswordEncoder passwordEncoder,
			IdentitySeedProperties properties) {
		this.repository = repository;
		this.users = users;
		this.faker = faker;
		this.passwordEncoder = passwordEncoder;
		this.properties = properties;
	}

	@Override
	public boolean alreadySeeded() {
		return repository.count() > 0;
	}

	@Override
	public void seed(SeedScale scale) {
		String adminPasswordHash = passwordEncoder.encode(properties.admin().password());
		String defaultPasswordHash = passwordEncoder.encode(properties.defaultPassword());
		repository.save(users.build(properties.admin().email(), Role.ADMIN, adminPasswordHash));
		repository.save(users.build(properties.staff().email(), Role.STAFF, defaultPasswordHash));
		repository.save(users.build(properties.customer().email(), Role.CUSTOMER, defaultPasswordHash));
		repository.saveAll(many(UserSeedConstants.EXTRA_STAFF_COUNT, Role.STAFF, defaultPasswordHash));
		repository.saveAll(withSomeDisabled(
				many(scale.of(UserSeedConstants.CUSTOMER_BASE_COUNT), Role.CUSTOMER, defaultPasswordHash)));
	}

	private List<User> withSomeDisabled(List<User> customers) {
		int disabledCount = customers.size() * UserSeedConstants.DISABLED_CUSTOMER_PERCENT / SeedConstants.PERCENT;
		Stream.generate(() -> faker.random().nextInt(customers.size()))
			.distinct()
			.limit(disabledCount)
			.forEach(index -> customers.get(index).disable());
		return customers;
	}

	private List<User> many(int count, Role role, String passwordHash) {
		return Stream.generate(() -> users.build(role, passwordHash)).limit(count).toList();
	}

}
