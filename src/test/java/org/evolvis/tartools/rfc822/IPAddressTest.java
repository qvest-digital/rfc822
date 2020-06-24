package org.evolvis.tartools.rfc822;

/*-
 * Copyright © 2020 mirabilos (t.glaser@tarent.de)
 * Licensor: tarent solutions GmbH, Bonn
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

import java.net.InetAddress;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test {@link IPAddress} class
 *
 * @author mirabilos (t.glaser@tarent.de)
 */
class IPAddressTest {

private static void
inv4(final String ip, final boolean canConstruct)
{
	final IPAddress p = IPAddress.of(ip);
	if (!canConstruct) {
		assertNull(p, () -> "could construct illegal " + ip);
		return;
	}
	assertNotNull(p);
	assertNull(p.asIPv4Address());
}

@Test
public void testPos()
{
	inv4(null, false);
	inv4("", true);
	inv4(String.format("%65s", ""), false);
	inv4("0.0.0.256", true);
	inv4("256.0.0.0", true);
	inv4("255.255.255.256", true);
	inv4(" ", true);
	inv4("a", true);
	inv4("0", true);
	inv4("0.0", true);
	inv4("0.0.0", true);
	inv4("0.0.0.", true);
	inv4("0.0.0.a", true);
	inv4("0.0.0.0a", true);
	inv4("0.0.0.00", true);
	inv4("00.0.0.0", true);
	inv4("300.0.0.0", true);
	inv4("400.0.0.0", true);
	inv4("500.0.0.0", true);
	inv4("600.0.0.0", true);
	inv4("700.0.0.0", true);
	inv4("800.0.0.0", true);
	inv4("900.0.0.0", true);
	inv4("260.0.0.0", true);
	inv4("-", true);
	inv4("0-", true);
	inv4("1-", true);
	inv4("3-", true);
	inv4("20-", true);
}

private static void
val4(final String ip)
{
	final IPAddress p = IPAddress.of(ip);
	assertNotNull(p, () -> "init failure for " + ip);
	final InetAddress a = p.asIPv4Address();
	assertNotNull(a, () -> "parse failure for " + ip);
	assertEquals(ip, a.getHostAddress(), "repr failure");
}

@Test
public void testPosIPv4()
{
	String[] fmt = new String[] {
	    "%d.0.0.0",
	    "0.%d.0.0",
	    "0.0.%d.0",
	    "0.0.0.%d"
	};
	for (final String f : fmt) {
		for (int i = 0; i <= 255; ++i) {
			val4(String.format(f, i));
		}
	}
	val4("255.255.255.255");
}

}
