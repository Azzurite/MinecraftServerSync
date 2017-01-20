package name.azzurite.mcserver.util;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public final class AsyncUtil {

	private AsyncUtil() {}

	public  static <R> R getResult(Future<R> future) throws ExecutionException {
		try {
			return future.get();
		} catch (InterruptedException ignored) {
			return null;
		}
	}

	public static void threadSleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException ignored) {
		}
	}
}
