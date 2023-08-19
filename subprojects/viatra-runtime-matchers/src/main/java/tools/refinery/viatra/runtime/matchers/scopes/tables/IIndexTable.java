/*******************************************************************************
 * Copyright (c) 2010-2018, Gabor Bergmann, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.viatra.runtime.matchers.scopes.tables;

import java.util.Iterator;
import java.util.Optional;
import java.util.stream.Stream;

import tools.refinery.viatra.runtime.matchers.context.IInputKey;
import tools.refinery.viatra.runtime.matchers.context.IQueryMetaContext;
import tools.refinery.viatra.runtime.matchers.context.IQueryRuntimeContext;
import tools.refinery.viatra.runtime.matchers.context.IQueryRuntimeContextListener;
import tools.refinery.viatra.runtime.matchers.tuple.ITuple;
import tools.refinery.viatra.runtime.matchers.tuple.Tuple;
import tools.refinery.viatra.runtime.matchers.tuple.TupleMask;
import tools.refinery.viatra.runtime.matchers.util.Accuracy;

/**
 * Read-only interface that provides the {@link IInputKey}-specific slice of an instance store to realize a
 * {@link IQueryRuntimeContext}. Implemented by a customizable data store that is responsible for:
 * <ul>
 * <li>storing the instance tuples of the {@link IInputKey},</li>
 * <li>providing efficient lookup via storage-specific indexing,</li>
 * <li>delivering notifications. (TODO not designed yet)</li>
 * </ul>
 * 
 * <p>
 * Can be specialized for unary / binary / etc., opposite edges or node subtypes, specific types, distributed storage,
 * etc.
 * <p>
 * Writeable API is specific to the customized implementations (e.g. unary).
 * 
 * <p>
 * <b>Precondition:</b> the associated input key is enumerable, see {@link IQueryMetaContext#isEnumerable(IInputKey)}.
 * <p>
 * <strong>EXPERIMENTAL</strong>. This class or interface has been added as
 * part of a work in progress. There is no guarantee that this API will
 * work or that it will remain the same.
 *
 * @since 2.0
 * @author Gabor Bergmann
 * @noimplement This interface is not intended to be implemented directly. Extend {@link AbstractIndexTable} instead.
 */
public interface IIndexTable {

    // TODO add superinterface that represents a statistics-only counter?

    /**
     * @return the input key indexed by this table
     */
    public IInputKey getInputKey();

    /**
     * Returns the tuples, optionally seeded with the given tuple.
     * 
     * <p> Consider using the more idiomatic {@link #streamTuples(TupleMask, ITuple)} instead.
     * 
     * @param seedMask
     *            a mask that extracts those parameters of the input key (from the entire parameter list) that should be
     *            bound to a fixed value; must not be null. <strong>Note</strong>: any given index must occur at most
     *            once in seedMask.
     * @param seed
     *            the tuple of fixed values restricting the row set to be considered, in the same order as given in
     *            parameterSeedMask, so that for each considered row tuple,
     *            projectedParameterSeed.equals(parameterSeedMask.transform(row)) should hold. Must not be null.
     * @return the tuples in the table for the given key and seed
     */
    @SuppressWarnings("unchecked")
    public default Iterable<Tuple> enumerateTuples(TupleMask seedMask, ITuple seed) {
        return () -> (Iterator<Tuple>) (streamTuples(seedMask, seed).iterator());
    }

    /**
     * Returns the tuples, optionally seeded with the given tuple.
     * 
     * @param seedMask
     *            a mask that extracts those parameters of the input key (from the entire parameter list) that should be
     *            bound to a fixed value; must not be null. <strong>Note</strong>: any given index must occur at most
     *            once in seedMask.
     * @param seed
     *            the tuple of fixed values restricting the row set to be considered, in the same order as given in
     *            parameterSeedMask, so that for each considered row tuple,
     *            projectedParameterSeed.equals(parameterSeedMask.transform(row)) should hold. Must not be null.
     * @return the tuples in the table for the given key and seed
     * @since 2.1
     */
    public Stream<? extends Tuple> streamTuples(TupleMask seedMask, ITuple seed);

    /**
     * Simpler form of {@link #enumerateTuples(TupleMask, ITuple)} in the case where all values of the tuples are bound
     * by the seed except for one.
     * 
     * <p>
     * Selects the tuples in the table, optionally seeded with the given tuple, and then returns the single value from
     * each tuple which is not bound by the seed mask.
     * 
     * <p> Consider using the more idiomatic {@link #streamValues(TupleMask, ITuple)} instead.
     * 
     * @param seedMask
     *            a mask that extracts those parameters of the input key (from the entire parameter list) that should be
     *            bound to a fixed value; must not be null. <strong>Note</strong>: any given index must occur at most
     *            once in seedMask, and seedMask must include all parameters in any arbitrary order except one.
     * @param seed
     *            the tuple of fixed values restricting the row set to be considered, in the same order as given in
     *            parameterSeedMask, so that for each considered row tuple,
     *            projectedParameterSeed.equals(parameterSeedMask.transform(row)) should hold. Must not be null.
     * @return the objects in the table for the given key and seed
     * 
     */
    @SuppressWarnings("unchecked")
    public default Iterable<? extends Object> enumerateValues(TupleMask seedMask, ITuple seed) {
        return () -> (Iterator<Object>) (streamValues(seedMask, seed).iterator());
    }

    /**
     * Simpler form of {@link #enumerateTuples(TupleMask, ITuple)} in the case where all values of the tuples are bound
     * by the seed except for one.
     * 
     * <p>
     * Selects the tuples in the table, optionally seeded with the given tuple, and then returns the single value from
     * each tuple which is not bound by the seed mask.
     * 
     * @param seedMask
     *            a mask that extracts those parameters of the input key (from the entire parameter list) that should be
     *            bound to a fixed value; must not be null. <strong>Note</strong>: any given index must occur at most
     *            once in seedMask, and seedMask must include all parameters in any arbitrary order except one.
     * @param seed
     *            the tuple of fixed values restricting the row set to be considered, in the same order as given in
     *            parameterSeedMask, so that for each considered row tuple,
     *            projectedParameterSeed.equals(parameterSeedMask.transform(row)) should hold. Must not be null.
     * @return the objects in the table for the given key and seed
     * 
     * @since 2.1
     */
    public Stream<? extends Object> streamValues(TupleMask seedMask, ITuple seed);

    /**
     * Simpler form of {@link #enumerateTuples(TupleMask, ITuple)} in the case where all values of the tuples are bound
     * by the seed.
     * 
     * <p>
     * Returns whether the given tuple is in the table identified by the input key.
     * 
     * @param seed
     *            a row tuple of fixed values whose presence in the table is queried
     * @return true iff there is a row tuple contained in the table that corresponds to the given seed
     */
    public boolean containsTuple(ITuple seed);

    /**
     * Returns the number of tuples, optionally seeded with the given tuple.
     * 
     * <p>
     * Selects the tuples in the table, optionally seeded with the given tuple, and then returns their number.
     * 
     * @param seedMask
     *            a mask that extracts those parameters of the input key (from the entire parameter list) that should be
     *            bound to a fixed value; must not be null. <strong>Note</strong>: any given index must occur at most
     *            once in seedMask.
     * @param seed
     *            the tuple of fixed values restricting the row set to be considered, in the same order as given in
     *            parameterSeedMask, so that for each considered row tuple,
     *            projectedParameterSeed.equals(parameterSeedMask.transform(row)) should hold. Must not be null.
     * @return the number of tuples in the table for the given key and seed
     * 
     */
    public int countTuples(TupleMask seedMask, ITuple seed);

    /**
     * Gives an estimate of the number of different groups the tuples of the table are projected into by the given mask
     * (e.g. for an identity mask, this means the full relation size). The estimate must meet the required accuracy.
     * 
     * <p> Derived tables may return {@link Optional#empty()} if it would be costly to provide an answer up to the required precision.
     * Direct storage tables are expected to always be able to give an exact count.  
     *  
     * <p> PRE: {@link TupleMask#isNonrepeating()} must hold for the group mask.
     * 
     * @since 2.1
     */
    public Optional<Long> estimateProjectionSize(TupleMask groupMask, Accuracy requiredAccuracy);
    
    /**
     * Subscribes for updates in the table, optionally seeded with the given tuple.
     * <p> This should be called after initializing a result cache by an enumeration method.
     *
     * @param seed can be null or a tuple with matching arity;
     *   if non-null, notifications will delivered only about those updates of the table
     *   that match the seed at positions where the seed is non-null.
     * @param listener will be notified of future changes
     * 
     * @since 2.1
     */
     public void addUpdateListener(Tuple seed, IQueryRuntimeContextListener listener);
    
     /**
     * Unsubscribes from updates in the table, optionally seeded with the given tuple.
     *
     * @param seed can be null or a tuple with matching arity;
     *   see {@link #addUpdateListener(Tuple, IQueryRuntimeContextListener)} for definition.
     * @param listener will no longer be notified of future changes
     * 
     * @since 2.1
     */
     public void removeUpdateListener(Tuple seed, IQueryRuntimeContextListener listener);

}
