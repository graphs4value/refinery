package tools.refinery.language.resource;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.resource.IResourceFactory;

import tools.refinery.language.model.problem.util.ProblemResourceFactoryImpl;

public class ProblemXmiResourceFactory implements IResourceFactory {
	private Resource.Factory problemResourceFactory = new ProblemResourceFactoryImpl();
	
	@Override
	public Resource createResource(URI uri) {
		return problemResourceFactory.createResource(uri);
	}
}
