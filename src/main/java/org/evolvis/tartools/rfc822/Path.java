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
 * <p>Represents an eMail address header content (parser). That is,
 * RFC822 (and successors) {@code From}, {@code To}, and subsets, for use on
 * the public internet.
 * Handling of line endings is lenient: {@code CRLF := ([CR] LF) / CR}</p>
 *
 * <p>In domain literals (square brackets), the {@code General-address-literal}
 * syntax is not recognised because downstream MUAs cannot support it as no use
 * is specified at the moment. Similarily, an IPv6 scope (Zone Identifier) is
 * not supported because this parser targets use on the general internet. This
 * class is concerned with on-wire formats; separate classes will implement
 * MIME support and the likes later.</p>
 *
 * <p>To use, create a new instance via the {@link #of(String)} factory method
 * passing the string to analyse for eMail address(es). Then call one of the
 * parse methods on the instance, depending on what to expect:</p><ul>
 * <li>{@link #asAddrSpec()} checks for unlabelled {@code addr-spec}, such as
 * {@code foo@example.com}, which are useful for MSA invocations.</li>
 * <li>{@link #forSender(boolean)} with {@code false} argument validates one
 * {@code mailbox}, that is {@code Foo <foo@example.com>}, such as used for
 * the {@code Sender} header. Labels must be ASCII and confirm to the RFC.</li>
 * <li>{@link #forSender(boolean)} with {@code true} argument validates one
 * {@code address}, that is <i>either</i> a {@code mailbox} as above <i>or</i>
 * a {@code group} ({@code Test:a@example.com,b@example.com;}); RFC6854 adds them
 * to {@code Sender} headers under the RFC2026 §3.3(d) Limited Use caveat.</li>
 * <li>{@link #asMailboxList()} validates a, comma-separated, list of mailboxen
 * as above, normally for the {@code From} header.</li>
 * <li>{@link #asAddressList()} validates a comma-separated list that can
 * include a mix of {@code mailbox} and {@code group} addresses and normally is
 * used for recipient headers ({@code To},&nbsp;…) but, under the same Limited
 * Use caveat, can be used per RFC6854 for a {@code From} (and like) header.</li>
 * </ul>
 * <p>All of these return an instance of {@link ParserResult} or {@code null} if
 * the parsing failed; {@link ParserResult#isValid()} will return true only if,
 * in addition, extra syntax and semantic checks passed; only if so, the address
 * list can be used on the public internet safely; {@link ParserResult#toString()}
 * pretty-prints the on-wire representation. Some result objects may have extra
 * methods that can be useful.</p>
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
 * <p>Representation for a substring of the input string, FWS unfolded.</p>
 *
 * <p>{@link #toString()} will return the (unfolded) wire representation,
 * getData() the unfolded user representation. Neither are true
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
	public String
	toString()
	{
		return string;
	}

}

/**
 * <p>Representation for a local-part (FWS unfolded) or a domain (dot-atom only).</p>
 * <p>Check isValid() first.</p>
 *
 * @author mirabilos (t.glaser@tarent.de)
 */
@Getter
protected final class AddrSpecSIDE extends Substring {

	/**
	 * Whether this addr-spec side is actually valid or merely parses
	 * but fails further validations (length limits, semantics, etc).
	 */
	private final boolean valid;

	private AddrSpecSIDE(final Substring src, final String us, final boolean v)
	{
		super(src, us);
		valid = v;
	}

	/**
	 * Returns the string representation of this local-part (FWS unfolded)
	 * or domain (dot-atom).
	 *
	 * @return String representation (identical to the user data)
	 */
	@Override
	public String
	toString()
	{
		return (String)getData();
	}

}

/**
 * Removes all occurrences of CR and/or LF from a string.
 *
 * @param s input string
 *
 * @return null if there was nothing to remove, a new shorter {@link String} otherwise
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
 * Unfolds FWS in the passed Substring if necessary.
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
 * <p>Represents the return value of something that can be a word production
 * (atom or quoted-string).</p>
 *
 * <p>The {@code body} member can be:</p><ul>
 * <li>a raw substring of an atom, surrounding CFWS stripped ({@link #pAtom()})</li>
 * <li>an {@link UnfoldedSubstring} of a quoted-string including surrounding double
 * quotes, whose user data is dequoted and backslash-removed ({@link #pQuotedString()})</li>
 * </ul>
 *
 * <p>The {@code cfws} member (optional trailing CFWS) is <i>not</i> unfolded.</p>
 *
 * @author mirabilos (t.glaser@tarent.de)
 */
@SuppressWarnings("ProtectedMemberInFinalClass")
@AllArgsConstructor
private static final class Word {

	protected final Substring body;
	protected final Substring cfws;

}

/**
 * Methods all {@link Path} parser results implement.
 *
 * @author mirabilos (m@mirbsd.org)
 * @see #isValid()
 * @see #toString()
 */
public interface ParserResult {

	/**
	 * Whether this parser result is actually valid or merely parses
	 * but fails further validations (length limits, semantics, etc).
	 *
	 * @return true if valid, false otherwise
	 */
	boolean isValid();

	/**
	 * Returns this parser result in some useful format.
	 *
	 * @return String representation
	 */
	@Override
	String toString();

}

/**
 * <p>Representation for an {@code addr-spec} (eMail address). These are
 * comprised of {@code localPart} and {@code domain}.</p>
 *
 * @author mirabilos (t.glaser@tarent.de)
 */
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public static final class AddrSpec implements ParserResult {

	/**
	 * The {@code local-part} of the {@code addr-}spec, as it occurs in the
	 * addr-spec, i.e. {@code dot-atom} or {@code quoted-string} in their wire
	 * representation; the parsed (unquoted) string content is available as
	 * {@link String} via {@link Substring#getData()}.
	 */
	@NonNull
	final Substring localPart;

	/**
	 * The {@code domain} of the {@code addr-spec}, either {@code dot-atom}
	 * ({@code host.example.com}) or one of two forms of {@code domain-literal}:
	 * {@code [192.0.2.1]} for Legacy IP, {@code [IPv6:2001:DB8:CAFE:1::1]}
	 * for IP addresses; {@link Substring#getData()} contains either the
	 * domain as {@link String} or the IP address as {@link InetAddress}.
	 */
	@NonNull
	final Substring domain;

	/**
	 * Whether this {@code addr-spec} is actually valid according to DNS,
	 * SMTP, etc. (true) or merely parses as RFC822 {@code addr-spec} and
	 * fails further validation (length limits, FQDN label syntax, etc).
	 */
	final boolean valid;

	/**
	 * Returns the {@code addr-spec} as eMail address (in wire format).
	 *
	 * @return String {@code localPart@domain}
	 */
	@Override
	public String
	toString()
	{
		return localPart + "@" + domain;
	}

}

/**
 * Representation for an {@code address} (either {@code mailbox} or {@code group}).
 *
 * @author mirabilos (t.glaser@tarent.de)
 */
@Getter
public static final class Address implements ParserResult {

	/**
	 * Whether this address is a {@code group} (true) or a {@code mailbox} (false).
	 */
	final boolean group;

	/**
	 * The {@code display-name} of this {@code mailbox}.{@code name-addr}
	 * (optional) or {@code group} (mandatory) with the human-readable /
	 * parsed form with comments available from {@link Substring#getData()}
	 * as String (in [@link UXAddress] cases this can return the value the
	 * user passed even if the wire form from {@link Substring#toString()}
	 * may be empty or (maybe later) MIME-encoded).
	 */
	final Substring label;

	/**
	 * The {@code addr-spec} behind this mailbox [isGroup()==false].
	 */
	final AddrSpec mailbox;

	/**
	 * The {@code group-list} behind this group [isGroup()==true], may be empty.
	 */
	final List<Address> mailboxen;

	/**
	 * Whether all constituents are valid.
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
	 * <p>Renders the mailbox or group as (non-wrapped) string.
	 * That is:</p><ul>
	 * <li>{@code localPart@domain} (mailbox)</li>
	 * <li>{@code label <localPart@domain>} (mailbox)</li>
	 * <li>{@code label:[group-list];} (group)</li>
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
 * Representation for an {@code address-list} or a {@code mailbox-list}.
 *
 * @author mirabilos (t.glaser@tarent.de)
 */
@Getter
public static final class AddressList implements ParserResult {

	/**
	 * The actual {@code address-list} or {@code mailbox-list} behind the
	 * scenes (which one it is depends on by which parser function this
	 * object was returned).
	 */
	final List<Address> addresses;

	/**
	 * Whether all constituents are valid.
	 */
	final boolean valid;

	/**
	 * Whether this is definitely an {@code address-list} ({@code group}
	 * addresses are present); note that if this is false, it may be either
	 * a {@code mailbox-list} or an {@code address-list} whose {@code address}
	 * members are all {@code mailbox}en.
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
	 * Returns the {@code address-list} or {@code mailbox-list} as (non-wrapped) string.
	 *
	 * @return String address/mailbox *( {@code ", "} address/mailbox )
	 */
	@Override
	public String
	toString()
	{
		return addresses.stream().
		    map(Address::toString).collect(Collectors.joining(", "));
	}

	/**
	 * Returns all invalid constituents as {@code ", "}-separated string,
	 * for error message construction.
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
	 * Flattens the constituents into a list of their formatted representations.
	 *
	 * @return list of formatted strings, each an address (or mailbox)
	 *
	 * @see Address#toString()
	 */
	public List<String>
	flattenAddresses()
	{
		return addresses.stream().
		    map(Address::toString).collect(Collectors.toList());
	}

	/**
	 * Flattens the constituents into their individual {@code addr-spec}
	 * members, for use by e.g. SMTP sending (Forward-Path construction).
	 *
	 * @return {@link List} of {@code addr-spec} {@link String}s
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
 * Creates and initialises a new (strict) parser for eMail addresses.
 *
 * @param addresses to parse
 *
 * @return null if {@code addresses} was null or very large,
 *     the new parser instance otherwise
 */
public static Path
of(final String addresses)
{
	return Parser.of(Path.class, addresses);
}

/**
 * Private constructor. Use the factory method {@link #of(String)} instead.
 *
 * @param input string to analyse
 */
protected Path(final String input)
{
	super(input, /* arbitrary but extremely large already */ 131072);
}

/**
 * Parses the address as {@code mailbox-list}, such as for the {@code From}
 * and {@code Resent-From} headers. See {@link #asAddressList()} for RFC6854’s
 * RFC2026 §3.3(d) Limited Use though.
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
 * <p>Parses the address for the {@code Sender} and {@code Resent-Sender} headers.</p>
 *
 * <p>These headers normally use the {@code mailbox} production, but RFC6854
 * allows for the {@code address} production under the RFC2026 §3.3(d) Limited
 * Use caveat that permits it but only for specific circumstances.</p>
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
 * <p>Parses the address as {@code addr-spec} (unlabelled address).</p>
 *
 * <p>This method is mostly used in input validation or for constructing
 * arguments for invoking an MSA. It may be better in most cases to instead
 * use {@link #forSender(boolean)}(false) which permits {@code mailbox}en
 * like “{@code user <lcl@example.com>}” then extract the {@code addr-spec}
 * “{@code lcl@example.com}” from the return value via {@code getMailbox()}.</p>
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
 * Parses the address as {@code address-list}, such as for the {@code Reply-To},
 * {@code To}, {@code Cc}, (optionally) {@code Bcc}, {@code Resent-To},&nbsp;…
 * headers. RFC6854 (under RFC2026 §3.3(d) Limited Use circumstances) permits
 * using this production for the {@code From} and {@code Resent-From} headers,
 * normally covered by the {@link #asMailboxList()} method.
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

// overridable for UX subclass
protected boolean
isMailboxListSeparator()
{
	return cur() == ',';
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
		while (isMailboxListSeparator()) {
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
		final List<Address> gl = ml == null ? new ArrayList<>() : ml.addresses;
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
 * <p>Returns the parse result of the {@code atom} production:</p>
 *
 * <p>result.{@code body} is a raw {@link Substring} of the atom, with
 * surrounding CFWS stripped (no unfolding necessary), no extra data</p>
 *
 * <p>result.{@code cfws} is null or the trailing CFWS as raw {@link Substring},
 * not unfolded</p>
 *
 * @return result (see above) as {@link Word}
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
 * <p>Returns the parse result of the {@code quoted-string} production:</p>
 *
 * <p>result.{@code body} is an {@link UnfoldedSubstring} of the entire
 * quoted string, with surrounding double quotes; its {@link String} data is
 * dequoted and backslash-removed</p>
 *
 * <p>result.{@code cfws} is null or the trailing CFWS as raw {@link Substring},
 * not unfolded</p>
 *
 * @return result (see above) as {@link Word}
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
			// note: this only works because we know qc is ASCII
			// otherwise we’d have to do .append(Character.toChars(qc))
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
 * Parses FWS.
 *
 * @return raw {@link Substring}, not unfolded
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
 * Parses comment.
 *
 * @return raw {@link Substring}, not unfolded
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
 * Parses CFWS.
 *
 * @return raw {@link Substring}, not unfolded
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
		return pDomainDotAtom(da);
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

protected AddrSpecSIDE
pDomainDotAtom(final Substring da)
{
	// dot-atom form of domain, does not need unfolding
	final String us = da.toString();
	boolean v = FQDN.isDomain(us);
	return new AddrSpecSIDE(da, us, v);
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
