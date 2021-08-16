package org.evolvis.tartools.rfc822;

/*-
 * Copyright © 2021 mirabilos (m@mirbsd.org)
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

import lombok.NonNull;

/**
 * <p>Utility class for the CLI.</p>
 *
 * @author mirabilos (m@mirbsd.org)
 */
final class MiscUtils {

/**
 * Prevent instantiation (this is a utility class with only static methods)
 */
private MiscUtils()
{
}

/**
 * <p>Escapes all codepoints that are not printable ASCII (graph class or SPACE).</p>
 *
 * @param s String to escape
 *
 * @return string with <code>&#92;uNNNN</code> and <code>&#92;U000NNNNN</code> escapes
 */
static String
escapeNonPrintASCII(@NonNull final String s)
{
	final int len = s.length();
	final StringBuilder sb = new StringBuilder(len);
	int ofs = 0;

	while (ofs < len) {
		final int ch = s.codePointAt(ofs);

		if (ch >= 0x20 && ch <= 0x7E)
			sb.append((char)ch);
		else if (ch <= 0xFFFF)
			sb.append(String.format("\\u%04X", ch));
		else
			sb.append(String.format("\\U%08X", ch));
		ofs += Character.charCount(ch);
	}
	return sb.toString();
}

/**
 * <p>Trims all leading and trailing whitespace off the string.</p>
 *
 * <p>The {@link Character#isWhitespace(int)} definition is used.</p>
 *
 * @param s String to trim
 *
 * @return string with leading and trailing whitespace removed
 */
static String
trim(@NonNull final String s)
{
	final int len = s.length();
	int ofs = 0;

	while (ofs < len) {
		final int ch = s.codePointAt(ofs);

		if (!Character.isWhitespace(ch))
			break;
		ofs += Character.charCount(ch);
	}
	if (ofs == len)
		return "";
	int beg = ofs;

	int max = ofs;
	while (ofs < len) {
		final int ch = s.codePointAt(ofs);

		ofs += Character.charCount(ch);
		if (!Character.isWhitespace(ch))
			max = ofs;
	}
	return s.substring(beg, max);
}

}
