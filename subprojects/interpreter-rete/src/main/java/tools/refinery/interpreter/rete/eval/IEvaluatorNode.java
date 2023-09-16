/*******************************************************************************
 * Copyright (c) 2010-2016, Gabor Bergmann, IncQueryLabs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.rete.eval;

import tools.refinery.interpreter.rete.network.ReteContainer;

/**
 * This interface is required for the communication between the evaluation core end the evaluator node.
 * @author Gabor Bergmann
 * @since 1.5
 */
public interface IEvaluatorNode {

    ReteContainer getReteContainer();

    String prettyPrintTraceInfoPatternList();


}
