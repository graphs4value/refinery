package tools.refinery.language.mapping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import tools.refinery.store.model.representation.Relation;
import tools.refinery.store.model.representation.TruthValue;
import tools.refinery.store.query.building.DNFAnd;
import tools.refinery.store.query.building.DNFAtom;
import tools.refinery.store.query.building.DNFPredicate;
import tools.refinery.store.query.building.DNFPredicateCallAtom;
import tools.refinery.store.query.building.DirectRelationAtom;
import tools.refinery.store.query.building.RelationAtom;
import tools.refinery.store.query.building.Variable;
import tools.refinery.store.query.view.FilteredRelationView;
import tools.refinery.store.query.view.RelationView;

public class RelationViewConverter {
	public RelationViewMappings convertDirectPredicates(Collection<DNFPredicate> originalPredicates,
			Collection<Relation<TruthValue>> relations) {
		Map<Relation<TruthValue>, RelationView<TruthValue>> mayViewMap = new HashMap<>();
		Map<Relation<TruthValue>, RelationView<TruthValue>> mustViewMap = new HashMap<>();
		for (Relation<TruthValue> relation : relations) {
			mayViewMap.put(relation, new FilteredRelationView<>(relation, (k, v) -> v.may()));
			mustViewMap.put(relation, new FilteredRelationView<>(relation, (k, v) -> v.must()));
		}
		Set<DNFPredicate> dnfPredicates = new HashSet<>();
		Map<RelationView<TruthValue>, DNFPredicate> dnfPredHelpers = new HashMap<>();
		for (DNFPredicate dnfPredicate : originalPredicates) {
			List<DNFAnd> clauses = new ArrayList<>();
			for (DNFAnd clause : dnfPredicate.getClauses()) {
				List<DNFAtom> dnfAtoms = new ArrayList<>();
				for (DNFAtom dnfAtom : clause.getConstraints()) {
					if (dnfAtom instanceof DirectRelationAtom directRelationAtom) {
						List<DNFAtom> newAtoms = convertDirectRelationView(dnfPredHelpers,
								mayViewMap.get(directRelationAtom.getRelation()),
								mustViewMap.get(directRelationAtom.getRelation()), directRelationAtom);
						dnfAtoms.addAll(newAtoms);
					} else if (dnfAtom instanceof DNFPredicateCallAtom dnfPredicateCallAtom) {
						dnfAtoms.add(dnfPredicateCallAtom);
					} else
						throw new UnsupportedOperationException("Unknown DNFAtom type");
				}
				clauses.add(new DNFAnd(clause.getExistentiallyQuantified(), dnfAtoms));
			}
			dnfPredicates.add(new DNFPredicate(dnfPredicate.getName(), dnfPredicate.getVariables(), clauses));
		}
		return new RelationViewMappings(dnfPredicates, dnfPredHelpers, mayViewMap, mustViewMap, new HashSet<>(relations));
	}

	public DNFPredicateCallAtom createMayNotDNFPredicateHelper(RelationView<TruthValue> relationView,
			Map<RelationView<TruthValue>, DNFPredicate> dnfPredHelpers, DirectRelationAtom directRelationAtom) {
		DNFPredicate mayNotPredicate = dnfPredHelpers.get(relationView);
		if (mayNotPredicate == null) {
			DNFAtom mayRelationAtom = new RelationAtom(
					new FilteredRelationView<>(directRelationAtom.getRelation(), (k, v) -> v.may()),
					directRelationAtom.getSubstitution());
			DNFAnd clauses = new DNFAnd(Set.of(), List.of(mayRelationAtom));
			DNFPredicate dnfPredicate = new DNFPredicate("mayNot" + relationView.getRepresentation().getName(),
					List.of(new Variable("V")), List.of(clauses));
			dnfPredHelpers.put(relationView, dnfPredicate);
		}
		return new DNFPredicateCallAtom(false, false, mayNotPredicate, directRelationAtom.getSubstitution());
	}

	public DNFPredicateCallAtom createMustNotDNFPredicateHelper(RelationView<TruthValue> relationView,
			Map<RelationView<TruthValue>, DNFPredicate> dnfPredHelpers, DirectRelationAtom directRelationAtom) {
		DNFPredicate mustNotPredicate = dnfPredHelpers.get(relationView);
		if (mustNotPredicate == null) {
			DNFAtom mustRelationAtom = new RelationAtom(
					new FilteredRelationView<>(directRelationAtom.getRelation(), (k, v) -> v.must()),
					directRelationAtom.getSubstitution());
			DNFAnd clauses = new DNFAnd(Set.of(), List.of(mustRelationAtom));
			DNFPredicate dnfPredicate = new DNFPredicate("mustNot" + relationView.getRepresentation().getName(),
					List.of(new Variable("V")), List.of(clauses));
			dnfPredHelpers.put(relationView, dnfPredicate);
		}
		return new DNFPredicateCallAtom(false, false, mustNotPredicate, directRelationAtom.getSubstitution());
	}

	public List<DNFAtom> convertDirectRelationView(Map<RelationView<TruthValue>, DNFPredicate> dnfPredHelpers,
			RelationView<TruthValue> mayRelView, RelationView<TruthValue> mustRelView,
			DirectRelationAtom directRelationAtom) {
		DNFAtom mayRelationAtom = new RelationAtom(mayRelView, directRelationAtom.getSubstitution());
		DNFAtom mustRelationAtom = new RelationAtom(mustRelView, directRelationAtom.getSubstitution());

		List<DNFAtom> dnfAtoms = new ArrayList<>();
		int truthValues = 0;
		final int T = 8, F = 4, U = 2, E = 1;
		for (TruthValue allowedTruthValues : directRelationAtom.getAllowedTruthValues()) {
			switch (allowedTruthValues) {
			case TRUE:
				truthValues += T;
				break;
			case FALSE:
				truthValues += F;
				break;
			case UNKNOWN:
				truthValues += U;
				break;
			case ERROR:
				truthValues += E;
				break;
			default:
				break;
			}
		}
		switch (truthValues) {
		case E:
			dnfAtoms.add(createMayNotDNFPredicateHelper(mayRelView, dnfPredHelpers, directRelationAtom));
			dnfAtoms.add(mustRelationAtom);
			break;
		case U:
			dnfAtoms.add(mayRelationAtom);
			dnfAtoms.add(createMustNotDNFPredicateHelper(mustRelView, dnfPredHelpers, directRelationAtom));
			break;
		case F:
			dnfAtoms.add(createMayNotDNFPredicateHelper(mayRelView, dnfPredHelpers, directRelationAtom));
			dnfAtoms.add(createMustNotDNFPredicateHelper(mustRelView, dnfPredHelpers, directRelationAtom));
			break;
		case F | E:
			dnfAtoms.add(createMayNotDNFPredicateHelper(mayRelView, dnfPredHelpers, directRelationAtom));
			break;
		case F | U:
			dnfAtoms.add(createMustNotDNFPredicateHelper(mustRelView, dnfPredHelpers, directRelationAtom));
			break;
		case T:
			dnfAtoms.add(mayRelationAtom);
			dnfAtoms.add(mustRelationAtom);
			break;
		case T | E:
			dnfAtoms.add(mustRelationAtom);
			break;
		case T | U:
			dnfAtoms.add(mayRelationAtom);
			break;
		case T | F | U | E:
			// Always satisfy
			break;
		default:
			throw new UnsupportedOperationException("Unsupported TruthValue combination");
		}
		return dnfAtoms;
	}
}
