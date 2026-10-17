package com.foody.api.shared.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.foody.testsupport.persistence.SampleActivatableEntity;
import org.junit.jupiter.api.Test;

class ActivationStatusTest {

	@Test
	void anActiveEntityIsActive() {
		SampleActivatableEntity entity = new SampleActivatableEntity("supplier");

		ActivationStatus status = ActivationStatus.of(entity);

		assertThat(status).isEqualTo(ActivationStatus.ACTIVE);
	}

	@Test
	void aDisabledEntityIsDisabled() {
		SampleActivatableEntity entity = new SampleActivatableEntity("supplier");
		entity.disable();

		ActivationStatus status = ActivationStatus.of(entity);

		assertThat(status).isEqualTo(ActivationStatus.DISABLED);
	}

}
