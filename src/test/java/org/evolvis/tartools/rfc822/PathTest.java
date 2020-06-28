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

import lombok.AllArgsConstructor;
import lombok.val;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test {@link Path} class
 *
 * @author mirabilos (t.glaser@tarent.de)
 */
class PathTest {

private static final byte RN = 0;        // does not parse
private static final byte PI = 1;        // parses, not valid, result = original
private static final byte PO = 2;        // parses, not valid, result = cmp
private static final byte VI = 3;        // parses, valid, result = original
private static final byte VO = 4;        // parses, valid, result = cmp

private static Tspec S(final byte spec)
{
	return new Tspec(spec, null);
}

private static Tspec S(final byte spec, final String cmp)
{
	return new Tspec(spec, cmp);
}

@AllArgsConstructor
private static final class Tspec {

	final byte t;
	final String cmp;

}

private static void
tm(final Tspec s, final String addr, final Path tp)
{
	if (s == null)
		return;
	final String what = "mailbox";

	val res = tp.forSender(false);
	if (s.t == RN) {
		assertNull(res,
		    () -> "unexpectedly parses as " + what + ": " + addr);
	} else {
		assertNotNull(res,
		    () -> "does not parse as " + what + ": " + addr);
		assertEquals(s.t == VI || s.t == VO, res.isValid(),
		    () -> "validity mismatch for " + what + ": " + addr);
		val cmp = s.t == PI || s.t == VI ? addr : s.cmp;
		assertEquals(cmp, res.toString(),
		    () -> "string mismatch for " + what + ": " + addr);
		assertFalse(res.isGroup(), "unexpectedly a group");
	}
}

private static void
ta(final Tspec s, final String addr, final Path tp)
{
	if (s == null)
		return;
	final String what = "address";

	val res = tp.forSender(true);
	if (s.t == RN) {
		assertNull(res,
		    () -> "unexpectedly parses as " + what + ": " + addr);
	} else {
		assertNotNull(res,
		    () -> "does not parse as " + what + ": " + addr);
		assertEquals(s.t == VI || s.t == VO, res.isValid(),
		    () -> "validity mismatch for " + what + ": " + addr);
		val cmp = s.t == PI || s.t == VI ? addr : s.cmp;
		assertEquals(cmp, res.toString(),
		    () -> "string mismatch for " + what + ": " + addr);
	}
}

private static void
tml(final Tspec s, final String addr, final Path tp, final Consumer<Path.AddressList> lt)
{
	if (s == null)
		return;
	final String what = "mailboxList";

	val res = tp.asMailboxList();
	if (s.t == RN) {
		assertNull(res,
		    () -> "unexpectedly parses as " + what + ": " + addr);
	} else {
		assertNotNull(res,
		    () -> "does not parse as " + what + ": " + addr);
		assertEquals(s.t == VI || s.t == VO, res.isValid(),
		    () -> "validity mismatch for " + what + ": " + addr);
		val cmp = s.t == PI || s.t == VI ? addr : s.cmp;
		assertEquals(cmp, res.toString(),
		    () -> "string mismatch for " + what + ": " + addr);
		assertFalse(res.isAddressList(), "unexpectedly an address-list");
		if (lt != null)
			lt.accept(res);
	}
}

private static void
tal(final Tspec s, final String addr, final Path tp, final Consumer<Path.AddressList> lt)
{
	if (s == null)
		return;
	final String what = "addressList";

	val res = tp.asAddressList();
	if (s.t == RN) {
		assertNull(res,
		    () -> "unexpectedly parses as " + what + ": " + addr);
	} else {
		assertNotNull(res,
		    () -> "does not parse as " + what + ": " + addr);
		assertEquals(s.t == VI || s.t == VO, res.isValid(),
		    () -> "validity mismatch for " + what + ": " + addr);
		val cmp = s.t == PI || s.t == VI ? addr : s.cmp;
		assertEquals(cmp, res.toString(),
		    () -> "string mismatch for " + what + ": " + addr);
		if (lt != null)
			lt.accept(res);
	}
}

private static void
t(final Tspec mailbox, final Tspec address, final Tspec mailboxList,
    final Tspec addressList, final String addr,
    final Consumer<Path.AddressList> listTest)
{
	final Path tp = Path.of(addr);
	assertNotNull(tp, () -> "cannot instantiate for: " + addr);
	tm(mailbox, addr, tp);
	ta(address, addr, tp);
	tml(mailboxList, addr, tp, listTest);
	tal(addressList, addr, tp, listTest);
}

@AllArgsConstructor
private static class Range {

	final int from;
	final int to;

	Range(final int c)
	{
		this(c, c);
	}

}

private void testCtype(final byte what, final Range... ranges)
{
	boolean[] a = new boolean[260];
	Arrays.fill(a, false);
	for (final Range r : ranges)
		for (int i = r.from; i <= r.to; ++i)
			a[i] = true;
	assertFalse(Path.is(-1, what), "-1 is not false");
	for (int i = 0; i < a.length; ++i) {
		final int c = i; // lambda vs. effectively final
		assertEquals(a[c], Path.is(c, what),
		    () -> String.format("%d is not %s for %02X", c, a[c], what));
	}
}

@Test
public void testCtypes()
{
	testCtype(Path.IS_ATEXT, new Range('A', 'Z'), new Range('a', 'z'),
	    new Range('0', '9'), new Range('!'), new Range('#'), new Range('$'),
	    new Range('%'), new Range('&'), new Range('\''), new Range('*'),
	    new Range('+'), new Range('-'), new Range('/'), new Range('='),
	    new Range('?'), new Range('^'), new Range('_'), new Range('`'),
	    new Range('{'), new Range('|'), new Range('}'), new Range('~'));
	testCtype(Path.IS_QTEXT, new Range(33, 33),
	    new Range(35, 91), new Range(93, 126));
	testCtype(Path.IS_CTEXT, new Range(33, 39),
	    new Range(42, 91), new Range(93, 126));
	testCtype(Path.IS_DTEXT, new Range(33, 90), new Range(94, 126));
	testCtype(Path.IS_ALNUM, new Range(0x41, 0x5A), new Range(0x61, 0x7A),
	    new Range(0x30, 0x39));
	testCtype(Path.IS_ALNUS, new Range(0x41, 0x5A), new Range(0x61, 0x7A),
	    new Range(0x30, 0x39), new Range('-'));
	testCtype(Path.IS_XDIGIT, new Range('0', '9'),
	    new Range('a', 'f'), new Range('A', 'F'));
}

@Test
public void testPos()
{
	val SN = S(RN);
	val SI = S(PI);
	val SV = S(VI);
	t(SV, SV, SV, SV, "user@host.domain.tld", null);
	t(SN, SN, SV, SV, "a@example.com, b@example.com", (l) -> {
		assertNull(l.invalidsToString(), "invalids present");
		val e = Arrays.asList("a@example.com", "b@example.com");
		assertIterableEquals(e, l.flattenAddresses());
		assertIterableEquals(e, l.flattenAddrSpecs());
	});
	t(SN, SN, SN, SN, "@", null);
	val s1 = S(VO, "a@example.com");
	t(s1, s1, s1, s1, " a @ example.com ", (l) -> {
		assertNull(l.invalidsToString(), "invalids present");
		val e = Collections.singletonList("a@example.com");
		assertIterableEquals(e, l.flattenAddresses());
		assertIterableEquals(e, l.flattenAddrSpecs());
	});
	t(s1, s1, s1, s1,
	    " < a @ example.com > ", (l) -> {
		    assertNull(l.invalidsToString(), "invalids present");
		    val e = Collections.singletonList("a@example.com");
		    assertIterableEquals(e, l.flattenAddresses());
		    assertIterableEquals(e, l.flattenAddrSpecs());
	    });
	val s2 = S(VO, "Foo   Bar <a@example.com>");
	t(s2, s2, s2, s2,
	    "    Foo   Bar   <   a   @   example.com   >   ", (l) -> {
		    assertNull(l.invalidsToString(), "invalids present");
		    val a = Collections.singletonList("Foo   Bar <a@example.com>");
		    assertIterableEquals(a, l.flattenAddresses());
		    val s = Collections.singletonList("a@example.com");
		    assertIterableEquals(s, l.flattenAddrSpecs());
	    });
	val lto = "Test:a@example.com,b@example.com;";
	t(SN, S(VO, lto), SN, S(VO, lto), "Test:a@example.com, b@example.com;", (l) -> {
		assertNull(l.invalidsToString(), "invalids present");
		val a = Collections.singletonList(lto);
		assertIterableEquals(a, l.flattenAddresses());
		val s = Arrays.asList("a@example.com", "b@example.com");
		assertIterableEquals(s, l.flattenAddrSpecs());
	});
	// the second entry is, surprisingly, not valid in SMTP;
	// sendmail uses it, internally, to avoid MX lookup and send to the host
	val s3 = S(PO, "One <a@example.com>, Two <b@[example.com]>");
	t(null, null, s3, s3, "One<a@example.com>,Two<b@[example.com]>", (l) -> {
		assertEquals("Two <b@[example.com]>", l.invalidsToString());
		val a = Arrays.asList("One <a@example.com>", "Two <b@[example.com]>");
		assertIterableEquals(a, l.flattenAddresses());
		val s = Arrays.asList("a@example.com", "b@[example.com]");
		assertIterableEquals(s, l.flattenAddrSpecs());
	});
	// from RFC5322
	val s4 = "John Doe <jdoe@machine.example>";
	t(SV, SV, SV, SV, s4, (l) -> {
		assertNull(l.invalidsToString(), "invalids present");
		val a = Collections.singletonList(s4);
		assertIterableEquals(a, l.flattenAddresses());
		val s = Collections.singletonList("jdoe@machine.example");
		assertIterableEquals(s, l.flattenAddrSpecs());
	});
	val s5 = "Mary Smith <mary@example.net>";
	t(SV, SV, SV, SV, s5, (l) -> {
		assertNull(l.invalidsToString(), "invalids present");
		val a = Collections.singletonList(s5);
		assertIterableEquals(a, l.flattenAddresses());
		val s = Collections.singletonList("mary@example.net");
		assertIterableEquals(s, l.flattenAddrSpecs());
	});
	val s6 = "Michael Jones <mjones@machine.example>";
	t(SV, SV, SV, SV, s6, (l) -> {
		assertNull(l.invalidsToString(), "invalids present");
		val a = Collections.singletonList(s6);
		assertIterableEquals(a, l.flattenAddresses());
		val s = Collections.singletonList("mjones@machine.example");
		assertIterableEquals(s, l.flattenAddrSpecs());
	});
	val s7 = "\"Joe Q. Public\" <john.q.public@example.com>";
	t(SV, SV, SV, SV, s7, (l) -> {
		assertNull(l.invalidsToString(), "invalids present");
		val a = Collections.singletonList(s7);
		assertIterableEquals(a, l.flattenAddresses());
		val s = Collections.singletonList("john.q.public@example.com");
		assertIterableEquals(s, l.flattenAddrSpecs());
	});
	val s8 = "Mary Smith <mary@x.test>, jdoe@example.org, Who? <one@y.test>";
	t(null, null, SV, SV, s8, (l) -> {
		assertNull(l.invalidsToString(), "invalids present");
		val a = Arrays.asList("Mary Smith <mary@x.test>", "jdoe@example.org", "Who? <one@y.test>");
		assertIterableEquals(a, l.flattenAddresses());
		val s = Arrays.asList("mary@x.test", "jdoe@example.org", "one@y.test");
		assertIterableEquals(s, l.flattenAddrSpecs());
	});
	val s9 = "<boss@nil.test>, \"Giant; \\\"Big\\\" Box\" <sysservices@example.net>";
	val S9 = S(VO, "boss@nil.test, \"Giant; \\\"Big\\\" Box\" <sysservices@example.net>");
	t(null, null, S9, S9, s9, (l) -> {
		assertNull(l.invalidsToString(), "invalids present");
		val a = Arrays.asList("boss@nil.test", "\"Giant; \\\"Big\\\" Box\" <sysservices@example.net>");
		assertIterableEquals(a, l.flattenAddresses());
		val s = Arrays.asList("boss@nil.test", "sysservices@example.net");
		assertIterableEquals(s, l.flattenAddrSpecs());
	});
	val s10 = "Pete <pete@silly.example>";
	t(SV, SV, SV, SV, s10, (l) -> {
		assertNull(l.invalidsToString(), "invalids present");
		val a = Collections.singletonList(s10);
		assertIterableEquals(a, l.flattenAddresses());
		val s = Collections.singletonList("pete@silly.example");
		assertIterableEquals(s, l.flattenAddrSpecs());
	});
	val s11 = "A Group:Ed Jones <c@a.test>,joe@where.test,John <jdoe@one.test>;";
	t(SN, SV, SN, SV, s11, (l) -> {
		assertNull(l.invalidsToString(), "invalids present");
		val a = Collections.singletonList(s11);
		assertIterableEquals(a, l.flattenAddresses());
		val s = Arrays.asList("c@a.test", "joe@where.test", "jdoe@one.test");
		assertIterableEquals(s, l.flattenAddrSpecs());
	});
	val s12 = "Undisclosed recipients:;";
	t(SN, SV, SN, SV, s12, (l) -> {
		assertNull(l.invalidsToString(), "invalids present");
		val a = Collections.singletonList("Undisclosed recipients:;");
		assertIterableEquals(a, l.flattenAddresses());
		val s = Collections.emptyList();
		assertIterableEquals(s, l.flattenAddrSpecs());
	});
	val i14 = "Pete(A nice \\) chap) <pete(his account)@silly.test(his host)>";
	val a14 = "Pete <pete@silly.test>";
	val s14 = "pete@silly.test";
	val S14 = S(VO, a14);
	t(S14, S14, S14, S14, i14, (l) -> {
		assertNull(l.invalidsToString(), "invalids present");
		val a = Collections.singletonList(a14);
		assertIterableEquals(a, l.flattenAddresses());
		val s = Collections.singletonList(s14);
		assertIterableEquals(s, l.flattenAddrSpecs());
	});
	val i15 =
	    "A Group(Some people)\n        :Chris Jones <c@(Chris's host.)public.example>,\r            joe@example.org,\r\n     John <jdoe@one.test> (my dear friend); (the end of the group)";
	val a15 = "A Group:Chris Jones <c@public.example>,joe@example.org,John <jdoe@one.test>;";
	val S15 = S(VO, a15);
	t(SN, S15, SN, S15, i15, (l) -> {
		val a = Collections.singletonList(a15);
		assertIterableEquals(a, l.flattenAddresses());
		val s = Arrays.asList("c@public.example", "joe@example.org", "jdoe@one.test");
		assertIterableEquals(s, l.flattenAddrSpecs());
	});
	val i16 = "(Empty list)(start)Hidden recipients  :(nobody(that I know))  ; ";
	val a16 = "Hidden recipients:;";
	val S16 = S(VO, a16);
	t(SN, S16, SN, S16, i16, (l) -> {
		assertNull(l.invalidsToString(), "invalids present");
		val a = Collections.singletonList(a16);
		assertIterableEquals(a, l.flattenAddresses());
		val s = Collections.emptyList();
		assertIterableEquals(s, l.flattenAddrSpecs());
	});
}

}
