/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.term;

import java.util.Objects;
import java.util.Optional;

public class Parameter {
	public static final Parameter NODE_IN_OUT = new Parameter(null, ParameterDirection.IN_OUT);

	private final Class<?> dataType;
	private final ParameterDirection direction;

	public Parameter(Class<?> dataType, ParameterDirection direction) {
		this.dataType = dataType;
		this.direction = direction;
		if (isDataVariable()) {
			if (direction == ParameterDirection.IN_OUT) {
				throw new IllegalArgumentException("IN_OUT direction is not supported for data parameters");
			}
		} else if (direction == ParameterDirection.OUT) {
			throw new IllegalArgumentException("OUT direction is not supported for node parameters");
		}
	}

	public boolean isNodeVariable() {
		return dataType == null;
	}

	public boolean isDataVariable() {
		return !isNodeVariable();
	}

	public Optional<Class<?>> tryGetType() {
		return Optional.ofNullable(dataType);
	}

	public ParameterDirection getDirection() {
		return direction;
	}

	public boolean isAssignable(Variable variable) {
		if (variable instanceof AnyDataVariable dataVariable) {
			return dataVariable.getType().equals(dataType);
		} else if (variable instanceof NodeVariable) {
			return !isDataVariable();
		} else {
			throw new IllegalArgumentException("Unknown variable " + variable);
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Parameter parameter = (Parameter) o;
		return Objects.equals(dataType, parameter.dataType) && direction == parameter.direction;
	}

	@Override
	public int hashCode() {
		return Objects.hash(dataType, direction);
	}
}
