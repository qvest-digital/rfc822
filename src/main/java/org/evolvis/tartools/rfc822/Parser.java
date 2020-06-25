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

import lombok.val;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Parser base class, abstracting initialisation and movement
 *
 * @author mirabilos (t.glaser@tarent.de)
 */
abstract class Parser {

static final String BOUNDS_JMP = "attempt to move (%d) beyond source string (%d)";
static final String ACCEPT_EOS = "cannot ACCEPT end of input";

/**
 * the {@link String} to analyse
 */
private final String source;
/**
 * offset into {@link #source}
 */
private int ofs;
/**
 * current wide character (wint_t at {@link #source}[{@link #ofs}] or -1 for EOD)
 */
private int cur;
/**
 * offset of next wide character into {@link #source}
 */
private int succ;
/**
 * next wide character (for peeking), cf. {@link #cur}
 */
private int next;
/**
 * source length
 */
private final int srcsz;

/**
 * Constructs a parser. Intended to be used by subclasses from static
 * factory methods *only*; see {@link Path#of(String)} for an example.
 *
 * Note that subclass constructors must also be of protected visibility
 * to allow for inheritance, but they should both be documented and
 * treated as private, never called other than from a subclass constructor.
 *
 * @param input  user-provided {@link String} to parse
 * @param maxlen subclass-provided maximum input string length, in characters
 */
protected Parser(final String input, final int maxlen)
{
	srcsz = input == null ? -1 : input.length();
	if (srcsz < 0 || srcsz > maxlen) {
		source = null;
	} else {
		source = input;
		jmp(0);
	}
}

/**
 * Constructs a parser. Intended to be used by subclasses from static
 * factory methods *only*; see {@link Path#of(String)} for an example.
 *
 * @param cls   subclass of Parser to construct
 * @param input user-provided {@link String} to parse
 * @param <T>   subclass of Parser to construct
 *
 * @return null if input was null or too large, the new instance otherwise
 */
protected static <T extends Parser> T
of(final Class<T> cls, final String input)
{
	try {
		val creator = cls.getDeclaredConstructor(String.class);
		final T obj = creator.newInstance(input);
		return obj.s() == null ? null : obj;
	} catch (ReflectiveOperationException e) {
		// should not happen, only subclasses can call it, they’d fail
		return null;
	}
}

/**
 * Jumps to a specified input character position, absolute jump
 *
 * @param pos to jump to
 *
 * @return the codepoint at that position
 *
 * @throws IndexOutOfBoundsException if pos is not in or just past the input
 */
protected final int
jmp(final int pos)
{
	if (pos < 0 || pos > srcsz)
		throw new IndexOutOfBoundsException(String.format(BOUNDS_JMP,
		    pos, srcsz));
	ofs = pos;
	if (ofs == srcsz) {
		succ = ofs;
		next = cur = -1;
		return cur;
	}
	cur = source.codePointAt(ofs);
	succ = ofs + Character.charCount(cur);
	next = succ < srcsz ? source.codePointAt(succ) : -1;
	return cur;
}

/**
 * Jumps to a specified input character position, relative jump
 *
 * @param deltapos to add to the current positioin
 *
 * @return the codepoint at that position
 *
 * @throws IndexOutOfBoundsException if pos is not in or just past the input
 */
protected final int
bra(final int deltapos)
{
	return jmp(ofs + deltapos);
}

/**
 * Returns the current input character position, for saving and restoring
 * (with {@link #jmp(int)}) and for error messages
 *
 * @return position
 */
protected final int
pos()
{
	return ofs;
}

/**
 * Returns the input string, for use with substring comparisons
 * (this is safe because Java™ strings are immutable)
 *
 * @return String input
 */
protected final String
s()
{
	return source;
}

/**
 * Returns the wide character at the current position
 *
 * @return UCS-4 codepoint, or -1 if end of input is reached
 */
protected final int
cur()
{
	return cur;
}

/**
 * Returns the wide character after the one at the current position
 *
 * @return UCS-4 codepoint, or -1 if end of input is reached
 */
protected final int
peek()
{
	return next;
}

/**
 * Advances the current position to the next character
 *
 * @return codepoint of the next character, or -1 if end of input is reached
 *
 * @throws IndexOutOfBoundsException if end of input was already reached
 */
protected final int
accept()
{
	if (cur == -1)
		throw new IndexOutOfBoundsException(ACCEPT_EOS);
	return jmp(succ);
}

/**
 * Advances the current position as long as the matcher returns true
 * and end of input is not yet reached
 *
 * @param matcher gets called with {@link #cur()} and {@link #peek()} as arguments
 *
 * @return codepoint of the first character where the matcher returned false, or -1
 */
protected final int
skip(final BiFunction<Integer, Integer, Boolean> matcher)
{
	while (cur != -1 && matcher.apply(cur, next))
		jmp(succ);
	return cur;
}

/**
 * Advances the current position as long as the matcher returns true
 * and end of input is not yet reached; cf. {@link #skip(BiFunction)}
 *
 * @param matcher gets called with just {@link #cur()} as argument
 *
 * @return codepoint of the first character where the matcher returned false, or -1
 */
protected final int
skip(final Function<Integer, Boolean> matcher)
{
	while (cur != -1 && matcher.apply(cur))
		jmp(succ);
	return cur;
}

/**
 * Representation for consecutive substrings of the input string,
 * for result passing.
 *
 * @author mirabilos (t.glaser@tarent.de)
 */
protected final class Substring {

	private final int beg;
	private final int end;

	/**
	 * Creates a new input substring from parser positions
	 *
	 * @param beg offset of the beginning of the substring
	 * @param end offset of the codepoint after the end
	 */
	protected Substring(final int beg, final int end)
	{
		// for internal consistency, not fatal if disabled at runtime
		assert end >= beg : "end after beginning";
		assert beg >= 0 : "negative beginning";
		assert end <= Parser.this.srcsz : "past end of input";
		this.beg = beg;
		this.end = end;
	}

	@Override
	public final String toString()
	{
		return Parser.this.s().substring(beg, end);
	}

}

/**
 * Transactions for positioning: on creation and {@link #commit()} the position
 * is saved, on {@link #rollback()} and closing, it is restored to the point
 * from the last commit (or creation). Implements {@link AutoCloseable}.
 *
 * @author mirabilos (t.glaser@tarent.de)
 */
final class Txn implements AutoCloseable {

	private int pos;

	/**
	 * Creates a new parser position transaction instance
	 *
	 * The current position, upon creation, is committed immediately.
	 */
	protected Txn()
	{
		commit();
	}

	/**
	 * Returns the last committed position
	 *
	 * @return position of last commit
	 */
	final int
	savepoint()
	{
		return pos;
	}

	/**
	 * Commits the current parser position (stores it in the transaction)
	 */
	final void
	commit()
	{
		pos = Parser.this.ofs;
	}

	/**
	 * Rolls the parser position back to the last committed position
	 *
	 * @return the codepoint at the new position, see {@link Parser#cur()}
	 */
	final int
	rollback()
	{
		return Parser.this.jmp(savepoint());
	}

	/**
	 * Rolls back to the last committed position upon “closing” the
	 * parser position transaction; see {@link #rollback()}
	 */
	@Override
	public final void
	close()
	{
		rollback();
	}

	/**
	 * Commits the current position and returns its argument.
	 * Helper method to avoid needing braces more often than necessary.
	 *
	 * @param returnValue the value to return
	 * @param <T>         the type of returnValue
	 *
	 * @return returnValue
	 */
	final <T> T
	accept(final T returnValue)
	{
		commit();
		return returnValue;
	}

	/**
	 * Returns a new {@link Substring} spanning from the last {@link #savepoint()}
	 * to the current parser position
	 *
	 * @return {@link Substring} object
	 */
	final Substring
	substring()
	{
		return new Substring(savepoint(), Parser.this.ofs);
	}

}

}
