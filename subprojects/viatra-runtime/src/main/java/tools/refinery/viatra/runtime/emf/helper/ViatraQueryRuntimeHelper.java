/*******************************************************************************
 * Copyright (c) 2010-2014, Abel Hegedus, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.viatra.runtime.emf.helper;

import java.util.function.Function;

import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;
import tools.refinery.viatra.runtime.api.IPatternMatch;

/**
 * Helper functions for dealing with the EMF objects with VIATRA Queries.
 * 
 * @author Abel Hegedus
 * @since 0.9
 *
 */
public class ViatraQueryRuntimeHelper {
    
    private ViatraQueryRuntimeHelper() {/*Utility class constructor*/}
    
    private static final Function<Object, String> STRING_VALUE_TRANSFORMER = input -> (input == null) ? "(null)" : input.toString();

    /**
     * Gives a human-readable name of an EMF type. 
     */
    public static String prettyPrintEMFType(Object typeObject) {
        if (typeObject == null) {
            return "(null)";
        } else if (typeObject instanceof EClassifier) {
            final EClassifier eClassifier = (EClassifier) typeObject;
            final EPackage ePackage = eClassifier.getEPackage();
            final String nsURI = ePackage == null ? null : ePackage.getNsURI();
            final String typeName = eClassifier.getName();
            return "" + nsURI + "/" + typeName;
        } else if (typeObject instanceof EStructuralFeature) {
            final EStructuralFeature feature = (EStructuralFeature) typeObject;
            return prettyPrintEMFType(feature.getEContainingClass()) + "." + feature.getName();
        } else
            return typeObject.toString();
    }


    /**
     * Get the structural feature with the given name of the given object.
     *
     * @param o
     *            the object (must be an EObject)
     * @param featureName
     *            the name of the feature
     * @return the EStructuralFeature of the object or null if it can not be found
     */
    public static EStructuralFeature getFeature(Object o, String featureName) {
        if (o instanceof EObject) {
            EStructuralFeature feature = ((EObject) o).eClass().getEStructuralFeature(featureName);
            return feature;
        }
        return null;
    }

    /**
     * Returns the message for the given match using the given format. The format string can refer to the value of
     * parameter A of the match with $A$ and even access features of A (if it's an EObject), e.g. $A.id$.
     * 
     * <p/>
     * If the selected parameter does not exist, the string "[no such parameter]" is added
     * 
     * <p/>
     * If no feature is defined, but A has a feature called "name", then its value is used.
     * 
     * <p/>
     * If the selected feature does not exist, A.toString() is added.
     * 
     * <p/>
     * If the selected feature is null, the string "null" is added.
     * 
     * @param match
     *            cannot be null!
     * @param messageFormat
     *            cannot be null!
     */
    public static String getMessage(IPatternMatch match, String messageFormat) {
        return getMessage(match, messageFormat, STRING_VALUE_TRANSFORMER);
    }
    
    /**
     * Returns the message for the given match using the given format while transforming values with the given function.
     * The format string can refer to the value of parameter A of the match with $A$ and even access features of A (if
     * it's an EObject), e.g. $A.id$. The function will be called to compute the final string representation of the
     * values selected by the message format.
     * 
     * <p/>
     * If the selected parameter does not exist, the string "[no such parameter]" is added
     * 
     * <p/>
     * If no feature is defined, but A has a feature called "name", then its value is passed to the function.
     * 
     * <p/>
     * If the selected feature does not exist, A is passed to the function.
     * 
     * <p/>
     * If the selected feature is null, the string "null" is added.
     * 
     * @param match
     *            cannot be null!
     * @param messageFormat
     *            cannot be null!
     * @param parameterValueTransformer
     *            cannot be null!
     * @since 2.0
     */
    public static String getMessage(IPatternMatch match, String messageFormat, Function<Object,String> parameterValueTransformer) {
        String[] tokens = messageFormat.split("\\$");
        StringBuilder newText = new StringBuilder();
    
        for (int i = 0; i < tokens.length; i++) {
            if (i % 2 == 0) {
                newText.append(tokens[i]);
            } else {
                String[] objectTokens = tokens[i].split("\\.");
                if (objectTokens.length > 0) {
                    Object o = null;
                    EStructuralFeature feature = null;
    
                    if (objectTokens.length == 1) {
                        o = match.get(objectTokens[0]);
                        feature = getFeature(o, "name");
                    }
                    if (objectTokens.length == 2) {
                        o = match.get(objectTokens[0]);
                        feature = getFeature(o, objectTokens[1]);
                    }
    
                    if (o != null && feature != null) {
                        Object value = ((EObject) o).eGet(feature);
                        if (value != null) {
                            newText.append(parameterValueTransformer.apply(value));
                        } else {
                            newText.append("null");
                        }
                    } else if (o != null) {
                        newText.append(parameterValueTransformer.apply(o));
                    }
                } else {
                    newText.append("[no such parameter]");
                }
            }
        }
    
        return newText.toString();
    }

}
