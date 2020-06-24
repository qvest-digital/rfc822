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

import lombok.SneakyThrows;
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
	assertNotNull(p, () -> "couldn’t construct: " + ip);
	assertNull(p.asIPv4Address(), () -> "not invalid: " + ip);
}

private static void
inv6(final String ip)
{
	final IPAddress p = IPAddress.of(ip);
	assertNotNull(p, () -> "couldn’t construct: " + ip);
	assertNull(p.asIPv6Address(), () -> "not invalid: " + ip);
}

@SneakyThrows
private static void
val6(final String ip)
{
	final IPAddress p = IPAddress.of(ip);
	assertNotNull(p, () -> "couldn’t construct: " + ip);
	final InetAddress a = p.asIPv6Address();
	assertNotNull(a, () -> "not valid: " + ip);
	final InetAddress b = InetAddress.getByName("[" + ip + "]");
	assertEquals(b, a, () -> "repr failure: " + ip);
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

	val6("2001:db8:0:0:1:0:0:1");
	val6("2001:0db8:0:0:1:0:0:1");
	val6("2001:db8::1:0:0:1");
	val6("2001:db8::0:1:0:0:1");
	val6("2001:0db8::1:0:0:1");
	val6("2001:db8:0:0:1::1");
	val6("2001:db8:0000:0:1::1");
	val6("2001:DB8:0:0:1::1");
	val6("2001:db8:aaaa:bbbb:cccc:dddd:eeee:0001");
	val6("2001:db8:aaaa:bbbb:cccc:dddd:eeee:001");
	val6("2001:db8:aaaa:bbbb:cccc:dddd:eeee:01");
	val6("2001:db8:aaaa:bbbb:cccc:dddd:eeee:1");
	val6("2001:db8:aaaa:bbbb:cccc:dddd::1");
	val6("2001:db8:aaaa:bbbb:cccc:dddd:0:1");
	val6("2001:db8:0:0:0::1");
	val6("2001:db8:0:0::1");
	val6("2001:db8:0::1");
	val6("2001:db8::1");
	val6("2001:db8::aaaa:0:0:1");
	val6("2001:db8:0:0:aaaa::1");
	val6("2001:db8:aaaa:bbbb:cccc:dddd:eeee:aaaa");
	val6("2001:db8:aaaa:bbbb:cccc:dddd:eeee:AAAA");
	val6("2001:db8:aaaa:bbbb:cccc:dddd:eeee:AaAa");

	inv6("");
	inv6(" ");
	inv6("::%em0");
	inv6("ff02::1%pppoe0");
	inv6("-");
	inv6("g");
	inv6("::-");
	inv6("::g");
	inv6("::ffff:1.270.3.4");
	inv6("-f::");
	inv6("%f::");
	inv6("0:0");
	inv6("0:0:0:0:0:0:0:0:0");
	inv6("0:::");
	inv6("1ffff::");
	inv6("[f::]::");
	inv6("111.222.333.444");
	inv6("2.3.4.5");
	inv6("fd00::40e/64");
	inv6("fd00::0xc");
	inv6("fd00::0x0");
	inv6("fd00::-0");
	inv6("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff");
	inv6("ff::ff::ff");
	inv6("ff:::ff");
	inv6("ff:.");
	inv6("f:f:f:f:f:f:f:f:f");
	inv6("f:f:f:f:f:f:f");
	inv6("1:2:3:4:5:6:7:8.9.10.11");
	inv6("1:2:3:4:5:8.9.10.11");
	inv6("10000::");
	inv6("1:2:3:4:5:6");
	inv6("1:2:3:4:5:1.2.3.4");
	inv6("1:2:3:4:5:6:7:1.2.3.4");
	inv6("1:2:3:4:5:6:7:8:1.2.3.4");

	val6("::");
	val6("::0");
	val6("::1");
	val6("0:0:0::1");
	val6("ffff::1");
	val6("ffff:0:0:0:0:0:0:1");
	val6("2001:0db8:0a0b:12f0:0:0:0:1");
	val6("2001:db8:a0b:12f0::1");
	val6("::ffff:1.2.3.4");
	val6("2001:0db8:85a3::8a2e:370:7334");
	val6("2001:db8:8a2e:370:7334::");
	val6("2001:0db8:85a3:d83f:8a2e:1010:3700:7334");
	val6("de:ad::beef");
	val6("::1:2:3:4:5");
	val6("0:0:0:1:2:3:4:5");
	val6("1:2::3:4:5");
	val6("1:2:0:0:0:3:4:5");
	val6("1:2:3:4:5::");
	val6("1:2:3:4:5:0:0:0");
	val6("0:0:0:0:0:ffff:102:405");
	val6("1:2:3:4:5::1.2.3.4");
	val6("1:2:3:4:5:6:1.2.3.4");
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
