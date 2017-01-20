package name.azzurite.mcserver.sync;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public abstract class SyncActionFuture<R> implements Future<R> {

	private final Future<R> future;

	public SyncActionFuture(Future<R> future) {
		this.future = future;
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		return future.cancel(mayInterruptIfRunning);
	}

	@Override
	public boolean isCancelled() {
		return future.isCancelled();
	}

	@Override
	public boolean isDone() {
		return future.isDone();
	}

	@Override
	public R get() throws InterruptedException, ExecutionException {
		return future.get();
	}

	@Override
	public R get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		return future.get(timeout, unit);
	}

	public abstract SyncActionProgress getProgress();
}
