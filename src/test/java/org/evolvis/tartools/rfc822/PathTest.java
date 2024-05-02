package org.evolvis.tartools.rfc822;

/*-
 * Copyright © 2020, 2021 mirabilos (m@mirbsd.org)
 * Copyright © 2020 mirabilos (t.glaser@qvest-digital.com)
 * Licensor: Qvest Digital AG, Bonn, Germany
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

import java.net.InetAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test {@link Path} class
 *
 * @author mirabilos (t.glaser@qvest-digital.com)
 */
class PathTest {

private static final byte RN = 0;        // does not parse
private static final byte PI = 1;        // parses, not valid, result = original
private static final byte PO = 2;        // parses, not valid, result = cmp
private static final byte VI = 3;        // parses, valid, result = original
private static final byte VO = 4;        // parses, valid, result = cmp
private static final byte BI = 5;        // addr-spec, not valid, result = original
private static final byte BO = 6;        // addr-spec, not valid, result = cmp
private static final byte WI = 7;        // addr-spec, valid, result = original
private static final byte WO = 8;        // addr-spec, valid, result = cmp

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
	val rs = tp.asAddrSpec();
	val cmp = s.t == PI || s.t == VI || s.t == BI || s.t == WI ? addr : s.cmp;
	if (s.t == RN) {
		assertNull(res,
		    () -> "unexpectedly parses as " + what + ": " + addr);
		assertNull(rs,
		    () -> "unexpectedly parses as addr-spec: " + addr);
	} else {
		assertNotNull(res,
		    () -> "does not parse as " + what + ": " + addr);
		assertEquals(s.t == VI || s.t == VO || s.t == WI || s.t == WO,
		    res.isValid(),
		    () -> "validity mismatch for " + what + ": " + addr);
		assertEquals(cmp, res.toString(),
		    () -> "string mismatch for " + what + ": " + addr);
		assertFalse(res.isGroup(), "unexpectedly a group");
		if (s.t == WI || s.t == WO || s.t == BI || s.t == BO) {
			assertNotNull(rs,
			    () -> "does not parse as addr-spec: " + addr);
			assertEquals(s.t == WI || s.t == WO,
			    rs.isValid(),
			    () -> "validity mismatch for addr-spec: " + addr);
			assertEquals(cmp, rs.toString(),
			    () -> "string mismatch for addr-spec: " + addr);
		} else {
			assertNull(rs,
			    () -> "unexpectedly parses as addr-spec: " + addr);
		}
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
		assertEquals(s.t == VI || s.t == VO || s.t == WI || s.t == WO,
		    res.isValid(),
		    () -> "validity mismatch for " + what + ": " + addr);
		val cmp = s.t == PI || s.t == VI || s.t == WI ? addr : s.cmp;
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
		assertEquals(s.t == VI || s.t == VO || s.t == WI || s.t == WO,
		    res.isValid(),
		    () -> "validity mismatch for " + what + ": " + addr);
		val cmp = s.t == PI || s.t == VI || s.t == WI ? addr : s.cmp;
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
		assertEquals(s.t == VI || s.t == VO || s.t == WI || s.t == WO,
		    res.isValid(),
		    () -> "validity mismatch for " + what + ": " + addr);
		val cmp = s.t == PI || s.t == VI || s.t == WI ? addr : s.cmp;
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
private static final class Range {

	final int from;
	final int to;

	Range(final int c)
	{
		this(c, c);
	}

}

private void
testCtype(final byte what, final Range... ranges)
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
public void
testCtypes()
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
public void
testPos()
{
	assertNull(Path.of(null));
	val SN = S(RN);
	val SI = S(PI);
	val SV = S(VI);
	val SW = S(WI);
	t(SN, SN, SN, SN, "", null);
	t(SW, SV, SV, SV, "user@host.domain.tld", null);
	// longest valid FQDN from FQDNTest is one too long for SMTP
	t(S(BI), SI, SI, SI,
	    "a@123456789012345678901234567890123456789012345678901234567890123.123456789012345678901234567890123456789012345678901234567890123.123456789012345678901234567890123456789012345678901234567890123.1234567890123456789012345678901234567890123456789012345678901",
	    null);
	// but one shorter is valid
	t(SW, SV, SV, SV,
	    "a@123456789012345678901234567890123456789012345678901234567890123.123456789012345678901234567890123456789012345678901234567890123.123456789012345678901234567890123456789012345678901234567890123.123456789012345678901234567890123456789012345678901234567890",
	    null);
	t(SN, SN, SV, SV, "a@example.com, b@example.com", (l) -> {
		assertNull(l.invalidsToString(), "invalids present");
		val e = Arrays.asList("a@example.com", "b@example.com");
		assertIterableEquals(e, l.flattenAddresses());
		assertIterableEquals(e, l.flattenAddrSpecs());
	});
	t(SN, SN, SN, SN, "@", null);
	val s0 = S(VO, "hal@ai");
	t(S(WO, "hal@ai"), s0, s0, s0, "hal@ai", null);
	// RFC821/822 do not accept a trailing dot after the Domain/dot-atom
	// so this is required to fail; UXAddress can make it succeed though
	t(S(RN, "hal@ai"), SN, SN, SN, "hal@ai.", null);//UXAddress-trailing-dot-test
	val s1 = S(VO, "a@example.com");
	t(S(WO, "a@example.com"), s1, s1, s1, " a @ example.com \t ", (l) -> {
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
	val S8 = s8.replace(',', ';');
	t(null, null, /*UXAddress-list-test*/ SN, SN, S8, (l) -> {
		// note the callback is ignored in PathTest but used for UX
		assertNull(l.invalidsToString(), "invalids present");
		val a = Arrays.asList("Mary Smith <mary@x.test>", "jdoe@example.org", "Who? <one@y.test>");
		assertIterableEquals(a, l.flattenAddresses());
		val s = Arrays.asList("mary@x.test", "jdoe@example.org", "one@y.test");
		assertIterableEquals(s, l.flattenAddrSpecs());
	});
	// ↑ but see s17 below
	val s9 = "<boss@nil.test>, \"Giant; \\\"Big\\\" Box\" <sysservices@example.net>";
	val S9 = S(VO, "boss@nil.test, \"Giant; \\\"Big\\\" Box\" <sysservices@example.net>");
	t(null, null, S9, S9, s9, (l) -> {
		assertNull(l.invalidsToString(), "invalids present");
		val a = Arrays.asList("boss@nil.test", "\"Giant; \\\"Big\\\" Box\" <sysservices@example.net>");
		assertIterableEquals(a, l.flattenAddresses());
		val s = Arrays.asList("boss@nil.test", "sysservices@example.net");
		assertIterableEquals(s, l.flattenAddrSpecs());
		assertTrue(l.isValid());
		val al = l.getAddresses();
		assertNotNull(al);
		assertEquals(2, al.size());
		val m2 = al.get(1);
		assertTrue(m2.isValid());
		assertFalse(m2.isGroup());
		val m2l = m2.getLabel();
		assertNotNull(m2l);
		assertEquals("\"Giant; \\\"Big\\\" Box\"", m2l.toString());
		val m2ld = m2l.getData();
		assertTrue(m2ld instanceof String, "Substring data is a String");
		assertEquals("Giant; \"Big\" Box", m2ld);
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
		val a = Collections.singletonList(s12);
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
	val i15 = "A Group(Some people)\n        :" +
	    "Chris Jones <c@(Chris's host.)public.example>," +
	    "\r            joe@example.org,\r\n     " +
	    "John <jdoe@one.test> (my dear friend); (the end of the group)";
	val a15 = "A Group:Chris Jones <c@public.example>,joe@example.org,John <jdoe@one.test>;";
	val S15 = S(VO, a15);
	t(SN, S15, SN, S15, i15, (l) -> {
		val a = Collections.singletonList(a15);
		assertIterableEquals(a, l.flattenAddresses());
		val s = Arrays.asList("c@public.example", "joe@example.org", "jdoe@one.test");
		assertIterableEquals(s, l.flattenAddrSpecs());
		assertTrue(l.isValid());
		val al = l.getAddresses();
		assertNotNull(al);
		assertEquals(1, al.size());
		val a1 = al.get(0);
		assertTrue(a1.isValid());
		assertTrue(a1.isGroup());
		assertNotNull(a1.getLabel());
		assertEquals("A Group", a1.getLabel().toString());
		assertNull(a1.getMailbox());
		val ml = a1.getMailboxen();
		assertNotNull(ml);
		assertEquals(3, ml.size());
		val m1 = ml.get(0);
		assertTrue(m1.isValid());
		assertFalse(m1.isGroup());
		assertNotNull(m1.getLabel());
		assertEquals("Chris Jones", m1.getLabel().toString());
		val mbx = m1.getMailbox();
		assertNotNull(mbx);
		assertNull(m1.getMailboxen());
		assertTrue(mbx.isValid());
		val lp = mbx.getLocalPart();
		val dom = mbx.getDomain();
		assertNotNull(lp);
		assertNotNull(dom);
		assertEquals("c", lp.toString());
		assertEquals("c", lp.getData());
		assertTrue(dom instanceof Path.AddrSpecSIDE);
		assertEquals("public.example", dom.toString());
		assertEquals("public.example", dom.getData());
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
	// see s8/S8 above
	val s17 = s8 + ", " + s12;
	t(null, null, SN, SV, s17, (l) -> {
		assertNull(l.invalidsToString(), "invalids present");
		val a = Arrays.asList("Mary Smith <mary@x.test>", "jdoe@example.org", "Who? <one@y.test>", s12);
		assertIterableEquals(a, l.flattenAddresses());
		val s = Arrays.asList("mary@x.test", "jdoe@example.org", "one@y.test");
		assertIterableEquals(s, l.flattenAddrSpecs());
	});
	val S17 = s17.replace(',', ';');
	t(null, null, SN, SN, S17, null);

	// more synthetic ones
	val lp16 = "0123456789ABCDEF";
	val lp32 = lp16 + lp16;
	val lp64 = lp32 + lp32;
	t(SV, null, null, null, "long localpart <" + lp64 + "@mail>", null);
	t(SI, null, null, null, "long localpart <" + lp64 + "g@mail>", null);
	t(SN, null, null, null, "un\r\nfold <user@domain>", null);
	val SU = S(VO, "un fold <user@domain>");
	t(SU, null, null, null, "un\r\n fold <user@domain>", null);
	t(SN, null, null, null, "un\rfold <user@domain>", null);
	t(SU, null, null, null, "un\r fold <user@domain>", null); // not RFC
	t(SN, null, null, null, "un\nfold <user@domain>", null);
	t(SU, null, null, null, "un\n fold <user@domain>", null); // not RFC
	t(S(VO, "\"un fold\" <user@domain>"), null, null, null,
	    "\"un\r\n fold\" <user@domain>", null);
	t(null, null, SN, SN, "user@domain,", null);
	t(null, SN, null, null, "displayname", null);
	t(null, SN, null, null, "displayname:", null);
	t(SN, null, null, null, "user <", null);
	t(SN, null, null, null, "user <user@domain", null);
	t(SN, null, null, null, "user <>", null);
	t(SN, null, null, null, "\"Joe Q. Public <john.q.public@example.com>", null);
	t(S(WO, "user@domain"), null, null, null,
	    "user@domain (comment(nested(ad(absurdum \\(-:))(et\\c)).pp)", null);
	t(SN, null, null, null, "user@domain (comment", null);
	t(SN, null, null, null, "user@", null);
	t(SN, null, null, null, "user@domain..", null);
	t(S(BO, "user@[do main]"), null, null, null, "user@[do\r\n main]", null);
	t(SN, null, null, null, "user@[IPv6:fec0::1", null);
	t(SW, null, null, SV, "user@[IPv6:fec0::1]", (l) -> {
		val al = l.getAddresses();
		assertNotNull(al);
		assertEquals(1, al.size());
		val m1 = al.get(0);
		assertTrue(m1.isValid());
		assertFalse(m1.isGroup());
		assertNull(m1.getLabel());
		val mbx = m1.getMailbox();
		assertNotNull(mbx);
		assertNull(m1.getMailboxen());
		assertTrue(mbx.isValid());
		val lp = mbx.getLocalPart();
		val dom = mbx.getDomain();
		assertNotNull(lp);
		assertNotNull(dom);
		assertEquals("user", lp.toString());
		assertEquals("user", lp.getData());
		assertFalse(dom instanceof Path.AddrSpecSIDE);
		assertEquals("[IPv6:fec0::1]", dom.toString());
		val ip = assertDoesNotThrow(() -> InetAddress.getByName("fec0::1"));
		assertEquals(ip, dom.getData());
	});
	t(SW, null, null, SV, "user@[192.168.0.1]", (l) -> {
		val al = l.getAddresses();
		assertNotNull(al);
		assertEquals(1, al.size());
		val m1 = al.get(0);
		assertTrue(m1.isValid());
		assertFalse(m1.isGroup());
		assertNull(m1.getLabel());
		val mbx = m1.getMailbox();
		assertNotNull(mbx);
		assertNull(m1.getMailboxen());
		assertTrue(mbx.isValid());
		val lp = mbx.getLocalPart();
		val dom = mbx.getDomain();
		assertNotNull(lp);
		assertNotNull(dom);
		assertEquals("user", lp.toString());
		assertEquals("user", lp.getData());
		assertFalse(dom instanceof Path.AddrSpecSIDE);
		assertEquals("[192.168.0.1]", dom.toString());
		val ip = assertDoesNotThrow(() -> InetAddress.getByName("192.168.0.1"));
		assertEquals(ip, dom.getData());
	});
	t(S(BI), null, null, null, "user@_xmpp.domain", null);
	val lh33 = lp32 + ".";
	val lhmax = lp64 + "@" + lh33 + lh33 + lh33 + lh33 + lh33 + lp16 + ".1234567";
	t(SW, null, null, null, lhmax, null);
	t(S(BI), null, null, null, lhmax + "8", null);
	t(SN, SV, SN, SV, "dis\"play\"name:;", null);
	t(S(BI), null, null, null, "\"\\\t\"@domain", null);
	val ut = Path.of("\\\n\\\u007F");
	assertNotNull(ut, "failure in unit test for quoted-pair");
	assertEquals(-1, ut.pQuotedPair());
	ut.jmp(2);
	assertEquals(-1, ut.pQuotedPair());
}

@Test
public void
testLombokNonNull()
{
	val r = Path.of("foo <bar@example.com>").forSender(false);
	assertNotNull(r);
	val l = r.getLabel();
	assertNotNull(l);
	assertEquals("foo", l.toString());
	// test extra branches caused by Lombok @NonNull
	//noinspection ConstantConditions
	assertThrows(NullPointerException.class, () -> new Path.AddrSpec(null, l, false));
	//noinspection ConstantConditions
	assertThrows(NullPointerException.class, () -> new Path.AddrSpec(l, null, false));
}

}
