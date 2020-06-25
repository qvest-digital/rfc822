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

import java.util.ArrayList;
import java.util.List;

/**
 * For {@link ParserTest}
 *
 * @author mirabilos (t.glaser@tarent.de)
 */
class TestParser extends Parser {

protected final String[] keywords = { "meow" };

/**
 * Creates and initialises a new TestParser instance.
 *
 * @param input string to analyse
 *
 * @return null if input was null or too large, the new instance otherwise
 */
public static TestParser
of(final String input)
{
	final TestParser obj = new TestParser(input);
	return obj.s() == null ? null : obj;
}

private TestParser(final String input)
{
	super(input, 16);
}

static boolean
isWsp(final int cur, final int next)
{
	return Character.isWhitespace(cur);
}

String
keyword()
{
	try (final Parser.Txn beg = new Parser.Txn()) {
		if (skip(TestParser::isWsp) == -1)
			return null;
		try (final Parser.Txn ofs = new Parser.Txn()) {
			for (final String keyword : keywords) {
				if (s().startsWith(keyword, pos())) {
					final int nextch = bra(keyword.length());
					if (nextch < 'a' || nextch > 'z') {
						// COMMIT in reverse BEGIN order
						ofs.commit();
						// last COMMIT implied here
						return beg.accept(keyword);
					}
					ofs.rollback();
				}
			}
		}
		return null;
	}
}

String
word()
{
	final int ofs = pos();
	if (skip(TestParser::isWsp) == -1) {
		jmp(ofs);
		return null;
	}
	final int beg = pos();
	while (!Character.isWhitespace(accept()))
		if (peek() == -1)
			break;
	final int end = pos();
	return s().substring(beg, end);
}

private List<String>
pWords()
{
	final List<String> res = new ArrayList<>();
	String s;
	while ((s = word()) != null)
		res.add(s);
	assert skip(Character::isWhitespace) == -1;
	System.out.print("asWords:");
	for (final String word : res)
		System.out.print(String.format("(%s)", word));
	System.out.println("->ok");
	return res;
}

private String
pFn()
{
	final int ofs = pos();
	final String kw = keyword();
	if (kw == null) {
		jmp(ofs);
		return null;
	}
	if (cur() != ':') {
		jmp(ofs);
		return null;
	}
	accept();
	final List<String> args = pWords();
	StringBuilder sb = new StringBuilder();
	sb.append(kw);
	char delim = '(';
	for (final String arg : args) {
		sb.append(delim).append(arg);
		delim = ',';
	}
	sb.append(')');
	final String res = sb.toString();
	System.out.println("asFn:" + res);
	return res;

}

public List<String>
asWords()
{
	jmp(0);
	return pWords();
}

public String
asFn()
{
	jmp(0);
	return pFn();
}

}
