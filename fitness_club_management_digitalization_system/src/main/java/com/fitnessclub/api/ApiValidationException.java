package com.fitnessclub.api;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import lombok.Getter;

@Getter
public class ApiValidationException extends RuntimeException {

	private final Map<String, String> fieldErrors;

	public ApiValidationException(Map<String, String> fieldErrors) {
		super("validation");
		this.fieldErrors = Collections.unmodifiableMap(new LinkedHashMap<>(fieldErrors));
	}

	public static ApiValidationException single(String field, String message) {
		Map<String, String> m = new LinkedHashMap<>();
		m.put(field, message);
		return new ApiValidationException(m);
	}
}
