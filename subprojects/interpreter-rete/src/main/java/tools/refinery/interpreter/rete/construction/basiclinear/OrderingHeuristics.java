/*******************************************************************************
 * Copyright (c) 2004-2010 Gabor Bergmann and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.interpreter.rete.construction.basiclinear;

import java.util.Comparator;
import java.util.Set;

import tools.refinery.interpreter.matchers.context.IQueryMetaContext;
import tools.refinery.interpreter.matchers.planning.SubPlan;
import tools.refinery.interpreter.matchers.psystem.DeferredPConstraint;
import tools.refinery.interpreter.matchers.psystem.EnumerablePConstraint;
import tools.refinery.interpreter.matchers.psystem.PConstraint;
import tools.refinery.interpreter.matchers.psystem.PVariable;
import tools.refinery.interpreter.matchers.psystem.basicenumerables.ConstantValue;
import tools.refinery.interpreter.matchers.util.CollectionsFactory;
import tools.refinery.interpreter.rete.util.OrderingCompareAgent;

/**
 * @author Gabor Bergmann
 *
 */
public class OrderingHeuristics implements Comparator<PConstraint> {
    private SubPlan plan;
    private IQueryMetaContext context;

    public OrderingHeuristics(SubPlan plan, IQueryMetaContext context) {
        super();
        this.plan = plan;
        this.context = context;
    }

    @Override
    public int compare(PConstraint o1, PConstraint o2) {
        return new OrderingCompareAgent<PConstraint>(o1, o2) {
            @Override
            protected void doCompare() {
                boolean temp = consider(preferTrue(isConstant(a), isConstant(b)))
                        && consider(preferTrue(isReady(a), isReady(b)));
                if (!temp)
                    return;

                Set<PVariable> bound1 = boundVariables(a);
                Set<PVariable> bound2 = boundVariables(b);
                swallowBoolean(temp && consider(preferTrue(isBound(a, bound1), isBound(b, bound2)))
                        && consider(preferMore(degreeBound(a, bound1), degreeBound(b, bound2)))
                        && consider(preferLess(degreeFree(a, bound1), degreeFree(b, bound2)))

                        // tie breaking
                        && consider(preferLess(a.getMonotonousID(), b.getMonotonousID())) // this is hopefully deterministic
                        && consider(preferLess(System.identityHashCode(a), System.identityHashCode(b))));
            }
        }.compare();
    }

    boolean isConstant(PConstraint o) {
        return (o instanceof ConstantValue);
    }

    boolean isReady(PConstraint o) {
        return (o instanceof EnumerablePConstraint)
                || (o instanceof DeferredPConstraint && ((DeferredPConstraint) o)
                        .isReadyAt(plan, context));
    }

    Set<PVariable> boundVariables(PConstraint o) {
        Set<PVariable> boundVariables = CollectionsFactory.createSet(o.getAffectedVariables());
        boundVariables.retainAll(plan.getVisibleVariables());
        return boundVariables;
    }

    boolean isBound(PConstraint o, Set<PVariable> boundVariables) {
        return boundVariables.size() == o.getAffectedVariables().size();
    }

    int degreeBound(PConstraint o, Set<PVariable> boundVariables) {
        return boundVariables.size();
    }

    int degreeFree(PConstraint o, Set<PVariable> boundVariables) {
        return o.getAffectedVariables().size() - boundVariables.size();
    }

}
