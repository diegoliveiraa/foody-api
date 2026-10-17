package com.foody.api.user.entity;

import com.foody.api.shared.persistence.ActivatableEntity;
import com.foody.api.shared.security.Role;
import com.foody.api.shared.util.EmailUtils;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends ActivatableEntity {

	@Column(nullable = false)
	private String name;

	@Column(nullable = false)
	private String email;

	@Column(nullable = false)
	private String passwordHash;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Role role;

	public User(String name, String email, String passwordHash, Role role) {
		this.name = name;
		this.email = EmailUtils.normalize(email);
		this.passwordHash = passwordHash;
		this.role = role;
	}

	public void changeRole(Role newRole) {
		this.role = newRole;
	}

	public void changePassword(String newPasswordHash) {
		this.passwordHash = newPasswordHash;
	}

}
