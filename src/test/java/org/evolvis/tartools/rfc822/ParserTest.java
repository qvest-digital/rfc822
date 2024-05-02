package org.evolvis.tartools.rfc822;

/*-
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

import lombok.val;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test {@link Parser} class
 *
 * @author mirabilos (t.glaser@qvest-digital.com)
 */
public class ParserTest {

final String t1 = TestUtils.staticB64UTF8Resource("t1.b64");

@Test
public void testPos()
{
	final TestParser tp = TestParser.of(t1);
	assertNotNull(tp, "cannot construct for t1");
	final String t1f = tp.asFn();
	assertNotNull(t1f, "cannot parse t1 as function");
	final List<String> t1w = tp.asWords();
	assertNotNull(t1w, "cannot parse t1 as words");
	assertEquals(4, t1w.size(), "unexpected #words");

	assertEquals("meow(🐈,☺,ä,x)", t1f, "functions didn’t pick it up");
	assertEquals("meow:🐈", t1w.get(0), "cat went missing");
	assertEquals("☺", t1w.get(1), "sad smiley ☹");
	assertEquals("ä", t1w.get(2), "no umlauts!");
	assertEquals("x", t1w.get(3), "X for an U?");

	final TestParser up = TestParser.of("\uD83D\uDC31");
	assertNotNull(up, "cannot meow");
	assertEquals(0, up.pos(), "init pos wrong");
	assertEquals(0x1F431, up.cur(), "cat picture close-up missing");
	assertEquals(0x1F431, up.skip(Character::isWhitespace), "cat isn’t spaced out");
	assertEquals(-1, up.peek(), "cat wasn’t fully loaded");
	assertEquals(-1, up.accept(), "accept ≠ peek");
	assertEquals(2, up.pos(), "cat doesn’t stretch");
	assertEquals(-1, up.cur(), "cur ≠ accept⁻¹");
	assertEquals(-1, up.peek(), "peek ≠ EOF with cur = EOF");
	up.jmp(1);
	assertEquals(0xDC31, up.cur(), "cheap surrogate instead of coffee");
	assertEquals(-1, up.peek(), "peek after surrogate wrong");
}

@SuppressWarnings("Convert2MethodRef")
@Test
public void testNeg()
{
	Exception e;
	assertNull(TestParser.of(t1 + " "), "length check didn’t trigger");
	assertNull(TestParser.of(null), "nil check didn’t trigger");
	final TestParser tp = TestParser.of("");
	assertNotNull(tp, "cannot instantiate for \"\"");
	assertEquals(-1, tp.cur(), "\"\" doesn’t start with EOF");
	e = assertThrows(IndexOutOfBoundsException.class, () ->
	    tp.jmp(-1), "jmp negative doesn’t throw");
	assertEquals(String.format(Parser.BOUNDS_JMP, -1, 0), e.getMessage());
	assertEquals(-1, tp.jmp(0), "jmp to 0=EOF weird");
	e = assertThrows(IndexOutOfBoundsException.class, () ->
	    tp.jmp(1), "jmp past EOF doesn’t throw");
	assertEquals(String.format(Parser.BOUNDS_JMP, 1, 0), e.getMessage());
	assertEquals(-1, tp.jmp(0), "jmp to 0=EOF weird");
	e = assertThrows(IndexOutOfBoundsException.class, () ->
	    tp.accept(), "accepting EOS doesn’t throw");
	assertEquals(Parser.ACCEPT_EOS, e.getMessage());

	final TestParser ap = TestParser.of("abc");
	assertNotNull(ap, "cannot instantiate for \"abc\"");
	val as = ap.testSubstringConstructor(1, 2);
	assertNotNull(as);
	assertEquals("b", as.toString());
	assertThrows(AssertionError.class, () -> ap.testSubstringConstructor(2, 1));
	assertThrows(AssertionError.class, () -> ap.testSubstringConstructor(-1, 2));
	assertThrows(AssertionError.class, () -> ap.testSubstringConstructor(1, 4));
	assertEquals("bc", ap.testSubstringConstructor(1, 3).toString());

	assertNull(Parser.of(Parser.class, ""), "unexpectedly able to instantiate Parser");
}

}
