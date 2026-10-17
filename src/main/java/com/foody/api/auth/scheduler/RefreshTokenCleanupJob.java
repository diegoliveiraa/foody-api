package com.foody.api.auth.scheduler;

import com.foody.api.auth.AuthConstants;
import com.foody.api.auth.service.RefreshTokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RefreshTokenCleanupJob {

	private final RefreshTokenService refreshTokens;

	public RefreshTokenCleanupJob(RefreshTokenService refreshTokens) {
		this.refreshTokens = refreshTokens;
	}

	@Scheduled(cron = AuthConstants.REFRESH_TOKEN_CLEANUP_CRON)
	public void deleteExpiredFamilies() {
		int deleted = refreshTokens.deleteExpiredFamilies();
		log.info(AuthConstants.EXPIRED_FAMILIES_DELETED_LOG, deleted);
	}

}
