package tools.refinery.language;

import org.eclipse.xtext.naming.IQualifiedNameConverter;
import org.eclipse.xtext.resource.IDefaultResourceDescriptionStrategy;
import org.eclipse.xtext.resource.IResourceFactory;
import org.eclipse.xtext.resource.generic.AbstractGenericResourceRuntimeModule;

import tools.refinery.language.model.ProblemEMFSetup;
import tools.refinery.language.naming.ProblemQualifiedNameConverter;
import tools.refinery.language.resource.ProblemResourceDescriptionStrategy;
import tools.refinery.language.resource.ProblemXmiResourceFactory;

public class ProblemXmiRuntimeModule extends AbstractGenericResourceRuntimeModule {
	@Override
	protected String getLanguageName() {
		return "tools.refinery.language.ProblemXmi";
	}

	@Override
	protected String getFileExtensions() {
		return ProblemEMFSetup.XMI_RESOURCE_EXTENSION;
	}
	
	public Class<? extends IResourceFactory> bindIResourceFactory() {
		return ProblemXmiResourceFactory.class;
	}
	
	public Class<? extends IQualifiedNameConverter> bindIQualifiedNameConverter() {
		return ProblemQualifiedNameConverter.class;
	}

	public Class<? extends IDefaultResourceDescriptionStrategy> bindIDefaultResourceDescriptionStrategy() {
		return ProblemResourceDescriptionStrategy.class;
	}
}
