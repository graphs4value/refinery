/*******************************************************************************
 * Copyright (c) 2010-2013, Abel Hegedus, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.viatra.runtime.base.api;

import java.util.Objects;

import tools.refinery.viatra.runtime.base.api.filters.IBaseIndexFeatureFilter;
import tools.refinery.viatra.runtime.base.api.filters.IBaseIndexObjectFilter;
import tools.refinery.viatra.runtime.base.api.filters.IBaseIndexResourceFilter;
import tools.refinery.viatra.runtime.base.api.profiler.ProfilerMode;

/**
 * The base index options indicate how the indices are built.
 * 
 * <p>
 * One of the options is to build indices in <em>wildcard mode</em>, meaning that all EClasses, EDataTypes, EReferences
 * and EAttributes are indexed. This is convenient, but comes at a high memory cost. To save memory, one can disable
 * <em>wildcard mode</em> and manually register those EClasses, EDataTypes, EReferences and EAttributes that should be
 * indexed.
 * </p>
 * 
 * <p>
 * Another choice is whether to build indices in <em>dynamic EMF mode</em>, meaning that types are identified by the
 * String IDs that are ultimately derived from the nsURI of the EPackage. Multiple types with the same ID are treated as
 * the same. This is useful if dynamic EMF is used, where there can be multiple copies (instantiations) of the same
 * EPackage, representing essentially the same metamodel. If one disables <em>dynamic EMF mode</em>, an error is logged
 * if duplicate EPackages with the same nsURI are encountered.
 * </p>
 * 
 * @author Abel Hegedus
 * @noimplement This class is not intended to be subclasses outside of VIATRA runtime
 * 
 */
public class BaseIndexOptions {

    /**
     * 
     * By default, base indices will be constructed with wildcard mode set as false.
     */
    protected static final IndexingLevel WILDCARD_MODE_DEFAULT = IndexingLevel.NONE;
    /**
     * 
     * By default, base indices will be constructed with only well-behaving features traversed.
     */
    private static final boolean TRAVERSE_ONLY_WELLBEHAVING_DERIVED_FEATURES_DEFAULT = true;
    /**
     * 
     * By default, base indices will be constructed with dynamic EMF mode set as false.
     */
    protected static final boolean DYNAMIC_EMF_MODE_DEFAULT = false;
    
    /**
     * 
     * By default, the scope will make the assumption that it is free from dangling edges.
     * @since 1.6
     */
    protected static final boolean DANGLING_FREE_ASSUMPTION_DEFAULT = true;
    
    /**
     * By default, duplicate notifications are only logged.
     * 
     * @since 1.6
     */
    protected static final boolean STRICT_NOTIFICATION_MODE_DEFAULT = true;
    
    /**
     * @since 2.3
     */
    protected static final ProfilerMode INDEX_PROFILER_MODE_DEFAULT = ProfilerMode.OFF;

    /**
     * @since 1.6
     */
    protected boolean danglingFreeAssumption = DANGLING_FREE_ASSUMPTION_DEFAULT;
    protected boolean dynamicEMFMode = DYNAMIC_EMF_MODE_DEFAULT;
    protected boolean traverseOnlyWellBehavingDerivedFeatures = TRAVERSE_ONLY_WELLBEHAVING_DERIVED_FEATURES_DEFAULT;
    protected IndexingLevel wildcardMode = WILDCARD_MODE_DEFAULT;
    protected IBaseIndexObjectFilter notifierFilterConfiguration;
    protected IBaseIndexResourceFilter resourceFilterConfiguration;
    /**
     * @since 1.5
     */
    protected IBaseIndexFeatureFilter featureFilterConfiguration;
    
    /**
     * If strict notification mode is turned on, errors related to inconsistent notifications, e.g. duplicate deletions
     * cause the entire Base index to be considered invalid, e.g. the query engine on top of the index should become
     * tainted.
     * 
     * @since 1.6
     */
    protected boolean strictNotificationMode = STRICT_NOTIFICATION_MODE_DEFAULT;
    
    /**
     * Returns whether base index profiling should be turned on.
     * 
     * @since 2.3
     */
    protected ProfilerMode indexerProfilerMode = INDEX_PROFILER_MODE_DEFAULT; 

    /**
     * Creates a base index options with the default values.
     */
    public BaseIndexOptions() {
    }
    
    /**
     * Creates a base index options using the provided values for dynamic EMF mode and wildcard mode.
     * @since 1.4
     */
    public BaseIndexOptions(boolean dynamicEMFMode, IndexingLevel wildcardMode) {
        this.dynamicEMFMode = dynamicEMFMode;
        this.wildcardMode = wildcardMode;
    }
    
    /**
     * 
     * @param dynamicEMFMode
     * @since 0.9
     */
    public BaseIndexOptions withDynamicEMFMode(boolean dynamicEMFMode) {
        BaseIndexOptions result = copy();
        result.dynamicEMFMode = dynamicEMFMode;
        return result;
    }
    
    /**
     * Sets the dangling edge handling property of the index option. If not set explicitly, it is considered as `true`.
     * @param danglingFreeAssumption if true, 
     *  the base index will assume that there are no dangling references 
     *  (pointing out of scope or to proxies)
     * @since 1.6
     */
    public BaseIndexOptions withDanglingFreeAssumption(boolean danglingFreeAssumption) {
        BaseIndexOptions result = copy();
        result.danglingFreeAssumption = danglingFreeAssumption;
        return result;
    }
    
    /**
     * Adds an object-level filter to the indexer configuration. Warning - object-level indexing can increase indexing time
     * noticeably. If possibly, use {@link #withResourceFilterConfiguration(IBaseIndexResourceFilter)} instead.
     * 
     * @param filter
     * @since 0.9
     */
    public BaseIndexOptions withObjectFilterConfiguration(IBaseIndexObjectFilter filter) {
        BaseIndexOptions result = copy();
        result.notifierFilterConfiguration = filter;
        return result;
    }

    /**
     * @return the selected object filter configuration, or null if not set
     */
    public IBaseIndexObjectFilter getObjectFilterConfiguration() {
        return notifierFilterConfiguration;
    }
    
    /**
     * Returns a copy of the configuration with a specified resource filter
     * 
     * @param filter
     * @since 0.9
     */
    public BaseIndexOptions withResourceFilterConfiguration(IBaseIndexResourceFilter filter) {
        BaseIndexOptions result = copy();
        result.resourceFilterConfiguration = filter;
        return result;
    }

    /**
     * @return the selected resource filter, or null if not set
     */
    public IBaseIndexResourceFilter getResourceFilterConfiguration() {
        return resourceFilterConfiguration;
    }
    
    
    /**
     * Returns a copy of the configuration with a specified feature filter
     * 
     * @param filter
     * @since 1.5
     */
    public BaseIndexOptions withFeatureFilterConfiguration(IBaseIndexFeatureFilter filter) {
        BaseIndexOptions result = copy();
        result.featureFilterConfiguration = filter;
        return result;
    }

    /**
     * @return the selected feature filter, or null if not set 
     * @since 1.5
     */
    public IBaseIndexFeatureFilter getFeatureFilterConfiguration() {
        return featureFilterConfiguration;
    }
    

    /**
     * @return whether the base index option has dynamic EMF mode set
     */
    public boolean isDynamicEMFMode() {
        return dynamicEMFMode;
    }

    /**
     * @return whether the base index makes the assumption that there can be no dangling edges
     * @since 1.6
     */
    public boolean isDanglingFreeAssumption() {
        return danglingFreeAssumption;
    }

    /**
     * @return whether the base index option has traverse only well-behaving derived features set
     */
    public boolean isTraverseOnlyWellBehavingDerivedFeatures() {
        return traverseOnlyWellBehavingDerivedFeatures;
    }
    
    /**
     * 
     * @param wildcardMode
     * @since 1.4
     */
    public BaseIndexOptions withWildcardLevel(IndexingLevel wildcardLevel) {
        BaseIndexOptions result = copy();
        result.wildcardMode = wildcardLevel;
        return result;
   }

    /**
     * @since 1.6
     */
    public BaseIndexOptions withStrictNotificationMode(boolean strictNotificationMode) {
        BaseIndexOptions result = copy();
        result.strictNotificationMode = strictNotificationMode;
        return result;
    }
    
    /**
     * @since 2.3
     */
    public BaseIndexOptions withIndexProfilerMode(ProfilerMode indexerProfilerMode) {
        BaseIndexOptions result = copy();
        result.indexerProfilerMode = indexerProfilerMode;
        return result;
    }
    
    /**
     * @return whether the base index option has wildcard mode set
     */
    public boolean isWildcardMode() {
        return wildcardMode == IndexingLevel.FULL;
    }
    
    /**
     * @return the wildcardMode level
     * @since 1.4
     */
    public IndexingLevel getWildcardLevel() {
        return wildcardMode;
    }

    /**
     * If strict notification mode is turned on, errors related to inconsistent notifications, e.g. duplicate deletions
     * cause the entire Base index to be considered invalid, e.g. the query engine on top of the index should become
     * tainted.
     * 
     * @since 1.6
     */
    public boolean isStrictNotificationMode() {
        return strictNotificationMode;
    }

    /**
     * Returns whether base indexer profiling is enabled. The profiling causes some slowdown, but provides information
     * about how much time the base indexer takes for initialization and updates.
     * 
     * @since 2.3
     */
    public ProfilerMode getIndexerProfilerMode() {
        return indexerProfilerMode;
    }

    /**
     * Creates an independent copy of itself. The values of each option will be the same as this options. This method is
     * used when a provided option must be copied to avoid external option changes afterward.
     * 
     * @return the copy of this options
     */
    public BaseIndexOptions copy() {
        BaseIndexOptions baseIndexOptions = new BaseIndexOptions(this.dynamicEMFMode, this.wildcardMode);
        baseIndexOptions.danglingFreeAssumption = this.danglingFreeAssumption;
        baseIndexOptions.traverseOnlyWellBehavingDerivedFeatures = this.traverseOnlyWellBehavingDerivedFeatures;
        baseIndexOptions.notifierFilterConfiguration = this.notifierFilterConfiguration;
        baseIndexOptions.resourceFilterConfiguration = this.resourceFilterConfiguration;
        baseIndexOptions.featureFilterConfiguration = this.featureFilterConfiguration;
        baseIndexOptions.strictNotificationMode = this.strictNotificationMode;
        baseIndexOptions.indexerProfilerMode = this.indexerProfilerMode;
        return baseIndexOptions;
    }

    @Override
    public int hashCode() {
        return Objects.hash(dynamicEMFMode, notifierFilterConfiguration, resourceFilterConfiguration,
                featureFilterConfiguration, traverseOnlyWellBehavingDerivedFeatures, wildcardMode, strictNotificationMode,
                danglingFreeAssumption, indexerProfilerMode);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof BaseIndexOptions))
            return false;
        BaseIndexOptions other = (BaseIndexOptions) obj;
        if (dynamicEMFMode != other.dynamicEMFMode)
            return false;
        if (danglingFreeAssumption != other.danglingFreeAssumption)
            return false;
        if (notifierFilterConfiguration == null) {
            if (other.notifierFilterConfiguration != null)
                return false;
        } else if (!notifierFilterConfiguration
                .equals(other.notifierFilterConfiguration))
            return false;
        if (resourceFilterConfiguration == null) {
            if (other.resourceFilterConfiguration != null)
                return false;
        } else if (!resourceFilterConfiguration
                .equals(other.resourceFilterConfiguration)){
            return false;
        }
        
        if (featureFilterConfiguration == null) {
            if (other.featureFilterConfiguration != null)
                return false;
        } else if (!featureFilterConfiguration
                .equals(other.featureFilterConfiguration)){
            return false;
        }
        
        if (traverseOnlyWellBehavingDerivedFeatures != other.traverseOnlyWellBehavingDerivedFeatures)
            return false;
        if (wildcardMode != other.wildcardMode)
            return false;
        if (strictNotificationMode != other.strictNotificationMode) {
            return false;
        }
        if (indexerProfilerMode != other.indexerProfilerMode) {
            return false;
        }
        return true;
    }
    
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        appendModifier(sb, dynamicEMFMode, DYNAMIC_EMF_MODE_DEFAULT, "dynamicEMF");
        appendModifier(sb, wildcardMode, WILDCARD_MODE_DEFAULT, "wildcard");
        appendModifier(sb, danglingFreeAssumption, DANGLING_FREE_ASSUMPTION_DEFAULT, "danglingFreeAssumption");
        appendModifier(sb, traverseOnlyWellBehavingDerivedFeatures, TRAVERSE_ONLY_WELLBEHAVING_DERIVED_FEATURES_DEFAULT, "wellBehavingOnly");
        appendModifier(sb, strictNotificationMode, STRICT_NOTIFICATION_MODE_DEFAULT, "strictNotificationMode");
        appendModifier(sb, indexerProfilerMode, INDEX_PROFILER_MODE_DEFAULT, "indexerProfilerMode");
        appendModifier(sb, notifierFilterConfiguration, null, "notifierFilter=");
        appendModifier(sb, resourceFilterConfiguration, null, "resourceFilter=");
        appendModifier(sb, featureFilterConfiguration, null, "featureFilterConfiguration=");
        final String result = sb.toString();
        return result.isEmpty() ? "defaults" : result;
    }

    private static void appendModifier(StringBuilder sb, Object actualValue, Object expectedValue, String switchName) {
        if (Objects.equals(expectedValue, actualValue)) {
            // silent
        } else {
            sb.append(Boolean.FALSE.equals(actualValue) ? '-' : '+');
            sb.append(switchName);
            if (! (actualValue instanceof Boolean)) 
                sb.append(actualValue);
        }
    }

}
