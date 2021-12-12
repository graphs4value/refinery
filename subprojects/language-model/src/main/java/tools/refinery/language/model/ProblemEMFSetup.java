package tools.refinery.language.model;

import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;

import tools.refinery.language.model.problem.ProblemPackage;
import tools.refinery.language.model.problem.impl.ProblemFactoryImpl;

public class ProblemEMFSetup {
	public static final String XMI_RESOURCE_EXTENSION = "problem_xmi";

	private ProblemEMFSetup() {
		throw new IllegalStateException("This is a static utility class and should not be instantiated directly");
	}

	// Here we can't rely on java.util.HashMap#putIfAbsent, because
	// org.eclipse.emf.ecore.impl.EPackageRegistryImpl#containsKey is overridden
	// without also overriding putIfAbsent. We must make sure to call the
	// overridden containsKey implementation.
	@SuppressWarnings("squid:S3824")
	public static void doEMFRegistration() {
		if (!EPackage.Registry.INSTANCE.containsKey(ProblemPackage.eNS_URI)) {
			EPackage.Registry.INSTANCE.put(ProblemPackage.eNS_URI, ProblemPackage.eINSTANCE);
		}

		// This Resource.Factory is not actually used once
		// tools.refinery.language.ProblemStandaloneSetup.createInjectorAndDoEMFRegistration()
		// is called, because if will be replaced by
		// tools.refinery.language.resource.ProblemXmiResourceFactory, which implements
		// org.eclipse.xtext.resource.IResourceFactory as required by Xtext.
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().putIfAbsent(XMI_RESOURCE_EXTENSION,
				new ProblemFactoryImpl());
	}
}
