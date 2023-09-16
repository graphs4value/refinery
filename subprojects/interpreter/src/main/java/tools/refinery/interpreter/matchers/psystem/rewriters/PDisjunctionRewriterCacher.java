/*******************************************************************************
 * Copyright (c) 2010-2014, Zoltan Ujhelyi, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.psystem.rewriters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.WeakHashMap;

import tools.refinery.interpreter.matchers.psystem.queries.PDisjunction;

/**
 * A rewriter that stores the previously computed results of a rewriter or a rewriter chain.
 *
 * @author Zoltan Ujhelyi
 * @since 1.0
 */
public class PDisjunctionRewriterCacher extends PDisjunctionRewriter {

    private final List<PDisjunctionRewriter> rewriterChain;
    private WeakHashMap<PDisjunction, PDisjunction> cachedResults =
            new WeakHashMap<PDisjunction, PDisjunction>();

    private void setupTraceCollectorInChain(){
        IRewriterTraceCollector collector = getTraceCollector();
        for(PDisjunctionRewriter rewriter: rewriterChain){
            rewriter.setTraceCollector(collector);
        }
    }

    public PDisjunctionRewriterCacher(PDisjunctionRewriter rewriter) {
        rewriterChain = Collections.singletonList(rewriter);
    }

    public PDisjunctionRewriterCacher(PDisjunctionRewriter... rewriters) {
        rewriterChain = new ArrayList<>(Arrays.asList(rewriters));
    }

    public PDisjunctionRewriterCacher(List<PDisjunctionRewriter> rewriterChain) {
        this.rewriterChain = new ArrayList<>(rewriterChain);
    }

    @Override
    public PDisjunction rewrite(PDisjunction disjunction) {
        if (!cachedResults.containsKey(disjunction)) {
            PDisjunction rewritten = disjunction;
            setupTraceCollectorInChain();
            for (PDisjunctionRewriter rewriter : rewriterChain) {
                rewritten = rewriter.rewrite(rewritten);
            }

            cachedResults.put(disjunction, rewritten);
        }
        return cachedResults.get(disjunction);
    }

}
