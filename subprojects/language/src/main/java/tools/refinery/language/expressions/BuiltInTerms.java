/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.expressions;

import tools.refinery.language.library.BuiltinLibrary;
import tools.refinery.language.typesystem.AggregatorName;
import tools.refinery.language.typesystem.DataExprType;
import tools.refinery.language.utils.BuiltinSymbols;

public class BuiltInTerms {
	public static final DataExprType BOOLEAN_TYPE = new DataExprType(BuiltinLibrary.BUILTIN_LIBRARY_NAME,
			BuiltinSymbols.BOOLEAN_NAME);
	public static final DataExprType INT_TYPE = new DataExprType(BuiltinLibrary.BUILTIN_LIBRARY_NAME,
			BuiltinSymbols.INT_NAME);
	public static final DataExprType REAL_TYPE = new DataExprType(BuiltinLibrary.BUILTIN_LIBRARY_NAME,
			BuiltinSymbols.REAL_NAME);
	public static final DataExprType STRING_TYPE = new DataExprType(BuiltinLibrary.BUILTIN_LIBRARY_NAME,
			BuiltinSymbols.STRING_NAME);
	public static final AggregatorName REIFY_AGGREGATOR = new AggregatorName(BuiltinLibrary.BUILTIN_LIBRARY_NAME,
			"reify");
	public static final AggregatorName COUNT_AGGREGATOR = new AggregatorName(BuiltinLibrary.BUILTIN_LIBRARY_NAME,
			"count");
	public static final AggregatorName MEET_AGGREGATOR = new AggregatorName(BuiltinLibrary.BUILTIN_LIBRARY_NAME,
			"meet");
	public static final AggregatorName JOIN_AGGREGATOR = new AggregatorName(BuiltinLibrary.BUILTIN_LIBRARY_NAME,
			"join");
	public static final AggregatorName SUM_AGGREGATOR = new AggregatorName(BuiltinLibrary.BUILTIN_LIBRARY_NAME, "sum");
	public static final AggregatorName MIN_AGGREGATOR = new AggregatorName(BuiltinLibrary.BUILTIN_LIBRARY_NAME, "min");
	public static final AggregatorName MAX_AGGREGATOR = new AggregatorName(BuiltinLibrary.BUILTIN_LIBRARY_NAME, "max");
}
