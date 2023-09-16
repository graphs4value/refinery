/*******************************************************************************
 * Copyright (c) 2010-2016, Balázs Grill, Zoltan Ujhelyi, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.api;


import tools.refinery.interpreter.matchers.backend.IQueryBackendFactory;
import tools.refinery.interpreter.matchers.backend.IQueryBackendFactoryProvider;
import tools.refinery.interpreter.matchers.backend.QueryEvaluationHint;
import tools.refinery.interpreter.matchers.util.Preconditions;

import java.util.Objects;
import java.util.ServiceLoader;

/**
 * This class is intended to provide options to a created {@link InterpreterEngine} instance. The {@link #DEFAULT}
 * instance represents the configuration that is selected when no explicit options are provided by the user. To create
 * new configurations, use the static builder methods {@link #defineOptions()} (starts with empty options) or
 * {@link #copyOptions(InterpreterEngineOptions)} (starts with all options from an existing configuration).
 *
 * @author Balázs Grill, Zoltan Ujhelyi
 * @since 1.4
 *
 */
public final class InterpreterEngineOptions {

    private static boolean areSystemDefaultsCalculated = false;
    private static IQueryBackendFactory systemDefaultBackendFactory;
    private static IQueryBackendFactory systemDefaultCachingBackendFactory;
    private static IQueryBackendFactory systemDefaultSearchBackendFactory;

    /**
     * @since 2.0
     */
    public static void setSystemDefaultBackends(IQueryBackendFactory systemDefaultBackendFactory,
            IQueryBackendFactory systemDefaultCachingBackendFactory,
            IQueryBackendFactory systemDefaultSearchBackendFactory) {
                areSystemDefaultsCalculated = true;
                InterpreterEngineOptions.systemDefaultBackendFactory = systemDefaultBackendFactory;
                InterpreterEngineOptions.systemDefaultCachingBackendFactory = systemDefaultCachingBackendFactory;
                InterpreterEngineOptions.systemDefaultSearchBackendFactory = systemDefaultSearchBackendFactory;
    }

    /**
     * If {@link #setSystemDefaultBackends(IQueryBackendFactory, IQueryBackendFactory, IQueryBackendFactory)} is not
     * called, this method is responsible of finding the corresponding backends on the classpath using Java Service
     * loaders.
     */
    private static void calculateSystemDefaultBackends() {
        for (IQueryBackendFactoryProvider provider : ServiceLoader.load(IQueryBackendFactoryProvider.class)) {
            if (provider.isSystemDefaultEngine()) {
                systemDefaultBackendFactory = provider.getFactory();
            }
            if (provider.isSystemDefaultCachingBackend()) {
                systemDefaultCachingBackendFactory = provider.getFactory();
            }
            if (provider.isSystemDefaultSearchBackend()) {
                systemDefaultSearchBackendFactory = provider.getFactory();
            }
        }
        areSystemDefaultsCalculated = true;
    }

    private static IQueryBackendFactory getSystemDefaultBackend() {
        if (!areSystemDefaultsCalculated) {
            calculateSystemDefaultBackends();
        }
        return Objects.requireNonNull(systemDefaultBackendFactory, "System default backend not found");
    }

    private static IQueryBackendFactory getSystemDefaultCachingBackend() {
        if (!areSystemDefaultsCalculated) {
            calculateSystemDefaultBackends();
        }
        return Objects.requireNonNull(systemDefaultCachingBackendFactory, "System default caching backend not found");
    }

    private static IQueryBackendFactory getSystemDefaultSearchBackend() {
        if (!areSystemDefaultsCalculated) {
            calculateSystemDefaultBackends();
        }
        return Objects.requireNonNull(systemDefaultSearchBackendFactory, "System default search backend not found");
    }

    private final QueryEvaluationHint engineDefaultHints;

    private final IQueryBackendFactory defaultCachingBackendFactory;
    private final IQueryBackendFactory defaultSearchBackendFactory;

    /** The default engine options; if options are not defined, this version will be used. */
    private static InterpreterEngineOptions DEFAULT;

    /**
     * @since 2.0
     */
    public static final InterpreterEngineOptions getDefault() {
        if (DEFAULT == null) {
            DEFAULT = new Builder().build();
        }
        return DEFAULT;
    }

    public static final class Builder {
        private QueryEvaluationHint engineDefaultHints;

        private IQueryBackendFactory defaultBackendFactory;
        private IQueryBackendFactory defaultCachingBackendFactory;
        private IQueryBackendFactory defaultSearchBackendFactory;

        public Builder() {

        }

        public Builder(InterpreterEngineOptions from) {
            this.engineDefaultHints = from.engineDefaultHints;
            this.defaultBackendFactory = engineDefaultHints.getQueryBackendFactory();
            this.defaultCachingBackendFactory = from.defaultCachingBackendFactory;
            this.defaultSearchBackendFactory = from.defaultSearchBackendFactory;
        }

        /**
         * Note that the backend factory in the hint is overridden by a factory added with
         * {@link #withDefaultBackend(IQueryBackendFactory)}.
         */
        public Builder withDefaultHint(QueryEvaluationHint engineDefaultHints) {
            this.engineDefaultHints = engineDefaultHints;
            return this;
        }

        /**
         * Note that this backend factory overrides the factory defined by the hint added by
         * {@link #withDefaultHint(QueryEvaluationHint)}.
         */
        public Builder withDefaultBackend(IQueryBackendFactory defaultBackendFactory) {
            this.defaultBackendFactory = defaultBackendFactory;
            return this;
        }

        /**
         * @since 2.0
         */
        public Builder withDefaultSearchBackend(IQueryBackendFactory defaultSearchBackendFactory) {
            Preconditions.checkArgument(!defaultSearchBackendFactory.isCaching(), "%s is not a search backend", defaultSearchBackendFactory.getClass());
            this.defaultSearchBackendFactory = defaultSearchBackendFactory;
            return this;
        }

        public Builder withDefaultCachingBackend(IQueryBackendFactory defaultCachingBackendFactory) {
            Preconditions.checkArgument(defaultCachingBackendFactory.isCaching(), "%s is not a caching backend", defaultCachingBackendFactory.getClass());
            this.defaultCachingBackendFactory = defaultCachingBackendFactory;
            return this;
        }

        public InterpreterEngineOptions build() {
            IQueryBackendFactory defaultFactory = getDefaultBackend();
            QueryEvaluationHint hint = getEngineDefaultHints(defaultFactory);
            return new InterpreterEngineOptions(hint, getDefaultCachingBackend(), getDefaultSearchBackend());
        }

        private IQueryBackendFactory getDefaultBackend() {
            if (defaultBackendFactory != null){
                return defaultBackendFactory;
            } else if (engineDefaultHints != null) {
                return engineDefaultHints.getQueryBackendFactory();
            } else {
                return getSystemDefaultBackend();
            }
        }

        private IQueryBackendFactory getDefaultCachingBackend() {
            if (defaultCachingBackendFactory != null) {
                return defaultCachingBackendFactory;
            } else if (defaultBackendFactory != null && defaultBackendFactory.isCaching()) {
                return defaultBackendFactory;
            } else {
                return getSystemDefaultCachingBackend();
            }
        }

        private IQueryBackendFactory getDefaultSearchBackend() {
            if (defaultSearchBackendFactory != null) {
                return defaultSearchBackendFactory;
            } else if (defaultBackendFactory != null && !defaultBackendFactory.isCaching()) {
                return defaultBackendFactory;
            } else {
                return getSystemDefaultSearchBackend();
            }
        }

        private QueryEvaluationHint getEngineDefaultHints(IQueryBackendFactory defaultFactory) {
            if (engineDefaultHints != null){
                return engineDefaultHints.overrideBy(new QueryEvaluationHint(null, defaultFactory));
            } else {
                return new QueryEvaluationHint(null, defaultFactory);
            }
        }
    }

    /**
     * Initializes an option builder with no previously set options.
     */
    public static Builder defineOptions() {
        return new Builder();
    }

    /**
     * Initializes an option builder with settings from an existing configuration.
     */
    public static Builder copyOptions(InterpreterEngineOptions options) {
        return new Builder(options);
    }

    private InterpreterEngineOptions(QueryEvaluationHint engineDefaultHints,
									 IQueryBackendFactory defaultCachingBackendFactory, IQueryBackendFactory defaultSearchBackendFactory) {
        this.engineDefaultHints = engineDefaultHints;
        this.defaultCachingBackendFactory = defaultCachingBackendFactory;
        this.defaultSearchBackendFactory = defaultSearchBackendFactory;
    }

    public QueryEvaluationHint getEngineDefaultHints() {
        return engineDefaultHints;
    }

    /**
     * Returns the configured default backend
     *
     * @return the defaultBackendFactory
     */
    public IQueryBackendFactory getDefaultBackendFactory() {
        switch (engineDefaultHints.getQueryBackendRequirementType()) {
        case DEFAULT_CACHING:
            return InterpreterEngineOptions.getSystemDefaultCachingBackend();
        case DEFAULT_SEARCH:
            return InterpreterEngineOptions.getSystemDefaultCachingBackend();
        case SPECIFIC:
            return engineDefaultHints.getQueryBackendFactory();
        case UNSPECIFIED:
        default:
            return InterpreterEngineOptions.getSystemDefaultBackend();
        }
    }

    /**
     * Returns the configured default caching backend. If the default backend caches matches, it is usually expected, but
     * not mandatory for the two default backends to be the same.
     */
    public IQueryBackendFactory getDefaultCachingBackendFactory() {
        return defaultCachingBackendFactory;
    }

    /**
     * Returns the configured default search-based backend. If the default backend is search-based, it is usually expected, but
     * not mandatory for the two default backends to be the same.
     * @since 2.0
     */
    public IQueryBackendFactory getDefaultSearchBackendFactory() {
        return defaultSearchBackendFactory;
    }

    @Override
    public String toString() {
        // TODO defaultCachingBackendFactory is ignored
        if(Objects.equals(engineDefaultHints, DEFAULT.engineDefaultHints))
            return "defaults";
        else
            return engineDefaultHints.toString();
    }

    /**
     * @since 2.0
     */
    public IQueryBackendFactory getQueryBackendFactory(QueryEvaluationHint hint) {
        if (hint == null) {
            return getDefaultBackendFactory();
        }

        switch (hint.getQueryBackendRequirementType()) {
        case DEFAULT_CACHING:
            return getDefaultCachingBackendFactory();
        case DEFAULT_SEARCH:
            return getDefaultSearchBackendFactory();
        case SPECIFIC:
            return hint.getQueryBackendFactory();
        default:
            return getDefaultBackendFactory();
        }
    }
}
