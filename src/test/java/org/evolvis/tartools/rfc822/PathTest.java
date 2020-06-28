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

@Test
public void testPos()
{
	t(S(VI), S(VI), S(VI), S(VI), "user@host.domain.tld", null);
	t(S(RN), S(RN), S(VI), S(VI), "a@example.com, b@example.com", (l) -> {
		assertNull(l.invalidsToString(), "invalids present");
		val e = Arrays.asList("a@example.com", "b@example.com");
		assertIterableEquals(e, l.flattenAddresses());
		assertIterableEquals(e, l.flattenAddrSpecs());
	});
	t(S(RN), S(RN), S(RN), S(RN), "@", null);
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
	t(S(RN), S(VI), S(RN), S(VI), "Meine Liste:a@example.com, b@example.com;", (l) -> {
		assertNull(l.invalidsToString(), "invalids present");
		val a = Collections.singletonList("Meine Liste:a@example.com, b@example.com;");
		assertIterableEquals(a, l.flattenAddresses());
		val s = Arrays.asList("a@example.com", "b@example.com");
		assertIterableEquals(s, l.flattenAddrSpecs());
	});
	t(S(RN), S(VI), S(RN), S(VI), "Test:a@example.com, b@example.com;", (l) -> {
		assertNull(l.invalidsToString(), "invalids present");
		val a = Collections.singletonList("Test:a@example.com, b@example.com;");
		assertIterableEquals(a, l.flattenAddresses());
		val s = Arrays.asList("a@example.com", "b@example.com");
		assertIterableEquals(s, l.flattenAddrSpecs());
	});
	t(S(RN), S(VI), S(RN), S(VI), "Undisclosed recipients:;", (l) -> {
		assertNull(l.invalidsToString(), "invalids present");
		val a = Collections.singletonList("Undisclosed recipients:;");
		assertIterableEquals(a, l.flattenAddresses());
		val s = Collections.emptyList();
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
}

}
