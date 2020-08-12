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
 * Utility to parse strings from the command line.
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

private static String
chk(Path.ParserResult arg)
{
	return arg == null ? BAD : arg.isValid() ? VALID : PARSES;
}

public static void
main(String[] argv)
{
	System.out.println(CLR);
	for (String arg : argv) {
		val asPath = Path.of(arg);
		val asAS = asPath.asAddrSpec();
		val asMbox = asPath.forSender(false);
		val asAddr = asPath.forSender(true);
		val asML = asPath.asMailboxList();
		val alAL = asPath.asAddressList();
		val isDom = FQDN.isDomain(arg);
		val i6 = IPAddress.v6(arg);
		val i4 = IPAddress.v4(arg);
		final String desc;
		final String dmbx;
		final String dadr;
		if (asML == null && alAL == null) {
			desc = NOLIST;
			dmbx = chk(asMbox);
			dadr = chk(asAddr);
		} else {
			desc = ISLIST;
			dmbx = chk(asML);
			dadr = chk(alAL);
		}
		System.out.print(String.format("‣ %s" + CLR + "%n\t", arg));
		System.out.println(String.format(desc,
		    chk(asAS), dmbx, dadr, isDom ? VALID : BAD,
		    i6 == null ? BAD : VALID, i4 == null ? BAD : VALID));
		System.out.println(i6 == null && i4 == null ? "" :
		    String.format("\tIP: %s%n",
			(i6 == null ? i4 : i6).getHostAddress()));
	}
}

}
