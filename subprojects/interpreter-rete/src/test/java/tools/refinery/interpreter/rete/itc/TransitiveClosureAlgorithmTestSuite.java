/*******************************************************************************
 * Copyright (c) 2010-2012, Tamas Szabo, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.rete.itc;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import tools.refinery.interpreter.rete.itc.counting.CountingCompleteGraphTestCase;
import tools.refinery.interpreter.rete.itc.dfs.DFSCompleteGraphTestCase;
import tools.refinery.interpreter.rete.itc.dred.DRedCompleteGraphTestCase;
import tools.refinery.interpreter.rete.itc.dred.DRedGraphsTestCase;
import tools.refinery.interpreter.rete.itc.incscc.IncSCCCompleteGraphTestCase;
import tools.refinery.interpreter.rete.itc.incscc.IncSCCGraphsTestCase;
import tools.refinery.interpreter.rete.itc.incscc.IncSCCPathConstructionTestCase;

@RunWith(Suite.class)
@SuiteClasses({
        DRedGraphsTestCase.class,
        DRedCompleteGraphTestCase.class,
        DFSCompleteGraphTestCase.class,
        CountingCompleteGraphTestCase.class,
        IncSCCGraphsTestCase.class,
        IncSCCCompleteGraphTestCase.class,
        IncSCCPathConstructionTestCase.class
})
public class TransitiveClosureAlgorithmTestSuite {

}
