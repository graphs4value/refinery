/*******************************************************************************
 * Copyright (c) 2010-2013, Bergmann Gabor, Istvan Rath and Daniel Varro
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.rete.eval;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import tools.refinery.interpreter.matchers.context.IQueryRuntimeContext;
import tools.refinery.interpreter.matchers.psystem.IExpressionEvaluator;
import tools.refinery.interpreter.matchers.tuple.Tuple;
import tools.refinery.interpreter.matchers.tuple.TupleValueProvider;
import tools.refinery.interpreter.matchers.tuple.Tuples;
import tools.refinery.interpreter.matchers.util.Sets;

/**
 * An instance of this class performs the evaluation of Java expressions.
 *
 * @author Bergmann Gabor
 * @author Tamas Szabo
 * @since 1.5
 */
public abstract class EvaluatorCore {

    protected Logger logger;
    protected IExpressionEvaluator evaluator;
    /**
     * @since 2.4
     */
    protected int sourceTupleWidth;
    private Map<String, Integer> parameterPositions;
    protected IQueryRuntimeContext runtimeContext;
    protected IEvaluatorNode evaluatorNode;

    public EvaluatorCore(final Logger logger, final IExpressionEvaluator evaluator,
            final Map<String, Integer> parameterPositions, final int sourceTupleWidth) {
        this.logger = logger;
        this.evaluator = evaluator;
        this.parameterPositions = parameterPositions;
        this.sourceTupleWidth = sourceTupleWidth;
    }

    public void init(final IEvaluatorNode evaluatorNode) {
        this.evaluatorNode = evaluatorNode;
        this.runtimeContext = evaluatorNode.getReteContainer().getNetwork().getEngine().getRuntimeContext();
    }

    /**
     * @since 2.4
     */
    public abstract Iterable<Tuple> performEvaluation(final Tuple input);

    protected abstract String evaluationKind();

    public Object evaluateTerm(final Tuple input) {
        // actual evaluation
        Object result = null;
        try {
            final TupleValueProvider tupleParameters = new TupleValueProvider(runtimeContext.unwrapTuple(input),
                    parameterPositions);
            result = evaluator.evaluateExpression(tupleParameters);
        } catch (final Exception e) {
            logger.warn(String.format(
                    "The incremental pattern matcher encountered an error during %s evaluation for pattern(s) %s over values %s. Error message: %s. (Developer note: %s in %s)",
                    evaluationKind(), evaluatorNode.prettyPrintTraceInfoPatternList(), prettyPrintTuple(input),
                    e.getMessage(), e.getClass().getSimpleName(), this.evaluatorNode), e);
            result = errorResult();
        }

        return result;
    }

    protected String prettyPrintTuple(final Tuple tuple) {
        return tuple.toString();
    }

    protected Object errorResult() {
        return null;
    }

    public static class PredicateEvaluatorCore extends EvaluatorCore {

        public PredicateEvaluatorCore(final Logger logger, final IExpressionEvaluator evaluator,
                final Map<String, Integer> parameterPositions, final int sourceTupleWidth) {
            super(logger, evaluator, parameterPositions, sourceTupleWidth);
        }

        @Override
        public Iterable<Tuple> performEvaluation(final Tuple input) {
            final Object result = evaluateTerm(input);
            if (Boolean.TRUE.equals(result)) {
                return Collections.singleton(input);
            } else {
                return null;
            }
        }

        @Override
        protected String evaluationKind() {
            return "check()";
        }

    }

    public static class FunctionEvaluatorCore extends EvaluatorCore {

        /**
         * @since 2.4
         */
        protected final boolean isUnwinding;

        public FunctionEvaluatorCore(final Logger logger, final IExpressionEvaluator evaluator,
                final Map<String, Integer> parameterPositions, final int sourceTupleWidth) {
            this(logger, evaluator, parameterPositions, sourceTupleWidth, false);
        }

        /**
         * @since 2.4
         */
        public FunctionEvaluatorCore(final Logger logger, final IExpressionEvaluator evaluator,
                final Map<String, Integer> parameterPositions, final int sourceTupleWidth, final boolean isUnwinding) {
            super(logger, evaluator, parameterPositions, sourceTupleWidth);
            this.isUnwinding = isUnwinding;
        }

        @Override
        public Iterable<Tuple> performEvaluation(final Tuple input) {
            final Object result = evaluateTerm(input);
            if (result != null) {
                if (this.isUnwinding) {
                    final Set<?> resultAsSet = (result instanceof Set<?>) ? (Set<?>) result
                            : (result instanceof Iterable<?>) ? Sets.newSet((Iterable<?>) result) : null;

                    if (resultAsSet != null) {
                        return () -> {
                            final Iterator<?> wrapped = resultAsSet.iterator();
                            return new Iterator<Tuple>() {
                                @Override
                                public boolean hasNext() {
                                    return wrapped.hasNext();
                                }

                                @Override
                                public Tuple next() {
                                    final Object next = wrapped.next();
                                    return Tuples.staticArityLeftInheritanceTupleOf(input,
                                            runtimeContext.wrapElement(next));
                                }
                            };
                        };
                    } else {
                        throw new IllegalStateException(
                                "This is an unwinding evaluator, which expects the evaluation result to either be a set or an iterable, but it was "
                                        + result);
                    }
                } else {
                    return Collections.singleton(
                            Tuples.staticArityLeftInheritanceTupleOf(input, runtimeContext.wrapElement(result)));
                }
            } else {
                return null;
            }
        }

        @Override
        protected String evaluationKind() {
            return "eval" + (this.isUnwinding ? "Unwind" : "") + "()";
        }

    }

}
