package tools.refinery.language.web.xtext;

import org.eclipse.xtext.ide.ExecutorServiceProvider;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VirtualThreadExecutorServiceProvider extends ExecutorServiceProvider {
	private static final String THREAD_POOL_NAME = "xtextWeb";

	@Override
	protected ExecutorService createInstance(String key) {
		var name = key == null ? THREAD_POOL_NAME : THREAD_POOL_NAME + "-" + key;
		return Executors.newThreadPerTaskExecutor(Thread.ofVirtual()
				.allowSetThreadLocals(true)
				.inheritInheritableThreadLocals(false)
				.name(name + "-", 0)
				.factory());
	}
}
