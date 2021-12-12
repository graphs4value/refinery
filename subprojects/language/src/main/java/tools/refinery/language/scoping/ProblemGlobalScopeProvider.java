package tools.refinery.language.scoping;

import java.util.LinkedHashSet;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.scoping.impl.ImportUriGlobalScopeProvider;

import tools.refinery.language.model.ProblemUtil;

public class ProblemGlobalScopeProvider extends ImportUriGlobalScopeProvider {
	@Override
	protected LinkedHashSet<URI> getImportedUris(Resource resource) {
		LinkedHashSet<URI> importedUris = new LinkedHashSet<>();
		importedUris.add(ProblemUtil.BUILTIN_LIBRARY_URI);
		return importedUris;
	}
}
