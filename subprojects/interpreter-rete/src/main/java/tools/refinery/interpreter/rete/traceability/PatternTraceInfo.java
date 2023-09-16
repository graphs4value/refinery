/*******************************************************************************
 * Copyright (c) 2010-2014, Bergmann Gabor, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.rete.traceability;

/**
 * One kind of trace marker that merely establishes the pattern for which the node was built.
 * @author Bergmann Gabor
 */
public interface PatternTraceInfo extends TraceInfo {
    String getPatternName();
}
