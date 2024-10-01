/*******************************************************************************
 * Copyright (c) 2010-2012, Tamas Szabo, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.rete.itc;

import org.junit.runners.Parameterized.Parameters;
import tools.refinery.interpreter.rete.itc.graphs.*;

import java.util.Arrays;
import java.util.Collection;

public abstract class BaseTransitiveClosureAlgorithmTest {

    @Parameters
    public static Collection<Object[]> getGraphs() {
        return Arrays.asList(new Object[][] {
                         { new SelfLoopGraph()},
                         { new Graph1() },
                         { new Graph2() },
                         { new Graph3() },
                         { new Graph4() },
                         { new Graph5() },
                         { new Graph6() },
                         { new Graph7() },
                         { new Graph8() }
        });
    }

}
