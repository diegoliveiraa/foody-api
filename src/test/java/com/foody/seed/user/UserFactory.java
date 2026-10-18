package com.foody.seed.user;

import com.foody.api.shared.security.Role;
import com.foody.api.user.entity.User;
import com.foody.api.user.repository.UserRepository;
import com.foody.seed.EntityFactory;
import java.text.Normalizer;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
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
		String firstName = faker.name().firstName();
		String lastName = faker.name().lastName();
		return new User(fullName(firstName, lastName), uniqueEmail(firstName, lastName), passwordHash, role);
	}

	public User build(String email, Role role, String passwordHash) {
		return new User(fullName(faker.name().firstName(), faker.name().lastName()), email, passwordHash, role);
	}

	public User create(String name, String email) {
		return save(build(name, email));
	}

	public User create(String name, String email, Consumer<User> customizer) {
		User user = build(name, email);
		customizer.accept(user);
		return save(user);
	}

	private User build(String name, String email) {
		return new User(name, email, unusablePasswordHash, Role.CUSTOMER);
	}

	private String fullName(String firstName, String lastName) {
		return firstName + UserSeedConstants.NAME_SEPARATOR + lastName;
	}

	private String uniqueEmail(String firstName, String lastName) {
		return emailWord(firstName) + UserSeedConstants.EMAIL_PART_SEPARATOR + emailWord(lastName)
				+ UserSeedConstants.EMAIL_PART_SEPARATOR + sequence.incrementAndGet()
				+ UserSeedConstants.EMAIL_DOMAIN_SEPARATOR
				+ faker.options().nextElement(UserSeedConstants.EMAIL_DOMAINS);
	}

	private String emailWord(String name) {
		String decomposed = Normalizer.normalize(name.toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
		return UserSeedConstants.NON_EMAIL_LETTERS.matcher(decomposed).replaceAll("");
	}

}
