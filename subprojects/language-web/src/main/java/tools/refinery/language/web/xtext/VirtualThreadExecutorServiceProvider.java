package tools.refinery.language.web.xtext;

import org.eclipse.xtext.ide.ExecutorServiceProvider;
import tools.refinery.language.web.VirtualThreadUtils;

import java.util.concurrent.ExecutorService;

public class VirtualThreadExecutorServiceProvider extends ExecutorServiceProvider {
	private static final String THREAD_POOL_NAME = "xtextWeb";

	@Override
	protected ExecutorService createInstance(String key) {
		var name = key == null ? THREAD_POOL_NAME : THREAD_POOL_NAME + "-" + key;
		return VirtualThreadUtils.newNamedVirtualThreadsExecutor(name);
	}
}
