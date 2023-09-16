/*******************************************************************************
 * Copyright (c) 2010-2015, Bergmann Gabor, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Data object representing the implication of an input key, in use cases including edge supertypes, edge opposites, node type constraints, etc.
 *
 * <p> Each instance tuple of the <i>implying input key</i> (if given) implies the presence of an instance tuple of the <i>implied input key</i> consisting of elements of the original tuple at given positions.
 * When the input key is null, it is not an input constraint but some other source that implies input keys.
 *
 * <p> The implication is an immutable data object.
 *
 * @author Bergmann Gabor
 *
 */
public final class InputKeyImplication {
    private IInputKey implyingKey;
    private IInputKey impliedKey;
    private List<Integer> impliedIndices;

    /**
     * Optional. Instance tuples of this input key imply an instance tuple of another key.
     * Sometimes it is not an input key that implies other input keys, so this attribute can be null.
     */
    public IInputKey getImplyingKey() {
        return implyingKey;
    }
    /**
     * An instance tuple of this input key is implied by another key.
     */
    public IInputKey getImpliedKey() {
        return impliedKey;
    }
    /**
     * The implied instance tuple consists of the values in the implying tuple at these indices.
     */
    public List<Integer> getImpliedIndices() {
        return impliedIndices;
    }
    /**
     * @param implyingKey instance tuples of this input key imply an instance tuple of the other key.
     * @param impliedKey instance tuple of this input key is implied by the other key.
     * @param implyingIndices the implied instance tuple consists of the values in the implying tuple at these indices.
     */
    public InputKeyImplication(IInputKey implyingKey, IInputKey impliedKey,
            List<Integer> implyingIndices) {
        super();
        this.implyingKey = implyingKey;
        this.impliedKey = impliedKey;
        this.impliedIndices = Collections.unmodifiableList(new ArrayList<>(implyingIndices));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((impliedIndices == null) ? 0 : impliedIndices.hashCode());
        result = prime * result
                + ((impliedKey == null) ? 0 : impliedKey.hashCode());
        result = prime * result
                + ((implyingKey == null) ? 0 : implyingKey.hashCode());
        return result;
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof InputKeyImplication))
            return false;
        InputKeyImplication other = (InputKeyImplication) obj;
        if (impliedIndices == null) {
            if (other.impliedIndices != null)
                return false;
        } else if (!impliedIndices.equals(other.impliedIndices))
            return false;
        if (impliedKey == null) {
            if (other.impliedKey != null)
                return false;
        } else if (!impliedKey.equals(other.impliedKey))
            return false;
        if (implyingKey == null) {
            if (other.implyingKey != null)
                return false;
        } else if (!implyingKey.equals(other.implyingKey))
            return false;
        return true;
    }


}
