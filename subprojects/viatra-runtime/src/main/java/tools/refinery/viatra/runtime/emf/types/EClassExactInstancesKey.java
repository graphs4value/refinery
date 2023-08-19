/*******************************************************************************
 * Copyright (c) 2010-2018, Gabor Bergmann, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.viatra.runtime.emf.types;

import org.eclipse.emf.ecore.EClass;
import tools.refinery.viatra.runtime.emf.EMFScope;
import tools.refinery.viatra.runtime.emf.helper.ViatraQueryRuntimeHelper;

/**
 * Instance tuples are of form (x), where x is an eObject instance of the given eClass, but <b>not</b> one of its subclasses, <b>within the scope</b>.
 * <p> This input key has the strict semantics that instances must be within the scope.
 * 
 * @noreference This class is not intended to be referenced by clients. Not currently supported by {@link EMFScope}, for internal use only at the time
 * 
 * @author Bergmann Gabor
 * @since 2.1
 */
public class EClassExactInstancesKey extends BaseEMFTypeKey<EClass> {

	public EClassExactInstancesKey(EClass emfKey) {
		super(emfKey);
	}

	@Override
	public String getPrettyPrintableName() {
        return "(scoped,exact) "+ViatraQueryRuntimeHelper.prettyPrintEMFType(wrappedKey);
	}

	@Override
	public String getStringID() {
        return "eClass(scoped,exact)#"+ ViatraQueryRuntimeHelper.prettyPrintEMFType(wrappedKey);
	}

	@Override
	public int getArity() {
		return 1;
	}

	@Override
	public boolean isEnumerable() {
		return true;
	}

}
