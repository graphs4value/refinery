/*******************************************************************************
 * Copyright (c) 2010-2015, Bergmann Gabor, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.viatra.runtime.emf;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import tools.refinery.viatra.runtime.base.api.DataTypeListener;
import tools.refinery.viatra.runtime.base.api.FeatureListener;
import tools.refinery.viatra.runtime.base.api.IndexingLevel;
import tools.refinery.viatra.runtime.base.api.InstanceListener;
import tools.refinery.viatra.runtime.base.api.NavigationHelper;
import tools.refinery.viatra.runtime.emf.types.EClassTransitiveInstancesKey;
import tools.refinery.viatra.runtime.emf.types.EClassUnscopedTransitiveInstancesKey;
import tools.refinery.viatra.runtime.emf.types.EDataTypeInSlotsKey;
import tools.refinery.viatra.runtime.emf.types.EStructuralFeatureInstancesKey;
import tools.refinery.viatra.runtime.matchers.context.AbstractQueryRuntimeContext;
import tools.refinery.viatra.runtime.matchers.context.IInputKey;
import tools.refinery.viatra.runtime.matchers.context.IQueryMetaContext;
import tools.refinery.viatra.runtime.matchers.context.IQueryRuntimeContextListener;
import tools.refinery.viatra.runtime.matchers.context.IndexingService;
import tools.refinery.viatra.runtime.matchers.context.common.JavaTransitiveInstancesKey;
import tools.refinery.viatra.runtime.matchers.tuple.ITuple;
import tools.refinery.viatra.runtime.matchers.tuple.Tuple;
import tools.refinery.viatra.runtime.matchers.tuple.TupleMask;
import tools.refinery.viatra.runtime.matchers.tuple.Tuples;
import tools.refinery.viatra.runtime.matchers.util.Accuracy;

/**
 * The EMF-based runtime query context, backed by an IQBase NavigationHelper.
 * 
 * @author Bergmann Gabor
 *
 * <p> TODO: {@link #ensureIndexed(EClass)} may be inefficient if supertype already cached.
 * @since 1.4
 */
public class EMFQueryRuntimeContext extends AbstractQueryRuntimeContext {
    protected final NavigationHelper baseIndex;
    //private BaseIndexListener listener;
    
    protected final Map<EClass, EnumSet<IndexingService>> indexedClasses = new HashMap<>();
    protected final Map<EDataType, EnumSet<IndexingService>> indexedDataTypes = new HashMap<>();
    protected final Map<EStructuralFeature, EnumSet<IndexingService>> indexedFeatures = new HashMap<>();
    
    protected final EMFQueryMetaContext metaContext;

    protected Logger logger;

    private EMFScope emfScope;

    public EMFQueryRuntimeContext(NavigationHelper baseIndex, Logger logger, EMFScope emfScope) {
        this.baseIndex = baseIndex;
        this.logger = logger;
        this.metaContext = new EMFQueryMetaContext(emfScope);
        this.emfScope = emfScope;
    }
    
    public EMFScope getEmfScope() {
        return emfScope;
    }
    
    /**
     * Utility method to add an indexing service to a given key. Returns true if the requested service was
     * not present before this call.
     * @param map
     * @param key
     * @param service
     * @return
     */
    private static <K> boolean addIndexingService(Map<K, EnumSet<IndexingService>> map, K key, IndexingService service){
        EnumSet<IndexingService> current = map.get(key);
        if (current == null){
            current = EnumSet.of(service);
            map.put(key, current);
            return true;
        }else{
            return current.add(service);
        }
    }
    
    public void dispose() {
        //baseIndex.removeFeatureListener(indexedFeatures, listener);
        indexedFeatures.clear();
        //baseIndex.removeInstanceListener(indexedClasses, listener);
        indexedClasses.clear();
        //baseIndex.removeDataTypeListener(indexedDataTypes, listener);
        indexedDataTypes.clear();
        
        // No need to remove listeners, as NavHelper will be disposed imminently.
    }

    @Override
    public <V> V coalesceTraversals(Callable<V> callable) throws InvocationTargetException {
        return baseIndex.coalesceTraversals(callable);
    }
    
    @Override
    public boolean isCoalescing() {
        return baseIndex.isCoalescing();
    }
    
    @Override
    public IQueryMetaContext getMetaContext() {
        return metaContext;
    }
    
    @Override
    public void ensureIndexed(IInputKey key, IndexingService service) {
        ensureEnumerableKey(key);
        if (key instanceof EClassTransitiveInstancesKey) {
            EClass eClass = ((EClassTransitiveInstancesKey) key).getEmfKey();
            ensureIndexed(eClass, service);
        } else if (key instanceof EDataTypeInSlotsKey) {
            EDataType dataType = ((EDataTypeInSlotsKey) key).getEmfKey();
            ensureIndexed(dataType, service);
        } else if (key instanceof EStructuralFeatureInstancesKey) {
            EStructuralFeature feature = ((EStructuralFeatureInstancesKey) key).getEmfKey();
            ensureIndexed(feature, service);
        } else {
            illegalInputKey(key);
        }
    }
    
    /**
     * Retrieve the current registered indexing services for the given key. May not return null,
     * returns an empty set if no indexing is registered.
     * 
     * @since 1.4
     */
    protected EnumSet<IndexingService> getCurrentIndexingServiceFor(IInputKey key){
        ensureEnumerableKey(key);
        if (key instanceof EClassTransitiveInstancesKey) {
            EClass eClass = ((EClassTransitiveInstancesKey) key).getEmfKey();
            EnumSet<IndexingService> is = indexedClasses.get(eClass);
            return is == null ? EnumSet.noneOf(IndexingService.class) : is; 
        } else if (key instanceof EDataTypeInSlotsKey) {
            EDataType dataType = ((EDataTypeInSlotsKey) key).getEmfKey();
            EnumSet<IndexingService> is =  indexedDataTypes.get(dataType);
            return is == null ? EnumSet.noneOf(IndexingService.class) : is;
        } else if (key instanceof EStructuralFeatureInstancesKey) {
            EStructuralFeature feature = ((EStructuralFeatureInstancesKey) key).getEmfKey();
            EnumSet<IndexingService> is =  indexedFeatures.get(feature);
            return is == null ? EnumSet.noneOf(IndexingService.class) : is;
        } else {
            illegalInputKey(key);
            return EnumSet.noneOf(IndexingService.class);
        }
    }
    
    @Override
    public boolean isIndexed(IInputKey key, IndexingService service) {
        return getCurrentIndexingServiceFor(key).contains(service);
    }
    
    @Override
    public boolean containsTuple(IInputKey key, ITuple seed) {
        ensureValidKey(key);
        if (key instanceof JavaTransitiveInstancesKey) {
            Class<?> instanceClass = forceGetWrapperInstanceClass((JavaTransitiveInstancesKey) key);
            return instanceClass != null && instanceClass.isInstance(seed.get(0));
        } else if (key instanceof EClassUnscopedTransitiveInstancesKey) {
            EClass emfKey = ((EClassUnscopedTransitiveInstancesKey) key).getEmfKey();
            Object candidateInstance = seed.get(0);
            return candidateInstance instanceof EObject
                    && baseIndex.isInstanceOfUnscoped((EObject) candidateInstance, emfKey);
        } else {
            ensureIndexed(key, IndexingService.INSTANCES);
            if (key instanceof EClassTransitiveInstancesKey) {
                EClass eClass = ((EClassTransitiveInstancesKey) key).getEmfKey();
                // instance check not enough to satisfy scoping, must lookup from index
                Object candidateInstance = seed.get(0);
                return candidateInstance instanceof EObject 
                        && baseIndex.isInstanceOfScoped((EObject) candidateInstance, eClass);
            } else if (key instanceof EDataTypeInSlotsKey) {
                EDataType dataType = ((EDataTypeInSlotsKey) key).getEmfKey();
                return baseIndex.isInstanceOfDatatype(seed.get(0), dataType);
            } else if (key instanceof EStructuralFeatureInstancesKey) {
                EStructuralFeature feature = ((EStructuralFeatureInstancesKey) key).getEmfKey();
                Object sourceCandidate = seed.get(0);
                return sourceCandidate instanceof EObject 
                        && baseIndex.isFeatureInstance((EObject) sourceCandidate, seed.get(1), feature);
            } else {
                illegalInputKey(key);
                return false;
            }
        }
    }

    private Class<?> forceGetWrapperInstanceClass(JavaTransitiveInstancesKey key) {
        Class<?> instanceClass;
        try {
            instanceClass = key.forceGetWrapperInstanceClass();
        } catch (ClassNotFoundException e) {
            logger.error("Could not load instance class for type constraint " + key.getWrappedKey(), e);
            instanceClass = null;
        }
        return instanceClass;
    }
    
    @Override
    public Iterable<Tuple> enumerateTuples(IInputKey key, TupleMask seedMask, ITuple seed) {
        ensureIndexed(key, IndexingService.INSTANCES);
        final Collection<Tuple> result = new HashSet<Tuple>();
        
        if (key instanceof EClassTransitiveInstancesKey) {
            EClass eClass = ((EClassTransitiveInstancesKey) key).getEmfKey();
            
            if (seedMask.indices.length == 0) { // unseeded
                return baseIndex.getAllInstances(eClass).stream().map(wrapUnary).collect(Collectors.toSet());
            } else { // fully seeded
                Object seedInstance = seedMask.getValue(seed, 0);
                if (containsTuple(key, seed)) 
                    result.add(Tuples.staticArityFlatTupleOf(seedInstance));
            }
        } else if (key instanceof EDataTypeInSlotsKey) {
            EDataType dataType = ((EDataTypeInSlotsKey) key).getEmfKey();
            
            if (seedMask.indices.length == 0) { // unseeded
                return baseIndex.getDataTypeInstances(dataType).stream().map(wrapUnary).collect(Collectors.toSet());
            } else { // fully seeded
                Object seedInstance = seedMask.getValue(seed, 0);
                if (containsTuple(key, seed)) 
                    result.add(Tuples.staticArityFlatTupleOf(seedInstance));
            }
        } else if (key instanceof EStructuralFeatureInstancesKey) {
            EStructuralFeature feature = ((EStructuralFeatureInstancesKey) key).getEmfKey();
            
            boolean isSourceBound = false;
            int sourceIndex = -1;
            boolean isTargetBound = false;
            int targetIndex = -1;
            for (int i = 0; i < seedMask.getSize(); i++) {
                int index = seedMask.indices[i];
                if (index == 0) {
                    isSourceBound = true;
                    sourceIndex = i;
                } else if (index == 1) {
                    isTargetBound = true;
                    targetIndex = i;
                }
            }
            
            if (!isSourceBound && isTargetBound) { 
                final Object seedTarget = seed.get(targetIndex);
                final Set<EObject> results = baseIndex.findByFeatureValue(seedTarget, feature);
                return results.stream().map(obj -> Tuples.staticArityFlatTupleOf(obj, seedTarget)).collect(Collectors.toSet());
            } else if (isSourceBound && isTargetBound) { // fully seeded
                final Object seedSource = seed.get(sourceIndex);
                final Object seedTarget = seed.get(targetIndex);
                if (containsTuple(key, seed)) 
                    result.add(Tuples.staticArityFlatTupleOf(seedSource, seedTarget));
            } else if (!isSourceBound && !isTargetBound) { // fully unseeded
                baseIndex.processAllFeatureInstances(feature, (source, target) -> result.add(Tuples.staticArityFlatTupleOf(source, target)));
            } else if (isSourceBound && !isTargetBound) { 
                final Object seedSource = seed.get(sourceIndex);
                final Set<Object> results = baseIndex.getFeatureTargets((EObject) seedSource, feature);
                return results.stream().map(obj -> Tuples.staticArityFlatTupleOf(seedSource, obj)).collect(Collectors.toSet());
            } 
        } else {
            illegalInputKey(key);
        }
        
        
        return result;
    }

    private static Function<Object, Tuple> wrapUnary = Tuples::staticArityFlatTupleOf;

    @Override
    public Iterable<? extends Object> enumerateValues(IInputKey key, TupleMask seedMask, ITuple seed) {
        ensureIndexed(key, IndexingService.INSTANCES);
        
        if (key instanceof EClassTransitiveInstancesKey) {
            EClass eClass = ((EClassTransitiveInstancesKey) key).getEmfKey();
            
            if (seedMask.indices.length == 0) { // unseeded
                return baseIndex.getAllInstances(eClass);
            } else {
                // must be unseeded, this is enumerateValues after all!
                illegalEnumerateValues(seed.toImmutable());
            }
        } else if (key instanceof EDataTypeInSlotsKey) {
            EDataType dataType = ((EDataTypeInSlotsKey) key).getEmfKey();
            
            if (seedMask.indices.length == 0) { // unseeded
                return baseIndex.getDataTypeInstances(dataType);
            } else {
                // must be unseeded, this is enumerateValues after all!
                illegalEnumerateValues(seed.toImmutable());
            }
        } else if (key instanceof EStructuralFeatureInstancesKey) {
            EStructuralFeature feature = ((EStructuralFeatureInstancesKey) key).getEmfKey();
            
            boolean isSourceBound = false;
            int sourceIndex = -1;
            boolean isTargetBound = false;
            int targetIndex = -1;
            for (int i = 0; i < seedMask.getSize(); i++) {
                int index = seedMask.indices[i];
                if (index == 0) {
                    isSourceBound = true;
                    sourceIndex = i;
                } else if (index == 1) {
                    isTargetBound = true;
                    targetIndex = i;
                }
            }
            
            if (!isSourceBound && isTargetBound) { 
                Object seedTarget = seed.get(targetIndex);
                return baseIndex.findByFeatureValue(seedTarget, feature);
            } else if (isSourceBound && !isTargetBound) { 
                Object seedSource = seed.get(sourceIndex);
                return baseIndex.getFeatureTargets((EObject) seedSource, feature);
            } else {
                // must be singly unseeded, this is enumerateValues after all!
                illegalEnumerateValues(seed.toImmutable());
            }
        } else {
            illegalInputKey(key);
        }
        return null;
    }
    
    @Override
    public int countTuples(IInputKey key, TupleMask seedMask, ITuple seed) {
        ensureIndexed(key, IndexingService.STATISTICS);
        
        if (key instanceof EClassTransitiveInstancesKey) {
            EClass eClass = ((EClassTransitiveInstancesKey) key).getEmfKey();
            
            if (seedMask.indices.length == 0) { // unseeded
                return baseIndex.countAllInstances(eClass);
            } else { // fully seeded
                return (containsTuple(key, seed)) ? 1 : 0;
            }
        } else if (key instanceof EDataTypeInSlotsKey) {
            EDataType dataType = ((EDataTypeInSlotsKey) key).getEmfKey();
            
            if (seedMask.indices.length == 0) { // unseeded
                return baseIndex.countDataTypeInstances(dataType);
            } else { // fully seeded
                return (containsTuple(key, seed)) ? 1 : 0;
            }
        } else if (key instanceof EStructuralFeatureInstancesKey) {
            EStructuralFeature feature = ((EStructuralFeatureInstancesKey) key).getEmfKey();
            
            boolean isSourceBound = false;
            int sourceIndex = -1;
            boolean isTargetBound = false;
            int targetIndex = -1;
            for (int i = 0; i < seedMask.getSize(); i++) {
                int index = seedMask.indices[i];
                if (index == 0) {
                    isSourceBound = true;
                    sourceIndex = i;
                } else if (index == 1) {
                    isTargetBound = true;
                    targetIndex = i;
                }
            }
            
            if (!isSourceBound && isTargetBound) { 
                final Object seedTarget = seed.get(targetIndex);
                return baseIndex.findByFeatureValue(seedTarget, feature).size();
            } else if (isSourceBound && isTargetBound) { // fully seeded
                return (containsTuple(key, seed)) ? 1 : 0;
            } else if (!isSourceBound && !isTargetBound) { // fully unseeded
                return baseIndex.countFeatures(feature);
            } else if (isSourceBound && !isTargetBound) { 
                final Object seedSource = seed.get(sourceIndex);
                return baseIndex.countFeatureTargets((EObject) seedSource, feature);
            } 
        } else {
            illegalInputKey(key);
        }
        return 0;
    }
    
    
    /**
     * @since 2.1
     */
    @Override
    public Optional<Long> estimateCardinality(IInputKey key, TupleMask groupMask, Accuracy requiredAccuracy) {

        if (key instanceof EClassTransitiveInstancesKey) {
            EClass eClass = ((EClassTransitiveInstancesKey) key).getEmfKey();
            
            if (isIndexed(key, IndexingService.STATISTICS)) { // exact answer known
                if (groupMask.indices.length == 0) { // empty projection
                    return (0 != baseIndex.countAllInstances(eClass)) ? Optional.of(1L) : Optional.of(0L);
                } else { // unprojected
                    return Optional.of((long)baseIndex.countAllInstances(eClass));
                }                
            } else return Optional.empty(); // TODO use known supertype counts as upper, subtypes as lower bounds
            
        } else if (key instanceof EClassUnscopedTransitiveInstancesKey) {
            EClass eClass = ((EClassUnscopedTransitiveInstancesKey) key).getEmfKey();
            
            // can give only lower bound based on the scoped key
            if (Accuracy.BEST_LOWER_BOUND.atLeastAsPreciseAs(requiredAccuracy)) {
                return estimateCardinality(new EClassTransitiveInstancesKey(eClass), groupMask, requiredAccuracy);
            } else return Optional.empty();
            
        } else if (key instanceof EDataTypeInSlotsKey) {
            EDataType dataType = ((EDataTypeInSlotsKey) key).getEmfKey();
            
            if (isIndexed(key, IndexingService.STATISTICS)) {
                if (groupMask.indices.length == 0) { // empty projection
                    return (0 != baseIndex.countDataTypeInstances(dataType)) ? Optional.of(1L) : Optional.of(0L);
                } else { // unprojected
                    return Optional.of((long)baseIndex.countDataTypeInstances(dataType));
                }                
            } else return Optional.empty();
            
        } else if (key instanceof EStructuralFeatureInstancesKey) {
            EStructuralFeatureInstancesKey featureKey = (EStructuralFeatureInstancesKey) key;
            EStructuralFeature feature = featureKey.getEmfKey();
            

            boolean isSourceSelected = false;
            boolean isTargetSelected = false;
            for (int i = 0; i < groupMask.getSize(); i++) {
                int index = groupMask.indices[i];
                if (index == 0) {
                    isSourceSelected = true;
                } else if (index == 1) {
                    isTargetSelected = true;
                }
            }
            
            Optional<Long> sourceTypeUpperEstimate = 
                    estimateCardinality(metaContext.getSourceTypeKey(featureKey), 
                            TupleMask.identity(1), Accuracy.BEST_UPPER_BOUND);
            Optional<Long> targetTypeUpperEstimate = 
                    estimateCardinality(metaContext.getTargetTypeKey(featureKey), 
                            TupleMask.identity(1), Accuracy.BEST_UPPER_BOUND);                    

            if (!isSourceSelected && !isTargetSelected) { // empty projection
                if (isIndexed(key, IndexingService.STATISTICS)) { // we have exact node counts
                    return (0 == baseIndex.countFeatures(feature)) ? Optional.of(0L) : Optional.of(1L);
                } else { // we can still say 0 in a few cases
                    if (0 == sourceTypeUpperEstimate.orElse(-1L))
                        return Optional.of(0L);

                    if (0 == targetTypeUpperEstimate.orElse(-1L))
                        return Optional.of(0L);
                                        
                    return Optional.empty();
                }
                
            } else if (isSourceSelected && !isTargetSelected) { // count sources
                if (isIndexed(key, IndexingService.INSTANCES)) { // we have instances, therefore feature end counts
                    return Optional.of((long)(baseIndex.getHoldersOfFeature(feature).size()));
                } else if (metaContext.isFeatureMultiplicityToOne(feature) && 
                        isIndexed(key, IndexingService.STATISTICS)) { // count of edges = count of sources due to func. dep.
                    return Optional.of((long)(baseIndex.countFeatures(feature)));
                } else if (Accuracy.BEST_UPPER_BOUND.atLeastAsPreciseAs(requiredAccuracy)) { 
                    // upper bound by source type
                    Optional<Long> estimate = sourceTypeUpperEstimate;
                    // total edge counts are another upper bound (even if instances are unindexed) 
                    if (isIndexed(key, IndexingService.STATISTICS)) { 
                        estimate = Optional.of(Math.min(
                                baseIndex.countFeatures(feature), 
                                estimate.orElse(Long.MAX_VALUE)));
                    }
                    return estimate;
                } else return Optional.empty();
                
            } else if (!isSourceSelected /*&& isTargetSelected*/) { // count targets
                if (isIndexed(key, IndexingService.INSTANCES)) { // we have instances, therefore feature end counts
                    return Optional.of((long)(baseIndex.getValuesOfFeature(feature).size()));
                } else if (metaContext.isFeatureMultiplicityOneTo(feature) && 
                        isIndexed(key, IndexingService.STATISTICS)) { // count of edges = count of targets due to func. dep.
                    return Optional.of((long)(baseIndex.countFeatures(feature)));
               } else if (Accuracy.BEST_UPPER_BOUND.atLeastAsPreciseAs(requiredAccuracy)) { // upper bound by target type
                   // upper bound by target type
                   Optional<Long> estimate = targetTypeUpperEstimate;                    
                   // total edge counts are another upper bound (even if instances are unindexed) 
                   if (isIndexed(key, IndexingService.STATISTICS)) { 
                       estimate = Optional.of(Math.min(
                               baseIndex.countFeatures(feature), 
                               estimate.orElse(Long.MAX_VALUE)));
                   }
                   return estimate;
                } else return Optional.empty();
                
            } else { // (isSourceSelected && isTargetSelected) // count edges
                if (isIndexed(key, IndexingService.STATISTICS)) { // we have exact edge counts
                    return Optional.of((long)baseIndex.countFeatures(feature));
                } else if (Accuracy.BEST_UPPER_BOUND.atLeastAsPreciseAs(requiredAccuracy)) { // overestimates may still be available
                    Optional<Long> estimate = // trivial upper bound: product of source & target type sizes (if available)
                            (sourceTypeUpperEstimate.isPresent() && targetTypeUpperEstimate.isPresent()) ?
                                    Optional.of(
                                            ((long)sourceTypeUpperEstimate.get()) * targetTypeUpperEstimate.get()
                                    ) : Optional.empty();
                                    
                    if (metaContext.isFeatureMultiplicityToOne(feature) && sourceTypeUpperEstimate.isPresent()) { 
                        // upper bounded by source type due to func. dep.              
                        estimate = Optional.of(Math.min(
                                sourceTypeUpperEstimate.get(), 
                                estimate.orElse(Long.MAX_VALUE)));
                    }
                    if (metaContext.isFeatureMultiplicityOneTo(feature) && targetTypeUpperEstimate.isPresent()) { 
                        // upper bounded by target type due to func. dep.              
                        estimate = Optional.of(Math.min(
                                targetTypeUpperEstimate.get(), 
                                estimate.orElse(Long.MAX_VALUE)));
                    }
                    
                    return estimate;
                } else return Optional.empty();
            }
            
        } else {
            return Optional.empty();
        }
    }
    
    /**
     * @since 2.1
     */
    @Override
    public Optional<Double> estimateAverageBucketSize(IInputKey key, TupleMask groupMask, Accuracy requiredAccuracy) {
        // smart handling of special cases
        if (key instanceof EStructuralFeatureInstancesKey) {
            EStructuralFeatureInstancesKey featureKey = (EStructuralFeatureInstancesKey) key;
            EStructuralFeature feature = featureKey.getEmfKey();

            // special treatment for edge navigation
            if (1 == groupMask.getSize()) {
                if (0 == groupMask.indices[0] && metaContext.isFeatureMultiplicityToOne(feature)) { // count targets per source
                    return Optional.of(1.0);
                } else if (1 == groupMask.indices[0] && metaContext.isFeatureMultiplicityOneTo(feature)) { // count sources per target
                    return Optional.of(1.0);
                }
            }
        }
        
        // keep the default behaviour
        return super.estimateAverageBucketSize(key, groupMask, requiredAccuracy);
    }
    
    
    public void ensureEnumerableKey(IInputKey key) {
        ensureValidKey(key);
        if (! metaContext.isEnumerable(key))
            throw new IllegalArgumentException("Key is not enumerable: " + key);
        
    }

    public void ensureValidKey(IInputKey key) {
        metaContext.ensureValidKey(key);
    }
    public void illegalInputKey(IInputKey key) {
        metaContext.illegalInputKey(key);
    }
    public void illegalEnumerateValues(Tuple seed) {
        throw new IllegalArgumentException("Must have exactly one unseeded element in enumerateValues() invocation, received instead: " + seed);
    }
    
    /**
     * @since 1.4
     */
    public void ensureIndexed(EClass eClass, IndexingService service) {
        if (addIndexingService(indexedClasses, eClass, service)) {
            final Set<EClass> newClasses = Collections.singleton(eClass);
            IndexingLevel level = IndexingLevel.toLevel(service);
            if (!baseIndex.getIndexingLevel(eClass).providesLevel(level)) {
                baseIndex.registerEClasses(newClasses, level);
            }
            //baseIndex.addInstanceListener(newClasses, listener);
        }
    }
    
    /**
     * @since 1.4
     */
    public void ensureIndexed(EDataType eDataType, IndexingService service) {
        if (addIndexingService(indexedDataTypes, eDataType, service)) {
            final Set<EDataType> newDataTypes = Collections.singleton(eDataType);
            IndexingLevel level = IndexingLevel.toLevel(service);
            if (!baseIndex.getIndexingLevel(eDataType).providesLevel(level)) {
                baseIndex.registerEDataTypes(newDataTypes, level);
            }
            //baseIndex.addDataTypeListener(newDataTypes, listener);
        }
    }
    
    /**
     * @since 1.4
     */
    public void ensureIndexed(EStructuralFeature feature, IndexingService service) {
        if (addIndexingService(indexedFeatures, feature, service)) {
            final Set<EStructuralFeature> newFeatures = Collections.singleton(feature);
            IndexingLevel level = IndexingLevel.toLevel(service);
            if (!baseIndex.getIndexingLevel(feature).providesLevel(level)) {
                baseIndex.registerEStructuralFeatures(newFeatures, level);
            }
            //baseIndex.addFeatureListener(newFeatures, listener);
        }
    }
    

    
    // UPDATE HANDLING SECTION 
    
    /**
     * Abstract internal listener wrapper for a {@link IQueryRuntimeContextListener}. 
     * Due to the overridden equals/hashCode(), it is safe to create a new instance for the same listener.
     * 
     * @author Bergmann Gabor
     */
    private abstract static class ListenerAdapter { 
        IQueryRuntimeContextListener listener;
        Tuple seed;
        /**
         * @param listener
         * @param seed must be non-null
         */
        public ListenerAdapter(IQueryRuntimeContextListener listener, Object... seed) {
            this.listener = listener;
            this.seed = Tuples.flatTupleOf(seed);
        }
                
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result
                    + ((listener == null) ? 0 : listener.hashCode());
            result = prime * result + ((seed == null) ? 0 : seed.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (!(obj.getClass().equals(this.getClass())))
                return false;
            ListenerAdapter other = (ListenerAdapter) obj;
            if (listener == null) {
                if (other.listener != null)
                    return false;
            } else if (!listener.equals(other.listener))
                return false;
            if (seed == null) {
                if (other.seed != null)
                    return false;
            } else if (!seed.equals(other.seed))
                return false;
            return true;
        }


        @Override
        public String toString() {
            return "Wrapped<Seed:" + seed + ">#" + listener;
        }
        
        
    }
    private static class EClassTransitiveInstancesAdapter extends ListenerAdapter implements InstanceListener {
        private Object seedInstance;
        public EClassTransitiveInstancesAdapter(IQueryRuntimeContextListener listener, Object seedInstance) {
            super(listener, seedInstance);
            this.seedInstance = seedInstance;
        }
        @Override
        public void instanceInserted(EClass clazz, EObject instance) {
            if (seedInstance != null && !seedInstance.equals(instance)) return;
            listener.update(new EClassTransitiveInstancesKey(clazz), 
                    Tuples.staticArityFlatTupleOf(instance), true);
        }
        @Override
        public void instanceDeleted(EClass clazz, EObject instance) {
            if (seedInstance != null && !seedInstance.equals(instance)) return;
            listener.update(new EClassTransitiveInstancesKey(clazz), 
                    Tuples.staticArityFlatTupleOf(instance), false);
        }    	
    }
    private static class EDataTypeInSlotsAdapter extends ListenerAdapter implements DataTypeListener {
        private Object seedValue;
        public EDataTypeInSlotsAdapter(IQueryRuntimeContextListener listener, Object seedValue) {
            super(listener, seedValue);
            this.seedValue = seedValue;
        }
        @Override
        public void dataTypeInstanceInserted(EDataType type, Object instance,
                boolean firstOccurrence) {
            if (firstOccurrence) {
                if (seedValue != null && !seedValue.equals(instance)) return;
                listener.update(new EDataTypeInSlotsKey(type), 
                        Tuples.staticArityFlatTupleOf(instance), true);
            }
        }
        @Override
        public void dataTypeInstanceDeleted(EDataType type, Object instance,
                boolean lastOccurrence) {
            if (lastOccurrence) {
                if (seedValue != null && !seedValue.equals(instance)) return;
                listener.update(new EDataTypeInSlotsKey(type), 
                        Tuples.staticArityFlatTupleOf(instance), false);
            }
        }
    }
    private static class EStructuralFeatureInstancesKeyAdapter extends ListenerAdapter implements FeatureListener {
        private Object seedHost;
        private Object seedValue;
        public EStructuralFeatureInstancesKeyAdapter(IQueryRuntimeContextListener listener, Object seedHost, Object seedValue) {
            super(listener, seedHost, seedValue);
            this.seedHost = seedHost;
            this.seedValue = seedValue;
        }
        @Override
        public void featureInserted(EObject host, EStructuralFeature feature,
                Object value) {
            if (seedHost != null && !seedHost.equals(host)) return;
            if (seedValue != null && !seedValue.equals(value)) return;
            listener.update(new EStructuralFeatureInstancesKey(feature), 
                    Tuples.staticArityFlatTupleOf(host, value), true);
        }
        @Override
        public void featureDeleted(EObject host, EStructuralFeature feature,
                Object value) {
            if (seedHost != null && !seedHost.equals(host)) return;
            if (seedValue != null && !seedValue.equals(value)) return;
            listener.update(new EStructuralFeatureInstancesKey(feature), 
                    Tuples.staticArityFlatTupleOf(host, value), false);
        }    	
    }
    
    @Override
    public void addUpdateListener(IInputKey key, Tuple seed /* TODO ignored */, IQueryRuntimeContextListener listener) {
        // stateless, so NOP
        if (key instanceof JavaTransitiveInstancesKey) return;

        ensureIndexed(key, IndexingService.INSTANCES);
        if (key instanceof EClassTransitiveInstancesKey) {
            EClass eClass = ((EClassTransitiveInstancesKey) key).getEmfKey();
            baseIndex.addInstanceListener(Collections.singleton(eClass), 
                    new EClassTransitiveInstancesAdapter(listener, seed.get(0)));
        } else if (key instanceof EDataTypeInSlotsKey) {
            EDataType dataType = ((EDataTypeInSlotsKey) key).getEmfKey();
            baseIndex.addDataTypeListener(Collections.singleton(dataType), 
                    new EDataTypeInSlotsAdapter(listener, seed.get(0)));
        } else if (key instanceof EStructuralFeatureInstancesKey) {
            EStructuralFeature feature = ((EStructuralFeatureInstancesKey) key).getEmfKey();
            baseIndex.addFeatureListener(Collections.singleton(feature), 
                    new EStructuralFeatureInstancesKeyAdapter(listener, seed.get(0), seed.get(1)));
        } else {
            illegalInputKey(key);
        }
    }
    @Override
    public void removeUpdateListener(IInputKey key, Tuple seed, IQueryRuntimeContextListener listener) {
        // stateless, so NOP
        if (key instanceof JavaTransitiveInstancesKey) return;

        ensureIndexed(key, IndexingService.INSTANCES);
        if (key instanceof EClassTransitiveInstancesKey) {
            EClass eClass = ((EClassTransitiveInstancesKey) key).getEmfKey();
            baseIndex.removeInstanceListener(Collections.singleton(eClass), 
                    new EClassTransitiveInstancesAdapter(listener, seed.get(0)));
        } else if (key instanceof EDataTypeInSlotsKey) {
            EDataType dataType = ((EDataTypeInSlotsKey) key).getEmfKey();
            baseIndex.removeDataTypeListener(Collections.singleton(dataType), 
                    new EDataTypeInSlotsAdapter(listener, seed.get(0)));
        } else if (key instanceof EStructuralFeatureInstancesKey) {
            EStructuralFeature feature = ((EStructuralFeatureInstancesKey) key).getEmfKey();
            baseIndex.removeFeatureListener(Collections.singleton(feature), 
                    new EStructuralFeatureInstancesKeyAdapter(listener, seed.get(0), seed.get(1)));
        } else {
            illegalInputKey(key);
        }
    }
    
    // TODO wrap / unwrap enum literals 
    // TODO use this in all other public methods (maybe wrap & delegate?)
    
    @Override
    public Object unwrapElement(Object internalElement) {
        return internalElement;
    }
    @Override
    public Tuple unwrapTuple(Tuple internalElements) {
        return internalElements;
    }
    @Override
    public Object wrapElement(Object externalElement) {
        return externalElement;
    }
    @Override
    public Tuple wrapTuple(Tuple externalElements) {
        return externalElements;
    }
    
    /**
     * @since 1.4
     */
    @Override
    public void ensureWildcardIndexing(IndexingService service) {
        baseIndex.setWildcardLevel(IndexingLevel.toLevel(service));
    }
    
    /**
     * @since 1.4
     */
    @Override
    public void executeAfterTraversal(Runnable runnable) throws InvocationTargetException {
        baseIndex.executeAfterTraversal(runnable);
    }
}

