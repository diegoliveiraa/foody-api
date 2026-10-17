package com.foody.api.user.service;

import com.foody.api.shared.security.ActiveUserLookup;
import com.foody.api.shared.security.Role;
import com.foody.api.user.entity.User;
import com.foody.api.user.exception.UserNotFoundException;
import com.foody.api.user.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService implements ActiveUserLookup {

	private final UserRepository repository;

	public UserService(UserRepository repository) {
		this.repository = repository;
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Role> findActiveRole(UUID userId) {
		return repository.findById(userId).filter(User::isActive).map(User::getRole);
	}

	@Transactional(readOnly = true)
	public boolean existsByEmail(String email) {
		return repository.existsByEmail(email);
	}

	@Transactional(readOnly = true)
	public Optional<User> findByEmail(String email) {
		return repository.findByEmail(email);
	}

	@Transactional
	public User createCustomer(String name, String email, String passwordHash) {
		return repository.save(new User(name, email, passwordHash, Role.CUSTOMER));
	}

	@Transactional(readOnly = true)
	public User getActive(UUID userId) {
		return repository.findById(userId).filter(User::isActive).orElseThrow(() -> new UserNotFoundException(userId));
	}

}
