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

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test {@link Parser} class
 *
 * @author mirabilos (t.glaser@tarent.de)
 */
public class ParserTest {

final String t1 = TestUtils.staticB64UTF8Resource("t1.b64");

@Test
public void testPos()
{
	final TestParser tp = new TestParser(t1);
	assertNotNull(tp);
	final String t1f = tp.asFn();
	assertNotNull(t1f);
	final List<String> t1w = tp.asWords();
	assertNotNull(t1w);
	assertEquals(4, t1w.size());

	assertEquals("meow(🐈,☺,ä,x)", t1f);
	assertEquals("meow:🐈", t1w.get(0));
	assertEquals("☺", t1w.get(1));
	assertEquals("ä", t1w.get(2));
	assertEquals("x", t1w.get(3));
}

@SuppressWarnings("Convert2MethodRef")
@Test
public void testNeg()
{
	Exception e;
	e = assertThrows(IllegalArgumentException.class, () ->
	    new TestParser(t1 + " "));
	assertEquals(String.format(Parser.BOUNDS_INP, 17, 16), e.getMessage());
	e = assertThrows(IllegalArgumentException.class, () ->
	    new TestParser(null));
	assertEquals(String.format(Parser.BOUNDS_INP, -1, 16), e.getMessage());
	final TestParser tp = new TestParser("");
	assertEquals(-1, tp.cur());
	e = assertThrows(IndexOutOfBoundsException.class, () ->
	    tp.jmp(-1));
	assertEquals(String.format(Parser.BOUNDS_JMP, -1, 0), e.getMessage());
	assertEquals(-1, tp.jmp(0));
	e = assertThrows(IndexOutOfBoundsException.class, () ->
	    tp.jmp(1));
	assertEquals(String.format(Parser.BOUNDS_JMP, 1, 0), e.getMessage());
	assertEquals(-1, tp.jmp(0));
	e = assertThrows(IndexOutOfBoundsException.class, () ->
	    tp.accept());
	assertEquals(Parser.ACCEPT_EOS, e.getMessage());
}

}
