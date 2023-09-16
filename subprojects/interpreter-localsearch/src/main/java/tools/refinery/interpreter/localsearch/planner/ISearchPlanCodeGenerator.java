/*******************************************************************************
 * Copyright (c) 2010-2014, Marton Bur, Akos Horvath, Zoltan Ujhelyi, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.localsearch.planner;

import java.util.List;

import tools.refinery.interpreter.localsearch.operations.ISearchOperation;

/**
 * @author Marton Bur
 *
 */
public interface ISearchPlanCodeGenerator {

    void compile(List<List<ISearchOperation>> plans);

}
