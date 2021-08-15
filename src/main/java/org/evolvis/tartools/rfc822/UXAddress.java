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
 * <p>Represents an RFC822 (and successors) eMail address header content,
 * like {@code Path}, except the parser accepts more varying input,
 * especially input by humans, and eventually will MIME-encode any
 * nōn-ASCII characters. (For now they cause dropping the label part
 * from the on-wire form.)</p>
 *
 * <p>Currently implemented user-friendly parse changes are:</p><ul>
 * <li>Allow trailing dot in domains</li>
 * </ul>
 *
 * <p><strong>Warning:</strong> This class is not yet fully implemented!</p>
 *
 * @author mirabilos (t.glaser@tarent.de)
 * @see Path
 */
public class UXAddress extends Path {

/**
 * Creates and initialises a new (forgiving) parser for eMail addresses.
 *
 * @param addresses to parse
 *
 * @return null if {@code addresses} was null or very large,
 *     the new parser instance otherwise
 *
 * @see Path#of(String)
 */
public static UXAddress
of(final String addresses)
{
	return Parser.of(UXAddress.class, addresses);
}

/**
 * Private constructor. Use the factory method {@link #of(String)} instead.
 *
 * @param input string to analyse
 */
protected UXAddress(final String input)
{
	super(input);
}

@Override
protected AddrSpecSIDE pDomainDotAtom(final Substring da)
{
	if (cur() == '.')
		accept();
	return super.pDomainDotAtom(da);
}

}
