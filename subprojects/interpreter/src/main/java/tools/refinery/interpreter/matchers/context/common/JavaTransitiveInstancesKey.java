/*******************************************************************************
 * Copyright (c) 2010-2015, Bergmann Gabor, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.matchers.context.common;



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
     * Preferred constructor.
     */
    public JavaTransitiveInstancesKey(Class<?> instanceClass) {
        this(getName(instanceClass));
        this.cachedOriginalInstanceClass = instanceClass;
    }

    /**
     * Call this constructor only in contexts where the class itself is not available for loading, e.g. it has not yet been compiled.
     */
    public JavaTransitiveInstancesKey(String className) {
        super(className);
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
        getWrapperInstanceClass();
		if (cachedWrapperInstanceClass == null) {
			return wrappedKey == null ? "<null>" : wrappedKey;
		}
		return cachedWrapperInstanceClass.getName();
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

    private static Class<?> primitiveTypeToWrapperClass(Class<?> instanceClass) {
        if (instanceClass != null && instanceClass.isPrimitive()) {
            if (Void.TYPE.equals(instanceClass))
                return Void.class;
            if (Boolean.TYPE.equals(instanceClass))
                return Boolean.class;
            if (Character.TYPE.equals(instanceClass))
                return Character.class;
            if (Byte.TYPE.equals(instanceClass))
                return Byte.class;
            if (Short.TYPE.equals(instanceClass))
                return Short.class;
            if (Integer.TYPE.equals(instanceClass))
                return Integer.class;
            if (Long.TYPE.equals(instanceClass))
                return Long.class;
            if (Float.TYPE.equals(instanceClass))
                return Float.class;
            if (Double.TYPE.equals(instanceClass))
                return Double.class;
        }
        return instanceClass;
    }

	private static String getName(Class<?> instanceClass) {
		Class<?> wrapperClass = primitiveTypeToWrapperClass(instanceClass);
		if (wrapperClass == null) {
			return "<null>";
		}
		return wrapperClass.getName();
	}
}
