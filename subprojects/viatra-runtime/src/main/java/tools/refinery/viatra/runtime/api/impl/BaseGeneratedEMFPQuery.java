/*******************************************************************************
 * Copyright (c) 2010-2015, Bergmann Gabor, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.viatra.runtime.api.impl;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;
import tools.refinery.viatra.runtime.exception.ViatraQueryException;
import tools.refinery.viatra.runtime.matchers.psystem.queries.BasePQuery;
import tools.refinery.viatra.runtime.matchers.psystem.queries.PVisibility;
import tools.refinery.viatra.runtime.matchers.psystem.queries.QueryInitializationException;

/**
 * Common superclass for EMF-based generated PQueries.
 * @author Bergmann Gabor
 *
 */
public abstract class BaseGeneratedEMFPQuery extends BasePQuery {

    public BaseGeneratedEMFPQuery() {
        this(PVisibility.PUBLIC);
    }
    
    /**
     * @since 2.0
     */
    public BaseGeneratedEMFPQuery(PVisibility visibility) {
        super(visibility);
    }
    
    protected QueryInitializationException processDependencyException(ViatraQueryException ex) {
        if (ex.getCause() instanceof QueryInitializationException) 
            return (QueryInitializationException) ex.getCause();
        return new QueryInitializationException(
            "Failed to initialize external dependencies of query specification - see 'caused by' for details.", 
            null, "Problem with query dependencies.", this, ex);
    }

    protected EClassifier getClassifierLiteral(String packageUri, String classifierName) {
        EPackage ePackage = EPackage.Registry.INSTANCE.getEPackage(packageUri);
        if (ePackage == null) 
            throw new QueryInitializationException(
                    "Query refers to EPackage {1} not found in EPackage Registry.", 
                    new String[]{packageUri}, 
                    "Query refers to missing EPackage.", this);
        EClassifier literal = ePackage.getEClassifier(classifierName);
        if (literal == null) 
            throw new QueryInitializationException(
                    "Query refers to classifier {1} not found in EPackage {2}.", 
                    new String[]{classifierName, packageUri}, 
                    "Query refers to missing type in EPackage.", this);
        return literal;
    }
    
    /**
     * For parameter type retrieval only.
     * 
     * <p>If parameter type declaration is erroneous, we still get a working parameter list (without the type declaration); 
     *  the exception will be thrown again later when the body is processed.
     */
    protected EClassifier getClassifierLiteralSafe(String packageURI, String classifierName) {
        try {
            return getClassifierLiteral(packageURI, classifierName);
        } catch (QueryInitializationException e) {
            return null;
        }
    }

    protected EStructuralFeature getFeatureLiteral(String packageUri, String className, String featureName) {
        EClassifier container = getClassifierLiteral(packageUri, className);
        if (! (container instanceof EClass)) 
            throw new QueryInitializationException(
                    "Query refers to EClass {1} in EPackage {2} which turned out not be an EClass.", 
                    new String[]{className, packageUri}, 
                    "Query refers to missing EClass.", this);
        EStructuralFeature feature = ((EClass)container).getEStructuralFeature(featureName);
        if (feature == null) 
            throw new QueryInitializationException(
                    "Query refers to feature {1} not found in EClass {2}.", 
                    new String[]{featureName, className}, 
                    "Query refers to missing feature.", this);
        return feature;
    }

    protected EEnumLiteral getEnumLiteral(String packageUri, String enumName, String literalName) {
        EClassifier enumContainer = getClassifierLiteral(packageUri, enumName);
        if (! (enumContainer instanceof EEnum)) 
            throw new QueryInitializationException(
                    "Query refers to EEnum {1} in EPackage {2} which turned out not be an EEnum.", 
                    new String[]{enumName, packageUri}, 
                    "Query refers to missing enumeration type.", this);
        EEnumLiteral literal = ((EEnum)enumContainer).getEEnumLiteral(literalName);
        if (literal == null) 
            throw new QueryInitializationException(
                    "Query refers to enumeration literal {1} not found in EEnum {2}.", 
                    new String[]{literalName, enumName}, 
                    "Query refers to missing enumeration literal.", this);
        return literal;
    }

}
