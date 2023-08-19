/*******************************************************************************
 * Copyright (c) 2004-2011 Abel Hegedus and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.viatra.runtime.base.comprehension;

import java.util.Collection;
import java.util.Collections;
import java.util.WeakHashMap;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;
import tools.refinery.viatra.runtime.base.ViatraBasePlugin;

/**
 * @author Abel Hegedus
 * 
 */
public class WellbehavingDerivedFeatureRegistry {

    
    private static Collection<EStructuralFeature> contributedWellbehavingDerivedFeatures = Collections.newSetFromMap(new WeakHashMap<EStructuralFeature, Boolean>());
    private static Collection<EClass> contributedWellbehavingDerivedClasses = Collections.newSetFromMap(new WeakHashMap<EClass, Boolean>());
    private static Collection<EPackage> contributedWellbehavingDerivedPackages = Collections.newSetFromMap(new WeakHashMap<EPackage, Boolean>());

    private WellbehavingDerivedFeatureRegistry() {
    }

    /**
     * Called by ViatraBasePlugin.
     */
    public static void initRegistry() {
        getContributedWellbehavingDerivedFeatures().clear();
        getContributedWellbehavingDerivedClasses().clear();
        getContributedWellbehavingDerivedPackages().clear();

        IExtensionRegistry reg = Platform.getExtensionRegistry();
        IExtensionPoint poi;

        poi = reg.getExtensionPoint(ViatraBasePlugin.WELLBEHAVING_DERIVED_FEATURE_EXTENSION_POINT_ID);
        if (poi != null) {
            IExtension[] exts = poi.getExtensions();

            for (IExtension ext : exts) {

                IConfigurationElement[] els = ext.getConfigurationElements();
                for (IConfigurationElement el : els) {
                    if (el.getName().equals("wellbehaving-derived-feature")) {
                        processWellbehavingExtension(el);
                    } else {
                        throw new UnsupportedOperationException("Unknown configuration element " + el.getName()
                                + " in plugin.xml of " + el.getDeclaringExtension().getUniqueIdentifier());
                    }
                }
            }
        }
    }

    private static void processWellbehavingExtension(IConfigurationElement el) {
        try {
            String packageUri = el.getAttribute("package-nsUri");
            String featureName = el.getAttribute("feature-name");
            String classifierName = el.getAttribute("classifier-name");
            String contributorName = el.getContributor().getName();
            StringBuilder featureIdBuilder = new StringBuilder();
            if (packageUri != null) {
                EPackage pckg = EPackage.Registry.INSTANCE.getEPackage(packageUri);
                featureIdBuilder.append(packageUri);
                if (pckg != null) {
                    if (classifierName != null) {
                        EClassifier clsr = pckg.getEClassifier(classifierName);
                        featureIdBuilder.append("##").append(classifierName);
                        if (clsr instanceof EClass) {
                            if (featureName != null) {
                                EClass cls = (EClass) clsr;
                                EStructuralFeature feature = cls.getEStructuralFeature(featureName);
                                featureIdBuilder.append("##").append(featureName);
                                if (feature != null) {
                                    registerWellbehavingDerivedFeature(feature);
                                } else {
                                    throw new IllegalStateException(String.format("Feature %s of EClass %s in package %s not found! (plug-in %s)", featureName, classifierName, packageUri, contributorName));
                                }
                            } else {
                                registerWellbehavingDerivedClass((EClass) clsr);
                            }
                        } else {
                            throw new IllegalStateException(String.format("EClassifier %s does not exist in package %s! (plug-in %s)", classifierName, packageUri, contributorName));
                        }
                    } else {
                        if(featureName != null){
                            throw new IllegalStateException(String.format("Feature name must be empty if classifier name is not set! (package %s, plug-in %s)", packageUri, contributorName));
                        }
                        registerWellbehavingDerivedPackage(pckg);
                    }
                }
            }
        } catch (Exception e) {
            final Logger logger = Logger.getLogger(WellbehavingDerivedFeatureRegistry.class);
            logger.error("Well-behaving feature registration failed", e);
        }
    }

    /**
     * 
     * @param feature
     * @return true if the feature (or its defining EClass or ) is registered as well-behaving
     */
    public static boolean isWellbehavingFeature(EStructuralFeature feature) {
        if(feature == null){
            return false;
        } else if (contributedWellbehavingDerivedFeatures.contains(feature)) {
            return true;
        } else if (contributedWellbehavingDerivedClasses.contains(feature.getEContainingClass())) {
            return true;
        } else return contributedWellbehavingDerivedPackages.contains(feature.getEContainingClass().getEPackage());
    }

    public static void registerWellbehavingDerivedFeature(EStructuralFeature feature) {
        contributedWellbehavingDerivedFeatures.add(feature);
    }

    public static void registerWellbehavingDerivedClass(EClass cls) {
        contributedWellbehavingDerivedClasses.add(cls);
    }

    public static void registerWellbehavingDerivedPackage(EPackage pkg) {
        contributedWellbehavingDerivedPackages.add(pkg);
    }

    public static Collection<EStructuralFeature> getContributedWellbehavingDerivedFeatures() {
        return contributedWellbehavingDerivedFeatures;
    }

    public static Collection<EClass> getContributedWellbehavingDerivedClasses() {
        return contributedWellbehavingDerivedClasses;
    }

    public static Collection<EPackage> getContributedWellbehavingDerivedPackages() {
        return contributedWellbehavingDerivedPackages;
    }

}
