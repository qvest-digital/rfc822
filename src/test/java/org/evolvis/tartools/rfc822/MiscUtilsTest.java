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

import org.junit.jupiter.api.Test;

import static net.trajano.commons.testing.UtilityClassTestUtil.assertUtilityClassWellDefined;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test {@link MiscUtils} class
 *
 * @author mirabilos (m@mirbsd.org)
 */
class MiscUtilsTest {

@Test
public void
testUtilityClass() throws ReflectiveOperationException
{
	assertUtilityClassWellDefined(MiscUtils.class);
}

private static void
te(final String expected, final String src)
{
	assertEquals(expected, MiscUtils.escapeNonPrintASCII(src), "improper escape");
}

private static void
tt(final String expected, final String src)
{
	assertEquals(expected, MiscUtils.trim(src), "improper trim");
}

@Test
public void testPos()
{
	te("", "");
	te("a", "a");
	te("\\u0001", "\u0001");
	te("~", "\u007E");
	te("\\u007F\\u0080\\u009F\\u00A0", "\u007F\u0080\u009F\u00A0");
	te("\\u00E4h \\U0001F408", "äh 🐈");
	tt("", "");
	tt("", " ");
	tt("a", "a");
	tt("a", " a");
	tt("a", "a ");
	tt("a \u00A0", "a \u00A0 ");
	tt("a", "\ta\r");
	tt("🐈", "🐈\n");
	tt("🐈🐈", "🐈🐈\n");
	tt("🐈🐈🐈", "🐈🐈🐈\n");
	tt("a🐈🐈🐈", "a🐈🐈🐈\n");
	tt("🐈🐈🐈o", "🐈🐈🐈o\n");
	tt("🐈🐈🐈\u202F", "\u2009\u200A\u2028\u2029🐈🐈🐈\u202F\f \t");
	tt("🐈  🐈   🐈 \u202F", " 🐈  🐈   🐈 \u202F 　");
}

@Test
public void
testLombokNonNull()
{
	// test extra branches caused by Lombok @NonNull
	//noinspection ConstantConditions
	assertThrows(NullPointerException.class, () -> MiscUtils.escapeNonPrintASCII(null));
	//noinspection ConstantConditions
	assertThrows(NullPointerException.class, () -> MiscUtils.trim(null));
}

}
