/*******************************************************************************
 * Copyright (c) 2010-2018, Gabor Bergmann, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.planning.helpers;

import java.util.Optional;
import java.util.function.BiFunction;

import tools.refinery.interpreter.matchers.tuple.TupleMask;
import tools.refinery.interpreter.matchers.util.Accuracy;

/**
 * Helpers dealing with optionally present statistics information
 *
 * @author Gabor Bergmann
 * @since 2.1
 *
 */
public class StatisticsHelper {

    private StatisticsHelper() {
        // Hidden utility class constructor
    }

    public static Optional<Double> estimateAverageBucketSize(TupleMask groupMask, Accuracy requiredAccuracy,
            BiFunction<TupleMask, Accuracy, Optional<Long>> estimateCardinality)
    {
        if (groupMask.isIdentity()) return Optional.of(1.0);

        Accuracy numeratorAccuracy = requiredAccuracy;
        Accuracy denominatorAccuracy = requiredAccuracy.reciprocal();
        TupleMask identityMask = TupleMask.identity(groupMask.sourceWidth);

        Optional<Long> totalCountEstimate  = estimateCardinality.apply(identityMask, numeratorAccuracy);
        Optional<Long> bucketCountEstimate = estimateCardinality.apply(groupMask,    denominatorAccuracy);

        return totalCountEstimate.flatMap(matchCount ->
            bucketCountEstimate.map(bucketCount ->
                bucketCount == 0L ? 0L : ((double) matchCount) / bucketCount
        ));
    }

    public static Optional<Double> min(Optional<Double> a,  Optional<Double> b) {
        if (b.isPresent()) {
            if (a.isPresent()) {
                return Optional.of(Math.min(a.get(), b.get()));
            } else return b;
        } else return a;
    }
    public static Optional<Double> min(Optional<Double> a, double b) {
        if (a.isPresent()) {
            return Optional.of(Math.min(a.get(), b));
        } else return Optional.of(b);
    }


}
