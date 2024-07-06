/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.semantics.internal.query;

import tools.refinery.language.model.problem.Expr;
import tools.refinery.language.model.problem.ModalExpr;

record ExtractedModalExpr(ConcreteModality modality, Expr body) {
	public static ExtractedModalExpr of(Expr expr) {
		if (expr instanceof ModalExpr modalExpr) {
			return new ExtractedModalExpr(new ConcreteModality(modalExpr.getConcreteness(), modalExpr.getModality()),
					modalExpr.getBody());
		}
		return new ExtractedModalExpr(ConcreteModality.NULL, expr);
	}
}
