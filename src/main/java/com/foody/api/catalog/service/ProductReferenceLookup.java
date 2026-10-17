package com.foody.api.catalog.service;

import java.util.UUID;

public interface ProductReferenceLookup {

	boolean isReferenced(UUID productId);

}
