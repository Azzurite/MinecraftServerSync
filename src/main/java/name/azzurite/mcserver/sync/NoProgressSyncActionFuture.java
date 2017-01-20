package name.azzurite.mcserver.sync;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Future;

public class NoProgressSyncActionFuture<R> extends SyncActionFuture<R> {

	private static class NoSyncActionProgress extends SyncActionProgress {

		NoSyncActionProgress(String actionName) {
			super(actionName);
		}

		@Override
		public double getPercent() {
			return 0;
		}

		@Override
		public Map<String, String> getAdditionalData() {
			return Collections.emptyMap();
		}

		@Override
		public String getStats() {
			return "";
		}
	}

	private final NoSyncActionProgress progress;

	public NoProgressSyncActionFuture(String actionName, Future<R> future) {
		super(future);
		progress = new NoSyncActionProgress(actionName);
	}

	@Override
	public SyncActionProgress getProgress() {
		return progress;
	}
}
