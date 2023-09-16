/*******************************************************************************
 * Copyright (c) 2004-2010 Gabor Bergmann and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.interpreter.matchers.psystem.basicdeferred;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import tools.refinery.interpreter.matchers.psystem.PBody;
import tools.refinery.interpreter.matchers.psystem.PVariable;
import tools.refinery.interpreter.matchers.psystem.VariableDeferredPConstraint;

/**
 * @author Gabor Bergmann
 *
 */
public class Inequality extends VariableDeferredPConstraint {

    private PVariable who;
    private PVariable withWhom;

    /**
     * The inequality constraint is weak if it can be ignored when who is the same as withWhom, or if any if them is
     * undeducible.
     */
    private boolean weak;

    public Inequality(PBody pBody, PVariable who, PVariable withWhom) {
        this(pBody, who, withWhom, false);
    }

    public Inequality(PBody pBody, PVariable who, PVariable withWhom,
            boolean weak) {
        super(pBody, new HashSet<>(Arrays.asList(who, withWhom) ));
        this.who = who;
        this.withWhom = withWhom;
        this.weak = weak;
    }

    // private Inequality(
    // PSystem<PatternDescription, StubHandle, ?> pSystem,
    // PVariable subject, Set<PVariable> inequals)
    // {
    // super(pSystem, include(inequals, subject));
    // this.subject = subject;
    // this.inequals = inequals;
    // }

    // private static HashSet<PVariable> include(Set<PVariable> inequals, PVariable subject) {
    // HashSet<PVariable> hashSet = new HashSet<PVariable>(inequals);
    // hashSet.add(subject);
    // return hashSet;
    // }

    @Override
    public Set<PVariable> getDeferringVariables() {
        return getAffectedVariables();
    }

    // private static int[] mapIndices(Map<Object, Integer> variablesIndex, Set<PVariable> keys) {
    // int[] result = new int[keys.size()];
    // int k = 0;
    // for (PVariable key : keys) {
    // result[k++] = variablesIndex.get(key);
    // }
    // return result;
    // }

    // @Override
    // public IFoldablePConstraint getIncorporator() {
    // return incorporator;
    // }
    //
    // @Override
    // public void registerIncorporatationInto(IFoldablePConstraint incorporator) {
    // this.incorporator = incorporator;
    // }
    //
    // @Override
    // public boolean incorporate(IFoldablePConstraint other) {
    // if (other instanceof Inequality<?, ?>) {
    // Inequality other2 = (Inequality) other;
    // if (subject.equals(other2.subject)) {
    // Set<PVariable> newInequals = new HashSet<PVariable>(inequals);
    // newInequals.addAll(other2.inequals);
    // return new Inequality<PatternDescription, StubHandle>(buildable, subject, newInequals);
    // }
    // } else return false;
    // }

    @Override
    protected String toStringRest() {
        return who.toString() + (isWeak() ? "!=?" : "!=") + withWhom.toString();
    }

    @Override
    public void doReplaceVariable(PVariable obsolete, PVariable replacement) {
        if (obsolete.equals(who))
            who = replacement;
        if (obsolete.equals(withWhom))
            withWhom = replacement;
    }

    @Override
    public Set<PVariable> getDeducedVariables() {
        return Collections.emptySet();
    }

    /**
     * The inequality constraint is weak if it can be ignored when who is the same as withWhom, or if any if them is
     * undeducible.
     *
     * @return the weak
     */
    public boolean isWeak() {
        return weak;
    }

    /**
     * A weak inequality constraint is eliminable if who is the same as withWhom, or if any if them is undeducible.
     */
    public boolean isEliminable() {
        return isWeak() && (who.equals(withWhom) || !who.isDeducable() || !withWhom.isDeducable());
    }

    /**
     * Eliminates a weak inequality constraint if it can be ignored when who is the same as withWhom, or if any if them
     * is undeducible.
     */
    public void eliminateWeak() {
        if (isEliminable())
            delete();
    }

    public PVariable getWho() {
        return who;
    }

    public PVariable getWithWhom() {
        return withWhom;
    }

}
