/*******************************************************************************
 * Copyright (c) 2010-2013, Zoltan Ujhelyi, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.viatra.runtime.matchers.psystem.queries;

import java.util.List;
import java.util.Set;

import tools.refinery.viatra.runtime.matchers.ViatraQueryRuntimeException;
import tools.refinery.viatra.runtime.matchers.backend.IQueryBackend;
import tools.refinery.viatra.runtime.matchers.backend.IQueryBackendHintProvider;
import tools.refinery.viatra.runtime.matchers.backend.QueryEvaluationHint;
import tools.refinery.viatra.runtime.matchers.psystem.PBody;
import tools.refinery.viatra.runtime.matchers.psystem.PTraceable;
import tools.refinery.viatra.runtime.matchers.psystem.TypeJudgement;

/**
 * Internal representation of a query / graph pattern (using a constraint system formalism), 
 * to be interpreted by a query evaluator ({@link IQueryBackend}). 
 * End-users of VIATRA Query should access a query as an IQuerySpecification instead. 
 * 
 * <p>
 * PQuerys are definitions of queries usable inside pattern descriptions. Such description always has (a non-null) name. The query
 * itself is defined as a (non-empty) set of {@link PBody} instances, the result is the disjunction of the single
 * {@link PBody} instances. </p>
 * <p>
 * A PQuery might be constructed from erroneous patterns or might be uninitialized - this is represented by its status.
 * 
 * @author Zoltan Ujhelyi
 * @since 0.8.0
 * @noimplement This interface is not intended to be implemented by clients. Use {@link BasePQuery} as a base class instead.
 */
public interface PQuery extends PQueryHeader, PTraceable {

    // TODO rewritten as / rewritten from traceability to PDisjunction?
    
    /**
     * @author Zoltan Ujhelyi
     * 
     */
    public enum PQueryStatus {
        /**
         * Marks that the query definition is not initialized
         */
        UNINITIALIZED,
        /**
         * Marks that the query definition is being initialized
         * @since 1.4
         */
        INITIALIZING,
        /**
         * The query definition was successfully initialized
         */
        OK,
        /**
         * The query definition was initialized, but some issues were present
         */
        WARNING,
        /**
         * The query definition was not successfully initialized because of an error
         */
        ERROR
    }

    /**
     * Returns all bodies associated with the query in their canonical form. If called multiple times, the same set with
     * the same contents will be returned.
     * 
     */
    PDisjunction getDisjunctBodies();

    /**
     * Returns all queries directly referred in the constraints. They are all required to evaluate this query
     * 
     * @return a non-null, but possibly empty list of query definitions
     */
    Set<PQuery> getDirectReferredQueries();

    /**
     * Returns all queries required to evaluate this query (transitively).
     * 
     * @return a non-null, but possibly empty list of query definitions
     */
    Set<PQuery> getAllReferredQueries();

    /**
     * Returns the initialization status of the definition
     * 
     */
    PQueryStatus getStatus();
    
    /**
     * Returns a list describing the problems that were found in this query.
     * 
     * <p> TODO: formulate invariant connecting {@link #getPProblems()} and {@link #getStatus()}.
     * 
     * @return a non-null, but possibly empty list of problems
     */
    List<PProblem> getPProblems();

    /**
     * Before a modification operation is executed, a mutability check is performed (via the {@link #getStatus()}
     * implementation, and in case of problems an {@link IllegalStateException} is thrown.
     */
    void checkMutability();

    /**
     * An option to check mutability of the query. It can be used to avoid getting an {@link IllegalStateException} by
     * the execution of {@link #checkMutability()}.
     * 
     * @return true if the query specification is still editable
     */
    boolean isMutable();

    /**
     * Optional hints regarding the query evaluation strategy, to be interpreted by the query engine.
     * <p> To ensure the possibility of external overrides, 
     * 	the evaluation engine should not directly consult this field, 
     * 	but use an {@link IQueryBackendHintProvider} instead.
     */
    public QueryEvaluationHint getEvaluationHints();

    
    /**
     * Type information, expressed on query parameters, that all matches of the query are guaranteed to respect. 
     * <p> At the very minimum, this should include the declared types of the parameters.
     * <p> The type judgement tuples shall contain the <i>parameter index</i>, NOT the {@link PParameter} object.
     * 
     * @return a non-null set of type judgements that the query guarantees for its matches
     */
    public Set<TypeJudgement> getTypeGuarantees();

    /**
     * If the query definition is uninitialized, initializes it.
     * @throws ViatraQueryRuntimeException if initialization of query specification fails
     */
    public abstract void ensureInitialized();
    
    /**
     * Returns the end-user query specification API objects that wrap this query.
     * 
     * <p> Intended for traceability and debug purposes, not part of normal operation. 
     * Returned list is intended to be appended during query specification construction time.
     * 
     * @return a non-null, but possibly empty list of query specification objects;
     */
    List<Object> publishedAs();

}