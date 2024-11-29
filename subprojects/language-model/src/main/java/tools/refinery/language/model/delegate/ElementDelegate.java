/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.model.delegate;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.util.BasicSettingDelegate;
import tools.refinery.language.model.problem.ProblemPackage;

public class ElementDelegate extends BasicSettingDelegate.Stateless {
	private final EClass type;

	public ElementDelegate(EReference reference) {
		super(reference);
		type = reference.getEReferenceType();
	}

	@Override
	public Object get(InternalEObject owner, boolean resolve, boolean coreType) {
		var value = owner.eGet(ProblemPackage.Literals.VARIABLE_OR_NODE_EXPR__ELEMENT, resolve, coreType);
		if (type.isInstance(value)) {
			return value;
		}
		return null;
	}

	@Override
	public void set(InternalEObject owner, Object newValue) {
		owner.eSet(ProblemPackage.Literals.VARIABLE_OR_NODE_EXPR__ELEMENT, newValue);
	}

	@Override
	public boolean isSet(InternalEObject owner) {
		return owner.eIsSet(ProblemPackage.Literals.VARIABLE_OR_NODE_EXPR__ELEMENT);
	}
}
