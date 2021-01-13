package org.evolvis.tartools.rfc822;

/*-
 * Copyright © 2020, 2021 mirabilos (t.glaser@tarent.de)
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
 * <p>Represents an FQDN (“domain” production) for use in eMail.</p>
 *
 * <p>The main entry point is the {@link #isDomain(String)} method.</p>
 *
 * @author mirabilos (t.glaser@tarent.de)
 */
public class FQDN extends Parser {

/**
 * Creates and initialises a new parser for Fully-Qualified Domain Names.
 *
 * @param hostname to parse
 *
 * @return null if {@code hostname} was null or longer than 254 characters,
 *     the new parser instance otherwise
 */
public static FQDN
of(final String hostname)
{
	return Parser.of(FQDN.class, hostname);
}

/**
 * Private constructor. Use the factory method {@link #of(String)} instead.
 *
 * @param input string to analyse
 */
protected FQDN(final String input)
{
	super(input, /* RFC5321 Forward-path limit */ 254);
}

/**
 * <p>Checks if a supposed hostname is a valid Fully-Qualified Domain Name.</p>
 *
 * <p>Valid FQDNs are up to 254 octets in length, comprised only of labels
 * (letters, digits and hyphen-minus, but not beginning or ending with
 * a hyphen-minus) up to 63 octets long, separated by dots (‘.’).</p>
 *
 * <p>Strictly speaking an FQDN could be 255 octets in length, but these may
 * cause problems with DNS and will not work in SMTP anyway.</p>
 *
 * @return true if the hostname passed during construction is valid, else false
 */
public boolean
isDomain()
{
	jmp(0);
	while (true) {
		final int begLabel = pos();
		if (!Path.is(cur(), Path.IS_ALNUM))
			return false;
		while (Path.is(peek(), Path.IS_ALNUS))
			accept();
		if (!Path.is(cur(), Path.IS_ALNUM))
			return false;
		accept();
		if (pos() - begLabel > 63)
			return false;
		if (cur() == -1)
			break;
		if (cur() != '.')
			return false;
		accept();
	}
	// domain length limit checked in constructor (characters)
	// characters are all octets, so limit in octets also checked
	return true;
}

/**
 * <p>Checks if a supposed hostname is a valid Fully-Qualified Domain Name.</p>
 *
 * <p>Valid FQDNs are up to 254 octets in length, comprised only of labels
 * (letters, digits and hyphen-minus, but not beginning or ending with
 * a hyphen-minus) up to 63 octets long, separated by dots (‘.’).</p>
 *
 * <p>Strictly speaking an FQDN could be 255 octets in length, but these may
 * cause problems with DNS and will not work in SMTP anyway.</p>
 *
 * @param hostname to check
 *
 * @return true if {@code hostname} is valid, false otherwise
 */
public static boolean
isDomain(final String hostname)
{
	final FQDN parser = of(hostname);
	return parser != null && parser.isDomain();
}

}
