package org.evolvis.tartools.rfc822;

/*-
 * Copyright © 2020 mirabilos (t.glaser@qvest-digital.com)
 * Licensor: Qvest Digital AG, Bonn, Germany
 *
 * Provided that these terms and disclaimer and all copyright notices
 * are retained or reproduced in an accompanying document, permission
 * is granted to deal in this work without restriction, including un‐
 * limited rights to use, publicly perform, distribute, sell, modify,
 * merge, give away, or sublicence.
 *
 * This work is provided “AS IS” and WITHOUT WARRANTY of any kind, to
 * the utmost extent permitted by applicable law, neither express nor
 * implied; without malicious intent or gross negligence. In no event
 * may a licensor, author or contributor be held liable for indirect,
 * direct, other damage, loss, or other issues arising in any way out
 * of dealing in the work, even if advised of the possibility of such
 * damage or existence of a defect, except proven that it results out
 * of said person’s immediate fault when using the work as intended.
 */

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Some test utilities
 *
 * @author mirabilos (t.glaser@qvest-digital.com)
 */
final class TestUtils {

private static final Base64.Decoder B64DEC = Base64.getMimeDecoder();

private TestUtils()
{
}

/**
 * Retrieves a file from the (test) classpath
 *
 * @param filename to retrieve
 *
 * @return {@link InputStream}
 */
public static InputStream
getResourceAsStream(final String filename)
{
	return Thread.currentThread().getContextClassLoader().getResourceAsStream(filename);
}

/**
 * Retrieves a file from the (test) classpath
 *
 * @param filename to retrieve
 *
 * @return {@link InputStream}, base64-decoded
 */
public static InputStream
getB64ResourceAsStream(final String filename)
{
	return B64DEC.wrap(getResourceAsStream(filename));
}

/**
 * Retrieves an {@link InputStream} as byte array
 *
 * @param is {@link InputStream} to read
 *
 * @return byte[] data
 *
 * @throws IOException if reading fails
 */
public static byte[]
bytesFromStream(final InputStream is) throws IOException
{
	try (final ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
		final byte[] buf = new byte[512];
		int n;

		while ((n = is.read(buf)) != -1)
			baos.write(buf, 0, n);
		return baos.toByteArray();
	}
}

/**
 * Retrieves an {@link InputStream} as {@link String}
 *
 * @param is  {@link InputStream} to read
 * @param enc encoding to use when decoding, e.g. {@link StandardCharsets#UTF_8}
 *
 * @return String data
 *
 * @throws IOException if reading fails
 */
public static String
stringFromStream(final InputStream is, final Charset enc) throws IOException
{
	return new String(bytesFromStream(is), enc);
}

/**
 * Retrieves a UTF-8-encoded, base64-encoded file from the (test) classpath
 *
 * @param filename to retrieve
 *
 * @return decoded {@link String}
 *
 * @throws IOException if reading fails
 */
public static String
getB64UTF8Resource(final String filename) throws IOException
{
	return stringFromStream(getB64ResourceAsStream(filename),
	    StandardCharsets.UTF_8);
}

/**
 * Retrieves a UTF-8-encoded, base64-encoded file from the (test) classpath
 *
 * @param filename to retrieve
 *
 * @return decoded {@link String}
 *
 * @throws RuntimeException if reading fails (sneaky throws)
 */
public static String
staticB64UTF8Resource(final String filename)
{
	try {
		return getB64UTF8Resource(filename);
	} catch (IOException e) {
		throw new RuntimeException(e);
	}
}

}
