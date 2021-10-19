package tools.refinery.language.web.xtext.servlet;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RestartableCachedThreadPool implements ExecutorService {
	private static final Logger LOG = LoggerFactory.getLogger(RestartableCachedThreadPool.class);
	
	private ExecutorService delegate;

	public RestartableCachedThreadPool() {
		delegate = createExecutorService();
	}
	
	public void waitForAllTasksToFinish() {
		delegate.shutdown();
		waitForTermination();
		delegate = createExecutorService();
	}
	
	public void waitForTermination() {
		try {
			delegate.awaitTermination(1, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			LOG.warn("Interrupted while waiting for delegate executor to stop", e);
		}
	}
	
	protected ExecutorService createExecutorService() {
		return Executors.newCachedThreadPool();
	}
	
	@Override
	public boolean awaitTermination(long arg0, TimeUnit arg1) throws InterruptedException {
		return delegate.awaitTermination(arg0, arg1);
	}

	@Override
	public void execute(Runnable arg0) {
		delegate.execute(arg0);
	}

	@Override
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> arg0, long arg1, TimeUnit arg2)
			throws InterruptedException {
		return delegate.invokeAll(arg0, arg1, arg2);
	}

	@Override
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> arg0) throws InterruptedException {
		return delegate.invokeAll(arg0);
	}

	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> arg0, long arg1, TimeUnit arg2)
			throws InterruptedException, ExecutionException, TimeoutException {
		return delegate.invokeAny(arg0, arg1, arg2);
	}

	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> arg0) throws InterruptedException, ExecutionException {
		return delegate.invokeAny(arg0);
	}

	@Override
	public boolean isShutdown() {
		return delegate.isShutdown();
	}

	@Override
	public boolean isTerminated() {
		return delegate.isTerminated();
	}

	@Override
	public void shutdown() {
		delegate.shutdown();
	}

	@Override
	public List<Runnable> shutdownNow() {
		return delegate.shutdownNow();
	}

	@Override
	public <T> Future<T> submit(Callable<T> arg0) {
		return delegate.submit(arg0);
	}

	@Override
	public <T> Future<T> submit(Runnable arg0, T arg1) {
		return delegate.submit(arg0, arg1);
	}

	@Override
	public Future<?> submit(Runnable arg0) {
		return delegate.submit(arg0);
	}
}
