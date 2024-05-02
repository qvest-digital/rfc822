package org.evolvis.tartools.rfc822;

/*-
 * Copyright © 2020, 2021 mirabilos (t.glaser@qvest-digital.com)
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

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test {@link FQDN} class
 *
 * @author mirabilos (t.glaser@qvest-digital.com)
 */
class FQDNTest {

private static void
val(final String hostname)
{
	assertTrue(FQDN.isDomain(hostname), () -> "not valid: " + hostname);
	assertNotNull(FQDN.asDomain(hostname), () -> "inconsistently valid: " + hostname);
}

@SuppressWarnings("SameParameterValue")
private static void
val(final String hostname, final String dotAtom)
{
	assertFalse(FQDN.isDomain(hostname), () -> "unexpectedly valid: " + hostname);
	val dn = FQDN.asDomain(hostname);
	assertNotNull(dn, () -> "not valid: " + hostname);
	assertEquals(dotAtom, dn, () -> "not correctly resolved: " + hostname);
}

private static void
inv(final String hostname)
{
	assertFalse(FQDN.isDomain(hostname), () -> "not invalid: " + hostname);
	assertNull(FQDN.asDomain(hostname), () -> "inconsistently invalid: " + hostname);
}

@Test
public void testPos()
{
	final FQDN tp = FQDN.of("host.domain.tld");
	assertNotNull(tp, "cannot instantiate for valid example");
	assertTrue(tp.isDomain(), "basic valid example doesn’t validate");

	inv(null);
	inv("");
	val("eu");
	val("example.com");
	val("a.example.com");
	val("ab.example.com");
	val("a-b.example.com");
	inv("a-.example.com");
	inv("-a.example.com");
	val("123.example.com");
	val("1.2.3");
	val("1.2.3.4");
	val("1.2.3.4.5");
	val("123456789012345678901234567890123456789012345678901234567890123");
	inv("1234567890123456789012345678901234567890123456789012345678901234");
	val("123456789012345678901234567890123456789012345678901234567890123.123456789012345678901234567890123456789012345678901234567890123.123456789012345678901234567890123456789012345678901234567890123.1234567890123456789012345678901234567890123456789012345678901");
	inv("123456789012345678901234567890123456789012345678901234567890123.123456789012345678901234567890123456789012345678901234567890123.123456789012345678901234567890123456789012345678901234567890123.12345678901234567890123456789012345678901234567890123456789012");
	val("a.b.c.d.e.f.g.h.i.j.k.l.m.n.o.p.q.r.s.t.u.v.w.x.y.z.A.B.C.D.E.F.G.H.I.J.K.L.M.N.O.P.Q.R.S.T.U.V.W.X.Y.Z.0.1.2.3.4.5.6.7.8.9.10.11.12.0-0.0-0-0.ab.a0.0a.c-d.e-f-g.h-i-j-k-l-m-n--o--p---q----r-----s------t.u--v--w--x--y--z---------------------A.0123456789");
	inv("_foo.example.com");
	inv("foo_.example.com");
	inv("f_oo.example.com");
	inv(" ");
	inv(" example.com");
	inv("example.com ");
	val("example.com.", "example.com");
	inv("example.com..");
}

}
