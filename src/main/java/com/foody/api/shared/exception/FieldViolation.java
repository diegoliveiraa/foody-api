package com.foody.api.shared.exception;

public record FieldViolation(String field, String message) {
}
