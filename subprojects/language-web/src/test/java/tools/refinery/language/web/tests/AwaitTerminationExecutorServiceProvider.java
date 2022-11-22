package tools.refinery.language.web.tests;

import com.google.inject.Singleton;
import org.eclipse.xtext.ide.ExecutorServiceProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

@Singleton
public class AwaitTerminationExecutorServiceProvider extends ExecutorServiceProvider {
	private final List<RestartableCachedThreadPool> servicesToShutDown = new ArrayList<>();

	@Override
	protected ExecutorService createInstance(String key) {
		var instance = new RestartableCachedThreadPool();
		synchronized (servicesToShutDown) {
			servicesToShutDown.add(instance);
		}
		return instance;
	}

	public void waitForAllTasksToFinish() {
		synchronized (servicesToShutDown) {
			for (var executorService : servicesToShutDown) {
				executorService.waitForAllTasksToFinish();
			}
		}
	}

	@Override
	public void dispose() {
		super.dispose();
		synchronized (servicesToShutDown) {
			for (var executorService : servicesToShutDown) {
				executorService.waitForTermination();
			}
			servicesToShutDown.clear();
		}
	}
}
