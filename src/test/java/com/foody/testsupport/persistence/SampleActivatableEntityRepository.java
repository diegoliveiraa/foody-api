package com.foody.testsupport.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SampleActivatableEntityRepository extends JpaRepository<SampleActivatableEntity, UUID> {

}
