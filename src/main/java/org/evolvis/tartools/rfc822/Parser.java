package org.evolvis.tartools.rfc822;

/*-
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

import lombok.Getter;
import lombok.val;

/**
 * <p>Parser base class. Abstracts initialisation and movement.</p>
 *
 * @author mirabilos (t.glaser@tarent.de)
 */
abstract class Parser {

static final String BOUNDS_JMP = "attempt to move (%d) beyond source string (%d)";
static final String ACCEPT_EOS = "cannot ACCEPT end of input";

/**
 * The source {@link String} to analyse.
 */
private final String source;
/**
 * Offset into {@link #source}.
 */
private int ofs;
/**
 * Current wide character (wint_t at {@link #source}[{@link #ofs}] or {@code -1} for EOD).
 */
private int cur;
/**
 * Offset of next wide character into {@link #source}.
 */
private int succ;
/**
 * Next wide character (for peeking) or {@code -1}, cf. {@link #cur}.
 */
private int next;
/**
 * {@link #source} length.
 */
private final int srcsz;

/**
 * <p>Constructs a parser. Intended to be used by subclasses from static
 * factory methods *only*; see {@link Path#of(String)} for an example.</p>
 *
 * <p>Note that subclass constructors must also be of protected visibility
 * to allow for inheritance, but they should both be documented and
 * treated as private, never called other than from a subclass constructor.</p>
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
 * <p>Constructs a parser. Intended to be used by subclasses from static
 * factory methods *only*; see {@link Path#of(String)} for an example.</p>
 *
 * @param cls   subclass of Parser to construct
 * @param input user-provided {@link String} to parse
 * @param <T>   subclass of Parser to construct
 *
 * @return null if input was null or too large,
 *     the new parser subclass instance otherwise
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
 * Jumps to a specified input character position, absolute jump.
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
 * Jumps to a specified input character position, relative jump.
 *
 * @param deltapos to add to the current position
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
 * Returns the current input character position. Useful for saving and restoring
 * (with {@link #jmp(int)}) and for error messages.
 *
 * @return position
 */
protected final int
pos()
{
	return ofs;
}

/**
 * Returns the input string, for use with substring comparisons.
 * (This is safe because Java™ strings are immutable.)
 *
 * @return String input
 */
protected final String
s()
{
	return source;
}

/**
 * Returns the wide character at the current position.
 *
 * @return UCS-4 codepoint, or {@code -1} if end of input is reached
 */
protected final int
cur()
{
	return cur;
}

/**
 * Returns the wide character after the one at the current position.
 *
 * @return UCS-4 codepoint, or {@code -1} if end of input is reached
 */
protected final int
peek()
{
	return next;
}

/**
 * Advances the current position to the next character.
 *
 * @return codepoint of the next character, or {@code -1} if end of input is reached
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
 * Advances the current position using a peeking matcher. Continues as long
 * as the {@code matcher} returns true and end of input is not yet reached.
 *
 * @param matcher {@link LookaheadMatcher} called with {@link #cur()} and
 *                {@link #peek()} as arguments to determine whether to skip ahead
 *
 * @return codepoint of the first character for which the matcher returned false,
 *     or {@code -1} if end of input is reached
 *
 * @see #skip(ContextlessMatcher)
 */
protected final int
skipPeek(final LookaheadMatcher matcher)
{
	while (cur != -1 && matcher.toSkip(cur, next))
		jmp(succ);
	return cur;
}

/**
 * Advances the current position using a regular matcher. Continues as long
 * as the {@code matcher} returns true and end of input is not yet reached.
 *
 * @param matcher {@link ContextlessMatcher} called with just {@link #cur()}
 *                as argument to determine whether to skip ahead
 *
 * @return codepoint of the first character for which the matcher returned false,
 *     or {@code -1} if end of input is reached
 *
 * @see #skipPeek(LookaheadMatcher)
 */
protected final int
skip(final ContextlessMatcher matcher)
{
	while (cur != -1 && matcher.toSkip(cur))
		jmp(succ);
	return cur;
}

/**
 * Representation for consecutive substrings of the input string,
 * for result passing. {@link #toString()} usually returns the
 * on-wire representation of the input, normally verbatim as
 * real substring; {@link #getData()} will provide additional
 * information if set.
 *
 * @author mirabilos (t.glaser@tarent.de)
 */
protected class Substring {
	// ↑ coverage: https://github.com/jacoco/jacoco/issues/1063

	protected final int beg;
	protected final int end;

	@Getter
	private final Object data;

	/**
	 * Creates a new input substring from parser positions.
	 *
	 * @param beg offset of the beginning of the substring
	 * @param end offset of the codepoint after the end
	 */
	protected Substring(final int beg, final int end)
	{
		this(beg, end, null);
	}

	/**
	 * Copy constructor.
	 *
	 * @param src Substring to copy positions and user data from
	 */
	protected Substring(final Substring src)
	{
		this(src.beg, src.end, src.data);
	}

	/**
	 * Copy constructor with data override.
	 *
	 * @param src  Substring to copy positions from
	 * @param data user-specified data to associate with the new object
	 */
	protected Substring(final Substring src, final Object data)
	{
		this(src.beg, src.end, data);
	}

	/**
	 * Creates a new input substring from parser positions and user data.
	 *
	 * @param beg  offset of the beginning of the substring
	 * @param end  offset of the codepoint after the end
	 * @param data user-specified data optionally associated with this object
	 */
	protected Substring(final int beg, final int end, final Object data)
	{
		// for internal consistency, not fatal if disabled at runtime
		assert end >= beg : "end after beginning";
		assert beg >= 0 : "negative beginning";
		assert end <= Parser.this.srcsz : "past end of input";
		// ↑ coverage: https://github.com/jacoco/jacoco/pull/613
		this.beg = beg;
		this.end = end;
		this.data = data;
	}

	/**
	 * Returns the string representation of this {@code Substring},
	 * that is, usually, a substring of an on-wire representation.
	 *
	 * @return String representation
	 */
	@Override
	public String
	toString()
	{
		return Parser.this.s().substring(beg, end);
	}

}

/**
 * Transactions for positioning. On creation and {@link #commit()}, the position
 * is saved, on {@link #rollback()} and closing, it is restored to the point
 * from the last commit (or creation). Implements {@link AutoCloseable}.
 *
 * @author mirabilos (t.glaser@tarent.de)
 */
final class Txn implements AutoCloseable {

	private int pos;

	/**
	 * <p>Creates a new parser position transaction instance.</p>
	 * <p>The current position, upon creation, is committed immediately.</p>
	 */
	@SuppressWarnings("ProtectedMemberInFinalClass")
	protected Txn()
	{
		commit();
	}

	/**
	 * Returns the last committed position.
	 *
	 * @return position of last commit
	 */
	int
	savepoint()
	{
		return pos;
	}

	/**
	 * Commits the current parser position (stores it in the transaction).
	 */
	void
	commit()
	{
		pos = Parser.this.ofs;
	}

	/**
	 * Rolls the parser position back to the last committed position.
	 *
	 * @return the codepoint at the new position, see {@link Parser#cur()}
	 */
	int
	rollback()
	{
		return Parser.this.jmp(savepoint());
	}

	/**
	 * Rolls back to the last committed position upon “closing” the
	 * parser position transaction. Just calls {@link #rollback()}.
	 */
	@Override
	public void
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
	<T> T
	accept(final T returnValue)
	{
		commit();
		return returnValue;
	}

	/**
	 * Returns a new {@code Substring} spanning from the last savepoint to
	 * the current parser position.
	 *
	 * @return new {@link Substring} object
	 *
	 * @see #savepoint()
	 */
	Substring
	substring()
	{
		return new Substring(savepoint(), Parser.this.ofs);
	}

	/**
	 * Returns a new {@code Substring} with user data, spanning from the
	 * last savepoint to the current parser position.
	 *
	 * @param data user-specified data optionally associated with the Substring
	 *
	 * @return new {@link Substring} object
	 *
	 * @see #savepoint()
	 */
	Substring
	substring(final Object data)
	{
		return new Substring(savepoint(), Parser.this.ofs, data);
	}

}

}
