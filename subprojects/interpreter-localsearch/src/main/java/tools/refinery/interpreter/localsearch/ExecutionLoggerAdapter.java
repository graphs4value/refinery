/*******************************************************************************
 * Copyright (c) 2010-2018, Zoltan Ujhelyi, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.localsearch;

import tools.refinery.interpreter.localsearch.matcher.ILocalSearchAdapter;
import tools.refinery.interpreter.localsearch.matcher.LocalSearchMatcher;
import tools.refinery.interpreter.localsearch.operations.IPatternMatcherOperation;
import tools.refinery.interpreter.localsearch.operations.ISearchOperation;
import tools.refinery.interpreter.localsearch.plan.SearchPlan;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * @since 2.0
 */
public final class ExecutionLoggerAdapter implements ILocalSearchAdapter {

    volatile String indentation = "";
    private final Consumer<String> outputConsumer;

    public ExecutionLoggerAdapter(Consumer<String> outputConsumer) {
        this.outputConsumer = outputConsumer;
    }

    private void logMessage(String message) {
        outputConsumer.accept(message);
    }

    private void logMessage(String message, Object...args) {
        outputConsumer.accept(String.format(message, args));
    }

    @Override
    public void patternMatchingStarted(LocalSearchMatcher lsMatcher) {
        logMessage(indentation + "[ START] " + lsMatcher.getQuerySpecification().getFullyQualifiedName());
    }

    @Override
    public void noMoreMatchesAvailable(LocalSearchMatcher lsMatcher) {
        logMessage(indentation + "[FINISH] " + lsMatcher.getQuerySpecification().getFullyQualifiedName());
    }

    @Override
    public void planChanged(Optional<SearchPlan> oldPlan, Optional<SearchPlan> newPlan) {
        logMessage(indentation + "[  PLAN] " + newPlan.map(p -> p.getSourceBody().getPattern().getFullyQualifiedName()).orElse(""));
        logMessage(indentation + newPlan.map(SearchPlan::toString).map(s -> s.replace("\n", "\n" + indentation)).orElse(""));
    }

    @Override
    public void operationSelected(SearchPlan plan, ISearchOperation operation, MatchingFrame frame, boolean isBacktrack) {
        String category = isBacktrack ? "[  BACK] "  : "[SELECT] ";
       logMessage(indentation + category + operation.toString());
        if (operation instanceof IPatternMatcherOperation) {
            indentation = indentation + "\t";
        }
    }

    @Override
    public void operationExecuted(SearchPlan plan, ISearchOperation operation, MatchingFrame frame,
            boolean isSuccessful) {
        if (operation instanceof IPatternMatcherOperation && indentation.length() > 0) {
            indentation = indentation.substring(1);
        }
        logMessage(indentation + "[    %s] %s %s", isSuccessful ? "OK" : "NO", operation.toString(), frame.toString());
    }

    @Override
    public void matchFound(SearchPlan plan, MatchingFrame frame) {
        logMessage(indentation + "[ MATCH] " + plan.getSourceBody().getPattern().getFullyQualifiedName() + " " + frame.toString());
    }

    @Override
    public void duplicateMatchFound(MatchingFrame frame) {
        logMessage(indentation + "[ DUPL.] " + frame.toString());
    }
}
