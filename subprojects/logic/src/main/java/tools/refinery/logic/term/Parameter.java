/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term;

import java.util.Objects;
import java.util.Optional;

public class Parameter {
	public static final Parameter NODE_OUT = new Parameter(null);

	private final Class<?> dataType;
	//IN or OUT
	private final ParameterDirection direction;

	//A default direction az out
	public Parameter(Class<?> dataType) {
		this(dataType, ParameterDirection.OUT);
	}

	public Parameter(Class<?> dataType, ParameterDirection direction) {
		this.dataType = dataType;
		this.direction = direction;
	}

	//Ha a dataType null akkor node variable egyébkét data variable
	public boolean isNodeVariable() {
		return dataType == null;
	}

	//Vagy ez vagy az
	public boolean isDataVariable() {
		return !isNodeVariable();
	}

	//Típusra getter de ugye lehet hogy nulla
	public Optional<Class<?>> tryGetType() {
		return Optional.ofNullable(dataType);
	}

	public ParameterDirection getDirection() {
		return direction;
	}

	//Ez a metódus a két paramétert hasonlítja össze
	public boolean matches(Parameter other) {
		return Objects.equals(dataType, other.dataType) && direction == other.direction;
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
		return matches(parameter);
	}

	@Override
	public int hashCode() {
		return Objects.hash(dataType, direction);
	}
}
