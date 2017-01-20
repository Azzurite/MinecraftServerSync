package name.azzurite.mcserver.util;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public final class AsyncUtil {

	private AsyncUtil() {}

	public  static <R> R getResult(Future<R> future) throws ExecutionException {
		try {
			return future.get();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	public static void threadSleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException ignored) {
		}
	}
}
