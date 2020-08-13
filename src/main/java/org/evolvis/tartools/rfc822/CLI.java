package org.evolvis.tartools.rfc822;

/*-
 * Copyright © 2020 mirabilos (m@mirbsd.org)
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

/**
 * Utility to parse and validate strings from the command line.
 * See {@link #usage()} for usage information.
 *
 * @author mirabilos (m@mirbsd.org)
 */
@SuppressWarnings("squid:S106")
public final class CLI {

private static final String EAS = "addr-spec %s  ";
private static final String EML = "mailbox-list %s  address-list %s";
private static final String EMA = "(no list) mailbox %s  address %s";
private static final String DIP = "  FQDN %s  IPv6 %s  IPv4 %s";
private static final String ISLIST = EAS + EML + DIP;
private static final String NOLIST = EAS + EMA + DIP;
private static final String CLR = "\u001B[0m";
private static final String BAD = "\u001B[31m✘" + CLR;
private static final String PARSES = "\u001B[1;33m✘" + CLR;
private static final String VALID = "\u001B[32m✔" + CLR;
private static final String BOLD = "\u001B[1m";

private static String
chk(Path.ParserResult arg)
{
	return arg == null ? BAD : arg.isValid() ? VALID : PARSES;
}

private static void
one(Path.ParserResult arg)
{
	if (arg == null)
		System.exit(41);
	if (!arg.isValid())
		System.exit(42);
	System.out.println(arg.toString());
	System.exit(0);
}

private static void
usage()
{
	System.err.println("Usage: java -jar rfc822.jar -TYPE input  # check mode (on success, exit 0)");
	System.err.println("       java -jar rfc822.jar [input ...]  # interactive colourful mode, exit 40");
	System.err.println("TYPE: addrspec, mailbox, address, mailboxlist, addresslist, domain, ipv4, ipv6");
	System.err.println("exit code 43 = unspecified bad input, 42 = invalid, 41 = cannot even be parsed");
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
		if (!FQDN.isDomain(input))
			System.exit(43);
		System.out.println(input);
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

@SuppressWarnings("squid:S3776")
public static void
main(String[] argv)
{
	boolean skipfirst = false;

	if (argv.length > 0 && argv[0].startsWith("-")) {
		if ("--".equals(argv[0]))
			skipfirst = true;
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
		val isDom = FQDN.isDomain(arg);
		val i6 = IPAddress.v6(arg);
		val i4 = IPAddress.v4(arg);
		final String desc;
		final String dmbx;
		final String dadr;
		final Path.ParserResult rmail;
		if (asML == null && asAL == null) {
			desc = NOLIST;
			dmbx = chk(asMbox);
			dadr = chk(asAddr);
			rmail = asAddr == null ? asMbox : asAddr;
		} else {
			desc = ISLIST;
			dmbx = chk(asML);
			dadr = chk(asAL);
			rmail = asAL == null ? asML : asAL;
		}
		System.out.print(String.format("‣ %s" + CLR + "%n\t", arg));
		System.out.println(String.format(desc,
		    chk(asAS), dmbx, dadr, isDom ? VALID : BAD,
		    i6 == null ? BAD : VALID, i4 == null ? BAD : VALID));
		if (rmail != null && rmail.isValid())
			System.out.println(String.format("\teMail: " + BOLD + "%s" + CLR,
			    rmail.toString()));
		if (i6 != null || i4 != null)
			System.out.println(String.format("\tIP: " + BOLD + "%s" + CLR,
			    (i6 == null ? i4 : i6).getHostAddress()));
		if (isDom)
			System.out.println(String.format("\tFQDN: " + BOLD + "%s" + CLR,
			    arg));
		System.out.println();
	}
	System.exit(40);
}

}
