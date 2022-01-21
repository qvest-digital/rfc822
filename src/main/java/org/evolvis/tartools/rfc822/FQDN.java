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
 * <p>The main entry points are the {@link #isDomain(String)} and
 * {@link #asDomain(String)} methods.
 * The parser does not trim surrounding whitespace by itself.</p>
 *
 * @author mirabilos (t.glaser@tarent.de)
 */
public class FQDN extends Parser {

/**
 * Creates and initialises a new parser for Fully-Qualified Domain Names.
 *
 * @param hostname to parse
 *
 * @return null if {@code hostname} was null or longer than 253 characters,
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
	super(input, /* see isDomain() javadoc */ 253);
}

/**
 * <p>Checks if a supposed hostname is a valid Fully-Qualified Domain Name.</p>
 *
 * <p>Valid FQDNs are up to 253 octets in length, comprised only of labels
 * (letters, digits and hyphen-minus, but not beginning or ending with
 * a hyphen-minus) one up to 63 octets long, separated by dots (‘.’). This
 * method accepts the dot-atom form only, without trailing dot.</p>
 *
 * <p>Strictly speaking an FQDN could be 255 octets in length, but these will
 * not work with DNS (the separating dots are matched by the length octets of
 * their succeeding label, but two extra octets are needed for the length octet
 * of the first label and of the root (i.e. nil) domain; SMTP has a 254-octet
 * limit for the Forward-path (in RFC5321) as well.</p>
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
 * <p>Valid FQDNs are up to 253 octets in length, comprised only of labels
 * (letters, digits and hyphen-minus, but not beginning or ending with
 * a hyphen-minus) one up to 63 octets long, separated by dots (‘.’). This
 * method accepts the dot-atom form only, without trailing dot.</p>
 *
 * <p>Strictly speaking an FQDN could be 255 octets in length, but these will
 * not work with DNS (the separating dots are matched by the length octets of
 * their succeeding label, but two extra octets are needed for the length octet
 * of the first label and of the root (i.e. nil) domain; SMTP has a 254-octet
 * limit for the Forward-path (in RFC5321) as well.</p>
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

/**
 * <p>Checks if a supposed hostname is a valid Fully-Qualified Domain Name
 * and retrieves a more canonical form.</p>
 *
 * <p>Valid FQDNs are up to 253 octets in length, comprised only of labels
 * (letters, digits and hyphen-minus, but not beginning or ending with
 * a hyphen-minus) one up to 63 octets long, separated by dots (‘.’). This
 * method accepts an optional additional trailing dot as commonly seen in
 * DNS data files, and which may be valid for some contexts; if present, the
 * input may be 254 octets long.</p>
 *
 * <p>Strictly speaking an FQDN could be 255 octets in length, but these will
 * not work with DNS (the separating dots are matched by the length octets of
 * their succeeding label, but two extra octets are needed for the length octet
 * of the first label and of the root (i.e. nil) domain; SMTP has a 254-octet
 * limit for the Forward-path (in RFC5321) as well.</p>
 *
 * <p>This method returns the dot-atom form: a trailing dot is removed, if any
 * existed. To get the canonical form from there, normalise letter case.</p>
 *
 * @param hostname to check
 *
 * @return dot-atom form of {@code hostname} if valid, null otherwise
 */
public static String
asDomain(final String hostname)
{
	final String dn = stripTrailingDot(hostname);
	final FQDN parser = of(dn);
	return parser != null && parser.isDomain() ? dn : null;
}

private static String
stripTrailingDot(final String s)
{
	final int len;
	if (s == null || (len = s.length()) < 1 || s.charAt(len - 1) != '.')
		return s;
	return s.substring(0, len - 1);
}

}
