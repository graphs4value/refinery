/*******************************************************************************
 * Copyright (c) 2004-2010 Gabor Bergmann and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.interpreter.matchers.psystem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.stream.Collectors;

import tools.refinery.interpreter.matchers.planning.helpers.TypeHelper;
import tools.refinery.interpreter.matchers.psystem.basicdeferred.ExportedParameter;
import tools.refinery.interpreter.matchers.psystem.basicenumerables.ConstantValue;
import tools.refinery.interpreter.matchers.psystem.queries.PDisjunction;
import tools.refinery.interpreter.matchers.psystem.queries.PQuery;
import tools.refinery.interpreter.matchers.context.IQueryMetaContext;
import tools.refinery.interpreter.matchers.util.Preconditions;

/**
 * A set of constraints representing a pattern body
 *
 * @author Gabor Bergmann
 *
 */
public class PBody implements PTraceable {

    public static final String VIRTUAL_VARIABLE_PREFIX = ".virtual";
    private static final String VIRTUAL_VARIABLE_PATTERN = VIRTUAL_VARIABLE_PREFIX + "{%d}";

    private PQuery query;

    /**
     * If null, then parent query status is reused
     */
    private PQuery.PQueryStatus status = PQuery.PQueryStatus.UNINITIALIZED;

    private Set<PVariable> allVariables;
    private Set<PVariable> uniqueVariables;
    private List<ExportedParameter> symbolicParameters;
    private Map<Object, PVariable> variablesByName;
    private Set<PConstraint> constraints;
    private int nextVirtualNodeID;
    private PDisjunction containerDisjunction;

    public PBody(PQuery query) {
        super();
        this.query = query;
        allVariables = new LinkedHashSet<>();
        uniqueVariables = new LinkedHashSet<>();
        variablesByName = new HashMap<>();
        constraints = new LinkedHashSet<>();
    }

    /**
     * @return whether the submission of the new variable was successful
     */
    private boolean addVariable(PVariable var) {
        checkMutability();
        Object name = var.getName();
        if (!variablesByName.containsKey(name)) {
            allVariables.add(var);
            if (var.isUnique())
                uniqueVariables.add(var);
            variablesByName.put(name, var);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Use this method to add a newly created constraint to the pSystem.
     *
     * @return whether the submission of the new constraint was successful
     */
    boolean registerConstraint(PConstraint constraint) {
        checkMutability();
        return constraints.add(constraint);
    }

    /**
     * Use this method to remove an obsolete constraint from the pSystem.
     *
     * @return whether the removal of the constraint was successful
     */
    boolean unregisterConstraint(PConstraint constraint) {
        checkMutability();
        return constraints.remove(constraint);
    }

    @SuppressWarnings("unchecked")
    public <ConstraintType> Set<ConstraintType> getConstraintsOfType(Class<ConstraintType> constraintClass) {
        Set<ConstraintType> result = new HashSet<ConstraintType>();
        for (PConstraint pConstraint : constraints) {
            if (constraintClass.isInstance(pConstraint))
                result.add((ConstraintType) pConstraint);
        }
        return result;
    }

    public PVariable newVirtualVariable() {
        checkMutability();
        String name;
        do {

            name = String.format(VIRTUAL_VARIABLE_PATTERN, nextVirtualNodeID++);
        } while (variablesByName.containsKey(name));
        PVariable var = new PVariable(this, name, true);
        addVariable(var);
        return var;
    }

    public PVariable newVirtualVariable(String name) {
        checkMutability();
        Preconditions.checkArgument(!variablesByName.containsKey(name), "ID %s already used for a virtual variable", name);
        PVariable var = new PVariable(this, name, true);
        addVariable(var);
        return var;
    }

    public PVariable newConstantVariable(Object value) {
        checkMutability();
        PVariable virtual = newVirtualVariable();
        new ConstantValue(this, virtual, value);
        return virtual;
    }

    public Set<PVariable> getAllVariables() {
        return allVariables;
    }

    public Set<PVariable> getUniqueVariables() {
        return uniqueVariables;
    }

    private PVariable getVariableByName(Object name) {
        return variablesByName.get(name).getUnifiedIntoRoot();
    }

    /**
     * Find a PVariable by name
     *
     * @param name
     * @return the found variable
     * @throws IllegalArgumentException
     *             if no PVariable is found with the selected name
     */
    public PVariable getVariableByNameChecked(Object name) {
        if (!variablesByName.containsKey(name))
            throw new IllegalArgumentException(String.format("Cannot find PVariable %s", name));
        return getVariableByName(name);
    }

    /**
     * Finds and returns a PVariable by name. If no PVariable exists with the name in the body, a new one is created. If
     * the name of the variable starts with {@value #VIRTUAL_VARIABLE_PREFIX}, the created variable will be considered
     * virtual.
     *
     * @param name
     * @return a PVariable with the selected name; never null
     */
    public PVariable getOrCreateVariableByName(String name) {
        checkMutability();
        if (!variablesByName.containsKey(name)) {
            addVariable(new PVariable(this, name, name.startsWith(VIRTUAL_VARIABLE_PREFIX)));
        }
        return getVariableByName(name);
    }

    public Set<PConstraint> getConstraints() {
        return constraints;
    }

    public PQuery getPattern() {
        return query;
    }

    void noLongerUnique(PVariable pVariable) {
        assert (!pVariable.isUnique());
        uniqueVariables.remove(pVariable);
    }

    /**
     * Returns the symbolic parameters of the body. </p>
     *
     * <p>
     * <strong>Warning</strong>: if two PVariables are unified, the returned list changes. If you want to have a stable
     * version, consider using {@link #getSymbolicParameters()}.
     *
     * @return a non-null, but possibly empty list
     */
    public List<PVariable> getSymbolicParameterVariables() {
        return getSymbolicParameters().stream().map(ExportedParameter::getParameterVariable)
                .collect(Collectors.toList());
    }

    /**
     * Returns the exported parameter constraints of the body.
     *
     * @return a non-null, but possibly empty list
     */
    public List<ExportedParameter> getSymbolicParameters() {
        if (symbolicParameters == null)
            symbolicParameters = new ArrayList<>();
        return symbolicParameters;
    }

    /**
     * Sets the exported parameter constraints of the body, if this instance is mutable.
     * @param symbolicParameters the new value
     */
    public void setSymbolicParameters(List<ExportedParameter> symbolicParameters) {
        checkMutability();
        this.symbolicParameters = new ArrayList<>(symbolicParameters);
    }

    /**
     * Sets a specific status for the body. If set, the parent PQuery status will not be checked; if set to null, its corresponding PQuery
     * status is checked for mutability.
     *
     * @param status
     *            the status to set
     */
    public void setStatus(PQuery.PQueryStatus status) {
        this.status = status;
    }

    public boolean isMutable() {
        if (status == null) {
            return query.isMutable();
        } else {
            return status.equals(PQuery.PQueryStatus.UNINITIALIZED);
        }
    }

    void checkMutability() {
        if (status == null) {
            query.checkMutability();
        } else {
            Preconditions.checkState(status.equals(PQuery.PQueryStatus.UNINITIALIZED), "Initialized queries are not mutable");
        }
    }

    /**
     * Returns the disjunction the body is contained with. This disjunction may either be the
     * {@link PQuery#getDisjunctBodies() canonical disjunction of the corresponding query} or something equivalent.
     *
     * @return the container disjunction of the body. Can be null if body is not in a disjunction yet.
     */
    public PDisjunction getContainerDisjunction() {
        return containerDisjunction;
    }

    /**
     * @param containerDisjunction the containerDisjunction to set
     */
    public void setContainerDisjunction(PDisjunction containerDisjunction) {
        Preconditions.checkArgument(query.equals(containerDisjunction.getQuery()), "Disjunction of pattern %s incompatible with body %s", containerDisjunction.getQuery().getFullyQualifiedName(), query.getFullyQualifiedName());
        Preconditions.checkState(this.containerDisjunction == null, "Disjunction is already set.");
        this.containerDisjunction = containerDisjunction;
    }

    /**
     * All unary input keys directly prescribed by constraints, grouped by variable.
     * <p> to supertype inference or subsumption applied at this point.
     */
    public Map<PVariable, Set<TypeJudgement>> getAllUnaryTypeRestrictions(IQueryMetaContext context) {
        Map<PVariable, Set<TypeJudgement>> currentRestrictions = allUnaryTypeRestrictions.get(context);
        if (currentRestrictions == null) {
            currentRestrictions = TypeHelper.inferUnaryTypes(getConstraints(), context);
            allUnaryTypeRestrictions.put(context, currentRestrictions);
        }
        return currentRestrictions;
    }
    private WeakHashMap<IQueryMetaContext, Map<PVariable, Set<TypeJudgement>>> allUnaryTypeRestrictions = new WeakHashMap<IQueryMetaContext, Map<PVariable,Set<TypeJudgement>>>();

}
