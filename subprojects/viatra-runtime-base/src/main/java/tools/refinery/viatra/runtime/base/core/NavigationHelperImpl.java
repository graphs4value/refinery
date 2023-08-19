/*******************************************************************************
 * Copyright (c) 2010-2012, Tamas Szabo, Gabor Bergmann, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.viatra.runtime.base.core;

import org.apache.log4j.Logger;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.emf.common.notify.NotifyingList;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.*;
import org.eclipse.emf.ecore.EStructuralFeature.Setting;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.EcoreUtil;
import tools.refinery.viatra.runtime.base.api.*;
import tools.refinery.viatra.runtime.base.api.IEClassifierProcessor.IEClassProcessor;
import tools.refinery.viatra.runtime.base.api.IEClassifierProcessor.IEDataTypeProcessor;
import tools.refinery.viatra.runtime.base.api.filters.IBaseIndexObjectFilter;
import tools.refinery.viatra.runtime.base.api.filters.IBaseIndexResourceFilter;
import tools.refinery.viatra.runtime.base.comprehension.EMFModelComprehension;
import tools.refinery.viatra.runtime.base.comprehension.EMFVisitor;
import tools.refinery.viatra.runtime.base.core.EMFBaseIndexInstanceStore.FeatureData;
import tools.refinery.viatra.runtime.base.core.NavigationHelperVisitor.TraversingVisitor;
import tools.refinery.viatra.runtime.base.core.profiler.ProfilingNavigationHelperContentAdapter;
import tools.refinery.viatra.runtime.base.exception.ViatraBaseException;
import tools.refinery.viatra.runtime.matchers.ViatraQueryRuntimeException;
import tools.refinery.viatra.runtime.matchers.util.CollectionsFactory;
import tools.refinery.viatra.runtime.matchers.util.CollectionsFactory.MemoryType;
import tools.refinery.viatra.runtime.matchers.util.IMultiLookup;
import tools.refinery.viatra.runtime.matchers.util.Preconditions;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.function.Function.identity;

/**
 * @noextend This class is not intended to be subclassed by clients.
 * @author Gabor Bergmann and Tamas Szabo
 */
public class NavigationHelperImpl implements NavigationHelper {

    /**
     * This is never null.
     */
    protected IndexingLevel wildcardMode;


    protected Set<Notifier> modelRoots;
    private boolean expansionAllowed;
    private boolean traversalDescendsAlongCrossResourceContainment;
    // protected NavigationHelperVisitor visitor;
    protected NavigationHelperContentAdapter contentAdapter;

    protected final Logger logger;

    // type object or String id
    protected Map<Object, IndexingLevel> directlyObservedClasses = new HashMap<Object, IndexingLevel>();
    // including subclasses; if null, must be recomputed
    protected Map<Object, IndexingLevel> allObservedClasses = null;
    protected Map<Object, IndexingLevel> observedDataTypes;
    protected Map<Object, IndexingLevel> observedFeatures;
    // ignore RESOLVE for these features, as they are just starting to be observed - see [428458]
    protected Set<Object> ignoreResolveNotificationFeatures;

    /**
     * Feature registration and model traversal is delayed while true
     */
    protected boolean delayTraversals = false;
    /**
     * Classes (or String ID in dynamic mode) to be registered once the coalescing period is over
     */
    protected Map<Object, IndexingLevel> delayedClasses = new HashMap<>();
    /**
     * EStructuralFeatures (or String ID in dynamic mode) to be registered once the coalescing period is over
     */
    protected Map<Object, IndexingLevel> delayedFeatures = new HashMap<>();
    /**
     * EDataTypes (or String ID in dynamic mode) to be registered once the coalescing period is over
     */
    protected Map<Object, IndexingLevel> delayedDataTypes = new HashMap<>();

    /**
     * Features per EObject to be resolved later (towards the end of a coalescing period when no Resources are loading)
     */
    protected IMultiLookup<EObject, EReference> delayedProxyResolutions = CollectionsFactory.createMultiLookup(Object.class, MemoryType.SETS, Object.class);
    /**
     * Reasources that are currently loading, implying the proxy resolution attempts should be delayed
     */
    protected Set<Resource> resolutionDelayingResources = new HashSet<Resource>();

    protected Queue<Runnable> traversalCallbacks = new LinkedList<Runnable>();

    /**
     * These global listeners will be called after updates.
     */
    // private final Set<Runnable> afterUpdateCallbacks;
    private final Set<EMFBaseIndexChangeListener> baseIndexChangeListeners;
    private final Map<EObject, Set<LightweightEObjectObserver>> lightweightObservers;

    // These are the user subscriptions to notifications
    private final Map<InstanceListener, Set<EClass>> subscribedInstanceListeners;
    private final Map<FeatureListener, Set<EStructuralFeature>> subscribedFeatureListeners;
    private final Map<DataTypeListener, Set<EDataType>> subscribedDataTypeListeners;

    // these are the internal notification tables
    // (element Type or String id) -> listener -> (subscription types)
    // if null, must be recomputed from subscriptions
    // potentially multiple subscription types for each element type because (a) nsURI collisions, (b) multiple
    // supertypes
    private Map<Object, Map<InstanceListener, Set<EClass>>> instanceListeners;
    private Map<Object, Map<FeatureListener, Set<EStructuralFeature>>> featureListeners;
    private Map<Object, Map<DataTypeListener, Set<EDataType>>> dataTypeListeners;

    private final Set<IEMFIndexingErrorListener> errorListeners;
    private final BaseIndexOptions baseIndexOptions;

    private EMFModelComprehension comprehension;

    private boolean loggedRegistrationMessage = false;

    EMFBaseIndexMetaStore metaStore;
    EMFBaseIndexInstanceStore instanceStore;
    EMFBaseIndexStatisticsStore statsStore;

    <T> Set<T> setMinus(Collection<? extends T> a, Collection<T> b) {
        Set<T> result = new HashSet<T>(a);
        result.removeAll(b);
        return result;
    }

    @SuppressWarnings("unchecked")
    <T extends EObject> Set<T> resolveAllInternal(Set<? extends T> a) {
        if (a == null)
            a = Collections.emptySet();
        Set<T> result = new HashSet<T>();
        for (T t : a) {
            if (t.eIsProxy()) {
                result.add((T) EcoreUtil.resolve(t, (ResourceSet) null));
            } else {
                result.add(t);
            }
        }
        return result;
    }

    Set<Object> resolveClassifiersToKey(Set<? extends EClassifier> classes) {
        Set<? extends EClassifier> resolveds = resolveAllInternal(classes);
        Set<Object> result = new HashSet<Object>();
        for (EClassifier resolved : resolveds) {
            result.add(toKey(resolved));
        }
        return result;
    }

    Set<Object> resolveFeaturesToKey(Set<? extends EStructuralFeature> features) {
        Set<EStructuralFeature> resolveds = resolveAllInternal(features);
        Set<Object> result = new HashSet<Object>();
        for (EStructuralFeature resolved : resolveds) {
            result.add(toKey(resolved));
        }
        return result;
    }

    @Override
    public boolean isInWildcardMode() {
        return isInWildcardMode(IndexingLevel.FULL);
    }

    @Override
    public boolean isInWildcardMode(IndexingLevel level) {
        return wildcardMode.providesLevel(level);
    }

    @Override
    public boolean isInDynamicEMFMode() {
        return baseIndexOptions.isDynamicEMFMode();
    }

    /**
     * @return the baseIndexOptions
     */
    public BaseIndexOptions getBaseIndexOptions() {
        return baseIndexOptions.copy();
    }

    /**
     * @return the comprehension
     */
    public EMFModelComprehension getComprehension() {
        return comprehension;
    }

    /**
     * @throws ViatraQueryRuntimeException
     */
    public NavigationHelperImpl(Notifier emfRoot, BaseIndexOptions options, Logger logger) {
        this.baseIndexOptions = options.copy();
        this.logger = logger;
        assert (logger != null);

        this.comprehension = initModelComprehension();
        this.wildcardMode = baseIndexOptions.getWildcardLevel();
        this.subscribedInstanceListeners = new HashMap<InstanceListener, Set<EClass>>();
        this.subscribedFeatureListeners = new HashMap<FeatureListener, Set<EStructuralFeature>>();
        this.subscribedDataTypeListeners = new HashMap<DataTypeListener, Set<EDataType>>();
        this.lightweightObservers = CollectionsFactory.createMap();
        this.observedFeatures = new HashMap<Object, IndexingLevel>();
        this.ignoreResolveNotificationFeatures = new HashSet<Object>();
        this.observedDataTypes = new HashMap<Object, IndexingLevel>();

        metaStore = initMetaStore();
        instanceStore = initInstanceStore();
        statsStore = initStatStore();

        this.contentAdapter = initContentAdapter();
        this.baseIndexChangeListeners = new HashSet<EMFBaseIndexChangeListener>();
        this.errorListeners = new LinkedHashSet<IEMFIndexingErrorListener>();

        this.modelRoots = new HashSet<Notifier>();
        this.expansionAllowed = false;
        this.traversalDescendsAlongCrossResourceContainment = false;

        if (emfRoot != null) {
            addRootInternal(emfRoot);
        }

    }

    @Override
    public IndexingLevel getWildcardLevel() {
        return wildcardMode;
    }

    @Override
    public void setWildcardLevel(final IndexingLevel level) {
        try{
            IndexingLevel mergedLevel = NavigationHelperImpl.this.wildcardMode.merge(level);
            if (mergedLevel != NavigationHelperImpl.this.wildcardMode){
                NavigationHelperImpl.this.wildcardMode = mergedLevel;

                // force traversal upon change of wildcard level
                final NavigationHelperVisitor visitor = initTraversingVisitor(
                       Collections.<Object, IndexingLevel>emptyMap(), Collections.<Object, IndexingLevel>emptyMap(), Collections.<Object, IndexingLevel>emptyMap(), Collections.<Object, IndexingLevel>emptyMap());
                coalesceTraversals(() -> traverse(visitor));
            }
        } catch (InvocationTargetException ex) {
            processingFatal(ex.getCause(), "Setting wildcard level: " + level);
        } catch (Exception ex) {
            processingFatal(ex, "Setting wildcard level: " + level);
        }
    }

    public NavigationHelperContentAdapter getContentAdapter() {
        return contentAdapter;
    }

    public Map<Object, IndexingLevel> getObservedFeaturesInternal() {
        return observedFeatures;
    }

    public boolean isFeatureResolveIgnored(EStructuralFeature feature) {
        return ignoreResolveNotificationFeatures.contains(toKey(feature));
    }

    @Override
    public void dispose() {
        ensureNoListenersForDispose();
        for (Notifier root : modelRoots) {
            contentAdapter.removeAdapter(root);
        }
    }

    @Override
    public Set<Object> getDataTypeInstances(EDataType type) {
        Object typeKey = toKey(type);
        return Collections.unmodifiableSet(instanceStore.getDistinctDataTypeInstances(typeKey));
    }

    @Override
    public boolean isInstanceOfDatatype(Object value, EDataType type) {
        Object typeKey = toKey(type);
        Set<Object> valMap = instanceStore.getDistinctDataTypeInstances(typeKey);
        return valMap.contains(value);
    }

    protected FeatureData featureData(EStructuralFeature feature) {
        return instanceStore.getFeatureData(toKey(feature));
    }

    @Override
    public Set<Setting> findByAttributeValue(Object value_) {
        Object value = toCanonicalValueRepresentation(value_);
        return getSettingsForTarget(value);
    }

    @Override
    public Set<Setting> findByAttributeValue(Object value_, Collection<EAttribute> attributes) {
        Object value = toCanonicalValueRepresentation(value_);
        Set<Setting> retSet = new HashSet<Setting>();

        for (EAttribute attr : attributes) {
            for (EObject holder : featureData(attr).getDistinctHoldersOfValue(value)) {
                retSet.add(new NavigationHelperSetting(attr, holder, value));
            }
        }

        return retSet;
    }

    @Override
    public Set<EObject> findByAttributeValue(Object value_, EAttribute attribute) {
        Object value = toCanonicalValueRepresentation(value_);
        final Set<EObject> holders = featureData(attribute).getDistinctHoldersOfValue(value);
        return Collections.unmodifiableSet(holders);
    }

    @Override
    public void processAllFeatureInstances(EStructuralFeature feature, IStructuralFeatureInstanceProcessor processor) {
        featureData(feature).forEach(processor);
    }

    @Override
    public void processDirectInstances(EClass type, IEClassProcessor processor) {
        Object typeKey = toKey(type);
        processDirectInstancesInternal(type, processor, typeKey);
    }

    @Override
    public void processAllInstances(EClass type, IEClassProcessor processor) {
        Object typeKey = toKey(type);
        Set<Object> subTypes = metaStore.getSubTypeMap().get(typeKey);
        if (subTypes != null) {
            for (Object subTypeKey : subTypes) {
                processDirectInstancesInternal(type, processor, subTypeKey);
            }
        }
        processDirectInstancesInternal(type, processor, typeKey);
    }

    @Override
    public void processDataTypeInstances(EDataType type, IEDataTypeProcessor processor) {
        Object typeKey = toKey(type);
        for (Object value : instanceStore.getDistinctDataTypeInstances(typeKey)) {
            processor.process(type, value);
        }
    }

    protected void processDirectInstancesInternal(EClass type, IEClassProcessor processor, Object typeKey) {
        final Set<EObject> instances = instanceStore.getInstanceSet(typeKey);
        if (instances != null) {
            for (EObject eObject : instances) {
                processor.process(type, eObject);
            }
        }
    }

    @Override
    public Set<Setting> getInverseReferences(EObject target) {
        return getSettingsForTarget(target);
    }

    protected Set<Setting> getSettingsForTarget(Object target) {
        Set<Setting> retSet = new HashSet<Setting>();
        for (Object featureKey : instanceStore.getFeatureKeysPointingTo(target)) {
            Set<EObject> holders = instanceStore.getFeatureData(featureKey).getDistinctHoldersOfValue(target);
            for (EObject holder : holders) {
                EStructuralFeature feature = metaStore.getKnownFeatureForKey(featureKey);
                retSet.add(new NavigationHelperSetting(feature, holder, target));
            }
        }
        return retSet;
    }

    @Override
    public Set<Setting> getInverseReferences(EObject target, Collection<EReference> references) {
        Set<Setting> retSet = new HashSet<>();
        for (EReference ref : references) {
            final Set<EObject> holders = featureData(ref).getDistinctHoldersOfValue(target);
            for (EObject source : holders) {
                retSet .add(new NavigationHelperSetting(ref, source, target));
            }
        }

        return retSet;
    }

    @Override
    public Set<EObject> getInverseReferences(EObject target, EReference reference) {
        final Set<EObject> holders = featureData(reference).getDistinctHoldersOfValue(target);
        return Collections.unmodifiableSet(holders);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Set<EObject> getReferenceValues(EObject source, EReference reference) {
        Set<Object> targets = getFeatureTargets(source, reference);
        return (Set<EObject>) (Set<?>) targets; // this is known to be safe, as EReferences can only point to EObjects
    }

    @Override
    public Set<Object> getFeatureTargets(EObject source, EStructuralFeature _feature) {
        return Collections.unmodifiableSet(featureData(_feature).getDistinctValuesOfHolder(source));
    }

    @Override
    public boolean isFeatureInstance(EObject source, Object target, EStructuralFeature _feature) {
        return featureData(_feature).isInstance(source, target);
    }

    @Override
    public Set<EObject> getDirectInstances(EClass type) {
        Object typeKey = toKey(type);
        Set<EObject> valSet = instanceStore.getInstanceSet(typeKey);
        if (valSet == null) {
            return Collections.emptySet();
        } else {
            return Collections.unmodifiableSet(valSet);
        }
    }

    protected Object toKey(EClassifier eClassifier) {
        return metaStore.toKey(eClassifier);
    }

    protected Object toKey(EStructuralFeature feature) {
        return metaStore.toKey(feature);
    }

    @Override
    public Object toCanonicalValueRepresentation(Object value) {
        return metaStore.toInternalValueRepresentation(value);
    }

    @Override
    public Set<EObject> getAllInstances(EClass type) {
        Set<EObject> retSet = new HashSet<EObject>();

        Object typeKey = toKey(type);
        Set<Object> subTypes = metaStore.getSubTypeMap().get(typeKey);
        if (subTypes != null) {
            for (Object subTypeKey : subTypes) {
                final Set<EObject> instances = instanceStore.getInstanceSet(subTypeKey);
                if (instances != null) {
                    retSet.addAll(instances);
                }
            }
        }
        final Set<EObject> instances = instanceStore.getInstanceSet(typeKey);
        if (instances != null) {
            retSet.addAll(instances);
        }

        return retSet;
    }

    @Override
    public boolean isInstanceOfUnscoped(EObject object, EClass clazz) {
        Object candidateTypeKey = toKey(clazz);
        Object typeKey = toKey(object.eClass());

        return doCalculateInstanceOf(candidateTypeKey, typeKey);
    }

    @Override
    public boolean isInstanceOfScoped(EObject object, EClass clazz) {
        Object typeKey = toKey(object.eClass());
        if (!doCalculateInstanceOf(toKey(clazz), typeKey)) {
            return false;
        }
        final Set<EObject> instances = instanceStore.getInstanceSet(typeKey);
        return instances != null && instances.contains(object);
    }

    protected boolean doCalculateInstanceOf(Object candidateTypeKey, Object typeKey) {
        if (candidateTypeKey.equals(typeKey)) return true;
        if (metaStore.getEObjectClassKey().equals(candidateTypeKey)) return true;

        Set<Object> superTypes = metaStore.getSuperTypeMap().get(typeKey);
        return superTypes.contains(candidateTypeKey);
    }

    @Override
    public Set<EObject> findByFeatureValue(Object value_, EStructuralFeature _feature) {
        Object value = toCanonicalValueRepresentation(value_);
        return Collections.unmodifiableSet(featureData(_feature).getDistinctHoldersOfValue(value));
    }

    @Override
    public Set<EObject> getHoldersOfFeature(EStructuralFeature _feature) {
        Object feature = toKey(_feature);
        return Collections.unmodifiableSet(instanceStore.getHoldersOfFeature(feature));
    }
    @Override
    public Set<Object> getValuesOfFeature(EStructuralFeature _feature) {
        Object feature = toKey(_feature);
        return Collections.unmodifiableSet(instanceStore.getValuesOfFeature(feature));
    }

    @Override
    public void addInstanceListener(Collection<EClass> classes, InstanceListener listener) {
        Set<EClass> registered = this.subscribedInstanceListeners.computeIfAbsent(listener, l -> new HashSet<>());
        Set<EClass> delta = setMinus(classes, registered);
        if (!delta.isEmpty()) {
            registered.addAll(delta);
            if (instanceListeners != null) { // if already computed
                for (EClass subscriptionType : delta) {
                    final Object superElementTypeKey = toKey(subscriptionType);
                    addInstanceListenerInternal(listener, subscriptionType, superElementTypeKey);
                    final Set<Object> subTypeKeys = metaStore.getSubTypeMap().get(superElementTypeKey);
                    if (subTypeKeys != null)
                        for (Object subTypeKey : subTypeKeys) {
                            addInstanceListenerInternal(listener, subscriptionType, subTypeKey);
                        }
                }
            }
        }
    }

    @Override
    public void removeInstanceListener(Collection<EClass> classes, InstanceListener listener) {
        Set<EClass> restriction = this.subscribedInstanceListeners.get(listener);
        if (restriction != null) {
            boolean changed = restriction.removeAll(classes);
            if (restriction.size() == 0) {
                this.subscribedInstanceListeners.remove(listener);
            }
            if (changed)
                instanceListeners = null; // recompute later on demand
        }
    }

    @Override
    public void addFeatureListener(Collection<? extends EStructuralFeature> features, FeatureListener listener) {
        Set<EStructuralFeature> registered = this.subscribedFeatureListeners.computeIfAbsent(listener, l -> new HashSet<>());
        Set<EStructuralFeature> delta = setMinus(features, registered);
        if (!delta.isEmpty()) {
            registered.addAll(delta);
            if (featureListeners != null) { // if already computed
                for (EStructuralFeature subscriptionType : delta) {
                    addFeatureListenerInternal(listener, subscriptionType, toKey(subscriptionType));
                }
            }
        }
    }

    @Override
    public void removeFeatureListener(Collection<? extends EStructuralFeature> features, FeatureListener listener) {
        Collection<EStructuralFeature> restriction = this.subscribedFeatureListeners.get(listener);
        if (restriction != null) {
            boolean changed = restriction.removeAll(features);
            if (restriction.size() == 0) {
                this.subscribedFeatureListeners.remove(listener);
            }
            if (changed)
                featureListeners = null; // recompute later on demand
        }
    }

    @Override
    public void addDataTypeListener(Collection<EDataType> types, DataTypeListener listener) {
        Set<EDataType> registered = this.subscribedDataTypeListeners.computeIfAbsent(listener, l -> new HashSet<>());
        Set<EDataType> delta = setMinus(types, registered);
        if (!delta.isEmpty()) {
            registered.addAll(delta);
            if (dataTypeListeners != null) { // if already computed
                for (EDataType subscriptionType : delta) {
                    addDatatypeListenerInternal(listener, subscriptionType, toKey(subscriptionType));
                }
            }
        }
    }

    @Override
    public void removeDataTypeListener(Collection<EDataType> types, DataTypeListener listener) {
        Collection<EDataType> restriction = this.subscribedDataTypeListeners.get(listener);
        if (restriction != null) {
            boolean changed = restriction.removeAll(types);
            if (restriction.size() == 0) {
                this.subscribedDataTypeListeners.remove(listener);
            }
            if (changed)
                dataTypeListeners = null; // recompute later on demand
        }
    }

    /**
     * @return the observedDataTypes
     */
    public Map<Object, IndexingLevel> getObservedDataTypesInternal() {
        return observedDataTypes;
    }

    @Override
    public boolean addLightweightEObjectObserver(LightweightEObjectObserver observer, EObject observedObject) {
        Set<LightweightEObjectObserver> observers = lightweightObservers.computeIfAbsent(observedObject, CollectionsFactory::emptySet);
        return observers.add(observer);
    }

    @Override
    public boolean removeLightweightEObjectObserver(LightweightEObjectObserver observer, EObject observedObject) {
        boolean result = false;
        Set<LightweightEObjectObserver> observers = lightweightObservers.get(observedObject);
        if (observers != null) {
            result = observers.remove(observer);
            if (observers.isEmpty()) {
                lightweightObservers.remove(observedObject);
            }
        }
        return result;
    }

    public void notifyBaseIndexChangeListeners() {
        notifyBaseIndexChangeListeners(instanceStore.isDirty);
        if (instanceStore.isDirty) {
            instanceStore.isDirty = false;
        }
    }

    /**
     * This will run after updates.
     */
    protected void notifyBaseIndexChangeListeners(boolean baseIndexChanged) {
        if (!baseIndexChangeListeners.isEmpty()) {
            for (EMFBaseIndexChangeListener listener : new ArrayList<>(baseIndexChangeListeners)) {
                try {
                    if (!listener.onlyOnIndexChange() || baseIndexChanged) {
                        listener.notifyChanged(baseIndexChanged);
                    }
                } catch (Exception ex) {
                    notifyFatalListener("VIATRA Base encountered an error in delivering notifications about changes. ",
                            ex);
                }
            }
        }
    }

    void notifyDataTypeListeners(final Object typeKey, final Object value, final boolean isInsertion,
            final boolean firstOrLastOccurrence) {
        for (final Entry<DataTypeListener, Set<EDataType>> entry : getDataTypeListeners().getOrDefault(typeKey, Collections.emptyMap()).entrySet()) {
            final DataTypeListener listener = entry.getKey();
            for (final EDataType subscriptionType : entry.getValue()) {
                if (isInsertion) {
                    listener.dataTypeInstanceInserted(subscriptionType, value, firstOrLastOccurrence);
                } else {
                    listener.dataTypeInstanceDeleted(subscriptionType, value, firstOrLastOccurrence);
                }
            }
        }
    }

    void notifyFeatureListeners(final EObject host, final Object featureKey, final Object value,
            final boolean isInsertion) {
        for (final Entry<FeatureListener, Set<EStructuralFeature>> entry : getFeatureListeners().getOrDefault(featureKey, Collections.emptyMap())
                .entrySet()) {
            final FeatureListener listener = entry.getKey();
            for (final EStructuralFeature subscriptionType : entry.getValue()) {
                if (isInsertion) {
                    listener.featureInserted(host, subscriptionType, value);
                } else {
                    listener.featureDeleted(host, subscriptionType, value);
                }
            }
        }
    }

    void notifyInstanceListeners(final Object clazzKey, final EObject instance, final boolean isInsertion) {
        for (final Entry<InstanceListener, Set<EClass>> entry : getInstanceListeners().getOrDefault(clazzKey, Collections.emptyMap()).entrySet()) {
            final InstanceListener listener = entry.getKey();
            for (final EClass subscriptionType : entry.getValue()) {
                if (isInsertion) {
                    listener.instanceInserted(subscriptionType, instance);
                } else {
                    listener.instanceDeleted(subscriptionType, instance);
                }
            }
        }
    }

    void notifyLightweightObservers(final EObject host, final EStructuralFeature feature,
            final Notification notification) {
        if (lightweightObservers.containsKey(host)) {
            Set<LightweightEObjectObserver> observers = lightweightObservers.get(host);
            for (final LightweightEObjectObserver observer : observers) {
                observer.notifyFeatureChanged(host, feature, notification);
            }
        }
    }

    @Override
    public void addBaseIndexChangeListener(EMFBaseIndexChangeListener listener) {
        Preconditions.checkArgument(listener != null, "Cannot add null listener!");
        baseIndexChangeListeners.add(listener);
    }

    @Override
    public void removeBaseIndexChangeListener(EMFBaseIndexChangeListener listener) {
        Preconditions.checkArgument(listener != null, "Cannot remove null listener!");
        baseIndexChangeListeners.remove(listener);
    }

    @Override
    public boolean addIndexingErrorListener(IEMFIndexingErrorListener listener) {
        return errorListeners.add(listener);
    }

    @Override
    public boolean removeIndexingErrorListener(IEMFIndexingErrorListener listener) {
        return errorListeners.remove(listener);
    }

    protected void processingFatal(final Throwable ex, final String task) {
        notifyFatalListener(logTaskFormat(task), ex);
    }

    protected void processingError(final Throwable ex, final String task) {
        notifyErrorListener(logTaskFormat(task), ex);
    }

    public void notifyErrorListener(String message, Throwable t) {
        logger.error(message, t);
        for (IEMFIndexingErrorListener listener : new ArrayList<>(errorListeners)) {
            listener.error(message, t);
        }
    }

    public void notifyFatalListener(String message, Throwable t) {
        logger.fatal(message, t);
        for (IEMFIndexingErrorListener listener : new ArrayList<>(errorListeners)) {
            listener.fatal(message, t);
        }
    }

    protected String logTaskFormat(final String task) {
        return "VIATRA Query encountered an error in processing the EMF model. " + "This happened while trying to "
                + task;
    }

    protected void considerForExpansion(EObject obj) {
        if (expansionAllowed) {
            Resource eResource = obj.eResource();
            if (eResource != null && eResource.getResourceSet() == null) {
                expandToAdditionalRoot(eResource);
            }
        }
    }

    protected void expandToAdditionalRoot(Notifier root) {
        if (modelRoots.contains(root))
            return;

        if (root instanceof ResourceSet) {
            expansionAllowed = true;
        } else if (root instanceof Resource) {
            IBaseIndexResourceFilter resourceFilter = baseIndexOptions.getResourceFilterConfiguration();
            if (resourceFilter != null && resourceFilter.isResourceFiltered((Resource) root))
                return;
        } else { // root instanceof EObject
            traversalDescendsAlongCrossResourceContainment = true;
        }
        final IBaseIndexObjectFilter objectFilter = baseIndexOptions.getObjectFilterConfiguration();
        if (objectFilter != null && objectFilter.isFiltered(root))
            return;

        // no veto by filters
        modelRoots.add(root);
        contentAdapter.addAdapter(root);
        notifyBaseIndexChangeListeners();
    }

    /**
     * @return the expansionAllowed
     */
    public boolean isExpansionAllowed() {
        return expansionAllowed;
    }

    public boolean traversalDescendsAlongCrossResourceContainment() {
        return traversalDescendsAlongCrossResourceContainment;
    }

    /**
     * @return the directlyObservedClasses
     */
    public Set<Object> getDirectlyObservedClassesInternal() {
        return directlyObservedClasses.keySet();
    }

    boolean isObservedInternal(Object clazzKey) {
        return isInWildcardMode() || getAllObservedClassesInternal().containsKey(clazzKey);
    }

    /**
     * Add the given item the map with the given indexing level if it wasn't already added with a higher level.
     * @param level non-null
     * @return whether actually changed
     */
    protected static <V> boolean putIntoMapMerged(Map<V, IndexingLevel> map, V key, IndexingLevel level) {
        IndexingLevel l = map.get(key);
        IndexingLevel merged = level.merge(l);
        if (merged != l) {
            map.put(key, merged);
            return true;
        } else {
            return false;
        }
    }

    /**
     * @return true if actually changed
     */
    protected boolean addObservedClassesInternal(Object eClassKey, IndexingLevel level) {
        boolean changed = putIntoMapMerged(allObservedClasses, eClassKey, level);
        if (!changed) return false;

        final Set<Object> subTypes = metaStore.getSubTypeMap().get(eClassKey);
        if (subTypes != null) {
            for (Object subType : subTypes) {
                /*
                 * It is necessary to check if the class has already been added with a higher indexing level as in case
                 * of multiple inheritance, a subclass may be registered for statistics only but full indexing may be
                 * required via one of its super classes.
                 */
                putIntoMapMerged(allObservedClasses, subType, level);
            }
        }
        return true;
    }

    /**
     * not just the directly observed classes, but also their known subtypes
     */
    public Map<Object, IndexingLevel> getAllObservedClassesInternal() {
        if (allObservedClasses == null) {
            allObservedClasses = new HashMap<Object, IndexingLevel>();
            for (Entry<Object, IndexingLevel> entry : directlyObservedClasses.entrySet()) {
                Object eClassKey = entry.getKey();
                IndexingLevel level = entry.getValue();
                addObservedClassesInternal(eClassKey, level);
            }
        }
        return allObservedClasses;
    }

    /**
     * @return the instanceListeners
     */
    Map<Object, Map<InstanceListener, Set<EClass>>> getInstanceListeners() {
        if (instanceListeners == null) {
            instanceListeners = CollectionsFactory.createMap();
            for (Entry<InstanceListener, Set<EClass>> subscription : subscribedInstanceListeners.entrySet()) {
                final InstanceListener listener = subscription.getKey();
                for (EClass subscriptionType : subscription.getValue()) {
                    final Object superElementTypeKey = toKey(subscriptionType);
                    addInstanceListenerInternal(listener, subscriptionType, superElementTypeKey);
                    final Set<Object> subTypeKeys = metaStore.getSubTypeMap().get(superElementTypeKey);
                    if (subTypeKeys != null)
                        for (Object subTypeKey : subTypeKeys) {
                            addInstanceListenerInternal(listener, subscriptionType, subTypeKey);
                        }
                }
            }
        }
        return instanceListeners;
    }

    Map<Object, Map<InstanceListener, Set<EClass>>> peekInstanceListeners() {
        return instanceListeners;
    }

    void addInstanceListenerInternal(final InstanceListener listener, EClass subscriptionType,
            final Object elementTypeKey) {
        Set<EClass> subscriptionTypes = instanceListeners
                .computeIfAbsent(elementTypeKey, k -> CollectionsFactory.createMap())
                .computeIfAbsent(listener, k -> CollectionsFactory.createSet());
        subscriptionTypes.add(subscriptionType);
    }

    /**
     * @return the featureListeners
     */
    Map<Object, Map<FeatureListener, Set<EStructuralFeature>>> getFeatureListeners() {
        if (featureListeners == null) {
            featureListeners = CollectionsFactory.createMap();
            for (Entry<FeatureListener, Set<EStructuralFeature>> subscription : subscribedFeatureListeners.entrySet()) {
                final FeatureListener listener = subscription.getKey();
                for (EStructuralFeature subscriptionType : subscription.getValue()) {
                    final Object elementTypeKey = toKey(subscriptionType);
                    addFeatureListenerInternal(listener, subscriptionType, elementTypeKey);
                }
            }
        }
        return featureListeners;
    }

    void addFeatureListenerInternal(final FeatureListener listener, EStructuralFeature subscriptionType,
            final Object elementTypeKey) {
        Set<EStructuralFeature> subscriptionTypes = featureListeners
                .computeIfAbsent(elementTypeKey, k -> CollectionsFactory.createMap())
                .computeIfAbsent(listener, k -> CollectionsFactory.createSet());
        subscriptionTypes.add(subscriptionType);
    }

    /**
     * @return the dataTypeListeners
     */
    Map<Object, Map<DataTypeListener, Set<EDataType>>> getDataTypeListeners() {
        if (dataTypeListeners == null) {
            dataTypeListeners = CollectionsFactory.createMap();
            for (Entry<DataTypeListener, Set<EDataType>> subscription : subscribedDataTypeListeners.entrySet()) {
                final DataTypeListener listener = subscription.getKey();
                for (EDataType subscriptionType : subscription.getValue()) {
                    final Object elementTypeKey = toKey(subscriptionType);
                    addDatatypeListenerInternal(listener, subscriptionType, elementTypeKey);
                }
            }
        }
        return dataTypeListeners;
    }

    void addDatatypeListenerInternal(final DataTypeListener listener, EDataType subscriptionType,
            final Object elementTypeKey) {
        Set<EDataType> subscriptionTypes = dataTypeListeners
                .computeIfAbsent(elementTypeKey, k -> CollectionsFactory.createMap())
                .computeIfAbsent(listener, k -> CollectionsFactory.createSet());
        subscriptionTypes.add(subscriptionType);
    }

    public void registerObservedTypes(Set<EClass> classes, Set<EDataType> dataTypes,
            Set<? extends EStructuralFeature> features) {
        registerObservedTypes(classes, dataTypes, features, IndexingLevel.FULL);
    }

    @Override
    public void registerObservedTypes(Set<EClass> classes, Set<EDataType> dataTypes,
            Set<? extends EStructuralFeature> features, final IndexingLevel level) {
        if (isRegistrationNecessary(level) && (classes != null || features != null || dataTypes != null)) {
            final Set<Object> resolvedFeatures = resolveFeaturesToKey(features);
            final Set<Object> resolvedClasses = resolveClassifiersToKey(classes);
            final Set<Object> resolvedDatatypes = resolveClassifiersToKey(dataTypes);

            try {
                coalesceTraversals(() -> {
                    Function<Object, IndexingLevel> f = input -> level;
                    delayedFeatures.putAll(resolvedFeatures.stream().collect(Collectors.toMap(identity(), f)));
                    delayedDataTypes.putAll(resolvedDatatypes.stream().collect(Collectors.toMap(identity(), f)));
                    delayedClasses.putAll(resolvedClasses.stream().collect(Collectors.toMap(identity(), f)));
                });
            } catch (InvocationTargetException ex) {
                processingFatal(ex.getCause(), "register en masse the observed EClasses " + resolvedClasses
                        + " and EDatatypes " + resolvedDatatypes + " and EStructuralFeatures " + resolvedFeatures);
            } catch (Exception ex) {
                processingFatal(ex, "register en masse the observed EClasses " + resolvedClasses + " and EDatatypes "
                        + resolvedDatatypes + " and EStructuralFeatures " + resolvedFeatures);
            }
        }
    }

    @Override
    public void unregisterObservedTypes(Set<EClass> classes, Set<EDataType> dataTypes,
            Set<? extends EStructuralFeature> features) {
        unregisterEClasses(classes);
        unregisterEDataTypes(dataTypes);
        unregisterEStructuralFeatures(features);
    }

    @Override
    public void registerEStructuralFeatures(Set<? extends EStructuralFeature> features, final IndexingLevel level) {
        if (isRegistrationNecessary(level) && features != null) {
            final Set<Object> resolved = resolveFeaturesToKey(features);

            try {
                coalesceTraversals(() -> resolved.forEach(o -> delayedFeatures.put(o, level)));
            } catch (InvocationTargetException ex) {
                processingFatal(ex.getCause(), "register the observed EStructuralFeatures: " + resolved);
            } catch (Exception ex) {
                processingFatal(ex, "register the observed EStructuralFeatures: " + resolved);
            }
        }
    }

    @Override
    public void unregisterEStructuralFeatures(Set<? extends EStructuralFeature> features) {
        if (isRegistrationNecessary(IndexingLevel.FULL) && features != null) {
            final Set<Object> resolved = resolveFeaturesToKey(features);
            ensureNoListeners(resolved, getFeatureListeners());
            observedFeatures.keySet().removeAll(resolved);
            delayedFeatures.keySet().removeAll(resolved);
            for (Object f : resolved) {
                instanceStore.forgetFeature(f);
                statsStore.removeType(f);
            }
        }
    }

    @Override
    public void registerEClasses(Set<EClass> classes, final IndexingLevel level) {
        if (isRegistrationNecessary(level) && classes != null) {
            final Set<Object> resolvedClasses = resolveClassifiersToKey(classes);

            try {
                coalesceTraversals(() -> resolvedClasses.forEach(o -> delayedClasses.put(o, level)));
            } catch (InvocationTargetException ex) {
                processingFatal(ex.getCause(), "register the observed EClasses: " + resolvedClasses);
            } catch (Exception ex) {
                processingFatal(ex, "register the observed EClasses: " + resolvedClasses);
            }
        }
    }

    /**
     * @return true if there is an actual change in the transitively computed observation levels,
     * warranting an actual traversal
     */
    protected boolean startObservingClasses(Map<Object, IndexingLevel> requestedClassObservations) {
        boolean warrantsTraversal = false;
        getAllObservedClassesInternal(); // pre-populate
        for (Entry<Object, IndexingLevel> request : requestedClassObservations.entrySet()) {
            if (putIntoMapMerged(directlyObservedClasses, request.getKey(), request.getValue())) {
                // maybe already observed for the sake of a supertype?
                if (addObservedClassesInternal(request.getKey(), request.getValue())) {
                    warrantsTraversal = true;
                };
            }
        }
        return warrantsTraversal;
    }

    @Override
    public void unregisterEClasses(Set<EClass> classes) {
        if (isRegistrationNecessary(IndexingLevel.FULL) && classes != null) {
            final Set<Object> resolved = resolveClassifiersToKey(classes);
            ensureNoListeners(resolved, getInstanceListeners());
            directlyObservedClasses.keySet().removeAll(resolved);
            allObservedClasses = null;
            delayedClasses.keySet().removeAll(resolved);
            for (Object c : resolved) {
                instanceStore.removeInstanceSet(c);
                statsStore.removeType(c);
            }
        }
    }

    @Override
    public void registerEDataTypes(Set<EDataType> dataTypes, final IndexingLevel level) {
        if (isRegistrationNecessary(level) && dataTypes != null) {
            final Set<Object> resolved = resolveClassifiersToKey(dataTypes);

            try {
                coalesceTraversals(() -> resolved.forEach(o -> delayedDataTypes.put(o, level)));
            } catch (InvocationTargetException ex) {
                processingFatal(ex.getCause(), "register the observed EDataTypes: " + resolved);
            } catch (Exception ex) {
                processingFatal(ex, "register the observed EDataTypes: " + resolved);
            }
        }
    }

    @Override
    public void unregisterEDataTypes(Set<EDataType> dataTypes) {
        if (isRegistrationNecessary(IndexingLevel.FULL) && dataTypes != null) {
            final Set<Object> resolved = resolveClassifiersToKey(dataTypes);
            ensureNoListeners(resolved, getDataTypeListeners());
            observedDataTypes.keySet().removeAll(resolved);
            delayedDataTypes.keySet().removeAll(resolved);
            for (Object dataType : resolved) {
                instanceStore.removeDataTypeMap(dataType);
                statsStore.removeType(dataType);
            }
        }
    }

    @Override
    public boolean isCoalescing() {
        return delayTraversals;
    }

    public void coalesceTraversals(final Runnable runnable) throws InvocationTargetException {
        coalesceTraversals(() -> {
            runnable.run();
            return null;
        });
    }

    @Override
    public <V> V coalesceTraversals(Callable<V> callable) throws InvocationTargetException {
        V finalResult = null;

        if (delayTraversals) { // reentrant case, no special action needed
            try {
                finalResult = callable.call();
            } catch (Exception e) {
                throw new InvocationTargetException(e);
            }
            return finalResult;
        }

        boolean firstRun = true;
        while (callable != null) { // repeat if post-processing needed

            try {
                delayTraversals = true;

                V result = callable.call();
                if (firstRun) {
                    firstRun = false;
                    finalResult = result;
                }

                // are there proxies left to be resolved? are we allowed to resolve them now?
                while ((!delayedProxyResolutions.isEmpty()) && resolutionDelayingResources.isEmpty()) {
                    // pop first entry
                    EObject toResolveObject = delayedProxyResolutions.distinctKeys().iterator().next();
                    EReference toResolveReference = delayedProxyResolutions.lookup(toResolveObject).iterator().next();
                    delayedProxyResolutions.removePair(toResolveObject, toResolveReference);

                    // see if we can resolve proxies
                    comprehension.tryResolveReference(toResolveObject, toResolveReference);
                }

                delayTraversals = false;
                callable = considerRevisit();
            } catch (Exception e) {
                // since this is a fatal error, it is OK if delayTraversals remains true,
                //  hence no need for a try-finally block

                notifyFatalListener(
                        "VIATRA Base encountered an error while traversing the EMF model to gather new information. ",
                        e);
                throw new InvocationTargetException(e);
            }
        }
        executeTraversalCallbacks();
        return finalResult;
    }

    protected <V> Callable<V> considerRevisit() {
        // has there been any requests for a retraversal at all?
        if (!delayedClasses.isEmpty() || !delayedFeatures.isEmpty() || !delayedDataTypes.isEmpty()) {
            // make copies of requested types so that
            // (a) original accumulators can be cleaned for the next cycle, also
            // (b) to remove entries that are already covered, or
            // (c) for the rare case that a coalesced traversal is invoked during visitation,
            //      e.g. by a derived feature implementation
            // initialize the collections empty (but with capacity), fill with new entries
            final Map<Object, IndexingLevel> toGatherClasses = new HashMap<Object, IndexingLevel>(delayedClasses.size());
            final Map<Object, IndexingLevel> toGatherFeatures = new HashMap<Object, IndexingLevel>(delayedFeatures.size());
            final Map<Object, IndexingLevel> toGatherDataTypes = new HashMap<Object, IndexingLevel>(delayedDataTypes.size());

            for (Entry<Object, IndexingLevel> requested : delayedFeatures.entrySet()) {
                Object typekey = requested.getKey();
                IndexingLevel old = observedFeatures.get(typekey);
                IndexingLevel merged = requested.getValue().merge(old);
                if (merged != old) toGatherFeatures.put(typekey, merged);
            }
            for (Entry<Object, IndexingLevel> requested : delayedClasses.entrySet()) {
                Object typekey = requested.getKey();
                IndexingLevel old = directlyObservedClasses.get(typekey);
                IndexingLevel merged = requested.getValue().merge(old);
                if (merged != old) toGatherClasses.put(typekey, merged);
            }
            for (Entry<Object, IndexingLevel> requested : delayedDataTypes.entrySet()) {
                Object typekey = requested.getKey();
                IndexingLevel old = observedDataTypes.get(typekey);
                IndexingLevel merged = requested.getValue().merge(old);
                if (merged != old) toGatherDataTypes.put(typekey, merged);
            }

            delayedClasses.clear();
            delayedFeatures.clear();
            delayedDataTypes.clear();

            // check if the filtered request sets are empty
            //      - could be false alarm if we already observe all of them
            if (!toGatherClasses.isEmpty() || !toGatherFeatures.isEmpty() || !toGatherDataTypes.isEmpty()) {
                final HashMap<Object, IndexingLevel> oldClasses = new HashMap<Object, IndexingLevel>(
                        directlyObservedClasses);

                /* Instance indexing would add extra entries to the statistics store, so we have to clean the
                 * appropriate entries. If no re-traversal is required, it is detected earlier; at this point we
                 * only have to consider the target indexing level. See bug
                 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=518356 for more details.
                 *
                 * This has to be executed before the old observed types are updated to check whether the indexing level increased.
                 *
                 * Technically, the statsStore cleanup seems only necessary for EDataTypes; otherwise everything
                 * works as expected, but it seems a better idea to do the cleanup for all types in the same way */
                toGatherClasses.forEach((key, value) -> {
                    IndexingLevel oldIndexingLevel = getIndexingLevel(metaStore.getKnownClassifierForKey(key));
                    if (value.hasInstances() && oldIndexingLevel.hasStatistics() && !oldIndexingLevel.hasInstances()) {
                        statsStore.removeType(key);
                    }

                });
                toGatherFeatures.forEach((key, value) -> {
                    IndexingLevel oldIndexingLevel = getIndexingLevel(metaStore.getKnownFeatureForKey(key));
                    if (value.hasInstances() && oldIndexingLevel.hasStatistics() && !oldIndexingLevel.hasInstances()) {
                        statsStore.removeType(key);
                    }

                });
                toGatherDataTypes.forEach((key, value) -> {
                    IndexingLevel oldIndexingLevel = getIndexingLevel(metaStore.getKnownClassifierForKey(key));
                    if (value.hasInstances() && oldIndexingLevel.hasStatistics() && !oldIndexingLevel.hasInstances()) {
                        statsStore.removeType(key);
                    }

                });

                // Are there new classes to be observed that are not available via superclasses?
                //      (at sufficient level)
                // if yes, model traversal needed
                // if not, index can be updated without retraversal
                boolean classesWarrantTraversal = startObservingClasses(toGatherClasses);
                observedDataTypes.putAll(toGatherDataTypes);
                observedFeatures.putAll(toGatherFeatures);


                // So, is an actual traversal needed, or are we done?
                if (classesWarrantTraversal || !toGatherFeatures.isEmpty() || !toGatherDataTypes.isEmpty()) {
                    // repeat the cycle with this visit
                    final NavigationHelperVisitor visitor = initTraversingVisitor(toGatherClasses, toGatherFeatures, toGatherDataTypes, oldClasses);

                    return new Callable<V>() {
                        @Override
                        public V call() throws Exception {
                            // temporarily ignoring RESOLVE on these features, as they were not observed before
                            ignoreResolveNotificationFeatures.addAll(toGatherFeatures.keySet());
                            try {
                                traverse(visitor);
                            } finally {
                                ignoreResolveNotificationFeatures.removeAll(toGatherFeatures.keySet());
                            }
                            return null;
                        }
                    };

                }
            }
        }

        return null; // no callable -> no further action
    }

    protected void executeTraversalCallbacks() throws InvocationTargetException{
        final Runnable[] callbacks = traversalCallbacks.toArray(new Runnable[traversalCallbacks.size()]);
        traversalCallbacks.clear();
        if (callbacks.length > 0){
            coalesceTraversals(() -> Arrays.stream(callbacks).forEach(Runnable::run));
        }
    }

    protected void traverse(final NavigationHelperVisitor visitor) {
        // Cloning model roots avoids a concurrent modification exception
        for (Notifier root : new HashSet<Notifier>(modelRoots)) {
            comprehension.traverseModel(visitor, root);
        }
        notifyBaseIndexChangeListeners();
    }

    /**
     * Returns a stream of model roots registered to the navigation helper instance
     * @since 2.3
     */
    protected Stream<Notifier> getModelRoots() {
        return modelRoots.stream();
    }

    @Override
    public void addRoot(Notifier emfRoot) {
        addRootInternal(emfRoot);
    }

    /**
     * Supports removing model roots
     * </p>
     * Note: for now this API is considered experimental thus it is not added to the {@link NavigationHelper} interface.
     * @since 2.3
     */
    protected void removeRoot(Notifier root) {
        if (!((root instanceof EObject) || (root instanceof Resource) || (root instanceof ResourceSet))) {
            throw new ViatraBaseException(ViatraBaseException.INVALID_EMFROOT);
        }

        if (!modelRoots.contains(root))
            return;

        if (root instanceof Resource) {
            IBaseIndexResourceFilter resourceFilter = getBaseIndexOptions().getResourceFilterConfiguration();
            if (resourceFilter != null && resourceFilter.isResourceFiltered((Resource) root))
                return;
        }
        final IBaseIndexObjectFilter objectFilter = getBaseIndexOptions().getObjectFilterConfiguration();
        if (objectFilter != null && objectFilter.isFiltered(root))
            return;

        // no veto by filters
        modelRoots.remove(root);
        contentAdapter.removeAdapter(root);
        notifyBaseIndexChangeListeners();
    }

    @Override
    public <T extends EObject> void cheapMoveTo(T element, EList<T> targetContainmentReferenceList) {
        if (element.eAdapters().contains(contentAdapter)
                && targetContainmentReferenceList instanceof NotifyingList<?>) {
            final Object listNotifier = ((NotifyingList<?>) targetContainmentReferenceList).getNotifier();
            if (listNotifier instanceof Notifier && ((Notifier) listNotifier).eAdapters().contains(contentAdapter)) {
                contentAdapter.ignoreInsertionAndDeletion = element;
                try {
                    targetContainmentReferenceList.add(element);
                } finally {
                    contentAdapter.ignoreInsertionAndDeletion = null;
                }
            } else {
                targetContainmentReferenceList.add(element);
            }
        } else {
            targetContainmentReferenceList.add(element);
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public void cheapMoveTo(EObject element, EObject parent, EReference containmentFeature) {
        metaStore.maintainMetamodel(containmentFeature);
        if (containmentFeature.isMany())
            cheapMoveTo(element, (EList) parent.eGet(containmentFeature));
        else if (element.eAdapters().contains(contentAdapter) && parent.eAdapters().contains(contentAdapter)) {
            contentAdapter.ignoreInsertionAndDeletion = element;
            try {
                parent.eSet(containmentFeature, element);
            } finally {
                contentAdapter.ignoreInsertionAndDeletion = null;
            }
        } else {
            parent.eSet(containmentFeature, element);
        }
    }

    protected void addRootInternal(Notifier emfRoot) {
        if (!((emfRoot instanceof EObject) || (emfRoot instanceof Resource) || (emfRoot instanceof ResourceSet))) {
            throw new ViatraBaseException(ViatraBaseException.INVALID_EMFROOT);
        }
        expandToAdditionalRoot(emfRoot);
    }

    @Override
    public Set<EClass> getAllCurrentClasses() {
        return instanceStore.getAllCurrentClasses();
    }

    protected boolean isRegistrationNecessary(IndexingLevel level) {
        boolean inWildcardMode = isInWildcardMode(level);
        if (inWildcardMode && !loggedRegistrationMessage) {
            loggedRegistrationMessage = true;
            logger.warn("Type registration/unregistration not required in wildcard mode. This message will not be repeated for future occurences.");
        }
        return !inWildcardMode;
    }

    protected <X, Y> void ensureNoListeners(Set<Object> unobservedTypes,
            final Map<Object, Map<X, Set<Y>>> listenerRegistry) {
        if (!Collections.disjoint(unobservedTypes, listenerRegistry.keySet()))
            throw new IllegalStateException("Cannot unregister observed types for which there are active listeners");
    }

    protected void ensureNoListenersForDispose() {
        if (!(baseIndexChangeListeners.isEmpty() && subscribedFeatureListeners.isEmpty()
                && subscribedDataTypeListeners.isEmpty() && subscribedInstanceListeners.isEmpty()))
            throw new IllegalStateException("Cannot dispose while there are active listeners");
    }

    /**
     * Resamples the values of not well-behaving derived features if those features are also indexed.
     */
    @Override
    public void resampleDerivedFeatures() {
        // otherwise notifications are delivered anyway
        if (!baseIndexOptions.isTraverseOnlyWellBehavingDerivedFeatures()) {
            // get all required classes
            Set<EClass> allCurrentClasses = instanceStore.getAllCurrentClasses();
            Set<EStructuralFeature> featuresToSample = new HashSet<>();
            // collect features to sample
            for (EClass cls : allCurrentClasses) {
                EList<EStructuralFeature> features = cls.getEAllStructuralFeatures();
                for (EStructuralFeature f : features) {
                    // is feature only sampled?
                    if (comprehension.onlySamplingFeature(f)) {
                        featuresToSample.add(f);
                    }
                }
            }

            final EMFVisitor removalVisitor = contentAdapter.getVisitorForChange(false);
            final EMFVisitor insertionVisitor = contentAdapter.getVisitorForChange(true);

            // iterate on instances
            for (final EStructuralFeature f : featuresToSample) {
                EClass containingClass = f.getEContainingClass();
                processAllInstances(containingClass, (type, instance) ->
                    resampleFeatureValueForHolder(instance, f, insertionVisitor, removalVisitor));
            }
            notifyBaseIndexChangeListeners();
        }
    }

    protected void resampleFeatureValueForHolder(EObject source, EStructuralFeature feature,
            EMFVisitor insertionVisitor, EMFVisitor removalVisitor) {
        // traverse features and update value
        Object newValue = source.eGet(feature);
        Set<Object> oldValues = instanceStore.getOldValuesForHolderAndFeature(source, toKey(feature));
        if (feature.isMany()) {
            resampleManyFeatureValueForHolder(source, feature, newValue, oldValues, insertionVisitor, removalVisitor);
        } else {
            resampleSingleFeatureValueForHolder(source, feature, newValue, oldValues, insertionVisitor, removalVisitor);
        }

    }

    protected void resampleManyFeatureValueForHolder(EObject source, EStructuralFeature feature, Object newValue,
            Set<Object> oldValues, EMFVisitor insertionVisitor, EMFVisitor removalVisitor) {
        InternalEObject internalEObject = (InternalEObject) source;
        Collection<?> newValues = (Collection<?>) newValue;
        // add those that are in new but not in old
        Set<Object> newValueSet = new HashSet<Object>(newValues);
        newValueSet.removeAll(oldValues);
        // remove those that are in old but not in new
        oldValues.removeAll(newValues);
        if (!oldValues.isEmpty()) {
            for (Object ov : oldValues) {
                comprehension.traverseFeature(removalVisitor, source, feature, ov, null);
            }
            ENotificationImpl removeNotification = new ENotificationImpl(internalEObject, Notification.REMOVE_MANY,
                    feature, oldValues, null);
            notifyLightweightObservers(source, feature, removeNotification);
        }
        if (!newValueSet.isEmpty()) {
            for (Object nv : newValueSet) {
                comprehension.traverseFeature(insertionVisitor, source, feature, nv, null);
            }
            ENotificationImpl addNotification = new ENotificationImpl(internalEObject, Notification.ADD_MANY, feature,
                    null, newValueSet);
            notifyLightweightObservers(source, feature, addNotification);
        }
    }

    protected void resampleSingleFeatureValueForHolder(EObject source, EStructuralFeature feature, Object newValue,
            Set<Object> oldValues, EMFVisitor insertionVisitor, EMFVisitor removalVisitor) {
        InternalEObject internalEObject = (InternalEObject) source;
        Object oldValue = oldValues.stream().findFirst().orElse(null);
        if (!Objects.equals(oldValue, newValue)) {
            // value changed
            comprehension.traverseFeature(removalVisitor, source, feature, oldValue, null);
            comprehension.traverseFeature(insertionVisitor, source, feature, newValue, null);
            ENotificationImpl notification = new ENotificationImpl(internalEObject, Notification.SET, feature, oldValue,
                    newValue);
            notifyLightweightObservers(source, feature, notification);
        }
    }

    @Override
    public int countAllInstances(EClass type) {
        int result = 0;

        Object typeKey = toKey(type);
        Set<Object> subTypes = metaStore.getSubTypeMap().get(typeKey);
        if (subTypes != null) {
            for (Object subTypeKey : subTypes) {
                result += statsStore.countInstances(subTypeKey);
            }
        }
        result += statsStore.countInstances(typeKey);

        return result;
    }

    @Override
    public int countDataTypeInstances(EDataType dataType) {
        return statsStore.countInstances(toKey(dataType));
    }

    @Override
    public int countFeatureTargets(EObject seedSource, EStructuralFeature feature) {
        return featureData(feature).getDistinctValuesOfHolder(seedSource).size();
    }

    @Override
    public int countFeatures(EStructuralFeature feature) {
        return statsStore.countFeatures(toKey(feature));
    }

    protected IndexingLevel getIndexingLevel(Object type) {
        if (type instanceof EClass) {
            return getIndexingLevel((EClass)type);
        } else if (type instanceof EDataType) {
            return getIndexingLevel((EDataType)type);
        } else if (type instanceof EStructuralFeature) {
            return getIndexingLevel((EStructuralFeature)type);
        } else {
            throw new IllegalArgumentException("Unexpected type descriptor " + type.toString());
        }
    }

    @Override
    public IndexingLevel getIndexingLevel(EClass type) {
        Object key = toKey(type);
        IndexingLevel level = directlyObservedClasses.get(key);
        if (level == null) {
            level = delayedClasses.get(key);
        }
        // Wildcard mode is never null
        return wildcardMode.merge(level);
    }

    @Override
    public IndexingLevel getIndexingLevel(EDataType type) {
        Object key = toKey(type);
        IndexingLevel level = observedDataTypes.get(key);
        if (level == null) {
            level = delayedDataTypes.get(key);
        }
        // Wildcard mode is never null
        return wildcardMode.merge(level);
    }

    @Override
    public IndexingLevel getIndexingLevel(EStructuralFeature feature) {
        Object key = toKey(feature);
        IndexingLevel level = observedFeatures.get(key);
        if (level == null) {
            level = delayedFeatures.get(key);
        }
        // Wildcard mode is never null
        return wildcardMode.merge(level);
    }

    @Override
    public void executeAfterTraversal(final Runnable traversalCallback) throws InvocationTargetException {
        coalesceTraversals(() -> traversalCallbacks.add(traversalCallback));
    }

    /**
     * Records a non-exception incident such as faulty notifications.
     * Depending on the strictness setting {@link BaseIndexOptions#isStrictNotificationMode()} and log levels,
     * this may be treated as a fatal error, merely logged, or just ignored.
     *
     * @param msgProvider message supplier that only invoked if the message actually gets logged.
     *
     * @since 2.3
     */
    protected void logIncident(Supplier<String> msgProvider) {
        if (baseIndexOptions.isStrictNotificationMode()) {
            // This will cause e.g. query engine to become tainted
            String msg = msgProvider.get();
            notifyFatalListener(msg, new IllegalStateException(msg));
        } else {
            if (notificationErrorReported) {
                if (logger.isDebugEnabled()) {
                    String msg = msgProvider.get();
                    logger.debug(msg);
                }
            } else {
                notificationErrorReported = true;
                String msg = msgProvider.get();
                logger.error(msg);
            }
        }
    }
    boolean notificationErrorReported = false;


// DESIGNATED CUSTOMIZATION POINTS FOR SUBCLASSES

    /**
     * Point of customization, called by constructor
     * @since 2.3
     */
    protected NavigationHelperContentAdapter initContentAdapter() {
        switch (baseIndexOptions.getIndexerProfilerMode()) {
        case START_DISABLED:
            return new ProfilingNavigationHelperContentAdapter(this, false);
        case START_ENABLED:
            return new ProfilingNavigationHelperContentAdapter(this, true);
        case OFF:
        default:
            return new NavigationHelperContentAdapter(this);
        }
    }

    /**
     * Point of customization, called by constructor
     * @since 2.3
     */
    protected EMFBaseIndexStatisticsStore initStatStore() {
        return new EMFBaseIndexStatisticsStore(this, logger);
    }

    /**
     * Point of customization, called by constructor
     * @since 2.3
     */
    protected EMFBaseIndexInstanceStore initInstanceStore() {
        return new EMFBaseIndexInstanceStore(this, logger);
    }

    /**
     * Point of customization, called by constructor
     * @since 2.3
     */
   protected EMFBaseIndexMetaStore initMetaStore() {
        return new EMFBaseIndexMetaStore(this);
    }

   /**
    * Point of customization, called by constructor
    * @since 2.3
    */
    protected EMFModelComprehension initModelComprehension() {
        return new EMFModelComprehension(baseIndexOptions);
    }

    /**
     * Point of customization, called at runtime
     * @since 2.3
     */
    protected TraversingVisitor initTraversingVisitor(final Map<Object, IndexingLevel> toGatherClasses,
            final Map<Object, IndexingLevel> toGatherFeatures, final Map<Object, IndexingLevel> toGatherDataTypes,
            final Map<Object, IndexingLevel> oldClasses) {
        return new NavigationHelperVisitor.TraversingVisitor(this,
                toGatherFeatures, toGatherClasses, oldClasses, toGatherDataTypes);
    }



    /**
     * Point of customization, e.g. override to suppress
     * @since 2.3
     */
    protected void logIncidentAdapterRemoval(final Notifier notifier) {
        logIncident(() -> String.format("Erroneous removal of unattached notification adapter from notifier %s", notifier));
    }

    /**
     * Point of customization, e.g. override to suppress
     * @since 2.3
     */
    protected void logIncidentFeatureTupleInsertion(final Object value, final EObject holder, Object featureKey) {
        logIncident(() -> String.format(
                "Error: trying to add duplicate value %s to the unique feature %s of host object %s. This indicates some errors in underlying model representation.",
                value, featureKey, holder));
    }

    /**
     * Point of customization, e.g. override to suppress
     * @since 2.3
     */
   protected void logIncidentFeatureTupleRemoval(final Object value, final EObject holder, Object featureKey) {
       logIncident(() -> String.format(
                "Error: trying to remove duplicate value %s from the unique feature %s of host object %s. This indicates some errors in underlying model representation.",
                value, featureKey, holder));
    }

   /**
    * Point of customization, e.g. override to suppress
    * @since 2.3
    */
    protected void logIncidentInstanceInsertion(final Object keyClass, final EObject value) {
        logIncident(() -> String.format("Notification received to index %s as a %s, but it already exists in the index. This indicates some errors in underlying model representation.", value, keyClass));
    }

    /**
     * Point of customization, e.g. override to suppress
     * @since 2.3
     */
    protected void logIncidentInstanceRemoval(final Object keyClass, final EObject value) {
        logIncident(() -> String.format("Notification received to remove %s as a %s, but it is missing from the index. This indicates some errors in underlying model representation.", value, keyClass));
    }

    /**
     * Point of customization, e.g. override to suppress
     * @since 2.3
     */
   protected void logIncidentStatRemoval(Object key) {
        logIncident(() -> String.format("No instances of %s is registered before calling removeInstance method.", key));
    }


}
