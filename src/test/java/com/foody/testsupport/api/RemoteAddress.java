package com.foody.testsupport.api;

import org.springframework.test.web.servlet.request.RequestPostProcessor;

public final class RemoteAddress {

	private RemoteAddress() {
	}

	public static RequestPostProcessor of(String address) {
		return request -> {
			request.setRemoteAddr(address);
			return request;
		};
	}

}
