/*******************************************************************************
 * Copyright (c) 2010-2014, Zoltan Ujhelyi, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.localsearch.operations;

import java.util.Map;

import tools.refinery.interpreter.localsearch.MatchingFrame;
import tools.refinery.interpreter.matchers.psystem.IValueProvider;
import tools.refinery.interpreter.matchers.util.Preconditions;

/**
 *
 *
 * @author Zoltan Ujhelyi
 *
 */
public class MatchingFrameValueProvider implements IValueProvider {

    final Map<String, Integer> nameMap;
    final MatchingFrame frame;

    public MatchingFrameValueProvider(MatchingFrame frame, Map<String, Integer> nameMap) {
        super();
        this.frame = frame;
        this.nameMap = nameMap;
    }

    @Override
    public Object getValue(String variableName) {
        Integer index = nameMap.get(variableName);
        Preconditions.checkArgument(index != null, "Unknown parameter variable name");
        return frame.get(index);
    }

}
