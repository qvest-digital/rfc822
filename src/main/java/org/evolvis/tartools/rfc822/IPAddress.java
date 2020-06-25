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
import lombok.val;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents an IP address (including Legacy IP) for use in eMail on
 * public Internet (no scoped addresses / IPv6 Zone ID)
 *
 * @author mirabilos (t.glaser@tarent.de)
 */
public class IPAddress extends Parser {

/**
 * Creates and initialises a new parser for IPv6 and IPv4 addresses
 * (excluding IPv6 Zone ID) to use by {@link #asIPv6Address()} and
 * {@link #asIPv4Address()}.
 *
 * @param address to parse (protocol depends on parser method called)
 *
 * @return null if address was null or much too large, the new instance otherwise
 */
public static IPAddress
of(final String address)
{
	return Parser.of(IPAddress.class, address);
}

/**
 * Private constructor, use the factory method {@link #of(String)} instead
 *
 * @param input string to analyse
 */
protected IPAddress(final String input)
{
	super(input, /* probably 45 */ 64);
}

@SneakyThrows(UnknownHostException.class)
private InetAddress
toAddress(final byte[] addr)
{
	if (addr == null || cur() != -1)
		return null;
	// assert: addr is byte[4] or byte[16]
	return InetAddress.getByAddress(s(), addr);
}

/**
 * Parses the passed address as IP address (IPv6).
 *
 * Note that the returned InetAddress object can be an {@link Inet4Address}
 * object, for example if the passed address represents a v4-mapped address;
 * in most cases it will be an {@link Inet6Address} object though. In either
 * case, if the address is no valid IPv6 address (e.g. because it is an IPv4
 * address), null will be returned instead, so the return value can be used
 * to distinguish the address families, even if a v4-mapped address occurs.
 *
 * The {@link InetAddress#getHostName()} method will return the original
 * string in all cases anyway.
 *
 * @return {@link InetAddress} representing the address, or null on failure
 */
public InetAddress
asIPv6Address()
{
	jmp(0);
	return toAddress(pIPv6Address());
}

/**
 * Parses the passed address as Legacy IP address (IPv4).
 *
 * The {@link InetAddress#getHostName()} method will return the original string.
 *
 * @return {@link InetAddress} representing the address, or null on failure
 */
public InetAddress
asIPv4Address()
{
	jmp(0);
	return toAddress(pIPv4Address());
}

private static boolean
dec(final int c, final int limit)
{
	return c >= '0' && c <= limit;
}

@SuppressWarnings("fallthrough")
protected byte[]
pIPv4Address()
{
	try (val ofs = new Parser.Txn()) {
		byte[] addr = new byte[4];
		for (int a = 0; a < addr.length; ++a) {
			if (a > 0) {
				if (cur() != '.')
					return null;
				accept();
			}
			final int b1 = cur();
			final int b2 = peek();
			int l2 = '9';
			int l3 = '9';
			switch (b1) {
			case '2':
				l2 = '5';
				if (b2 == '5')
					l3 = '5';
				/* FALLTHROUGH */
			case '1':
				if (dec(b2, l2)) {
					int v = Character.digit(b1, 10) * 10 + Character.digit(b2, 10);
					final int b3 = bra(2);
					if (dec(b3, l3)) {
						v = v * 10 + Character.digit(b3, 10);
						accept();
					}
					addr[a] = (byte)v;
					continue;
				}
				break;
			case '0':
				addr[a] = (byte)0;
				accept();
				continue;
			}
			if (!dec(b1, '9'))
				return null;
			int v = Character.digit(b1, 10);
			accept();
			if (dec(b2, '9')) {
				v = v * 10 + Character.digit(b2, 10);
				accept();
			}
			addr[a] = (byte)v;
		}
		return ofs.accept(addr);
	}
}

/**
 * Checks for h16 or possibly an IPv4 address (ls32 production). This method
 * solely exists because Sonar otherwise thinks {@link #pIPv6Address()} too
 * complex for the feeble minds of Java™ programmers…
 *
 * It checks whether the current char can be an h32 or IPv4 address (xdigit);
 * if not, or if ls32 indicates we should check for IPv4 addresses and there
 * indeed is one, i.e. when further parsing should be stopped, it returns
 * true; otherwise, the parsed h16 is added to dst, and false returned to
 * indicate that parsing should continue.
 *
 * @param dst  collecting h16s
 * @param ls32 whether an IPv4 address is allowed here
 *
 * @return whether parsing should stop
 */
private boolean
h16OrIPv4(final List<Integer> dst, final boolean ls32)
{
	if (!Path.is(cur(), Path.IS_XDIGIT)) {
		return true;
	}
	if (ls32) {
		final byte[] v4 = pIPv4Address();
		if (v4 != null) {
			dst.add((v4[0] << 8) | (v4[1] & 0xFF));
			dst.add((v4[2] << 8) | (v4[3] & 0xFF));
			return true;
		}
	}

	int x = 0;
	for (int i = 0; i < 4; ++i) {
		if (!Path.is(cur(), Path.IS_XDIGIT))
			break;
		x = (x << 4) + Character.digit(cur(), 16);
		accept();
	}
	dst.add(x);
	return false;
}

private static byte[]
rtnIPv6Address(final Parser.Txn txn, final List<Integer> h16s,
    final List<Integer> afterDoubleColon)
{
	if (afterDoubleColon != null) {
		// stuff the double colon
		final int afters = afterDoubleColon.size();
		while (h16s.size() + afters < 8)
			h16s.add(0);
		h16s.addAll(afterDoubleColon);
	}

	if (h16s.size() != 8)
		return null;
	txn.commit();

	final byte[] addr = new byte[16];
	for (int i = 0; i < 8; ++i) {
		final int v = h16s.get(i);
		addr[i * 2] = (byte)(v >>> 8);
		addr[i * 2 + 1] = (byte)(v & 0xFF);
	}
	return addr;
}

protected byte[]
pIPv6Address()
{
	try (val ofs = new Parser.Txn()) {
		final List<Integer> beforeDoubleColon = new ArrayList<>();
		boolean hasDoubleColon = false;
		int cnt = 0;
		while (cnt < 8) {
			if (cur() == ':' && peek() == ':') {
				hasDoubleColon = true;
				break;
			}
			if (cnt > 0) {
				if (cur() != ':')
					break;
				accept();
			}
			if (h16OrIPv4(beforeDoubleColon, cnt == 6))
				break;
			++cnt;
		}
		// arrive here either with double colon (0 ≤ cnt ≤ 7)
		// or at end of h16 list (0 ≤ cnt ≤ 8)
		if (!hasDoubleColon)
			return rtnIPv6Address(ofs, beforeDoubleColon, null);
		// remainder string begins with double colon, leave ONE
		accept();
		final List<Integer> afterDoubleColon = new ArrayList<>();
		int maxh16 = 7 - cnt;
		while (maxh16 > 0) {
			if (cur() != ':')
				break;
			accept();
			// must check this first
			if (h16OrIPv4(afterDoubleColon, maxh16 >= 2))
				break;
			--maxh16;
		}
		// return result
		return rtnIPv6Address(ofs, beforeDoubleColon, afterDoubleColon);
	}
}

}
