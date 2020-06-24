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

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

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
	final IPAddress obj = new IPAddress(address);
	return obj.s() == null ? null : obj;
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
	byte[] addr = pIPv6Address();
	if (addr == null || cur() != -1)
		return null;
	// assert: addr is byte[16]
	try {
		return InetAddress.getByAddress(s(), addr);
	} catch (UnknownHostException e) {
		// can’t happen if addr is a 4‑ or 16-byte array
		throw new RuntimeException(e);
	}
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
	byte[] addr = pIPv4Address();
	if (addr == null || cur() != -1)
		return null;
	// assert: addr is byte[4]
	try {
		return InetAddress.getByAddress(s(), addr);
	} catch (UnknownHostException e) {
		// can’t happen if addr is a 4‑ or 16-byte array
		throw new RuntimeException(e);
	}
}

protected byte[]
pIPv4Address()
{
	byte[] addr = new byte[4];
	int a = 0;
	return addr;
}

protected byte[]
pIPv6Address()
{
	byte[] addr = new byte[16];
	int a = 0;
	return addr;
}

}
