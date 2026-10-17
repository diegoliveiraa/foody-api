package com.foody.api.shared.util;

import com.foody.api.shared.message.Messages;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class HashUtils {

	private static final String SHA_256 = "SHA-256";

	private HashUtils() {
	}

	public static String sha256Hex(String value) {
		try {
			byte[] digest = MessageDigest.getInstance(SHA_256).digest(value.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		}
		catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException(Messages.INTERNAL_ERROR.text(), exception);
		}
	}

}
