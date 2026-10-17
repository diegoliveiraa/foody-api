package com.foody.testsupport.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

final class JwtTestKeys {

	private static final String ALGORITHM = "RSA";

	private static final int KEY_SIZE = 2048;

	private static final int PEM_LINE_LENGTH = 64;

	private static final String LINE_BREAK = "\n";

	private static final String DIRECTORY_PREFIX = "foody-jwt-";

	private static final String PUBLIC_KEY_FILE = "jwt-public.pem";

	private static final String PRIVATE_KEY_FILE = "jwt-private.pem";

	private static final String PUBLIC_KEY_LABEL = "PUBLIC KEY";

	private static final String PRIVATE_KEY_LABEL = "PRIVATE KEY";

	private static final String PEM_BEGIN = "-----BEGIN %s-----";

	private static final String PEM_END = "-----END %s-----";

	static final JwtKeysContextCustomizer CUSTOMIZER = generate();

	private JwtTestKeys() {
	}

	private static JwtKeysContextCustomizer generate() {
		try {
			KeyPairGenerator generator = KeyPairGenerator.getInstance(ALGORITHM);
			generator.initialize(KEY_SIZE);
			KeyPair pair = generator.generateKeyPair();
			Path directory = Files.createTempDirectory(DIRECTORY_PREFIX);
			directory.toFile().deleteOnExit();
			Path publicKey = write(directory.resolve(PUBLIC_KEY_FILE), PUBLIC_KEY_LABEL, pair.getPublic().getEncoded());
			Path privateKey = write(directory.resolve(PRIVATE_KEY_FILE), PRIVATE_KEY_LABEL,
					pair.getPrivate().getEncoded());
			return new JwtKeysContextCustomizer(publicKey.toUri().toString(), privateKey.toUri().toString());
		}
		catch (NoSuchAlgorithmException | IOException exception) {
			throw new IllegalStateException(exception);
		}
	}

	private static Path write(Path file, String label, byte[] encoded) throws IOException {
		Base64.Encoder encoder = Base64.getMimeEncoder(PEM_LINE_LENGTH, LINE_BREAK.getBytes(StandardCharsets.US_ASCII));
		String pem = String.join(LINE_BREAK, PEM_BEGIN.formatted(label), encoder.encodeToString(encoded),
				PEM_END.formatted(label), "");
		Path written = Files.writeString(file, pem, StandardCharsets.US_ASCII);
		written.toFile().deleteOnExit();
		return written;
	}

}
