/*******************************************************************************
 * Copyright (c) 2004-2010 Gabor Bergmann and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.interpreter.matchers.psystem.rewriters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import tools.refinery.interpreter.matchers.planning.QueryProcessingException;
import tools.refinery.interpreter.matchers.planning.helpers.TypeHelper;
import tools.refinery.interpreter.matchers.psystem.basicdeferred.Equality;
import tools.refinery.interpreter.matchers.psystem.basicdeferred.Inequality;
import tools.refinery.interpreter.matchers.context.IInputKey;
import tools.refinery.interpreter.matchers.context.IQueryMetaContext;
import tools.refinery.interpreter.matchers.psystem.ITypeConstraint;
import tools.refinery.interpreter.matchers.psystem.ITypeInfoProviderConstraint;
import tools.refinery.interpreter.matchers.psystem.PBody;
import tools.refinery.interpreter.matchers.psystem.PConstraint;
import tools.refinery.interpreter.matchers.psystem.TypeJudgement;
import tools.refinery.interpreter.matchers.psystem.queries.PDisjunction;
import tools.refinery.interpreter.matchers.psystem.queries.PQuery;
import tools.refinery.interpreter.matchers.psystem.queries.PQuery.PQueryStatus;
import tools.refinery.interpreter.matchers.util.CollectionsFactory;

/**
 * A disjunction rewriter for creating a normalized form of specification, unifying variables and running basic sanity
 * checks. This rewriter does not copy but modifies directly the original specification, requiring a mutable
 * disjunction.
 *
 * @author Gabor Bergmann
 *
 */
public class PBodyNormalizer extends PDisjunctionRewriter {

    private IQueryMetaContext context;

    public PBodyNormalizer(IQueryMetaContext context) {
        this.context = context;
    }

    /**
     * Returns whether unary constraint elimination is enabled. This behavior can be customized by creating a subclass
     * with a custom implementation.
     *
     * @since 1.6
     */
    protected boolean shouldCalculateImpliedTypes(PQuery query) {
        return true;
    }

    /**
     * Returns whether 'weakened alternative' suggestions of the context shall be expanded as additional PConstraints.
     * This behavior can be customized by creating a subclass
     * with a custom implementation.
     *
     * @since 1.6
     */
    protected boolean shouldExpandWeakenedAlternatives(PQuery query) {
        return false;
    }

    @Override
    public PDisjunction rewrite(PDisjunction disjunction) {
        Set<PBody> normalizedBodies = new LinkedHashSet<>();
        for (PBody body : disjunction.getBodies()) {
            PBodyCopier copier = new PBodyCopier(body, getTraceCollector());
            PBody modifiedBody = copier.getCopiedBody();
            normalizeBody(modifiedBody);
            normalizedBodies.add(modifiedBody);
            modifiedBody.setStatus(PQueryStatus.OK);
        }
        return new PDisjunction(normalizedBodies);
    }

    public void setContext(IQueryMetaContext context) {
        this.context = context;
    }

    /**
     * Provides a normalized version of the pattern body. May return a different version than the original version if
     * needed.
     *
     * @param body
     */
    public PBody normalizeBody(PBody body) {
        try {
            return normalizeBodyInternal(body);
        } catch (QueryProcessingException e) {
            throw new RewriterException("Error during rewriting: {1}", new String[] { e.getMessage() },
                    e.getShortMessage(), body.getPattern(), e);
        }
    }

    PBody normalizeBodyInternal(PBody body) {
        // UNIFICATION AND WEAK INEQUALITY ELIMINATION
        unifyVariablesAlongEqualities(body);
        eliminateWeakInequalities(body);
        removeMootEqualities(body);

        // ADDING WEAKENED ALTERNATIVES
        if (shouldExpandWeakenedAlternatives(body.getPattern())) {
            expandWeakenedAlternativeConstraints(body);
        }

        // CONSTRAINT ELIMINATION WITH TYPE INFERENCE
        if (shouldCalculateImpliedTypes(body.getPattern())) {
            eliminateInferrableTypes(body, context);
        } else {
            // ELIMINATE DUPLICATE TYPE CONSTRAINTS
            eliminateDuplicateTypeConstraints(body);
        }


        // PREVENTIVE CHECKS
        checkSanity(body);
        return body;
    }

    private void removeMootEqualities(PBody body) {
        Set<Equality> equals = body.getConstraintsOfType(Equality.class);
        for (Equality equality : equals) {
            if (equality.isMoot()) {
                equality.delete();
                derivativeRemoved(equality, ConstraintRemovalReason.MOOT_EQUALITY);
            }
        }
    }

    /**
     * Unifies allVariables along equalities so that they can be handled as one.
     *
     * @param body
     */
    void unifyVariablesAlongEqualities(PBody body) {
        Set<Equality> equals = body.getConstraintsOfType(Equality.class);
        for (Equality equality : equals) {
            if (!equality.isMoot()) {
                equality.getWho().unifyInto(equality.getWithWhom());
            }
        }
    }

    /**
     * Eliminates weak inequalities if they are not substantiated.
     *
     * @param body
     */
    void eliminateWeakInequalities(PBody body) {
        for (Inequality inequality : body.getConstraintsOfType(Inequality.class)){
            if (inequality.isEliminable()){
                inequality.eliminateWeak();
                derivativeRemoved(inequality, ConstraintRemovalReason.WEAK_INEQUALITY_SELF_LOOP);
            }
        }
    }

    /**
     * Eliminates all type constraints that are inferrable from other constraints.
     */
    void eliminateInferrableTypes(final PBody body, IQueryMetaContext context) {
        Set<TypeJudgement> subsumedByRetainedConstraints = new HashSet<TypeJudgement>();
        LinkedList<ITypeConstraint> allTypeConstraints = new LinkedList<ITypeConstraint>();
        for (PConstraint pConstraint : body.getConstraints()) {
            if (pConstraint instanceof ITypeConstraint) {
                allTypeConstraints.add((ITypeConstraint) pConstraint);
            } else if (pConstraint instanceof ITypeInfoProviderConstraint) {
                // non-type constraints are all retained
                final Set<TypeJudgement> directJudgements = ((ITypeInfoProviderConstraint) pConstraint)
                        .getImpliedJudgements(context);
                subsumedByRetainedConstraints = TypeHelper.typeClosure(subsumedByRetainedConstraints, directJudgements,
                        context);
            }
        }
        Comparator<ITypeConstraint> eliminationOrder = (o1, o2) -> {
            IInputKey type1 = o1.getEquivalentJudgement().getInputKey();
            IInputKey type2 = o2.getEquivalentJudgement().getInputKey();

            int result = context.getSuggestedEliminationOrdering().compare(type1, type2);
            return (result == 0)
                    ? PConstraint.COMPARE_BY_MONOTONOUS_ID.compare(o1, o2)
                    : result;
        };

        Collections.sort(allTypeConstraints, eliminationOrder);
        Queue<ITypeConstraint> potentialConstraints = allTypeConstraints; // rename for better comprehension

        while (!potentialConstraints.isEmpty()) {
            ITypeConstraint candidate = potentialConstraints.poll();

            boolean isSubsumed = subsumedByRetainedConstraints.contains(candidate.getEquivalentJudgement());
            if (!isSubsumed) {
                Set<TypeJudgement> typeClosure = subsumedByRetainedConstraints;
                for (ITypeConstraint subsuming : potentialConstraints) { // the remaining ones
                    final Set<TypeJudgement> directJudgements = subsuming.getImpliedJudgements(context);
                    typeClosure = TypeHelper.typeClosure(typeClosure, directJudgements, context);

                    if (typeClosure.contains(candidate.getEquivalentJudgement())) {
                        isSubsumed = true;
                        break;
                    }
                }
            }
            if (isSubsumed) { // eliminated
                candidate.delete();
                derivativeRemoved(candidate, ConstraintRemovalReason.TYPE_SUBSUMED);
            } else { // retained
                subsumedByRetainedConstraints = TypeHelper.typeClosure(subsumedByRetainedConstraints,
                        candidate.getImpliedJudgements(context), context);
            }
        }
    }

    /**
     * Inserts "weakened alternative" constraints suggested by the meta context that aid in coming up with a query plan.
     */
    void expandWeakenedAlternativeConstraints(PBody body) {
        Set<TypeJudgement> allJudgements = new HashSet<TypeJudgement>();
        Set<TypeJudgement> newJudgementsToAdd = new HashSet<TypeJudgement>();
        Queue<TypeJudgement> judgementsToProcess = new LinkedList<TypeJudgement>();
        Map<TypeJudgement, List<PConstraint>> traceability = CollectionsFactory.createMap();

        for (ITypeConstraint typeConstraint : body.getConstraintsOfType(ITypeConstraint.class)) {
            TypeJudgement equivalentJudgement = typeConstraint.getEquivalentJudgement();
            judgementsToProcess.add(equivalentJudgement);
            allJudgements.add(equivalentJudgement);
            traceability.computeIfAbsent(equivalentJudgement, k-> new ArrayList<>()).add(typeConstraint);
        }

        while (!judgementsToProcess.isEmpty()) {
            TypeJudgement judgement = judgementsToProcess.poll();
            for (TypeJudgement alternativeJudgement : judgement.getWeakenedAlternativeJudgements(context)) {
                if (allJudgements.add(alternativeJudgement)) {
                    newJudgementsToAdd.add(alternativeJudgement);
                    judgementsToProcess.add(alternativeJudgement);
                    traceability.merge(
                            alternativeJudgement,
                            traceability.getOrDefault(judgement, new ArrayList<>()),
                            (old,further) -> {old.addAll(further); return old;}
                    );
                }
            }
        }

        for (TypeJudgement typeJudgement : newJudgementsToAdd) {
            PConstraint newConstraint = typeJudgement.createConstraintFor(body);
            for (PConstraint source : traceability.getOrDefault(typeJudgement, Collections.emptyList())) {
                addTrace(source, newConstraint);
            }
        }
    }

    private Object getConstraintKey(PConstraint constraint) {
        if (constraint instanceof ITypeConstraint) {
            return ((ITypeConstraint) constraint).getEquivalentJudgement();
        }
        // Do not check duplication for any other types
        return constraint;
    }

    void eliminateDuplicateTypeConstraints(PBody body) {
        Map<Object, PConstraint> constraints = new HashMap<>();
        for (PConstraint constraint : body.getConstraints()) {
            Object key = getConstraintKey(constraint);
            // Retain first found instance of a constraint
            if (!constraints.containsKey(key)) {
                constraints.put(key, constraint);
            }
        }

        // Retain collected constraints, remove everything else
        Iterator<PConstraint> iterator = body.getConstraints().iterator();
        Collection<PConstraint> toRetain = constraints.values();
        while(iterator.hasNext()){
            PConstraint next = iterator.next();
            if (!toRetain.contains(next)){
                derivativeRemoved(next, ConstraintRemovalReason.DUPLICATE);
                iterator.remove();
            }
        }
    }

    /**
     * Verifies the sanity of all constraints. Should be issued as a preventive check before layouting.
     *
     * @param body
     * @throws RetePatternBuildException
     */
    void checkSanity(PBody body) {
        for (PConstraint pConstraint : body.getConstraints())
            pConstraint.checkSanity();
    }

}
