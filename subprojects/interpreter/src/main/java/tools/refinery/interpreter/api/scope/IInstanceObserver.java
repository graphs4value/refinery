/*******************************************************************************
 * Copyright (c) 2010-2014, Bergmann Gabor, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.api.scope;


/**
 * Listener interface for lightweight observation of changes in edges leaving from given source instance elements.
 * @author Bergmann Gabor
 * @since 0.9
 *
 */
public interface IInstanceObserver {
    void notifyBinaryChanged(Object sourceElement, Object edgeType);
    void notifyTernaryChanged(Object sourceElement, Object edgeType);
}
