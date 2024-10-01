/*******************************************************************************
 * Copyright (c) 2010-2015, Bergmann Gabor, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.context.common;


import java.util.Map;
import java.util.Objects;

/**
 * Instance tuples are of form (x), where object x is an instance of the given Java class or its subclasses.
 * <p> Fine print 1: classes with the same name are considered equivalent.
 * Can be instantiated with class name, even if the class itself is not loaded yet; but if the class is available, passing it in the constructor is beneficial to avoid classloading troubles.
 * <p> Fine print 2: primitive types (char, etc.) are transparently treated as their wrapper class (Character, etc.).
 * <p> Non-enumerable type, can only be checked.
 * <p> Stateless type (objects can't change their type)
 * @author Bergmann Gabor
 *
*/
public class JavaTransitiveInstancesKey extends BaseInputKeyWrapper<String> {

    /**
     * The actual Class whose (transitive) instances this relation contains. Can be null at compile time, if only the name is available.
     * Can be a primitive.
     */
    private Class<?> cachedOriginalInstanceClass;

    /**
     * Same as {@link #cachedOriginalInstanceClass}, but primitive classes are replaced with their wrapper classes (e.g. int --> java.lang.Integer).
     */
    private Class<?> cachedWrapperInstanceClass;

	/**
	 * Use this name to refer to the String in Java code
	 */
	private final String nameInJavaCode;

	/**
	 * Call this constructor only in contexts where the class itself is not available for loading, e.g. it has not yet been compiled.
	 * @since 2.9
	 * @param jvmClassName {@link Class#getName()}
	 * @param javaClassName {@link Class#getCanonicalName()}
	 */
	public JavaTransitiveInstancesKey(String jvmClassName, String javaClassName) {
		super(jvmClassName);
		this.nameInJavaCode = javaClassName;
	}

	/**
	 * Convenience constructor for the case where the JVM Class is available
	 *  (already precompiled and loaded)
	 *  in the context where this input key is built.
	 *  @param instanceClass a non-null class definition
	 */
	public JavaTransitiveInstancesKey(Class<?> instanceClass) {
		this(
				primitiveTypeToWrapperClass(Objects.requireNonNull(instanceClass)).getName(),
				primitiveTypeToWrapperClass(Objects.requireNonNull(instanceClass)).getCanonicalName()
		);
		this.cachedOriginalInstanceClass = instanceClass;
	}

	/**
	 * Call this constructor only as a last resort, if the Java canonical name of the class is unavailable.
	 * @deprecated Use {@link #JavaTransitiveInstancesKey(Class)} or {@link #JavaTransitiveInstancesKey(String, String)} instead
	 */
	@Deprecated(since = "2.9")
	public JavaTransitiveInstancesKey(String jvmClassName) {
		this(jvmClassName, jvmClassName.replace('$', '.'));
	}

    /**
     * Returns null if class cannot be loaded.
     */
    private Class<?> getOriginalInstanceClass() {
        if (cachedOriginalInstanceClass == null) {
            try {
                resolveClassInternal();
            } catch (ClassNotFoundException e) {
                // class not yet available at this point
            }
        }
        return cachedOriginalInstanceClass;
    }

    /**
     * @return non-null instance class
     * @throws ClassNotFoundException
     */
    private Class<?> forceGetOriginalInstanceClass() throws ClassNotFoundException {
        if (cachedOriginalInstanceClass == null) {
            resolveClassInternal();
        }
        return cachedOriginalInstanceClass;
    }

    /**
     * @return non-null instance class, wrapped if primitive class
     * @throws ClassNotFoundException
     */
    public Class<?> forceGetWrapperInstanceClass() throws ClassNotFoundException {
        forceGetOriginalInstanceClass();
        return getWrapperInstanceClass();
    }
    /**
     * @return non-null instance class, wrapped if primitive class
     * @throws ClassNotFoundException
     */
    public Class<?> forceGetInstanceClass() throws ClassNotFoundException {
        return forceGetWrapperInstanceClass();
    }

    /**
     * @return instance class, wrapped if primitive class, null if class cannot be loaded
     */
    public Class<?> getWrapperInstanceClass()  {
        if (cachedWrapperInstanceClass == null) {
            cachedWrapperInstanceClass = primitiveTypeToWrapperClass(getOriginalInstanceClass());
        }
        return cachedWrapperInstanceClass;
    }
    /**
     * @return instance class, wrapped if primitive class, null if class cannot be loaded
     */
    public Class<?> getInstanceClass()  {
        return getWrapperInstanceClass();
    }

    private void resolveClassInternal() throws ClassNotFoundException {
        cachedOriginalInstanceClass = Class.forName(wrappedKey);
    }

    @Override
    public String getPrettyPrintableName() {
        return this.nameInJavaCode;
	}

    @Override
    public String getStringID() {
        return "javaClass#"+ getPrettyPrintableName();
    }

    @Override
    public int getArity() {
        return 1;
    }

    @Override
    public boolean isEnumerable() {
        return false;
    }

    @Override
    public String toString() {
        return this.getPrettyPrintableName();
    }

	// @formatting:off
	private static final Map<Class<?>, Class<?>> PRIMITIVE_TYPE_MAP = Map.of(
			Void.TYPE, Void.class,
			Boolean.TYPE, Boolean.class,
			Character.TYPE, Character.class,
			Byte.TYPE, Byte.class,
			Short.TYPE, Short.class,
			Integer.TYPE, Integer.class,
			Long.TYPE, Long.class,
			Float.TYPE, Float.class,
			Double.TYPE, Double.class
	);
	// @formatting:on

    private static Class<?> primitiveTypeToWrapperClass(Class<?> instanceClass) {
        return PRIMITIVE_TYPE_MAP.getOrDefault(instanceClass, instanceClass);
    }
}
