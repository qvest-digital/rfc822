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

import java.util.Arrays;

/**
 * Represents an RFC822 (and successors) eMail address header content,
 * either From or To, or subsets. In domain literals (square brackets)
 * the General-address-literal syntax is not recognised (as downstream
 * MTAs cannot support it as no use is specified yet), and a IPv6 Zone
 * Identifier isn’t supported as it’s special local use only. Handling
 * of line endings is lenient: CRLF := ([CR] LF) / CR
 *
 * Create a new instance via the {@link #of(String)} factory method by
 * passing it the address list string to analyse. Then call one of the
 * parse methods on the instance: {@link #asAddressList()} to validate
 * recipients, {@link #asMailboxList()} or {@link #forSender(boolean)}
 * for message senders (but read their JavaDoc).
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
private static final byte F_DTEXT = 0x20;

private static final byte IS_ATEXT = F_ALPHA | F_DIGIT | F_HYPHN | F_ATEXT;
private static final byte IS_QTEXT = F_QTEXT;
private static final byte IS_CTEXT = F_CTEXT;
private static final byte IS_DTEXT = F_DTEXT;

private static final byte[] ASCII = new byte[128];

static {
	Arrays.fill(ASCII, (byte)0);

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
	for (int d = 33; d <= 90; ++d)
		ASCII[d] = F_DTEXT;
	for (int d = 94; d <= 126; ++d)
		ASCII[d] = F_DTEXT;
}

/**
 * Creates and initialises a new parser for eMail addresses.
 *
 * @param addresses to parse
 *
 * @return null if addresses was null or very large, the new instance otherwise
 */
public static Address
of(final String addresses)
{
	final Address obj = new Address(addresses);
	return obj.s() == null ? null : obj;
}

/**
 * Private constructor, use the factory method {@link #of(String)} instead
 *
 * @param input string to analyse
 */
protected Address(final String input)
{
	super(input, /* arbitrary but extremely large already */ 131072);
}

/**
 * Parses the address as mailbox-list, e.g. for the From and Resent-From headers
 * (but see {@link #asAddressList()} for RFC6854’s RFC2026 §3.3(d) Limited Use)
 *
 * @return parser result
 */
public String
asMailboxList()
{
	jmp(0);
	final String rv = pMailboxList();
	return cur() == -1 ? rv : null;
}

/**
 * Parses the address for the Sender and Resent-Sender headers
 *
 * These headers normally use the address production, but RFC6854 allows for
 * the mailbox production, with the RFC2026 §3.3(d) Limited Use caveat that
 * permits it but only for specific circumstances.
 *
 * @param allowRFC6854forLimitedUse use mailbox instead of address parsing
 *
 * @return parser result
 */
public String
forSender(boolean allowRFC6854forLimitedUse)
{
	jmp(0);
	final String rv = allowRFC6854forLimitedUse ? pAddress() : pMailbox();
	return cur() == -1 ? rv : null;
}

/**
 * Parses the address as address-list, e.g. for the Reply-To, To, Cc,
 * (optionally) Bcc, Resent-To, Resent-Cc and (optionally) Resent-Bcc
 * headers. RFC6854 (under RFC2026 §3.3(d) Limited Use circumstances)
 * allows using this for the From and Resent-From headers, normally
 * covered by the {@link #asMailboxList()} method.
 *
 * @return parser result
 */
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
		if (a == null)
			return null;
		ofs.commit();
		rv += a;
		while (cur() == ',') {
			accept();
			final String a2 = pAddress();
			if (a2 == null)
				break;
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
		if (m == null)
			return null;
		ofs.commit();
		rv += m;
		while (cur() == ',') {
			accept();
			final String m2 = pMailbox();
			if (m2 == null)
				break;
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
	if ((rv = pMailbox()) != null)
		return rv;
	if ((rv = pGroup()) != null)
		return rv;
	return null;
}

protected String
pGroup()
{
	try (final Parser.Txn ofs = begin()) {
		String rv = "";
		final String dn = pDisplayName();
		if (dn == null)
			return null;
		if (cur() != ':')
			return null;
		accept();
		rv += dn;
		final String gl = pGroupList();
		if (gl != null)
			rv += gl;
		if (cur() != ';')
			return null;
		accept();
		pCFWS();
		return ofs.accept(rv);
	}
}

protected String
pGroupList()
{
	final String ml = pMailboxList();
	if (ml != null)
		return ml;
	return pCFWS() == null ? null : "";
}

protected String
pMailbox()
{
	final String na = pNameAddr();
	if (na != null)
		return na;
	final String as = pAddrSpec();
	if (as != null)
		return as;
	return null;
}

protected String
pNameAddr()
{
	try (final Parser.Txn ofs = begin()) {
		String rv = "";
		final String dn = pDisplayName();
		if (dn != null)
			rv += dn;
		final String aa = pAngleAddr();
		if (aa == null)
			return null;
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
		if (cur() != '<')
			return null;
		accept();
		final String as = pAddrSpec();
		if (as == null)
			return null;
		rv += as;
		if (cur() != '>')
			return null;
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
	if (s == null)
		return null;
	rv += s;
	while ((s = pWord()) != null)
		rv += s;
	return rv;
}

protected String
pWord()
{
	String rv;
	if ((rv = pAtom()) != null)
		return rv;
	if ((rv = pQuotedString()) != null)
		return rv;
	return null;
}

protected String
pAtom()
{
	try (final Parser.Txn ofs = begin()) {
		String rv = "";
		pCFWS();
		int c = pAtext();
		if (c == -1)
			return null;
		rv += c;
		while ((c = pAtext()) != -1)
			rv += c;
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
	if (c == -1)
		return null;
	rv += c;
	while ((c = pAtext()) != -1)
		rv += c;
	while (cur() == '.' && is(peek(), IS_ATEXT)) {
		rv += '.';
		accept();
		while ((c = pAtext()) != -1)
			rv += c;
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
		if (cur() != '"')
			return null;
		accept();
		while (true) {
			final String wsp = pFWS();
			if (wsp != null)
				rv += wsp;
			final int qc = pQcontent();
			if (qc == -1)
				break;
			rv += qc;
		}
		// [FWS] after *([FWS] qcontent) already parsed above
		if (cur() != '"')
			return null;
		accept();
		pCFWS();
		return ofs.accept(rv);
	}
}

protected String
pCcontent()
{
	int c;
	if ((c = pCtext()) != -1)
		return String.valueOf((char)c);
	if ((c = pQuotedPair()) != -1)
		return String.valueOf((char)c);
	return pComment();
}

protected String
pComment()
{
	try (final Parser.Txn ofs = begin()) {
		String rv = "";
		if (cur() != '(')
			return null;
		accept();
		while (true) {
			final String wsp = pFWS();
			if (wsp != null)
				rv += wsp;
			final String cc = pCcontent();
			if (cc == null)
				break;
			rv += cc;
		}
		// [FWS] after *([FWS] ccontent) already parsed above
		if (cur() != ')')
			return null;
		accept();
		return ofs.accept(rv);
	}
}

protected String
pCFWS()
{
	String wsp = pFWS();
	String c = pComment();
	// second alternative (FWS⇒success or null⇒failure)?
	if (c == null)
		return wsp;
	// first alternative, at least one comment, optional FWS before
	String rv = wsp == null ? c : wsp + c;
	while (true) {
		wsp = pFWS();
		if (wsp != null)
			rv += wsp;
		if ((c = pComment()) == null) {
			// [FWS] after 1*([FWS] comment) already parsed above
			return rv;
		}
		rv += c;
	}
}

static boolean
isWSP(final int cur)
{
	return cur == 0x20 || cur == 0x09;
}

protected String
pFWS()
{
	String w = null;

	int c = cur();
	if (isWSP(c)) {
		final int beg = pos();
		c = skip(Address::isWSP);
		w = s().substring(beg, pos());
	}

	if (c != 0x0D && c != 0x0A)
		return w;
	final int c2 = peek();
	if (c == 0x0D && c2 == 0x0A) {
		// possibly need backtracking
		if (!isWSP(bra(2))) {
			bra(-2);
			return w;
		}
	} else {
		if (!isWSP(c2))
			return w;
		accept();
	}

	final int p1 = pos();
	skip(Address::isWSP);
	final int p2 = pos();
	final String w2 = s().substring(p1, p2);
	// unfold
	return w == null ? w2 : w + w2;
}

protected String
pAddrSpec()
{
	try (final Parser.Txn ofs = begin()) {
		final String lp = pLocalPart();
		if (lp == null)
			return null;
		if (cur() != '@')
			return null;
		accept();
		final String dom = pDomain();
		if (dom == null)
			return null;
		// pass on validation results of lp and dom
		return ofs.accept(lp + "@" + dom);
	}
}

protected String
pLocalPart()
{
	String rv = pDotAtom();
	if (rv == null)
		rv = pQuotedString();
	if (rv == null)
		return null;
	// validate
	return rv;
}

protected String
pDomain()
{
	String rv = pDotAtom();
	if (rv == null)
		rv = pDomainLiteral();
	if (rv == null)
		return null;
	// validate
	return rv;
}

protected String
pDomainLiteral()
{
	try (final Parser.Txn ofs = begin()) {
		String rv = "";
		pCFWS();
		if (cur() != '[')
			return null;
		accept();
		while (true) {
			final String wsp = pFWS();
			if (wsp != null)
				rv += wsp;
			final int dt = pDtext();
			if (dt == -1)
				break;
			rv += dt;
		}
		// [FWS] after *([FWS] dtext) already parsed above
		if (cur() != ']')
			return null;
		accept();
		pCFWS();
		return ofs.accept(rv);
	}
}

protected int
pDtext()
{
	final int c = cur();
	if (is(c, IS_DTEXT)) {
		accept();
		return c;
	}
	return -1;
}

}
