/*******************************************************************************
 * Copyright (c) 2010-2012, Tamas Szabo, Gabor Bergmann, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package tools.refinery.viatra.runtime.base.api;

import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.Enumerator;
import org.eclipse.emf.ecore.*;
import org.eclipse.emf.ecore.EStructuralFeature.Setting;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import tools.refinery.viatra.runtime.base.api.IEClassifierProcessor.IEClassProcessor;
import tools.refinery.viatra.runtime.base.api.IEClassifierProcessor.IEDataTypeProcessor;
import tools.refinery.viatra.runtime.matchers.ViatraQueryRuntimeException;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 *
 * Using an index of the EMF model, this interface exposes useful query functionality, such as:
 * <ul>
 * <li>
 * Getting all the (direct or descendant) instances of a given {@link EClass}
 * <li>
 * Inverse navigation along arbitrary {@link EReference} instances (heterogenous paths too)
 * <li>
 * Finding model elements by attribute value (i.e. inverse navigation along {@link EAttribute})
 * <li>
 * Querying instances of given data types, or structural features.
 * </ul>
 * As queries are served from an index, results are always instantaneous.
 *
 * <p>
 * Such indices will be built on an EMF model rooted at an {@link EObject}, {@link Resource} or {@link ResourceSet}.
 * The boundaries of the model are defined by the containment (sub)tree.
 * The indices will be <strong>maintained incrementally</strong> on changes to the model; these updates can also be
 * observed by registering listeners.
 * </p>
 *
 * <p>
 * One of the options is to build indices in <em>wildcard mode</em>, meaning that all EClasses, EDataTypes, EReferences
 * and EAttributes are indexed. This is convenient, but comes at a high memory cost. To save memory, one can disable
 * <em>wildcard mode</em> and manually register those EClasses, EDataTypes, EReferences and EAttributes that should be
 * indexed.
 * </p>
 *
 * <p>
 * Another choice is whether to build indices in <em>dynamic EMF mode</em>, meaning that types are identified by the String IDs
 * that are ultimately derived from the nsURI of the EPackage. Multiple types with the same ID are treated as the same.
 * This is useful if dynamic EMF is used, where there can be multiple copies (instantiations) of the same EPackage,
 * representing essentially the same metamodel. If one disables <em>dynamic EMF mode</em>, an error is logged if
 * duplicate EPackages with the same nsURI are encountered.
 * </p>
 *
 * <p>
 * Note that none of the defined query methods return null upon empty result sets. All query methods return either a copy of
 * the result sets (where {@link Setting} is instantiated) or an unmodifiable collection of the result view.
 *
 * <p>
 * Instantiate using {@link ViatraBaseFactory}
 *
 * @author Tamas Szabo
 * @noimplement This interface is not intended to be implemented by clients.
 *
 */
public interface NavigationHelper {

    /**
     * Indicates whether indexing is performed in <em>wildcard mode</em>, where every aspect of the EMF model is
     * automatically indexed.
     *
     * @return true if everything is indexed, false if manual registration of interesting EClassifiers and
     *         EStructuralFeatures is required.
     */
    public boolean isInWildcardMode();

    /**
     * Indicates whether indexing is performed in <em>wildcard mode</em> for a selected indexing level
     *
     * @return true if everything is indexed, false if manual registration of interesting EClassifiers and
     *         EStructuralFeatures is required.
     * @since 1.5
     */
    public boolean isInWildcardMode(IndexingLevel level);

    /**
     * Returns the current {@link IndexingLevel} applied to all model elements. For specific types it is possible to request a higher indexing levels, but cannot be lowered.
     * @return the current level of index specified
     * @since 1.4
     */
    public IndexingLevel getWildcardLevel();

    /**
     * Starts wildcard indexing at the given level. After this call, no registration is required for this {@link IndexingLevel}.
     * a previously set wildcard level cannot be lowered, only extended.
     *
     * @since 1.4
     */
    public void setWildcardLevel(IndexingLevel level);

    /**
     * Indicates whether indexing is performed in <em>dynamic EMF mode</em>, i.e. EPackage nsURI collisions are
     *   tolerated and EPackages with the same URI are automatically considered as equal.
     *
     * @return true if multiple EPackages with the same nsURI are treated as the same,
     *         false if an error is logged instead in this case.
     */
    public boolean isInDynamicEMFMode();

    /**
     * For a given attribute value <code>value</code>, find each {@link EAttribute} and host {@link EObject}
     * such that this attribute of the the host object takes the given value. The method will
     * return a set of {@link Setting}s, one for each such host object - EAttribute - value triplet.
     *
     * <p>
     * <strong>Precondition:</strong> Unset / null attribute values are not indexed, so <code>value!=null</code>
     *
     * <p>
     * <strong>Precondition:</strong> Will only find those EAttributes that have already been registered using
     * {@link #registerEStructuralFeatures(Set)}, unless running in <em>wildcard mode</em> (see
     * {@link #isInWildcardMode()}).
     *
     * @param value
     *            the value of the attribute
     * @return a set of {@link Setting}s, one for each EObject and EAttribute that have the given value
     * @see #findByAttributeValue(Object)
     */
    public Set<Setting> findByAttributeValue(Object value);

    /**
     * For given <code>attributes</code> and an attribute value <code>value</code>, find each host {@link EObject}
     * such that any of these attributes of the the host object takes the given value. The method will
     * return a set of {@link Setting}s, one for each such host object - EAttribute - value triplet.
     *
     * <p>
     * <strong>Precondition:</strong> Unset / null attribute values are not indexed, so <code>value!=null</code>
     *
     * <p>
     * <strong>Precondition:</strong> Will only find those EAttributes that have already been registered using
     * {@link #registerEStructuralFeatures(Set)}, unless running in <em>wildcard mode</em> (see
     * {@link #isInWildcardMode()}).
     *
     * @param value
     *            the value of the attribute
     * @param attributes
     *            the collection of attributes that should take the given value
     * @return a set of {@link Setting}s, one for each EObject and attribute that have the given value
     */
    public Set<Setting> findByAttributeValue(Object value, Collection<EAttribute> attributes);

    /**
     * Find all {@link EObject}s for which the given <code>attribute</code> takes the given <code>value</code>.
     *
     * <p>
     * <strong>Precondition:</strong> Unset / null attribute values are not indexed, so <code>value!=null</code>
     *
     * <p>
     * <strong>Precondition:</strong> Results will be returned only if either (a) the EAttribute has already been
     * registered using {@link #registerEStructuralFeatures(Set)}, or (b) running in <em>wildcard mode</em> (see
     * {@link #isInWildcardMode()}).
     *
     * @param value
     *            the value of the attribute
     * @param attribute
     *            the EAttribute that should take the given value
     * @return the set of {@link EObject}s for which the given attribute has the given value
     */
    public Set<EObject> findByAttributeValue(Object value, EAttribute attribute);

    /**
     * Returns the set of instances for the given {@link EDataType} that can be found in the model.
     *
     * <p>
     * <strong>Precondition:</strong> Results will be returned only if either (a) the EDataType has already been
     * registered using {@link #registerEDataTypes(Set)}, or (b) running in <em>wildcard mode</em> (see
     * {@link #isInWildcardMode()}).
     *
     * @param type
     *            the data type
     * @return the set of all attribute values found in the model that are of the given data type
     */
    public Set<Object> getDataTypeInstances(EDataType type);

    /**
     * Returns whether an object is an instance for the given {@link EDataType} that can be found in the current scope.
     * <p>
     * <strong>Precondition:</strong> Result will be true only if either (a) the EDataType has already been registered
     * using {@link #registerEDataTypes(Set)}, or (b) running in <em>wildcard mode</em> (see
     * {@link #isInWildcardMode()}).
     *
     * @param value a non-null value to decide whether it is available as an EDataType instance
     * @param type a non-null EDataType
     * @return true, if a corresponding instance was found
     * @since 1.7
     */
    public boolean isInstanceOfDatatype(Object value, EDataType type);

    /**
     * Find all {@link EObject}s that are the target of the EReference <code>reference</code> from the given
     * <code>source</code> {@link EObject}.
     *
     * <p>
     * Unset / null-valued references are not indexed, and will not be included in the results.
     *
     * <p>
     * <strong>Precondition:</strong> Results will be returned only if either (a) the reference has already been
     * registered using {@link #registerEStructuralFeatures(Set)}, or (b) running in <em>wildcard mode</em> (see
     * {@link #isInWildcardMode()}).
     *
     * @param source the host object
     * @param reference an EReference of the host object
     * @return the set of {@link EObject}s that the given reference points to, from the given source object
     */
    public Set<EObject> getReferenceValues(EObject source, EReference reference);

    /**
     * Find all {@link Object}s that are the target of the EStructuralFeature <code>feature</code> from the given
     * <code>source</code> {@link EObject}.
     *
     * <p>
     * Unset / null-valued features are not indexed, and will not be included in the results.
     *
     * <p>
     * <strong>Precondition:</strong> Results will be returned only if either (a) the feature has already been
     * registered, or (b) running in <em>wildcard mode</em> (see
     * {@link #isInWildcardMode()}).
     *
     * @param source the host object
     * @param feature an EStructuralFeature of the host object
     * @return the set of values that the given feature takes at the given source object
     *
     * @see #getReferenceValues(EObject, EReference)
     */
    public Set<Object> getFeatureTargets(EObject source, EStructuralFeature feature);

    /**
     * Decides whether the given non-null source and target objects are connected via a specific, indexed EStructuralFeature instance.
     *
     * <p>
     * Unset / null-valued features are not indexed, and will not be included in the results.
     *
     * <p>
     * <strong>Precondition:</strong> Result will be true only if either (a) the feature has already been
     * registered, or (b) running in <em>wildcard mode</em> (see
     * {@link #isInWildcardMode()}).
     * @since 1.7
     */
    public boolean isFeatureInstance(EObject source, Object target, EStructuralFeature feature);

    /**
     * For a given {@link EObject} <code>target</code>, find each {@link EReference} and source {@link EObject}
     * such that this reference (list) of the the host object points to the given target object. The method will
     * return a set of {@link Setting}s, one for each such source object - EReference - target triplet.
     *
     * <p>
     * <strong>Precondition:</strong> Unset / null reference values are not indexed, so <code>target!=null</code>
     *
     * <p>
     * <strong>Precondition:</strong> Results will be returned only for those references that have already been
     * registered using {@link #registerEStructuralFeatures(Set)}, or all references if running in
     * <em>wildcard mode</em> (see {@link #isInWildcardMode()}).
     *
     * @param target
     *            the EObject pointed to by the references
     * @return a set of {@link Setting}s, one for each source EObject and reference that point to the given target
     */
    public Set<Setting> getInverseReferences(EObject target);

    /**
     * For given <code>references</code> and an {@link EObject} <code>target</code>, find each source {@link EObject}
     * such that any of these references of the the source object points to the given target object. The method will
     * return a set of {@link Setting}s, one for each such source object - EReference - target triplet.
     *
     * <p>
     * <strong>Precondition:</strong> Unset / null reference values are not indexed, so <code>target!=null</code>
     *
     * <p>
     * <strong>Precondition:</strong> Will only find those EReferences that have already been registered using
     * {@link #registerEStructuralFeatures(Set)}, unless running in <em>wildcard mode</em> (see
     * {@link #isInWildcardMode()}).
     *
     * @param target
     *            the EObject pointed to by the references
     * @param references a set of EReferences pointing to the target
     * @return a set of {@link Setting}s, one for each source EObject and reference that point to the given target
     */
    public Set<Setting> getInverseReferences(EObject target, Collection<EReference> references);

    /**
     * Find all source {@link EObject}s for which the given <code>reference</code> points to the given <code>target</code> object.
     *
     * <p>
     * <strong>Precondition:</strong> Unset / null reference values are not indexed, so <code>target!=null</code>
     *
     * <p>
     * <strong>Precondition:</strong> Results will be returned only if either (a) the reference has already been
     * registered using {@link #registerEStructuralFeatures(Set)}, or (b) running in <em>wildcard mode</em> (see
     * {@link #isInWildcardMode()}).
     *
     * @param target
     *            the EObject pointed to by the references
     * @param reference
     *            an EReference pointing to the target
     * @return the collection of {@link EObject}s for which the given reference points to the given target object
     */
    public Set<EObject> getInverseReferences(EObject target, EReference reference);

    /**
     * Get the direct {@link EObject} instances of the given {@link EClass}. Instances of subclasses will be excluded.
     *
     * <p>
     * <strong>Precondition:</strong> Results will be returned only if either (a) the EClass (or any superclass) has
     * already been registered using {@link #registerEClasses(Set)}, or (b) running in <em>wildcard mode</em> (see
     * {@link #isInWildcardMode()}).
     *
     * @param clazz
     *            an EClass
     * @return the collection of {@link EObject} direct instances of the given EClass (not of subclasses)
     *
     * @see #getAllInstances(EClass)
     */
    public Set<EObject> getDirectInstances(EClass clazz);

    /**
     * Get the all {@link EObject} instances of the given {@link EClass}.
     * This includes instances of subclasses.
     *
     * <p>
     * <strong>Precondition:</strong> Results will be returned only if either (a) the EClass (or any superclass) has
     * already been registered using {@link #registerEClasses(Set)}, or (b) running in <em>wildcard mode</em> (see
     * {@link #isInWildcardMode()}).
     *
     * @param clazz
     *            an EClass
     * @return the collection of {@link EObject} instances of the given EClass and any of its subclasses
     *
     * @see #getDirectInstances(EClass)
     */
    public Set<EObject> getAllInstances(EClass clazz);

    /**
     * Checks whether the given {@link EObject} is an instance of the given {@link EClass}.
     * This includes instances of subclasses.
     * <p> Special note: this method does not check whether the object is indexed in the scope,
     * and will return true for out-of-scope objects as well (as long as they are instances of the class).
     * <p> The given class does not have to be indexed.
     * <p> The difference between this method and {@link EClassifier#isInstance(Object)} is that in dynamic EMF mode, EPackage equivalence is taken into account.
     * @since 1.6
     */
    public boolean isInstanceOfUnscoped(EObject object, EClass clazz);

    /**
     * Checks whether the given {@link EObject} is an instance of the given {@link EClass}.
     * This includes instances of subclasses.
     * <p> Special note: this method does check whether the object is indexed in the scope,
     * and will return false for out-of-scope objects as well (as long as they are instances of the class).
     * <p> The given class does have to be indexed.
     * @since 1.7
     */
    public boolean isInstanceOfScoped(EObject object, EClass clazz);

    /**
     * Get the total number of instances of the given {@link EClass} and all of its subclasses.
     *
     * @since 1.4
     */
    public int countAllInstances(EClass clazz);

    /**
     * Find all source {@link EObject}s for which the given <code>feature</code> points to / takes the given <code>value</code>.
     *
     * <p>
     * <strong>Precondition:</strong> Unset / null-valued features are not indexed, so <code>value!=null</code>
     *
     * <p>
     * <strong>Precondition:</strong> Results will be returned only if either (a) the feature has already been
     * registered using {@link #registerEStructuralFeatures(Set)}, or (b) running in <em>wildcard mode</em> (see
     * {@link #isInWildcardMode()}).
     *
     * @param value
     *            the value of the feature
     * @param feature
     *            the feature instance
     * @return the collection of {@link EObject} instances
     */
    public Set<EObject> findByFeatureValue(Object value, EStructuralFeature feature);

    /**
     * Returns those host {@link EObject}s that have a non-null value for the given feature
     * (at least one, in case of multi-valued references).
     *
     * <p>
     * Unset / null-valued features are not indexed, and will not be included in the results.
     *
     * <p>
     * <strong>Precondition:</strong> Results will be returned only if either (a) the feature has already been
     * registered using {@link #registerEStructuralFeatures(Set)}, or (b) running in <em>wildcard mode</em> (see
     * {@link #isInWildcardMode()}).
     *
     * @param feature
     *            a structural feature
     * @return the collection of {@link EObject}s that have some value for the given structural feature
     */
    public Set<EObject> getHoldersOfFeature(EStructuralFeature feature);
    /**
     * Returns all non-null values that the given feature takes at least once for any {@link EObject} in the scope
     *
     * <p>
     * Unset / null-valued features are not indexed, and will not be included in the results.
     *
     * <p>
     * <strong>Precondition:</strong> Results will be returned only if either (a) the feature has already been
     * registered using {@link #registerEStructuralFeatures(Set)}, or (b) running in <em>wildcard mode</em> (see
     * {@link #isInWildcardMode()}).
     *
     * @param feature
     *            a structural feature
     * @return the collection of values that the given structural feature takes
     * @since 2.1
     */
    public Set<Object> getValuesOfFeature(EStructuralFeature feature);

    /**
     * Call this method to dispose the NavigationHelper.
     *
     * <p>After its disposal, the NavigationHelper will no longer listen to EMF change notifications,
     *   and it will be possible to GC it even if the model is retained in memory.
     *
     * <dt><b>Precondition:</b><dd> no listeners can be registered at all.
     * @throws IllegalStateException if there are any active listeners
     *
     */
    public void dispose();

    /**
     * The given <code>listener</code> will be notified from now on whenever instances the given {@link EClass}es
     * (and any of their subtypes) are added to or removed from the model.
     *
     * <br/>
     * <b>Important</b>: Do not call this method from {@link InstanceListener} methods as it may cause a
     * {@link ConcurrentModificationException}, if you want to add a listener
     * at that point, wrap the call with {@link #executeAfterTraversal(Runnable)}.
     *
     * @param classes
     *            the collection of classes whose instances the listener should be notified of
     * @param listener
     *            the listener instance
     */
    public void addInstanceListener(Collection<EClass> classes, InstanceListener listener);

    /**
     * Unregisters an instance listener for the given classes.
     *
     * <br/>
     * <b>Important</b>: Do not call this method from {@link InstanceListener} methods as it may cause a
     * {@link ConcurrentModificationException}, if you want to remove a listener at that point, wrap the call with
     * {@link #executeAfterTraversal(Runnable)}.
     *
     * @param classes
     *            the collection of classes
     * @param listener
     *            the listener instance
     */
    public void removeInstanceListener(Collection<EClass> classes, InstanceListener listener);

    /**
     * The given <code>listener</code> will be notified from now on whenever instances the given {@link EDataType}s are
     * added to or removed from the model.
     *
     * <br/>
     * <b>Important</b>: Do not call this method from {@link DataTypeListener} methods as it may cause a
     * {@link ConcurrentModificationException}, if you want to add a listener at that point, wrap the call with
     * {@link #executeAfterTraversal(Runnable)}.
     *
     * @param types
     *            the collection of types associated to the listener
     * @param listener
     *            the listener instance
     */
    public void addDataTypeListener(Collection<EDataType> types, DataTypeListener listener);

    /**
     * Unregisters a data type listener for the given types.
     *
     * <br/>
     * <b>Important</b>: Do not call this method from {@link DataTypeListener} methods as it may cause a
     * {@link ConcurrentModificationException}, if you want to remove a listener at that point, wrap the call with
     * {@link #executeAfterTraversal(Runnable)}.
     *
     * @param types
     *            the collection of data types
     * @param listener
     *            the listener instance
     */
    public void removeDataTypeListener(Collection<EDataType> types, DataTypeListener listener);

    /**
     * The given <code>listener</code> will be notified from now on whenever instances the given
     * {@link EStructuralFeature}s are added to or removed from the model.
     *
     * <br/>
     * <b>Important</b>: Do not call this method from {@link FeatureListener} methods as it may cause a
     * {@link ConcurrentModificationException}, if you want to add a listener at that point, wrap the call with
     * {@link #executeAfterTraversal(Runnable)}.
     *
     * @param features
     *            the collection of features associated to the listener
     * @param listener
     *            the listener instance
     */
    public void addFeatureListener(Collection<? extends EStructuralFeature> features, FeatureListener listener);

    /**
     * Unregisters a feature listener for the given features.
     *
     * <br/>
     * <b>Important</b>: Do not call this method from {@link FeatureListener} methods as it may cause a
     * {@link ConcurrentModificationException}, if you want to remove a listener at that point, wrap the call with
     * {@link #executeAfterTraversal(Runnable)}.
     *
     * @param listener
     *            the listener instance
     * @param features
     *            the collection of features
     */
    public void removeFeatureListener(Collection<? extends EStructuralFeature> features, FeatureListener listener);

    /**
     * Register a lightweight observer that is notified if the value of any feature of the given EObject changes.
     *
     * <br/>
     * <b>Important</b>: Do not call this method from {@link LightweightEObjectObserver} methods as it may cause a
     * {@link ConcurrentModificationException}, if you want to add an observer at that point, wrap the call with
     * {@link #executeAfterTraversal(Runnable)}.
     *
     * @param observer
     *            the listener instance
     * @param observedObject
     *            the observed EObject
     * @return false if the observer was already attached to the object (call has no effect), true otherwise
     */
    public boolean addLightweightEObjectObserver(LightweightEObjectObserver observer, EObject observedObject);

    /**
     * Unregisters a lightweight observer for the given EObject.
     *
     * <br/>
     * <b>Important</b>: Do not call this method from {@link LightweightEObjectObserver} methods as it may cause a
     * {@link ConcurrentModificationException}, if you want to remove an observer at that point, wrap the call with
     * {@link #executeAfterTraversal(Runnable)}.
     *
     * @param observer
     *            the listener instance
     * @param observedObject
     *            the observed EObject
     * @return false if the observer has not been previously attached to the object (call has no effect), true otherwise
     */
    public boolean removeLightweightEObjectObserver(LightweightEObjectObserver observer, EObject observedObject);

    /**
     * Manually turns on indexing for the given types (indexing of others are unaffected). Note that
     * registering new types will result in a single iteration through the whole attached model.
     * <b> Not usable in <em>wildcard mode</em>.</b>
     *
     * @param classes
     *            the set of classes to observe (null okay)
     * @param dataTypes
     *            the set of data types to observe (null okay)
     * @param features
     *            the set of features to observe (null okay)
     * @throws IllegalStateException if in wildcard mode
     * @since 1.4
     */
    public void registerObservedTypes(Set<EClass> classes, Set<EDataType> dataTypes, Set<? extends EStructuralFeature> features, IndexingLevel level);

    /**
     * Manually turns off indexing for the given types (indexing of others are unaffected). Note that if the
     * unregistered types are re-registered later, the whole attached model needs to be visited again.
     * <b> Not usable in <em>wildcard mode</em>.</b>
     *
     * <dt><b>Precondition:</b><dd> no listeners can be registered for the given types.
     * @param classes
     *            the set of classes that will be ignored again from now on (null okay)
     * @param dataTypes
     *            the set of data types that will be ignored again from now on (null okay)
     * @param features
     *            the set of features that will be ignored again from now on (null okay)
     * @throws IllegalStateException if in wildcard mode, or if there are listeners registered for the given types
     */
    public void unregisterObservedTypes(Set<EClass> classes, Set<EDataType> dataTypes, Set<? extends EStructuralFeature> features);

    /**
     * Manually turns on indexing for the given features (indexing of other features are unaffected). Note that
     * registering new features will result in a single iteration through the whole attached model.
     * <b> Not usable in <em>wildcard mode</em>.</b>
     *
     * @param features
     *            the set of features to observe
     * @throws IllegalStateException if in wildcard mode
     * @since 1.4
     */
    public void registerEStructuralFeatures(Set<? extends EStructuralFeature> features, IndexingLevel level);

    /**
     * Manually turns off indexing for the given features (indexing of other features are unaffected). Note that if the
     * unregistered features are re-registered later, the whole attached model needs to be visited again.
     * <b> Not usable in <em>wildcard mode</em>.</b>
     *
     * <dt><b>Precondition:</b><dd> no listeners can be registered for the given features.
     *
     * @param features
     *            the set of features that will be ignored again from now on
     * @throws IllegalStateException if in wildcard mode, or if there are listeners registered for the given types
     */
    public void unregisterEStructuralFeatures(Set<? extends EStructuralFeature> features);

    /**
     * Manually turns on indexing for the given classes (indexing of other classes are unaffected). Instances of
     * subclasses will also be indexed. Note that registering new classes will result in a single iteration through the whole
     * attached model.
     * <b> Not usable in <em>wildcard mode</em>.</b>
     *
     * @param classes
     *            the set of classes to observe
     * @throws IllegalStateException if in wildcard mode
     * @since 1.4
     */
    public void registerEClasses(Set<EClass> classes, IndexingLevel level);

    /**
     * Manually turns off indexing for the given classes (indexing of other classes are unaffected). Note that if the
     * unregistered classes are re-registered later, the whole attached model needs to be visited again.
     * <b> Not usable in <em>wildcard mode</em>.</b>
     *
     * <dt><b>Precondition:</b><dd> no listeners can be registered for the given classes.
     * @param classes
     *            the set of classes that will be ignored again from now on
     * @throws IllegalStateException if in wildcard mode, or if there are listeners registered for the given types
     */
    public void unregisterEClasses(Set<EClass> classes);

    /**
     * Manually turns on indexing for the given data types (indexing of other features are unaffected). Note that
     * registering new data types will result in a single iteration through the whole attached model.
     * <b> Not usable in <em>wildcard mode</em>.</b>
     *
     * @param dataTypes
     *            the set of data types to observe
     * @throws IllegalStateException if in wildcard mode
     * @since 1.4
     */
    public void registerEDataTypes(Set<EDataType> dataTypes, IndexingLevel level);

    /**
     * Manually turns off indexing for the given data types (indexing of other data types are unaffected). Note that if
     * the unregistered data types are re-registered later, the whole attached model needs to be visited again.
     * <b> Not usable in <em>wildcard mode</em>.</b>
     *
     * <dt><b>Precondition:</b><dd> no listeners can be registered for the given datatypes.
     *
     * @param dataTypes
     *            the set of data types that will be ignored again from now on
     * @throws IllegalStateException if in wildcard mode, or if there are listeners registered for the given types
     */
    public void unregisterEDataTypes(Set<EDataType> dataTypes);

    /**
     * The given callback will be executed, and all model traversals and index registrations will be delayed until the
     * execution is done. If there are any outstanding feature, class or datatype registrations, a single coalesced model
     * traversal will initialize the caches and deliver the notifications.
     *
     * @param callable
     */
    public <V> V coalesceTraversals(Callable<V> callable) throws InvocationTargetException;

    /**
     * Execute the given runnable after traversal. It is guaranteed that the runnable is executed as soon as
     * the indexing is finished. The callback is executed only once, then is removed from the callback queue.
     * @param traversalCallback
     * @throws InvocationTargetException
     * @since 1.4
     */
    public void executeAfterTraversal(Runnable traversalCallback) throws InvocationTargetException;

    /**
     * Examines whether execution is currently in the callable
     * 	block of an invocation of {#link {@link #coalesceTraversals(Callable)}}.
     */
    public boolean isCoalescing();

    /**
     * Adds a coarse-grained listener that will be invoked after the NavigationHelper index or the underlying model is changed. Can be used
     * e.g. to check model contents. Not intended for general use.
     *
     * <p/> See {@link #removeBaseIndexChangeListener(EMFBaseIndexChangeListener)}
     * @param listener
     */
    public void addBaseIndexChangeListener(EMFBaseIndexChangeListener listener);

    /**
     * Removes a registered listener.
     *
     * <p/> See {@link #addBaseIndexChangeListener(EMFBaseIndexChangeListener)}
     *
     * @param listener
     */
    public void removeBaseIndexChangeListener(EMFBaseIndexChangeListener listener);

    /**
     * Adds an additional EMF model root.
     *
     * @param emfRoot
     * @throws ViatraQueryRuntimeException
     */
    public void addRoot(Notifier emfRoot);

    /**
     * Moves an EObject (along with its entire containment subtree) within the containment hierarchy of the EMF model.
     *   The object will be relocated from the original parent object to a different parent, or a different containment
     *   list of the same parent.
     *
     * <p> When indexing is enabled, such a relocation is costly if performed through normal getters/setters, as the index
     * for the entire subtree is pruned at the old location and reconstructed at the new one.
     * This method provides a workaround to keep the operation cheap.
     *
     * <p> This method is experimental. Re-entrancy not supported.
     *
     * @param element the eObject to be moved
     * @param targetContainmentReferenceList containment list of the new parent object into which the element has to be moved
     *
     */
    public <T extends EObject> void cheapMoveTo(T element, EList<T> targetContainmentReferenceList);

    /**
     * Moves an EObject (along with its entire containment subtree) within the containment hierarchy of the EMF model.
     *   The object will be relocated from the original parent object to a different parent, or a different containment
     *   list of the same parent.
     *
     * <p> When indexing is enabled, such a relocation is costly if performed through normal getters/setters, as the index
     * for the entire subtree is pruned at the old location and reconstructed at the new one.
     * This method provides a workaround to keep the operation cheap.
     *
     * <p> This method is experimental. Re-entrancy not supported.
     *
     * @param element the eObject to be moved
     * @param parent  the new parent object under which the element has to be moved
     * @param containmentFeature the kind of containment reference that should be established between the new parent and the element
     *
     */
    public void cheapMoveTo(EObject element, EObject parent, EReference containmentFeature);


    /**
     * Traverses all instances of a selected data type stored in the base index, and allows executing a custom function on
     * it. There is no guaranteed order in which the processor will be called with the selected features.
     *
     * @param type
     * @param processor
     * @since 0.8
     */
    void processDataTypeInstances(EDataType type, IEDataTypeProcessor processor);

    /**
     * Traverses all direct instances of a selected class stored in the base index, and allows executing a custom function on
     * it. There is no guaranteed order in which the processor will be called with the selected features.
     *
     * @param type
     * @param processor
     * @since 0.8
     */
    void processAllInstances(EClass type, IEClassProcessor processor);

    /**
     * Traverses all direct instances of a selected class stored in the base index, and allows executing a custom function on
     * it. There is no guaranteed order in which the processor will be called with the selected features.
     *
     * @param type
     * @param processor
     * @since 0.8
     */
    void processDirectInstances(EClass type, IEClassProcessor processor);

    /**
     * Traverses all instances of a selected feature stored in the base index, and allows executing a custom function on
     * it. There is no guaranteed order in which the processor will be called with the selected features.
     *
     * <p>
     * <strong>Precondition:</strong> Will only find those {@link EStructuralFeature}s that have already been registered using
     * {@link #registerEStructuralFeatures(Set)}, unless running in <em>wildcard mode</em> (see
     * {@link #isInWildcardMode()}).
     *
     * @since 1.7
     */
    void processAllFeatureInstances(EStructuralFeature feature, IStructuralFeatureInstanceProcessor processor);
    /**
     * Returns all EClasses that currently have direct instances cached by the index. <ul>
     * <li> Supertypes will not be returned, unless they have direct instances in the model as well.
     * <li> If not in <em>wildcard mode</em>, only registered EClasses and their subtypes will be considered.
     * <li> Note for advanced users: if a type is represented by multiple EClass objects, one of them is chosen as representative and returned.
     * </ul>
     */
    public Set<EClass> getAllCurrentClasses();

    /**
     * Updates the value of indexed derived features that are not well-behaving.
     */
    void resampleDerivedFeatures();

    /**
     * Adds a listener for internal errors in the index. A listener can only be added once.
     *
     * @param listener
     * @returns true if the listener was not already added
     * @since 0.8
     */
    boolean addIndexingErrorListener(IEMFIndexingErrorListener listener);

    /**
     * Removes a listener for internal errors in the index.
     *
     * @param listener
     * @returns true if the listener was successfully removed (e.g. it did exist)
     * @since 0.8
     */
    boolean removeIndexingErrorListener(IEMFIndexingErrorListener listener);

    /**
     * Returns the internal, canonicalized implementation of an attribute value.
     *
     * <p> Behaviour: when in dynamic EMF mode, substitutes enum literals with a canonical version of the enum literal.
     * Otherwise, returns the input.
     *
     * <p> The canonical enum literal will be guaranteed to be a valid EMF enum literal ({@link Enumerator}),
     * 	and the best effort is made to ensure that it will be the same for all versions of the {@link EEnum},
     * 	including {@link EEnumLiteral}s in different versions of ecore packages, as well as Java enums generated from them..
     *
     * <p> Usage is not required when simply querying the indexed model through the {@link NavigationHelper} API,
     * 	as both method inputs and the results returned are automatically canonicalized in dynamic EMF mode.
     * Using this method is required only if the client wants to do querying/filtering on the results returned, and wants to know what to look for.
     */
    Object toCanonicalValueRepresentation(Object value);

    /**
     * @since 1.4
     */
    IndexingLevel getIndexingLevel(EClass type);

    /**
     * @since 1.4
     */
    IndexingLevel getIndexingLevel(EDataType type);

    /**
     * @since 1.4
     */
    IndexingLevel getIndexingLevel(EStructuralFeature feature);

    /**
     * @since 1.4
     */
    public int countDataTypeInstances(EDataType dataType);

    /**
     * @since 1.4
     */
    public int countFeatureTargets(EObject seedSource, EStructuralFeature feature);

    /**
     * @since 1.4
     */
    public int countFeatures(EStructuralFeature feature);


}
