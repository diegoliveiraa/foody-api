package com.foody.seed.user;

import com.foody.api.shared.security.Role;
import com.foody.api.user.entity.User;
import com.foody.api.user.repository.UserRepository;
import com.foody.seed.EntityFactory;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import net.datafaker.Faker;
import org.springframework.security.crypto.password.PasswordEncoder;

public class UserFactory extends EntityFactory<User> {

	private final Faker faker;

	private final String unusablePasswordHash;

	private final AtomicInteger sequence = new AtomicInteger();

	public UserFactory(UserRepository repository, Faker faker, PasswordEncoder passwordEncoder) {
		super(repository);
		this.faker = faker;
		this.unusablePasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());
	}

	@Override
	public User build() {
		return build(Role.CUSTOMER, unusablePasswordHash);
	}

	public User build(Role role, String passwordHash) {
		return build(uniqueEmail(), role, passwordHash);
	}

	public User build(String email, Role role, String passwordHash) {
		return new User(faker.name().fullName(), email, passwordHash, role);
	}

	private String uniqueEmail() {
		String email = faker.internet().emailAddress();
		String uniqueSuffix = UserSeedConstants.EMAIL_SEQUENCE_SEPARATOR + sequence.incrementAndGet()
				+ UserSeedConstants.EMAIL_DOMAIN_SEPARATOR;
		return email.replace(UserSeedConstants.EMAIL_DOMAIN_SEPARATOR, uniqueSuffix);
	}

}
