package tools.refinery.language.mapping;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import tools.refinery.language.model.ProblemUtil;
import tools.refinery.language.model.problem.PredicateDefinition;
import tools.refinery.language.model.problem.Problem;
import tools.refinery.language.model.problem.Statement;
import tools.refinery.store.query.QueriableModel;
import tools.refinery.store.query.QueriableModelStore;
import tools.refinery.store.query.QueriableModelStoreImpl;
import tools.refinery.store.query.building.DNFPredicate;
import tools.refinery.store.query.view.RelationView;

public class QueryableModelMapper extends PartialModelMapper {
	@Override
	public PartialModelMapperDTO transformProblem(Problem problem) throws PartialModelMapperException {
		// Defining an integer in order to assign different values to all the nodes
		int[] nodeIter = new int[] { 0 };

		// Getting the relations and the nodes from the given problem
		PartialModelMapperDTO pmmDTO = initTransform(problem, nodeIter);

		// Getting the relations and the nodes from the built in problem
		Optional<Problem> builtinProblem = ProblemUtil.getBuiltInLibrary(problem);
		if (builtinProblem.isEmpty())
			throw new PartialModelMapperException("builtin.problem not found");
		PartialModelMapperDTO builtinProblemDTO = initTransform(builtinProblem.get(), nodeIter);

		prepareRelations(pmmDTO, builtinProblemDTO);

		// Collecting direct predicates from statements
		Set<PredicateDefinition> predSet = new HashSet<>();
		for (Statement statement : problem.getStatements()) {
			if (statement instanceof PredicateDefinition predDef) {
				predSet.add(predDef);
			}
		}

		// Converting parsed model to DNF
		ParsedModelToDNFConverter dnfConverter = new ParsedModelToDNFConverter();
		Mappings mappings = null;
		try {
			mappings = dnfConverter.transformPred(predSet, pmmDTO.getNodeMap(), pmmDTO.getRelationMap());
		} catch (Exception e) {
			e.printStackTrace();
		}

		// Converting DirectRelationAtoms to RelationViews
		RelationViewConverter relationViewConverter = new RelationViewConverter();
		RelationViewMappings relationViewMappings = relationViewConverter
				.convertDirectPredicates(mappings.getPredicateMap().values(), mappings.getRelationMap().values());

		// Collecting RelationViews (union of may and must views)
		Set<RelationView<?>> viewSet = new HashSet<>(relationViewMappings.getMayViewMap().values());
		viewSet.addAll(relationViewMappings.getMustViewMap().values());
		
		// Collecting DNFPredicates (union of predefined DNFPredicates and may/must helpers)
		Set<DNFPredicate> dnfPredSet = relationViewMappings.getDirectPredicates();
		dnfPredSet.addAll(relationViewMappings.getMayHelperDirectPredicates().values());
		dnfPredSet.addAll(relationViewMappings.getMustHelperDirectPredicates().values());
		
		// Creating store and model
		QueriableModelStore store = new QueriableModelStoreImpl(Set.copyOf(relationViewMappings.getRelations()),
				viewSet, dnfPredSet);
		QueriableModel model = store.createModel();
		pmmDTO.setModel(model);

		fillModel(problem, pmmDTO, builtinProblem, builtinProblemDTO);
		
		model.flushChanges();

		return pmmDTO;
	}
}
