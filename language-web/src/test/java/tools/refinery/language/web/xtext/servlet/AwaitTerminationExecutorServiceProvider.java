package tools.refinery.language.web.xtext.servlet;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.xtext.ide.ExecutorServiceProvider;

import com.google.inject.Singleton;

@Singleton
public class AwaitTerminationExecutorServiceProvider extends ExecutorServiceProvider {
	private List<ExecutorService> servicesToShutDown = new ArrayList<>();

	@Override
	protected ExecutorService createInstance(String key) {
		var instance = super.createInstance(key);
		servicesToShutDown.add(instance);
		return instance;
	}

	@Override
	public void dispose() {
		super.dispose();
		for (var executorService : servicesToShutDown) {
			try {
				executorService.awaitTermination(1, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				// Continue normally.
			}
		}
	}
}
