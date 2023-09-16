/*******************************************************************************
 * Copyright (c) 2004-2010 Gabor Bergmann and Daniel Varro
 * Copyright (c) 2023 The Refinery Authors <https://refinery.tools>
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.interpreter.api;

import tools.refinery.interpreter.api.scope.IBaseIndex;
import tools.refinery.interpreter.api.scope.QueryScope;
import tools.refinery.interpreter.matchers.InterpreterRuntimeException;

import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * A Viatra Query (incremental) evaluation engine, attached to a model such as an EMF resource. The engine hosts pattern matchers, and
 * will listen on model update notifications stemming from the given model in order to maintain live results.
 *
 * <p>
 * By default, ViatraQueryEngines do not need to be separately disposed; they will be garbage collected along with the model.
 * Advanced users: see {@link AdvancedInterpreterEngine} if you want fine control over the lifecycle of an engine.
 *
 * <p>
 * Pattern matchers within this engine may be instantiated in the following ways:
 * <ul>
 * <li>Recommended: instantiate the specific matcher class generated for the pattern by e.g. MyPatternMatcher.on(engine).
 * <li>Use {@link #getMatcher(IQuerySpecification)} if the pattern-specific generated matcher API is not available.
 * <li>Advanced: use the query specification associated with the generated matcher class to achieve the same.
 * </ul>
 * Additionally, a group of patterns (see {@link IQueryGroup}) can be initialized together before usage; this may improve
 * the performance of pattern matcher construction by trying to gather all necessary information from the model in one go.
 * Note that no such improvement is to be expected if the engine is specifically constructed in wildcard mode,
 * an option available in some scope implementations
 * (see {@link EMFScope#EMFScope(Notifier, BaseIndexOptions)} and {@link BaseIndexOptions#withWildcardMode(boolean)}).
 *
 *
 * @author Bergmann Gábor
 * @noextend This class is not intended to be subclassed by clients.
 */
public abstract class InterpreterEngine {

    /**
     * Provides access to the internal base index component of the engine, responsible for keeping track of basic
     * contents of the model.
     *
     * <p>If using an {@link EMFScope},
     *  consider {@link EMFScope#extractUnderlyingEMFIndex(InterpreterEngine)} instead to access EMF-specific details.
     *
     * @return the baseIndex the NavigationHelper maintaining the base index
     * @throws InterpreterRuntimeException
     *             if the base index could not be constructed
     */
    public abstract IBaseIndex getBaseIndex();

    /**
     * Access a pattern matcher based on a {@link IQuerySpecification}.
     * Multiple calls will return the same matcher.
     * @param querySpecification a {@link IQuerySpecification} that describes a Refinery Interpreter query specification
     * @return a pattern matcher corresponding to the specification
     * @throws InterpreterRuntimeException if the matcher could not be initialized
     */
    public abstract <Matcher extends InterpreterMatcher<? extends IPatternMatch>> Matcher getMatcher(IQuerySpecification<Matcher> querySpecification);

    /**
     * Access a pattern matcher for the graph pattern with the given fully qualified name.
     * Will succeed only if a query specification for this fully qualified name has been generated and registered.
     * Multiple calls will return the same matcher unless the registered specification changes.
     *
     * @param patternFQN the fully qualified name of a Refinery Interpreter query specification
     * @return a pattern matcher corresponding to the specification
     * @throws InterpreterRuntimeException if the matcher could not be initialized
     */
    public abstract InterpreterMatcher<? extends IPatternMatch> getMatcher(String patternFQN);

    /**
     * Access an existing pattern matcher based on a {@link IQuerySpecification}.
     * @param querySpecification a {@link IQuerySpecification} that describes a Refinery Interpreter query specification
     * @return a pattern matcher corresponding to the specification, <code>null</code> if a matcher does not exist yet.
     */
    public abstract <Matcher extends InterpreterMatcher<? extends IPatternMatch>> Matcher getExistingMatcher(IQuerySpecification<Matcher> querySpecification);


    /**
     * Access a copy of available {@link InterpreterMatcher} pattern matchers.
     * @return a copy of the set of currently available pattern matchers registered on this engine instance
     */
    public abstract Set<? extends InterpreterMatcher<? extends IPatternMatch>> getCurrentMatchers();

    public Set<IQuerySpecification<? extends InterpreterMatcher<? extends IPatternMatch>>> getRegisteredQuerySpecifications() {
        return getCurrentMatchers().stream().map(InterpreterMatcher::getSpecification).collect(Collectors.toSet());
    }

    /**
     * @return the scope of query evaluation; the definition of the set of model elements that this engine is operates on.
     */
    public abstract QueryScope getScope();

	public abstract void flushChanges();

	public abstract <T> T withFlushingChanges(Supplier<T> supplier);
}
