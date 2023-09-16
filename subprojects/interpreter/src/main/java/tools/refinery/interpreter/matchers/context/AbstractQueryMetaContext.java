/*******************************************************************************
 * Copyright (c) 2010-2016, Grill Balázs, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.context;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Common abstract class for implementers of {@link IQueryMetaContext}
 *
 * @author Grill Balázs
 * @since 1.3
 *
 */
public abstract class AbstractQueryMetaContext implements IQueryMetaContext {

    /**
     * @since 2.0
     */
    @Override
    public Map<InputKeyImplication, Set<InputKeyImplication>> getConditionalImplications(IInputKey implyingKey) {
        return new HashMap<>();
    }

    /**
     * @since 1.6
     */
    @Override
    public boolean canLeadOutOfScope(IInputKey key) {
        return key.getArity() > 1;
    }

    /**
     * @since 1.6
     */
    @Override
    public Comparator<IInputKey> getSuggestedEliminationOrdering() {
        return (o1, o2) -> 0;
    }

    /**
     * @since 1.6
     */
    @Override
    public Collection<InputKeyImplication> getWeakenedAlternatives(IInputKey implyingKey) {
        return Collections.emptySet();
    }

    @Override
    public boolean isPosetKey(IInputKey key) {
        return false;
    }

    @Override
    public IPosetComparator getPosetComparator(Iterable<IInputKey> key) {
        return null;
    }

}
