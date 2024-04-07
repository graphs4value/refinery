/*
 * SPDX-FileCopyrightText: 2021-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic;

public non-sealed interface AbstractDomain<A extends AbstractValue<A, C>, C> extends AnyAbstractDomain {
	@Override
	Class<A> abstractType();

	@Override
	Class<C> concreteType();

	A unknown();

	A error();

	A toAbstract(C concreteValue);
}
