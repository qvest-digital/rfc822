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

/**
 * Transactions for positioning: on creation and {@link #commit()} the position
 * is saved, on {@link #rollback()} and closing, it is restored to the point
 * from the last commit (or creation). Implements {@link AutoCloseable}.
 *
 * @author mirabilos (t.glaser@tarent.de)
 */
final class Txn implements AutoCloseable {

private final Parser parent;
private int pos;

/**
 * Creates a new parser position transaction instance
 *
 * The current position, upon creation, is committed immediately.
 */
Txn(final Parser that)
{
	parent = that;
	commit();
}

/**
 * Returns the last committed position
 *
 * @return position of last commit
 */
public final int
savepoint()
{
	return pos;
}

/**
 * Commits the current parser position (stores it in the transaction)
 */
public final void
commit()
{
	pos = parent.pos();
}

/**
 * Rolls the parser position back to the last committed position
 *
 * @return the codepoint at the new position, see {@link Parser#cur()}
 */
public final int
rollback()
{
	return parent.jmp(savepoint());
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
public final <T> T
accept(T returnValue)
{
	commit();
	return returnValue;
}

}
