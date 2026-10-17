package com.foody.api.auth.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import com.foody.api.auth.repository.RefreshTokenRepository;
import com.foody.api.shared.util.HashUtils;
import com.foody.seed.user.UserFactory;
import com.foody.testsupport.api.ApiIntegrationTest;
import com.foody.testsupport.security.AuthClient;
import com.foody.testsupport.security.TestCredentials;
import com.foody.testsupport.time.MutableClock;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

@ApiIntegrationTest
class RefreshTokenCleanupJobTest {

	@Autowired
	private RefreshTokenCleanupJob cleanupJob;

	@Autowired
	private AuthClient authClient;

	@Autowired
	private UserFactory users;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private RefreshTokenRepository refreshTokens;

	@Autowired
	private MutableClock clock;

	@AfterEach
	void resetClock() {
		clock.reset();
	}

	@Test
	void refreshTokenCleanupDeletesExpiredFamiliesOnly() {
		String email = users.create(user -> user.changePassword(passwordEncoder.encode(TestCredentials.PASSWORD)))
			.getEmail();
		clock.fixAt(Instant.now().minus(Duration.ofDays(31)));
		String expiredFamilyToken = authClient.refreshTokenOf(authClient.login(email, TestCredentials.PASSWORD));
		clock.reset();
		String rotatedLiveToken = authClient.refreshTokenOf(authClient.login(email, TestCredentials.PASSWORD));
		String currentLiveToken = authClient.refreshTokenOf(authClient.refresh(rotatedLiveToken));

		cleanupJob.deleteExpiredFamilies();

		assertThat(refreshTokens.findByTokenHash(HashUtils.sha256Hex(expiredFamilyToken))).isEmpty();
		assertThat(refreshTokens.findByTokenHash(HashUtils.sha256Hex(rotatedLiveToken))).isPresent();
		assertThat(refreshTokens.findByTokenHash(HashUtils.sha256Hex(currentLiveToken))).isPresent();
	}

}
