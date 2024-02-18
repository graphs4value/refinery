/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.scoping;

import com.google.common.base.Predicate;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.eclipse.xtext.resource.ISelectable;
import org.eclipse.xtext.scoping.IScope;
import org.eclipse.xtext.scoping.impl.SelectableBasedScope;
import tools.refinery.language.resource.ProblemResourceDescriptionStrategy;

import java.util.Objects;

public class ShadowingKeyAwareSelectableBasedScope extends SelectableBasedScope {
	public static IScope createScope(IScope outer, ISelectable selectable, EClass type, boolean ignoreCase) {
		return createScope(outer, selectable, null, type, ignoreCase);
	}

	// {@link com.google.common.base.Predicate} required by Xtext API.
	@SuppressWarnings("squid:S4738")
	public static IScope createScope(IScope outer, ISelectable selectable, Predicate<IEObjectDescription> filter,
									 EClass type, boolean ignoreCase) {
		if (selectable == null || selectable.isEmpty())
			return outer;
		return new ShadowingKeyAwareSelectableBasedScope(outer, selectable, filter, type, ignoreCase);
	}

	// {@link com.google.common.base.Predicate} required by Xtext API.
	@SuppressWarnings("squid:S4738")
	protected ShadowingKeyAwareSelectableBasedScope(IScope outer, ISelectable selectable,
													Predicate<IEObjectDescription> filter,
													EClass type, boolean ignoreCase) {
		super(outer, selectable, filter, type, ignoreCase);
	}

	@Override
	protected boolean isShadowed(IEObjectDescription input) {
		var shadowingKey = input.getUserData(ProblemResourceDescriptionStrategy.SHADOWING_KEY);
		var localElements = getLocalElementsByName(input.getName());
		for (var localElement : localElements) {
			var localElementKey = localElement.getUserData(ProblemResourceDescriptionStrategy.SHADOWING_KEY);
			if (Objects.equals(shadowingKey, localElementKey)) {
				return true;
			}
		}
		return false;
	}
}
