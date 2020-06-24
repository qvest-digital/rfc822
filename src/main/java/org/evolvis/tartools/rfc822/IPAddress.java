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

/**
 * Represents an IP address (including Legacy IP) for use in eMail on
 * public Internet (no scoped addresses / IPv6 Zone ID)
 *
 * @author mirabilos (t.glaser@tarent.de)
 */
public class IPAddress extends Parser {

/**
 * Creates and initialises a new parser for IPv6 and IPv4 addresses
 * (excluding IPv6 Zone ID)
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
private IPAddress(final String input)
{
	super(input, /* probably 45 */ 64);
}

}
