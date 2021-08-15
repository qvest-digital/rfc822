package org.evolvis.tartools.rfc822;

/*-
 * Copyright © 2020, 2021 mirabilos (m@mirbsd.org)
 * Copyright © 2021 mirabilos (t.glaser@tarent.de)
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

import lombok.val;

import java.util.Locale;

/**
 * <p>Utility to parse and validate strings from the command line.</p>
 * <p>See {@link #usage()} for usage information.</p>
 *
 * @author mirabilos (m@mirbsd.org)
 */
@SuppressWarnings("squid:S106")
public final class CLI {

private static final String EAS = "addr-spec %s  ";
private static final String EML = "mailbox-list %s  address-list %s";
private static final String EMA = " list) mailbox %s  address %s";
private static final String DIP = "  FQDN %s  IPv6 %s  IPv4 %s";
private static final String ISLIST = EAS + EML + DIP;
private static final String NOLIST = EAS + "(no" + EMA + DIP;
private static final String ORLIST = EAS + "(or" + EMA + DIP;
private static final String CLR = "\u001B[0m";
private static final String BAD = "\u001B[31m✘" + CLR;
private static final String PARSES = "\u001B[1;33m✘" + CLR;
private static final String VALID = "\u001B[32m✔" + CLR;
private static final String BOLD = "\u001B[1m";

private static String
chk(final Path.ParserResult arg)
{
	return arg == null ? BAD : arg.isValid() ? VALID : PARSES;
}

private static void
one(final Path.ParserResult arg)
{
	if (arg == null)
		System.exit(41);
	if (!arg.isValid())
		System.exit(42);
	System.out.println(arg);
	System.exit(0);
}

private static void
usage()
{
	System.err.println("Usage: java -jar rfc822.jar [input ...]  # interactive colourful mode, exit 40");
	System.err.println("       java -jar rfc822.jar -TYPE input  # check mode (on success, exit 0)");
	System.err.println("TYPE: addrspec, mailbox, address, mailboxlist, addresslist, domain, ipv4, ipv6");
	System.err.println("exit code 43 = unspecified bad input, 42 = invalid, 41 = cannot even be parsed");
	System.err.println("       java -jar rfc822.jar -extract input ...  # list addr-spec of each input");
	System.err.println("exit code 45 = no valid input, 0 = all inputs valid, 44 = some invalid present");
	System.exit(1);
}

@SuppressWarnings("squid:S3776")
private static void
batch(final String flag, final String input)
{
	if ("-addrspec".equals(flag)) {
		val asPath = Path.of(input);
		one(asPath.asAddrSpec());
	} else if ("-mailbox".equals(flag)) {
		val asPath = Path.of(input);
		one(asPath.forSender(false));
	} else if ("-address".equals(flag)) {
		val asPath = Path.of(input);
		one(asPath.forSender(true));
	} else if ("-mailboxlist".equals(flag)) {
		val asPath = Path.of(input);
		one(asPath.asMailboxList());
	} else if ("-addresslist".equals(flag)) {
		val asPath = Path.of(input);
		one(asPath.asAddressList());
	} else if ("-domain".equals(flag)) {
		val asDomain = FQDN.asDomain(input);
		if (asDomain == null)
			System.exit(43);
		System.out.println(asDomain.toLowerCase(Locale.ROOT));
		System.exit(0);
	} else if ("-ipv4".equals(flag)) {
		val i4 = IPAddress.v4(input);
		if (i4 == null)
			System.exit(43);
		System.out.println(i4.getHostAddress());
		System.exit(0);
	} else if ("-ipv6".equals(flag)) {
		val i6 = IPAddress.v6(input);
		if (i6 == null)
			System.exit(43);
		System.out.println(i6.getHostAddress());
		System.exit(0);
	}
	usage();
}

private static String
escapeNonPrintASCII(final String s)
{
	final int len = s.length();
	final StringBuilder sb = new StringBuilder(len);
	int ofs = 0;

	while (ofs < len) {
		final int ch = s.codePointAt(ofs);

		if (ch >= 0x20 && ch <= 0x7E) {
			sb.append((char)ch);
			++ofs;
		} else if (ch <= 0xFFFF) {
			sb.append(String.format("\\u%04X", ch));
			++ofs;
		} else {
			sb.append(String.format("\\U%08X", ch));
			ofs += 2;
		}
	}
	return sb.toString();
}

private static void
extract(final String[] args, int skip)
{
	boolean anyValid = false;
	boolean anyInvalid = false;

	for (String arg : args) {
		if (skip > 0) {
			--skip;
			continue;
		}
		val p = UXAddress.of(arg);
		val l = p != null ? p.asAddressList() : null;
		if (l != null && l.isValid()) {
			System.out.println(String.join(", ", l.flattenAddrSpecs()));
			anyValid = true;
		} else {
			System.err.println("N: not valid: " + escapeNonPrintASCII(arg));
			System.out.println();
			anyInvalid = true;
		}
	}
	if (!anyValid) {
		System.err.println("E: no valid inputs provided");
		System.exit(45);
	}
	if (!anyInvalid)
		System.exit(0);
	System.err.println("E: there were some invalid inputs");
	System.exit(44);
}

@SuppressWarnings("squid:S3776")
public static void
main(final String[] argv)
{
	boolean skipfirst = false;

	if (argv.length > 0 && argv[0].startsWith("-")) {
		if ("--".equals(argv[0]))
			skipfirst = true;
		else if (argv.length > 1 && "-extract".equals(argv[0]) &&
		    (!"--".equals(argv[1]) || argv.length > 2))
			extract(argv, "--".equals(argv[1]) ? 2 : 1);
		else if (argv.length == 2)
			batch(argv[0], argv[1]);
		else if (argv.length == 3 && "--".equals(argv[1]))
			batch(argv[0], argv[2]);
		else
			usage();
	}

	System.out.println(CLR);
	for (String arg : argv) {
		if (skipfirst) {
			skipfirst = false;
			continue;
		}
		val asPath = Path.of(arg);
		val asAS = asPath.asAddrSpec();
		val asMbox = asPath.forSender(false);
		val asAddr = asPath.forSender(true);
		val asML = asPath.asMailboxList();
		val asAL = asPath.asAddressList();
		val isDom = FQDN.asDomain(arg);
		val i6 = IPAddress.v6(arg);
		val i4 = IPAddress.v4(arg);
		final String desc;
		final String dmbx;
		final String dadr;
		final Path.ParserResult rmail;
		if (/* known not-list */ asMbox != null || asAddr != null ||
		    /* known not a list */ (asML == null && asAL == null)) {
			desc = asML == null && asAL == null ? NOLIST : ORLIST;
			dmbx = chk(asMbox);
			dadr = chk(asAddr);
			rmail = asAddr == null ? asMbox : asAddr;
		} else {
			desc = ISLIST;
			dmbx = chk(asML);
			dadr = chk(asAL);
			rmail = asAL == null ? asML : asAL;
		}
		System.out.printf("‣ %s" + CLR + "%n\t", arg);
		System.out.printf((desc) + "%n",
		    chk(asAS), dmbx, dadr, isDom != null ? VALID : BAD,
		    i6 == null ? BAD : VALID, i4 == null ? BAD : VALID);
		if (rmail != null && rmail.isValid())
			System.out.printf("\teMail: " + BOLD + "%s" + CLR + "%n",
			    rmail);
		if (i6 != null || i4 != null)
			System.out.printf("\tIP: " + BOLD + "%s" + CLR + "%n",
			    (i6 == null ? i4 : i6).getHostAddress());
		if (isDom != null)
			System.out.printf("\tFQDN: " + BOLD + "%s" + CLR + "%n",
			    isDom.toLowerCase(Locale.ROOT));
		System.out.println();
	}
	System.exit(40);
}

}
