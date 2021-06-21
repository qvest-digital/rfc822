package org.evolvis.tartools.rfc822;

/*-
 * Copyright © 2021 mirabilos (t.glaser@tarent.de)
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

import java.util.function.BiFunction;

/**
 * <p>Look-ahead matcher callback for {@link Parser#skipPeek(LookaheadMatcher)}.</p>
 *
 * <p>This is a functional interface for method references or lambdas.
 * It contains one method {@link #toSkip(int, int)} that determines
 * whether to abort or continue skipping, based on the characters in
 * the input data.</p>
 *
 * <p>This interface is identical to {@code BiFunction<int, int, boolean>}
 * except for the name of its functional method.</p>
 *
 * @author mirabilos (t.glaser@tarent.de)
 * @see ContextlessMatcher
 * @see BiFunction
 */
@FunctionalInterface
public interface LookaheadMatcher {

/**
 * <p>Determines whether the current character is to be skipped.</p>
 *
 * <p>Skipping ahead is determined based on the codepoints in the file, that is,
 * the input fully decoded to UCS (ISO 10646) codepoints, not just bytes or
 * Java™-internal UTF-16 characters. If the end of input is reached, {@code -1}
 * will be passed; otherwise, the 21-bit UCS codepoint of the respective wide
 * (possibly multibyte, in the input) character.</p>
 *
 * @param cur  the codepoint of the current character
 * @param next the codepoint of the character past the current one
 *
 * @return {@code true} to continue skipping ahead, {@code false} to stop skipping
 */
boolean toSkip(final int cur, final int next);

}
