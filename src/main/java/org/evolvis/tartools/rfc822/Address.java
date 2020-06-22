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
 * Represents an RFC822 (and successors) eMail address header content,
 * either From or To, or subsets.
 *
 * @author mirabilos (t.glaser@tarent.de)
 */
public class Address extends Parser {

private static final byte F_ALPHA = 0x01;
private static final byte F_DIGIT = 0x02;
private static final byte F_HYPHN = 0x04;
private static final byte F_ATEXT = 0x08;
private static final byte F_QTEXT = 0x10;
private static final byte F_CTEXT = 0x20;

private static final byte IS_ATEXT = F_ALPHA | F_DIGIT | F_HYPHN | F_ATEXT;
private static final byte IS_QTEXT = F_QTEXT;
private static final byte IS_CTEXT = F_CTEXT;

private static final byte[] ASCII = new byte[128];

static {
	for (int i = 0; i < ASCII.length; ++i)
		ASCII[i] = 0;
	for (char c = 'A'; c <= 'Z'; ++c)
		ASCII[c] |= F_ALPHA;
	for (char c = 'a'; c <= 'z'; ++c)
		ASCII[c] |= F_ALPHA;
	for (char c = '0'; c <= '9'; ++c)
		ASCII[c] |= F_DIGIT;
	ASCII['-'] = F_HYPHN;

	ASCII['!'] = F_ATEXT;
	ASCII['#'] = F_ATEXT;
	ASCII['$'] = F_ATEXT;
	ASCII['%'] = F_ATEXT;
	ASCII['&'] = F_ATEXT;
	ASCII['\''] = F_ATEXT;
	ASCII['*'] = F_ATEXT;
	ASCII['+'] = F_ATEXT;
	ASCII['/'] = F_ATEXT;
	ASCII['='] = F_ATEXT;
	ASCII['?'] = F_ATEXT;
	ASCII['^'] = F_ATEXT;
	ASCII['_'] = F_ATEXT;
	ASCII['`'] = F_ATEXT;
	ASCII['{'] = F_ATEXT;
	ASCII['|'] = F_ATEXT;
	ASCII['}'] = F_ATEXT;
	ASCII['~'] = F_ATEXT;

	ASCII[33] = F_QTEXT;
	for (int d = 35; d <= 91; ++d)
		ASCII[d] = F_QTEXT;
	for (int d = 33; d <= 39; ++d)
		ASCII[d] = F_CTEXT;
	for (int d = 42; d <= 91; ++d)
		ASCII[d] = F_CTEXT;
	for (int d = 93; d <= 126; ++d)
		ASCII[d] = F_QTEXT | F_CTEXT;
}

/**
 * Constructs a parser for eMail addresses
 *
 * @param addresses to parse
 */
public Address(final String addresses)
{
	super(addresses, /* might want to limit this */ Integer.MAX_VALUE);
}

// From:
public String
asMailboxList()
{
	jmp(0);
	final String rv = pMailboxList();
	return cur() == -1 ? rv : null;
}

// To:
public String
asAddressList()
{
	jmp(0);
	final String rv = pAddressList();
	return cur() == -1 ? rv : null;
}

protected String
pAddressList()
{
	try (final Parser.Txn ofs = begin()) {
		String rv = "";
		final String a = pAddress();
		if (a == null) return null;
		ofs.commit();
		rv += a;
		while (cur() == ',') {
			accept();
			final String a2 = pAddress();
			if (a2 == null) break;
			ofs.commit();
			rv += a2;
		}
		return rv;
	}
}

protected String
pMailboxList()
{
	try (final Parser.Txn ofs = begin()) {
		String rv = "";
		final String m = pMailbox();
		if (m == null) return null;
		ofs.commit();
		rv += m;
		while (cur() == ',') {
			accept();
			final String m2 = pMailbox();
			if (m2 == null) break;
			ofs.commit();
			rv += m2;
		}
		return rv;
	}
}

protected String
pAddress()
{
	String rv;
	if ((rv = pMailbox()) != null) return rv;
	if ((rv = pGroup()) != null) return rv;
	return null;
}

protected String
pGroup()
{
	try (final Parser.Txn ofs = begin()) {
		String rv = "";
		final String dn = pDisplayName();
		if (dn == null) return null;
		if (cur() != ':') return null;
		accept();
		rv += dn;
		final String gl = pGroupList();
		if (gl != null) rv += gl;
		if (cur() != ';') return null;
		accept();
		pCFWS();
		return ofs.accept(rv);
	}
}

protected String
pGroupList()
{
	final String ml = pMailboxList();
	if (ml != null) return ml;
	return pCFWS() == null ? null : "";
}

protected String
pMailbox()
{
	final String na = pNameAddr();
	if (na != null) return na;
	final String as = pAddrSpec();
	if (as != null) return as;
	return null;
}

protected String
pNameAddr()
{
	try (final Parser.Txn ofs = begin()) {
		String rv = "";
		final String dn = pDisplayName();
		if (dn != null) rv += dn;
		final String aa = pAngleAddr();
		if (aa == null) return null;
		rv += aa;
		return ofs.accept(rv);
	}
}

protected String
pAngleAddr()
{
	try (final Parser.Txn ofs = begin()) {
		String rv = "";
		pCFWS();
		if (cur() != '<') return null;
		accept();
		final String as = pAddrSpec();
		if (as == null) return null;
		rv += as;
		if (cur() != '>') return null;
		accept();
		pCFWS();
		return ofs.accept(rv);
	}
}

protected String
pDisplayName()
{
	return pPhrase();
}

protected String
pPhrase()
{
	String rv = "";
	String s = pWord();
	if (s == null) return null;
	rv += s;
	while ((s = pWord()) != null) rv += s;
	return rv;
}

protected String
pWord()
{
        String rv;
        if ((rv = pAtom()) != null) return rv;
        if ((rv = pQuotedString()) != null) return rv;
        return null;
}

protected String
pAtom()
{
	try (final Parser.Txn ofs = begin()) {
		String rv = "";
		pCFWS();
		int c = pAtext();
		if (c == -1) return null;
		rv += c;
		while ((c = pAtext()) != -1) rv += c;
		pCFWS();
		return ofs.accept(rv);
	}
}

protected static boolean
is(final int c, final byte what)
{
	if (c < 0 || c > ASCII.length)
		return false;
	return (ASCII[c] & what) != 0;
}

protected int
pAtext()
{
	final int c = cur();
	if (is(c, IS_ATEXT)) {
		accept();
		return c;
	}
	return -1;
}

protected String
pDotAtomText()
{
	String rv = "";
	int c = pAtext();
	if (c == -1) return null;
	rv += c;
	while ((c = pAtext()) != -1) rv += c;
	while (cur() == '.' && is(peek(), IS_ATEXT)) {
		rv += '.';
		accept();
		while ((c = pAtext()) != -1) rv += c;
	}
	return rv;
}

protected String
pDotAtom()
{
	final int ofs = pos();
	pCFWS();
	final String rv = pDotAtomText();
	if (rv == null) {
		jmp(ofs);
		return null;
	}
	pCFWS();
	return rv;
}

protected int
pQtext()
{
	final int c = cur();
	if (is(c, IS_QTEXT)) {
		accept();
		return c;
	}
	return -1;
}

protected int
pCtext()
{
	final int c = cur();
	if (is(c, IS_CTEXT)) {
		accept();
		return c;
	}
	return -1;
}

protected int
pQuotedPair()
{
	if (cur() == '\\') {
		final int c = peek();
		if ((c >= 0x20 && c <= 0x7E) || c == 0x09) {
			accept();
			accept();
			return c;
		}
	}
	return -1;
}

protected int
pQcontent()
{
	final int qt = pQtext();
	return qt != -1 ? qt : pQuotedPair();
}

protected String
pQuotedString()
{
	try (final Parser.Txn ofs = begin()) {
		String rv = "";
		pCFWS();
		if (cur() != '"') return null;
		accept();
		while (true) {
			final String wsp = pFWS();
			if (wsp != null) rv += wsp; //XXX pFWS must unfold (drop the CRLF)
			final int qc = pQcontent();
			if (qc == -1)
				break;
			rv += qc;
		}
		// [FWS] after *([FWS] qcontent) already parsed above
		if (cur() != '"') return null;
		accept();
		pCFWS();
		return ofs.accept(rv);
	}
}



}
