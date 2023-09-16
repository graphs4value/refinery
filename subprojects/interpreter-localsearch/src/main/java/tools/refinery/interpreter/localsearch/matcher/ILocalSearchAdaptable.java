/*******************************************************************************
 * Copyright (c) 2010-2016, Peter Lunk, Zoltan Ujhelyi, IncQuery Labs Ltd.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-v20.html.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package tools.refinery.interpreter.localsearch.matcher;

import java.util.List;

/**
 * @author Zoltan Ujhelyi
 *
 */
public interface ILocalSearchAdaptable {

    List<ILocalSearchAdapter> getAdapters();

    void addAdapter(ILocalSearchAdapter adapter);

    void removeAdapter(ILocalSearchAdapter adapter);

    void removeAdapters(List<ILocalSearchAdapter> adapter);

    void addAdapters(List<ILocalSearchAdapter> adapter);

}
