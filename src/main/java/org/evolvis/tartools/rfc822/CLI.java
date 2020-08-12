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
public final class CLI {

public static void main(String[] argv)
{
	for (String arg : argv) {
		val asPath = Path.of(arg);
		final String desc;
		if (asPath.forSender(false) != null)
			desc = "mailbox ⇒ address, mailbox-list, address-list";
		else if (asPath.forSender(true) != null)
			desc = "address ⇒ address-list (group, not just mailbox)";
		else if (asPath.asMailboxList() != null)
			desc = "mailbox-list ⇒ address-list (with only mailboxen)";
		else if (asPath.asAddressList() != null)
			desc = "address-list (with group(s), not just mailboxen)";
		else
			desc = "not a valid eMail address";
		System.out.println(String.format("‣ %s\n\t%s\n", arg, desc));
	}
}

}
