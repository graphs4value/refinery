/**
 */
package org.eclipse.viatra.solver.language.model.problem;

import org.eclipse.emf.common.util.EList;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Enum Declaration</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.viatra.solver.language.model.problem.EnumDeclaration#getLiterals <em>Literals</em>}</li>
 * </ul>
 *
 * @see org.eclipse.viatra.solver.language.model.problem.ProblemPackage#getEnumDeclaration()
 * @model
 * @generated
 */
public interface EnumDeclaration extends Relation, Statement {
	/**
	 * Returns the value of the '<em><b>Literals</b></em>' containment reference list.
	 * The list contents are of type {@link org.eclipse.viatra.solver.language.model.problem.Node}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Literals</em>' containment reference list.
	 * @see org.eclipse.viatra.solver.language.model.problem.ProblemPackage#getEnumDeclaration_Literals()
	 * @model containment="true"
	 * @generated
	 */
	EList<Node> getLiterals();

} // EnumDeclaration
