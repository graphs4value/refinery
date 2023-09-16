/*******************************************************************************
 * Copyright (c) 2010-2014, Bergmann Gabor, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.rete.util;

import java.util.Comparator;
import java.util.Iterator;

/**
 * A comparator that compares two iterables based on the lexicographic sorting induced by a comparator on elements.
 * @author Bergmann Gabor
 *
 */
public class LexicographicComparator<T> implements Comparator<Iterable<? extends T>> {

    final Comparator<T> elementComparator;

    public LexicographicComparator(Comparator<T> elementComparator) {
        super();
        this.elementComparator = elementComparator;
    }

    @Override
    public int compare(Iterable<? extends T> o1, Iterable<? extends T> o2) {
        Iterator<? extends T> it1 = o1.iterator();
        Iterator<? extends T> it2 = o2.iterator();

        boolean has1, has2, bothHaveNext;
        do {
            has1 = it1.hasNext();
            has2 = it2.hasNext();
            bothHaveNext = has1 && has2;
            if (bothHaveNext) {
                T element1 = it1.next();
                T element2 = it2.next();
                int elementComparison = elementComparator.compare(element1, element2);
                if (elementComparison != 0)
                    return elementComparison;
            }
        } while (bothHaveNext);
        if (has1 && !has2) {
            return +1;
        } else if (!has1 && has2) {
            return -1;
        } else /*if (!has1 && !has2)*/ {
            return 0;
        }
    }




}
