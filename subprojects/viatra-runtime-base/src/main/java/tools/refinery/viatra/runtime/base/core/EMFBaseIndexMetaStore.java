/*******************************************************************************
 * Copyright (c) 2010-2016, Gabor Bergmann, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.viatra.runtime.base.core;

import org.eclipse.emf.common.util.Enumerator;
import org.eclipse.emf.ecore.*;
import tools.refinery.viatra.runtime.base.api.BaseIndexOptions;
import tools.refinery.viatra.runtime.base.api.IndexingLevel;
import tools.refinery.viatra.runtime.base.api.InstanceListener;
import tools.refinery.viatra.runtime.base.exception.ViatraBaseException;
import tools.refinery.viatra.runtime.matchers.util.CollectionsFactory;
import tools.refinery.viatra.runtime.matchers.util.CollectionsFactory.MemoryType;
import tools.refinery.viatra.runtime.matchers.util.IMemoryView;
import tools.refinery.viatra.runtime.matchers.util.IMultiLookup;
import tools.refinery.viatra.runtime.matchers.util.Preconditions;

import java.util.*;
import java.util.Map.Entry;

/**
 * Stores the indexed metamodel information.
 *
 * @author Gabor Bergmann
 * @noextend This class is not intended to be subclassed by clients.
 */
public class EMFBaseIndexMetaStore {

    private static final EClass EOBJECT_CLASS = EcorePackage.eINSTANCE.getEObject();
    private final boolean isDynamicModel;
    private NavigationHelperImpl navigationHelper;

    /**
     *
     */
    public EMFBaseIndexMetaStore(final NavigationHelperImpl navigationHelper) {
        this.navigationHelper = navigationHelper;
        final BaseIndexOptions options = navigationHelper.getBaseIndexOptions();
        this.isDynamicModel = options.isDynamicEMFMode();
    }

    /**
     * Supports collision detection and EEnum canonicalization. Used for all EPackages that have types whose instances
     * were encountered at least once.
     */
    private final Set<EPackage> knownPackages = new HashSet<EPackage>();

    /**
     * Field variable because it is needed for collision detection. Used for all EClasses whose instances were
     * encountered at least once.
     */
    private final Set<EClassifier> knownClassifiers = new HashSet<EClassifier>();
    /**
     * Field variable because it is needed for collision detection. Used for all EStructuralFeatures whose instances
     * were encountered at least once.
     */
    private final Set<EStructuralFeature> knownFeatures = new HashSet<EStructuralFeature>();

    /**
     * (EClass or String ID) -> all subtypes in knownClasses
     */
    private final Map<Object, Set<Object>> subTypeMap = new HashMap<Object, Set<Object>>();
    /**
     * (EClass or String ID) -> all supertypes in knownClasses
     */
    private final Map<Object, Set<Object>> superTypeMap = new HashMap<Object, Set<Object>>();

    /**
     * EPacakge NsURI -> EPackage instances; this is instance-level to detect collisions
     */
    private final IMultiLookup<String, EPackage> uniqueIDToPackage = CollectionsFactory.createMultiLookup(Object.class, MemoryType.SETS, Object.class);

    /**
     * static maps between metamodel elements and their unique IDs
     */
    private final Map<EClassifier, String> uniqueIDFromClassifier = new HashMap<EClassifier, String>();
    private final Map<ETypedElement, String> uniqueIDFromTypedElement = new HashMap<ETypedElement, String>();
    private final Map<Enumerator, String> uniqueIDFromEnumerator = new HashMap<Enumerator, String>();
    private final IMultiLookup<String, EClassifier> uniqueIDToClassifier = CollectionsFactory.createMultiLookup(Object.class, MemoryType.SETS, Object.class);
    private final IMultiLookup<String, ETypedElement> uniqueIDToTypedElement = CollectionsFactory.createMultiLookup(Object.class, MemoryType.SETS, Object.class);
    private final IMultiLookup<String, Enumerator> uniqueIDToEnumerator = CollectionsFactory.createMultiLookup(Object.class, MemoryType.SETS, Object.class);
    private final Map<String, Enumerator> uniqueIDToCanonicalEnumerator = new HashMap<String, Enumerator>();

    /**
     * Map from enum classes generated for {@link EEnum}s to the actual EEnum.
     */
    private Map<Class<?>, EEnum> generatedEENumClasses = new HashMap<Class<?>, EEnum>();

    /**
     * @return the eObjectClassKey
     */
    public Object getEObjectClassKey() {
        if (eObjectClassKey == null) {
            eObjectClassKey = toKey(EOBJECT_CLASS);
        }
        return eObjectClassKey;
    }
    private Object eObjectClassKey = null;

    protected Object toKey(final EClassifier classifier) {
        if (isDynamicModel) {
            return toKeyDynamicInternal(classifier);
        } else {
            maintainMetamodel(classifier);
            return classifier;
        }
    }

    protected String toKeyDynamicInternal(final EClassifier classifier) {
        String id = uniqueIDFromClassifier.get(classifier);
        if (id == null) {
            Preconditions.checkArgument(!classifier.eIsProxy(),
                    "Classifier %s is an unresolved proxy", classifier);
            id = classifier.getEPackage().getNsURI() + "##" + classifier.getName();
            uniqueIDFromClassifier.put(classifier, id);
            uniqueIDToClassifier.addPair(id, classifier);
            // metamodel maintenance will call back toKey(), but now the ID maps are already filled
            maintainMetamodel(classifier);
        }
        return id;
    }

    protected String enumToKeyDynamicInternal(Enumerator enumerator) {
        String id = uniqueIDFromEnumerator.get(enumerator);
        if (id == null) {
            if (enumerator instanceof EEnumLiteral) {
                EEnumLiteral enumLiteral = (EEnumLiteral) enumerator;
                final EEnum eEnum = enumLiteral.getEEnum();
                maintainMetamodel(eEnum);

                id = constructEnumID(eEnum.getEPackage().getNsURI(), eEnum.getName(), enumLiteral.getLiteral());

                // there might be a generated enum for this enum literal!
                // generated enum should pre-empt the ecore enum literal as canonical enumerator
                Enumerator instanceEnum = enumLiteral.getInstance();
                if (instanceEnum != null && !uniqueIDToCanonicalEnumerator.containsKey(id)) {
                    uniqueIDToCanonicalEnumerator.put(id, instanceEnum);
                }
                // if generated enum not found... delay selection of canonical enumerator
            } else { // generated enum
                final EEnum eEnum = generatedEENumClasses.get(enumerator.getClass());
                if (eEnum != null)
                    id = constructEnumID(eEnum.getEPackage().getNsURI(), eEnum.getName(), enumerator.getLiteral());
                else
                    id = constructEnumID("unkownPackage URI", enumerator.getClass().getSimpleName(),
                            enumerator.getLiteral());

                // generated enum should pre-empt the ecore enum literal as canonical enumerator
                if (!uniqueIDToCanonicalEnumerator.containsKey(id)) {
                    uniqueIDToCanonicalEnumerator.put(id, enumerator);
                }
            }
            uniqueIDFromEnumerator.put(enumerator, id);
            uniqueIDToEnumerator.addPair(id, enumerator);
        }
        return id;
    }

    protected String constructEnumID(String nsURI, String name, String literal) {
        return String.format("%s##%s##%s", nsURI, name, literal);
    }

    protected Object toKey(final EStructuralFeature feature) {
        if (isDynamicModel) {
            String id = uniqueIDFromTypedElement.get(feature);
            if (id == null) {
                Preconditions.checkArgument(!feature.eIsProxy(),
                        "Element %s is an unresolved proxy", feature);
                id = toKeyDynamicInternal((EClassifier) feature.eContainer()) + "##" + feature.getEType().getName()
                        + "##" + feature.getName();
                uniqueIDFromTypedElement.put(feature, id);
                uniqueIDToTypedElement.addPair(id, feature);
                // metamodel maintenance will call back toKey(), but now the ID maps are already filled
                maintainMetamodel(feature);
            }
            return id;
        } else {
            maintainMetamodel(feature);
            return feature;
        }
    }

    protected Enumerator enumToCanonicalDynamicInternal(final Enumerator value) {
        final String key = enumToKeyDynamicInternal(value);
        Enumerator canonicalEnumerator = uniqueIDToCanonicalEnumerator.computeIfAbsent(key,
                // if no canonical version appointed yet, appoint first version
                k -> uniqueIDToEnumerator.lookup(k).iterator().next());
        return canonicalEnumerator;
    }

    /**
     * If in dynamic EMF mode, substitutes enum literals with a canonical version of the enum literal.
     */
    protected Object toInternalValueRepresentation(final Object value) {
        if (isDynamicModel) {
            if (value instanceof Enumerator)
                return enumToCanonicalDynamicInternal((Enumerator) value);
            else
                return value;
        } else {
            return value;
        }
    }

    /**
     * Checks the {@link EStructuralFeature}'s source and target {@link EPackage} for NsURI collision. An error message
     * will be logged if a model element from an other {@link EPackage} instance with the same NsURI has been already
     * processed. The error message will be logged only for the first time for a given {@link EPackage} instance.
     *
     * @param classifier
     *            the classifier instance
     */
    protected void maintainMetamodel(final EStructuralFeature feature) {
        if (!knownFeatures.contains(feature)) {
            knownFeatures.add(feature);
            maintainMetamodel(feature.getEContainingClass());
            maintainMetamodel(feature.getEType());
        }
    }

    /**
     * put subtype information into cache
     */
    protected void maintainMetamodel(final EClassifier classifier) {
        if (!knownClassifiers.contains(classifier)) {
            checkEPackage(classifier);
            knownClassifiers.add(classifier);

            if (classifier instanceof EClass) {
                final EClass clazz = (EClass) classifier;
                final Object clazzKey = toKey(clazz);
                for (final EClass superType : clazz.getEAllSuperTypes()) {
                    maintainTypeHierarhyInternal(clazzKey, toKey(superType));
                }
                maintainTypeHierarhyInternal(clazzKey, getEObjectClassKey());
            } else if (classifier instanceof EEnum) {
                EEnum eEnum = (EEnum) classifier;

                if (isDynamicModel) {
                    // if there is a generated enum class, save this model element for describing that class
                    if (eEnum.getInstanceClass() != null)
                        generatedEENumClasses.put(eEnum.getInstanceClass(), eEnum);

                    for (EEnumLiteral eEnumLiteral : eEnum.getELiterals()) {
                        // create string ID; register generated enum values
                        enumToKeyDynamicInternal(eEnumLiteral);
                    }
                }
            }
        }
    }

    /**
     * Checks the {@link EClassifier}'s {@link EPackage} for NsURI collision. An error message will be logged if a model
     * element from an other {@link EPackage} instance with the same NsURI has been already processed. The error message
     * will be logged only for the first time for a given {@link EPackage} instance.
     *
     * @param classifier
     *            the classifier instance
     */
    protected void checkEPackage(final EClassifier classifier) {
        final EPackage ePackage = classifier.getEPackage();
        if (knownPackages.add(ePackage)) { // this is a new EPackage
            final String nsURI = ePackage.getNsURI();
            final IMemoryView<EPackage> packagesOfURI = uniqueIDToPackage.lookupOrEmpty(nsURI);
            if (!packagesOfURI.containsNonZero(ePackage)) { // this should be true
                uniqueIDToPackage.addPair(nsURI, ePackage);
                // collision detection between EPackages (disabled in dynamic model mode)
                if (!isDynamicModel && packagesOfURI.size() == 2) { // only report the issue if the new EPackage
                                                                    // instance is the second for the same URI
                    navigationHelper.processingError(
                            new ViatraBaseException("NsURI (" + nsURI
                                    + ") collision detected between different instances of EPackages. If this is normal, try using dynamic EMF mode."),
                            "process new metamodel elements.");
                }
            }
        }
    }

    /**
     * Maintains subtype hierarchy
     *
     * @param subClassKey
     *            EClass or String id of subclass
     * @param superClassKey
     *            EClass or String id of superclass
     */
    protected void maintainTypeHierarhyInternal(final Object subClassKey, final Object superClassKey) {
        // update observed class and instance listener tables according to new subtype information
        Map<Object, IndexingLevel> allObservedClasses = navigationHelper.getAllObservedClassesInternal();
        if (allObservedClasses.containsKey(superClassKey)) {
            // we know that there are no known subtypes of subClassKey at this point, so a single insert should suffice
            allObservedClasses.put(subClassKey, allObservedClasses.get(superClassKey));
        }
        final Map<Object, Map<InstanceListener, Set<EClass>>> instanceListeners = navigationHelper.peekInstanceListeners();
        if (instanceListeners != null) { // table already constructed
            for (final Entry<InstanceListener, Set<EClass>> entry : instanceListeners.getOrDefault(superClassKey, Collections.emptyMap()).entrySet()) {
                final InstanceListener listener = entry.getKey();
                for (final EClass subscriptionType : entry.getValue()) {
                    navigationHelper.addInstanceListenerInternal(listener, subscriptionType, subClassKey);
                }
            }
        }

        // update subtype maps
        Set<Object> subTypes = subTypeMap.computeIfAbsent(superClassKey, k -> new HashSet<>());
        subTypes.add(subClassKey);
        Set<Object> superTypes = superTypeMap.computeIfAbsent(subClassKey, k -> new HashSet<>());
        superTypes.add(superClassKey);
    }

    /**
     * @return the subTypeMap
     */
    protected Map<Object, Set<Object>> getSubTypeMap() {
        return subTypeMap;
    }

    protected Map<Object, Set<Object>> getSuperTypeMap() {
        return superTypeMap;
    }

    /**
     * Returns the corresponding {@link EStructuralFeature} instance for the id.
     *
     * @param featureId
     *            the id of the feature
     * @return the {@link EStructuralFeature} instance
     */
    public EStructuralFeature getKnownFeature(final String featureId) {
        final IMemoryView<ETypedElement> features = uniqueIDToTypedElement.lookup(featureId);
        if (features != null && !features.isEmpty()) {
            final ETypedElement next = features.iterator().next();
            if (next instanceof EStructuralFeature) {
                return (EStructuralFeature) next;
            }
        }
        return null;

    }

    public EStructuralFeature getKnownFeatureForKey(Object featureKey) {
        EStructuralFeature feature;
        if (isDynamicModel) {
            feature = getKnownFeature((String) featureKey);
        } else {
            feature = (EStructuralFeature) featureKey;
        }
        return feature;
    }

    /**
     * Returns the corresponding {@link EClassifier} instance for the id.
     */
    public EClassifier getKnownClassifier(final String key) {
        final IMemoryView<EClassifier> classifiersOfThisID = uniqueIDToClassifier.lookup(key);
        if (classifiersOfThisID != null && !classifiersOfThisID.isEmpty()) {
            return classifiersOfThisID.iterator().next();
        } else {
            return null;
        }
    }

    public EClassifier getKnownClassifierForKey(Object classifierKey) {
        EClassifier cls;
        if (isDynamicModel) {
            cls = getKnownClassifier((String) classifierKey);
        } else {
            cls = (EClassifier) classifierKey;
        }
        return cls;
    }


}
