package org.evolvis.tartools.rfc822;

/*-
 * Copyright © 2020 mirabilos (m@mirbsd.org)
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

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.val;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Represents an RFC822 (and successors) eMail address header content,
 * either From or To, or subsets. In domain literals (square brackets)
 * the General-address-literal syntax is not recognised (as downstream
 * MTAs cannot support it as no use is specified yet), and a IPv6 Zone
 * Identifier isn’t supported as it’s special local use only. Handling
 * of line endings is lenient: CRLF := ([CR] LF) / CR<p>
 *
 * Create a new instance via the {@link #of(String)} factory method by
 * passing it the address list string to analyse. Then call one of the
 * parse methods on the instance: {@link #asAddressList()} to validate
 * recipients, {@link #asMailboxList()} or {@link #forSender(boolean)}
 * for message senders (but read their JavaDoc). Validating unlabelled
 * addr-specs is possible with {@link #asAddrSpec()}.</p>
 *
 * @author mirabilos (t.glaser@tarent.de)
 */
public class Path extends Parser {

private static final byte F_ALPHA = 0x01;
private static final byte F_DIGIT = 0x02;
private static final byte F_HYPHN = 0x04;
private static final byte F_ATEXT = 0x08;
private static final byte F_QTEXT = 0x10;
private static final byte F_CTEXT = 0x20;
private static final byte F_DTEXT = 0x40;
private static final byte F_ABISF = (byte)0x80;

static final byte IS_ATEXT = F_ALPHA | F_DIGIT | F_HYPHN | F_ATEXT;
static final byte IS_QTEXT = F_QTEXT;
static final byte IS_CTEXT = F_CTEXT;
static final byte IS_DTEXT = F_DTEXT;
static final byte IS_ALNUM = F_ALPHA | F_DIGIT;
static final byte IS_ALNUS = F_ALPHA | F_DIGIT | F_HYPHN;
static final byte IS_XDIGIT = F_DIGIT | F_ABISF;

private static final byte[] ASCII = new byte[128];

static {
	Arrays.fill(ASCII, (byte)0);

	for (char c = 'A'; c <= 'Z'; ++c)
		ASCII[c] |= F_ALPHA;
	for (char c = 'a'; c <= 'z'; ++c)
		ASCII[c] |= F_ALPHA;
	for (char c = '0'; c <= '9'; ++c)
		ASCII[c] |= F_DIGIT;
	ASCII['-'] |= F_HYPHN;

	ASCII['!'] |= F_ATEXT;
	ASCII['#'] |= F_ATEXT;
	ASCII['$'] |= F_ATEXT;
	ASCII['%'] |= F_ATEXT;
	ASCII['&'] |= F_ATEXT;
	ASCII['\''] |= F_ATEXT;
	ASCII['*'] |= F_ATEXT;
	ASCII['+'] |= F_ATEXT;
	ASCII['/'] |= F_ATEXT;
	ASCII['='] |= F_ATEXT;
	ASCII['?'] |= F_ATEXT;
	ASCII['^'] |= F_ATEXT;
	ASCII['_'] |= F_ATEXT;
	ASCII['`'] |= F_ATEXT;
	ASCII['{'] |= F_ATEXT;
	ASCII['|'] |= F_ATEXT;
	ASCII['}'] |= F_ATEXT;
	ASCII['~'] |= F_ATEXT;

	ASCII['A'] |= F_ABISF;
	ASCII['B'] |= F_ABISF;
	ASCII['C'] |= F_ABISF;
	ASCII['D'] |= F_ABISF;
	ASCII['E'] |= F_ABISF;
	ASCII['F'] |= F_ABISF;
	ASCII['a'] |= F_ABISF;
	ASCII['b'] |= F_ABISF;
	ASCII['c'] |= F_ABISF;
	ASCII['d'] |= F_ABISF;
	ASCII['e'] |= F_ABISF;
	ASCII['f'] |= F_ABISF;

	ASCII[33] |= F_QTEXT;
	for (int d = 35; d <= 91; ++d)
		ASCII[d] |= F_QTEXT;
	for (int d = 33; d <= 39; ++d)
		ASCII[d] |= F_CTEXT;
	for (int d = 42; d <= 91; ++d)
		ASCII[d] |= F_CTEXT;
	for (int d = 93; d <= 126; ++d)
		ASCII[d] |= F_QTEXT | F_CTEXT;
	for (int d = 33; d <= 90; ++d)
		ASCII[d] |= F_DTEXT;
	for (int d = 94; d <= 126; ++d)
		ASCII[d] |= F_DTEXT;
}

protected static boolean
is(final int c, final byte what)
{
	if (c < 0 || c >= ASCII.length)
		return false;
	return (ASCII[c] & what) != 0;
}

protected static boolean
isAtext(final int c)
{
	return is(c, IS_ATEXT);
}

protected static boolean
isCtext(final int c)
{
	return is(c, IS_CTEXT);
}

protected static boolean
isDtext(final int c)
{
	return is(c, IS_DTEXT);
}

protected static boolean
isQtext(final int c)
{
	return is(c, IS_QTEXT);
}

/**
 * Representation for a substring of the input string, FWS unfolded
 *
 * <p>{@link #toString()} will return the (unfolded) wire representation,
 * {@link #getData()} the unfolded user representation. Neither are true
 * substrings of the input any more if unfolding, as in {@link #unfold(String)},
 * was necessary.</p>
 *
 * @author mirabilos (t.glaser@tarent.de)
 */
protected final class UnfoldedSubstring extends Substring {

	private final String string;

	private UnfoldedSubstring(final Substring ss, final String us)
	{
		super(ss);
		string = us;
	}

	private UnfoldedSubstring(final Substring ss, final String us,
	    final Object data)
	{
		super(ss, data);
		string = us;
	}

	/**
	 * Returns the string representation of this {@link Parser.Substring},
	 * in our case, an unfolded (see {@link #unfold(String)}) copy of the
	 * on-wire representation.
	 *
	 * @return String representation
	 */
	@Override
	public String toString()
	{
		return string;
	}

}

/**
 * Representation for a local-part (FWS unfolded) or a domain (dot-atom only)
 *
 * <p>Test {@link #isValid()} first.</p>
 *
 * @author mirabilos (t.glaser@tarent.de)
 */
@Getter
protected final class AddrSpecSIDE extends Substring {

	/**
	 * Whether this addr-spec side is actually valid or merely parses
	 * but fails further validations (length limits, semantics, etc.)
	 *
	 * @return true if valid, false otherwise
	 */
	private final boolean valid;

	private AddrSpecSIDE(final Substring src, final String us, final boolean v)
	{
		super(src, us);
		valid = v;
	}

	/**
	 * Returns the string representation of this local-part (FWS unfolded)
	 * or domain (dot-atom)
	 *
	 * @return String representation (identical to the user data)
	 */
	@Override
	public String toString()
	{
		return (String)getData();
	}

}

/**
 * Removes all occurrences of CR and/or LF from a string.
 *
 * @param s input string
 *
 * @return null if there was nothing to remove, a new shorter String otherwise
 */
public static String
unfold(final String s)
{
	final char[] buf = s.toCharArray();
	boolean eq = true;
	int dst = 0;
	for (char b : buf) {
		if (b == 0x0D || b == 0x0A)
			eq = false;
		else
			buf[dst++] = b;
	}
	return eq ? null : new String(buf, 0, dst);
}

/**
 * Unfolds FWS in the passed Substring if necessary
 *
 * @param ss {@link Substring} to unfold
 *
 * @return instance of an unfolded equivalent of the original substring
 */
protected Substring
unfold(final Substring ss)
{
	final String u = unfold(ss.toString());
	if (u == null)
		return ss;
	return new UnfoldedSubstring(ss, u);
}

/**
 * Represents the return value of something that can be a word production
 * (atom or quoted-string)
 *
 * <p>The {@code body} member can be:</p><ul>
 * <li>a raw substring of an atom, surrounding CFWS stripped ({@link #pAtom()})</li>
 * <li>an {@link UnfoldedSubstring} of a quoted-string including surrounding double
 * quotes, whose user data is dequoted and backslash-removed ({@link #pQuotedString()})</li>
 * </ul>
 *
 * <p>The {@code cfws} member (optional trailing CFWS) is <em>not</em> unfolded.</p>
 *
 * @author mirabilos (t.glaser@tarent.de)
 */
@AllArgsConstructor
private static final class Word {

	protected final Substring body;
	protected final Substring cfws;

}

/**
 * Methods all {@link Path} parser results implement.
 *
 * @author mirabilos (m@mirbsd.org)
 */
public interface ParserResult {

	/**
	 * Whether this parser result is actually valid or merely parses
	 * but fails further validations (length limits, semantics, etc.)
	 *
	 * @return true if valid, false otherwise
	 */
	boolean isValid();

	/**
	 * Returns this parser result in some useful format
	 *
	 * @return String
	 */
	@Override
	String toString();

}

/**
 * Representation for an addr-spec (eMail address)
 * comprised of localPart and domain
 *
 * @author mirabilos (t.glaser@tarent.de)
 */
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public static final class AddrSpec implements ParserResult {

	/**
	 * The local-part of the addr-spec, as it occurs in the addr-spec,
	 * i.e. dot-atom or quoted-string in their wire representation;
	 * the parsed string content is available as String getData()
	 *
	 * @return local-part of the addr-spec as {@link Substring}
	 */
	@NonNull
	final Substring localPart;

	/**
	 * The domain of the addr-spec, either dot-atom (host.example.com)
	 * or one of two forms of domain-literal: [192.0.2.1] for Legacy IP,
	 * [IPv6:2001:DB8:CAFE:1::1] for IP addresses; getData() contains
	 * either the domain as String or the IP address as {@link InetAddress}
	 *
	 * @return domain of the addr-spec as {@link Substring}
	 */
	@NonNull
	final Substring domain;

	/**
	 * Whether this addr-spec is actually valid according to DNS, SMTP,
	 * etc. (true) or merely parses as RFC822 addr-spec (false) and fails
	 * further validation (length limits, FQDN label syntax, etc.)
	 *
	 * @return true if valid, false otherwise
	 */
	final boolean valid;

	/**
	 * Returns the addr-spec as eMail address (in wire format)
	 *
	 * @return String localPart@domain
	 */
	@Override
	public String
	toString()
	{
		return localPart + "@" + domain;
	}

}

/**
 * Representation for an address (either mailbox or group)
 *
 * @author mirabilos (t.glaser@tarent.de)
 */
@Getter
public static final class Address implements ParserResult {

	/**
	 * Whether this address is a group (true) or a mailbox (false)
	 *
	 * @return true if a group, false if a mailbox
	 */
	final boolean group;

	/**
	 * The display-name of this mailbox.name-addr (optional) or
	 * group (mandatory) with the human-readable / parsed form
	 * with comments available via String {@link Substring#getData()}
	 * (in [@link UXAddress] cases, {@link Substring#toString()} (the
	 * “wire form”) may be empty whereas {@link Substring#getData()}
	 * can return the value the user provided)
	 *
	 * @return display-name as {@link Substring}
	 */
	final Substring label;

	/**
	 * The addr-spec behind this mailbox [isGroup()==false]
	 *
	 * @return addr-spec as {@link AddrSpec}
	 */
	final AddrSpec mailbox;

	/**
	 * The group-list behind this group [isGroup()==true], may be empty
	 *
	 * @return group-list as {@link List} of {@link Address}
	 */
	final List<Address> mailboxen;

	/**
	 * Whether all constituents are valid
	 *
	 * @return true if valid, false otherwise
	 */
	final boolean valid;

	private Address(final Substring label, final AddrSpec mailbox)
	{
		this.group = false;
		this.label = label;
		this.mailbox = mailbox;
		this.mailboxen = null;
		this.valid = mailbox.isValid();
	}

	private Address(final Substring label, final List<Address> mailboxen)
	{
		this.group = true;
		this.label = label;
		this.mailbox = null;
		this.mailboxen = mailboxen;
		this.valid = mailboxen.stream().allMatch(Address::isValid);
		// normally we’d need to check that all mailboxen are not group
	}

	/**
	 * Renders the mailbox or group as (non-wrapped) string, i.e.:
	 *
	 * <ul>
	 *         <li>localPart@domain (mailbox)</li>
	 *         <li>label &lt;localPart@domain&gt; (mailbox)</li>
	 *         <li>label:[group-list]; (group)</li>
	 * </ul>
	 *
	 * @return String rendered address
	 */
	@Override
	public String
	toString()
	{
		if (!group)
			return label == null ? mailbox.toString() :
			    String.format("%s <%s>", label, mailbox);
		return String.format("%s:%s;", label, mailboxen.stream().
		    map(Address::toString).collect(Collectors.joining(",")));
	}

}

/**
 * Representation for an address-list or a mailbox-list
 *
 * @author mirabilos (t.glaser@tarent.de)
 */
@Getter
public static final class AddressList implements ParserResult {

	/**
	 * The actual address-list or mailbox-list behind the scenes
	 * (which one it is depends on by which parser function this
	 * object was returned)
	 *
	 * @return address-list / mailbox-list as {@link List} of {@link Address}
	 */
	final List<Address> addresses;

	/**
	 * Whether all constituents are valid
	 *
	 * @return true if valid, false otherwise
	 */
	final boolean valid;

	/**
	 * Whether this is definitely an address-list (group addresses are
	 * present); note that if this is false, it may be either a mailbox-list
	 * or an address-list whose address members are all mailbox)
	 *
	 * @return true if group {@link Address}es are present
	 */
	@SuppressWarnings("squid:S1700")
	final boolean addressList;

	private AddressList(final List<Address> addresses)
	{
		this.addresses = addresses;
		valid = /*!addresses.isEmpty() &&*/
		    addresses.stream().allMatch(Address::isValid);
		addressList = addresses.stream().anyMatch(Address::isGroup);
	}

	/**
	 * Returns the address-list or mailbox-list as (non-wrapped) string
	 *
	 * @return String address/mailbox *( ", " address/mailbox )
	 */
	@Override
	public String
	toString()
	{
		return addresses.stream().
		    map(Address::toString).collect(Collectors.joining(", "));
	}

	/**
	 * Returns all invalid constituents as ", "-separated string,
	 * for error message construction
	 *
	 * @return null if all constituents are valid, a String otherwise
	 */
	public String
	invalidsToString()
	{
		if (valid)
			return null;
		return addresses.stream().
		    filter(((Predicate<Address>)Address::isValid).negate()).
		    map(Address::toString).collect(Collectors.joining(", "));

	}

	/**
	 * Flattens the constituents into a list of their formatted
	 * representations, see {@link Address#toString()}
	 *
	 * @return list of formatted strings, each an address (or mailbox)
	 */
	public List<String>
	flattenAddresses()
	{
		return addresses.stream().
		    map(Address::toString).collect(Collectors.toList());
	}

	/**
	 * Flattens the constituents into their individual addr-spec members,
	 * for use by e.g. SMTP sending (Forward-Path construction)
	 *
	 * @return list of addr-spec strings
	 */
	public List<String>
	flattenAddrSpecs()
	{
		val rv = new ArrayList<String>();
		for (final Address address : addresses)
			if (address.isGroup())
				for (final Address mailbox : address.mailboxen)
					rv.add(mailbox.mailbox.toString());
			else
				rv.add(address.mailbox.toString());
		return rv;
	}

}

/**
 * Creates and initialises a new parser for eMail addresses.
 *
 * @param addresses to parse
 *
 * @return null if addresses was null or very large, the new instance otherwise
 */
public static Path
of(final String addresses)
{
	return Parser.of(Path.class, addresses);
}

/**
 * Private constructor, use the factory method {@link #of(String)} instead
 *
 * @param input string to analyse
 */
protected Path(final String input)
{
	super(input, /* arbitrary but extremely large already */ 131072);
}

/**
 * Parses the address as mailbox-list, e.g. for the From and Resent-From headers
 * (but see {@link #asAddressList()} for RFC6854’s RFC2026 §3.3(d) Limited Use)
 *
 * @return parser result; remember to call isValid() on it first!
 */
public AddressList
asMailboxList()
{
	jmp(0);
	final AddressList rv = pMailboxList();
	return cur() == -1 ? rv : null;
}

/**
 * Parses the address for the Sender and Resent-Sender headers
 *
 * These headers normally use the address production, but RFC6854 allows for
 * the mailbox production, with the RFC2026 §3.3(d) Limited Use caveat that
 * permits it but only for specific circumstances.
 *
 * @param allowRFC6854forLimitedUse use address instead of mailbox parsing
 *
 * @return parser result; remember to call isValid() on it first!
 */
public Address
forSender(final boolean allowRFC6854forLimitedUse)
{
	jmp(0);
	final Address rv = allowRFC6854forLimitedUse ? pAddress() : pMailbox();
	return cur() == -1 ? rv : null;
}

/**
 * Parses the address as addr-spec (unlabelled address)
 *
 * This method is mostly used in input validation. In most cases,
 * use {@link #forSender(boolean)}(false) instead which allows
 * “user &lt;lcl@example.com&gt;” then extract the addr-spec
 * “lcl@example.com” from the return value.
 *
 * @return parser result; remember to call isValid() on it first!
 */
public AddrSpec
asAddrSpec()
{
	jmp(0);
	final AddrSpec rv = pAddrSpec();
	return cur() == -1 ? rv : null;
}

/**
 * Parses the address as address-list, e.g. for the Reply-To, To, Cc,
 * (optionally) Bcc, Resent-To, Resent-Cc and (optionally) Resent-Bcc
 * headers. RFC6854 (under RFC2026 §3.3(d) Limited Use circumstances)
 * allows using this for the From and Resent-From headers, normally
 * covered by the {@link #asMailboxList()} method.
 *
 * @return parser result; remember to call isValid() on it first!
 */
public AddressList
asAddressList()
{
	jmp(0);
	final AddressList rv = pAddressList();
	return cur() == -1 ? rv : null;
}

protected AddressList
pAddressList()
{
	try (val ofs = new Parser.Txn()) {
		final Address a = pAddress();
		if (a == null)
			return null;
		ofs.commit();
		val rv = new ArrayList<Address>();
		rv.add(a);
		while (cur() == ',') {
			accept();
			final Address a2 = pAddress();
			if (a2 == null)
				break;
			ofs.commit();
			rv.add(a2);
		}
		return new AddressList(rv);
	}
}

protected AddressList
pMailboxList()
{
	try (val ofs = new Parser.Txn()) {
		final Address m = pMailbox();
		if (m == null)
			return null;
		ofs.commit();
		val rv = new ArrayList<Address>();
		rv.add(m);
		while (cur() == ',') {
			accept();
			final Address m2 = pMailbox();
			if (m2 == null)
				break;
			ofs.commit();
			rv.add(m2);
		}
		return new AddressList(rv);
	}
}

protected Address
pAddress()
{
	Address rv;
	if ((rv = pMailbox()) != null)
		return rv;
	if ((rv = pGroup()) != null)
		return rv;
	return null;
}

protected Address
pGroup()
{
	try (val ofs = new Parser.Txn()) {
		final Substring dn = pDisplayName();
		if (dn == null)
			return null;
		if (cur() != ':')
			return null;
		accept();
		// { [pGroupList]
		final AddressList ml = pMailboxList();
		if (ml == null)
			pCFWS();
		val gl = ml == null ? new ArrayList<Address>() : ml.addresses;
		// } [pGroupList]
		if (cur() != ';')
			return null;
		accept();
		pCFWS();
		return ofs.accept(new Address(dn, gl));
	}
}

protected Address
pMailbox()
{
	final Address na = pNameAddr();
	if (na != null)
		return na;
	final AddrSpec as = pAddrSpec();
	if (as != null)
		return new Address(null, as);
	return null;
}

protected Address
pNameAddr()
{
	try (val ofs = new Parser.Txn()) {
		final Substring dn = pDisplayName();
		final AddrSpec aa = pAngleAddr();
		if (aa == null)
			return null;
		return ofs.accept(new Address(dn, aa));
	}
}

protected AddrSpec
pAngleAddr()
{
	try (val ofs = new Parser.Txn()) {
		pCFWS();
		if (cur() != '<')
			return null;
		accept();
		final AddrSpec as = pAddrSpec();
		if (as == null)
			return null;
		if (cur() != '>')
			return null;
		accept();
		pCFWS();
		return ofs.accept(as);
	}
}

protected Substring
pDisplayName()
{
	return pPhrase();
}

protected Substring
pPhrase()
{
	final int beg = pos();
	pCFWS();
	final int ofs = pos();
	// jmp(beg); but pWord() starts with pCFWS() in all cases anyway
	Word w = pWord();
	if (w == null) {
		jmp(beg);
		return null;
	}
	StringBuilder d = new StringBuilder();
	int lpos;
	do {
		d.append(w.body.getData() == null ? w.body.toString() :
		    (String)w.body.getData());
		lpos = w.body.end;
		val wsp = w.cfws;
		w = pWord();
		if (w != null && wsp != null)
			d.append(unfold(wsp).toString());
	} while (w != null);
	return unfold(new Substring(ofs, lpos, d.toString()));
}

protected Word
pWord()
{
	Word rv;
	if ((rv = pAtom()) != null)
		return rv;
	if ((rv = pQuotedString()) != null)
		return rv;
	return null;
}

/**
 * Returns the parse result of the atom production:
 *
 * result.body is a raw Substring of the atom, with surrounding CFWS stripped
 * (no unfolding necessary), no extra data
 *
 * result.cfws is null or the trailing CFWS as raw Substring, not unfolded
 *
 * @return result (see above)
 */
protected Word
pAtom()
{
	try (val ofs = new Parser.Txn()) {
		pCFWS();
		if (!isAtext(cur()))
			return null;
		ofs.commit();
		skip(Path::isAtext);
		val atom = ofs.substring();
		val wsp = pCFWS();
		return ofs.accept(new Word(atom, wsp));
	}
}

protected int
pQuotedPair()
{
	if (cur() == '\\') {
		final int c = peek();
		if ((c >= 0x20 && c <= 0x7E) || c == 0x09) {
			bra(2);
			return c;
		}
	}
	return -1;
}

protected int
pQcontent()
{
	final int c = cur();
	if (isQtext(c)) {
		accept();
		return c;
	}
	return pQuotedPair();
}

/**
 * Returns the parse result of the quoted-string production:
 *
 * result.body is an {@link UnfoldedSubstring} of the entire quoted string, with
 * surrounding double quotes; its String data is dequoted and backslash-removed
 *
 * result.cfws is null or the trailing CFWS as raw Substring, not unfolded
 *
 * @return result (see above)
 */
protected Word
pQuotedString()
{
	try (val ofs = new Parser.Txn()) {
		pCFWS();
		if (cur() != '"')
			return null;
		final int content = pos();
		accept();

		StringBuilder rv = new StringBuilder();
		while (true) {
			val wsp = pFWS();
			if (wsp != null)
				rv.append(unfold(wsp).toString());
			final int qc = pQcontent();
			if (qc == -1)
				break;
			rv.append((char)qc);
		}
		// [FWS] after *([FWS] qcontent) already parsed above
		if (cur() != '"')
			return null;
		accept();
		val qs = unfold(new Substring(content, pos(), rv.toString()));
		val wsp = pCFWS();
		return ofs.accept(new Word(qs, wsp));
	}
}

static boolean
isWSP(final int cur)
{
	return cur == 0x20 || cur == 0x09;
}

/**
 * Parses FWS
 *
 * @return raw Substring, not unfolded
 */
protected Substring
pFWS()
{
	final int beg = pos();
	Substring w = null;

	int c = cur();
	if (isWSP(c)) {
		c = skip(Path::isWSP);
		w = new Substring(beg, pos());
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

	skip(Path::isWSP);
	return new Substring(beg, pos());
}

protected boolean
pCcontent()
{
	if (isCtext(cur())) {
		accept();
		return true;
	}
	return pQuotedPair() != -1 || pComment() != null;
}

/**
 * Parses comment
 *
 * @return raw Substring, not unfolded
 *     (unfolded is human-visible form for now; may wish to simplify quoted-pairs)
 */
protected Substring
pComment()
{
	try (val ofs = new Parser.Txn()) {
		if (cur() != '(')
			return null;
		accept();
		do {
			pFWS();
		} while (pCcontent());
		// [FWS] after *([FWS] ccontent) already parsed above
		if (cur() != ')')
			return null;
		accept();
		return ofs.accept(ofs.substring());
	}
}

/**
 * Parses CFWS
 *
 * @return raw Substring, not unfolded
 */
protected Substring
pCFWS()
{
	final int beg = pos();
	val wsp = pFWS();
	// second alternative (FWS⇒success or null⇒failure)?
	if (pComment() == null)
		return wsp;
	// first alternative, at least one comment, optional FWS before
	do {
		pFWS();
	} while (pComment() != null);
	// [FWS] after 1*([FWS] comment) already parsed above
	return new Substring(beg, pos());
}

protected Substring
pDotAtom()
{
	try (val ofs = new Parser.Txn()) {
		pCFWS();
		if (!isAtext(cur()))
			return null;
		ofs.commit();
		// { pDotAtomText
		int c;
		do {
			accept(); // first round: first atext; other rounds: dot
			c = skip(Path::isAtext);
		} while (c == '.' && isAtext(peek()));
		// } pDotAtomText
		val rv = ofs.substring();
		pCFWS();
		return ofs.accept(rv);
	}
}

protected AddrSpecSIDE
pLocalPart()
{
	final Substring da = pDotAtom();
	final Word qs = da == null ? pQuotedString() : null;
	if (da == null && qs == null)
		return null;
	final Substring ss = da == null ? qs.body : da;
	final String us = ss.toString();
	// us is always unfolded because:
	// - pDotAtom returns a raw Substring comprised of atext and ‘.’ only
	// - pQuotedString returns UnfoldedSubstring
	boolean v = us.length() <= 64 && us.indexOf(0x09) == -1;
	return new AddrSpecSIDE(ss, us, v);
}

protected Substring
pDomainLiteral()
{
	try (val ofs = new Parser.Txn()) {
		pCFWS();
		if (cur() != '[')
			return null;
		final int content = pos();
		accept();
		pFWS();
		while (isDtext(cur())) {
			accept();
			pFWS();
		}
		if (cur() != ']')
			return null;
		accept();
		val rv = new Substring(content, pos());
		pCFWS();
		return ofs.accept(rv);
	}
}

protected Substring
pDomain()
{
	final Substring da = pDotAtom();
	if (da != null) {
		// dot-atom form of domain, does not need unfolding
		final String us = da.toString();
		boolean v = FQDN.isDomain(us);
		return new AddrSpecSIDE(da, us, v);
	}
	final Substring dl = pDomainLiteral();
	if (dl == null)
		return null;
	final String dls = dl.toString();
	final String dlu = unfold(dls);
	final String us = dlu == null ? dls : dlu;
	// validation "must not contain HTAB" implicit from IPAddress check
	InetAddress v;
	if (us.toLowerCase(Locale.ROOT).startsWith("[ipv6:")) {
		final String addr = us.substring(6, us.length() - 1);
		v = IPAddress.v6(addr);
	} else {
		final String addr = us.substring(1, us.length() - 1);
		v = IPAddress.v4(addr);
	}
	return new UnfoldedSubstring(dl, us, v);
}

protected AddrSpec
pAddrSpec()
{
	try (val ofs = new Parser.Txn()) {
		val lp = pLocalPart();
		if (lp == null)
			return null;
		if (cur() != '@')
			return null;
		accept();
		val dom = pDomain();
		if (dom == null)
			return null;
		final boolean v = lp.isValid() && ((dom instanceof AddrSpecSIDE) ?
		    ((AddrSpecSIDE)dom).isValid() : dom.getData() != null) &&
		    /* local-part + '@' + domain; octets = characters (ASCII) */
		    (lp.toString().length() + 1 + dom.toString().length()) <= 254;
		return ofs.accept(new AddrSpec(lp, dom, v));
	}
}

}
