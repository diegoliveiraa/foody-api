package com.foody.seed.user;

import com.foody.api.shared.security.Role;
import com.foody.api.user.entity.User;
import com.foody.api.user.repository.UserRepository;
import com.foody.seed.SeedScale;
import com.foody.seed.Seeder;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;

@Order(1)
public class IdentitySeeder implements Seeder {

	private final UserRepository repository;

	private final UserFactory users;

	private final PasswordEncoder passwordEncoder;

	private final IdentitySeedProperties properties;

	public IdentitySeeder(UserRepository repository, UserFactory users, PasswordEncoder passwordEncoder,
			IdentitySeedProperties properties) {
		this.repository = repository;
		this.users = users;
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
		repository.saveAll(many(scale.of(UserSeedConstants.CUSTOMER_BASE_COUNT), Role.CUSTOMER, defaultPasswordHash));
	}

	private List<User> many(int count, Role role, String passwordHash) {
		return Stream.generate(() -> users.build(role, passwordHash)).limit(count).toList();
	}

}
