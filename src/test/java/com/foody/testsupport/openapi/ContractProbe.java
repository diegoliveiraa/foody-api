package com.foody.testsupport.openapi;

import org.jspecify.annotations.Nullable;

public record ContractProbe(String name, @Nullable String note, @Nullable ContractProbeDetail detail) {
}
