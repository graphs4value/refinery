/*******************************************************************************
 * Copyright (c) 2010-2014, Zoltan Ujhelyi, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.psystem.queries;

import java.util.Objects;

import tools.refinery.interpreter.matchers.context.IInputKey;

/**
 * A descriptor for declared PQuery parameters. A parameter has a name, a declared type and a direction constraint
 *
 * @author Zoltan Ujhelyi
 *
 */
public class PParameter {

    private final String name;
    private final String typeName;
    private final IInputKey declaredUnaryType;
    private final PParameterDirection direction;

    public PParameter(String name) {
        this(name, (String) null);
    }

    public PParameter(String name, String typeName) {
        this(name, typeName, (IInputKey) null);
    }

    public PParameter(String name, String typeName, IInputKey declaredUnaryType) {
        this(name, typeName, declaredUnaryType, PParameterDirection.INOUT);
    }

    /**
     * @since 1.4
     */
    public PParameter(String name, String typeName, IInputKey declaredUnaryType, PParameterDirection direction) {
        super();
        this.name = name;
        this.typeName = typeName;
        this.declaredUnaryType = declaredUnaryType;
        this.direction = direction;

        if (declaredUnaryType != null && declaredUnaryType.getArity() != 1) {
            throw new IllegalArgumentException(
                    "PParameter declared type must be unary instead of " + declaredUnaryType.getPrettyPrintableName());
        }
    }

    /**
     * @return the direction
     * @since 1.4
     */
    public PParameterDirection getDirection() {
        return direction;
    }

    /**
     * @return the name of the parameter
     */
    public String getName() {
        return name;
    }

    /**
     * Returns a textual representation of the declared type of the parameter
     *
     * @return the type description, or null if not available
     */
    public String getTypeName() {
        return typeName;
    }

    /**
     * Yield an {@link IInputKey} representation of the type declared for this parameter.
     *
     * @return the unary type that was declared on this parameter in the query header, or null if not available
     */
    public IInputKey getDeclaredUnaryType() {
        return declaredUnaryType;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PParameter) {
            return Objects.equals(name, ((PParameter) obj).name)
                    && Objects.equals(typeName, ((PParameter) obj).typeName)
                    && Objects.equals(declaredUnaryType, ((PParameter) obj).declaredUnaryType)
                    && Objects.equals(direction, ((PParameter) obj).direction);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, typeName, declaredUnaryType);
    }

}
