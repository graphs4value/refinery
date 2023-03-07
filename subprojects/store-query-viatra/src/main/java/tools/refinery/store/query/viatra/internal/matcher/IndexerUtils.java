package tools.refinery.store.query.viatra.internal.matcher;

import org.eclipse.viatra.query.runtime.matchers.tuple.TupleMask;
import org.eclipse.viatra.query.runtime.rete.index.Indexer;
import org.eclipse.viatra.query.runtime.rete.matcher.ReteEngine;
import org.eclipse.viatra.query.runtime.rete.matcher.RetePatternMatcher;
import org.eclipse.viatra.query.runtime.rete.traceability.RecipeTraceInfo;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

final class IndexerUtils {
    private static final MethodHandle GET_ENGINE_HANDLE;
    private static final MethodHandle GET_PRODUCTION_NODE_TRACE_HANDLE;
    private static final MethodHandle ACCESS_PROJECTION_HANDLE;

    static {
        try {
            var lookup = MethodHandles.privateLookupIn(RetePatternMatcher.class, MethodHandles.lookup());
            GET_ENGINE_HANDLE = lookup.findGetter(RetePatternMatcher.class, "engine", ReteEngine.class);
            GET_PRODUCTION_NODE_TRACE_HANDLE = lookup.findGetter(RetePatternMatcher.class, "productionNodeTrace",
                    RecipeTraceInfo.class);
            ACCESS_PROJECTION_HANDLE = lookup.findVirtual(ReteEngine.class, "accessProjection",
                    MethodType.methodType(Indexer.class, RecipeTraceInfo.class, TupleMask.class));
        } catch (IllegalAccessException | NoSuchFieldException | NoSuchMethodException e) {
            throw new IllegalStateException("Cannot access private members of %s"
                    .formatted(RetePatternMatcher.class.getPackageName()), e);
        }
    }

    private IndexerUtils() {
        throw new IllegalStateException("This is a static utility class and should not be instantiated directly");
    }

    public static Indexer getIndexer(RetePatternMatcher backend, TupleMask mask) {
        try {
            var engine = (ReteEngine) GET_ENGINE_HANDLE.invokeExact(backend);
            var trace = (RecipeTraceInfo) GET_PRODUCTION_NODE_TRACE_HANDLE.invokeExact(backend);
            return (Indexer) ACCESS_PROJECTION_HANDLE.invokeExact(engine, trace, mask);
        } catch (Error e) {
            // Fatal JVM errors should not be wrapped.
            throw e;
        } catch (Throwable e) {
            throw new IllegalStateException("Cannot access matcher for mask " + mask, e);
        }
    }
}
