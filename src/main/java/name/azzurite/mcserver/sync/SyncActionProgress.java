package name.azzurite.mcserver.sync;

import java.util.Map;

public abstract class SyncActionProgress {

	private final String  actionName;

	private final SyncActionProgress lastProgress;

	private final long nanos = System.nanoTime();

	protected SyncActionProgress(String actionName, SyncActionProgress lastProgress) {
		this.actionName = actionName;
		this.lastProgress = lastProgress;
	}

	public abstract double getPercent();

	public abstract Map<String, String> getAdditionalData();

	public String getActionName() {
		return actionName;
	}

	public long getNanoTime() {
		return nanos;
	}

	public SyncActionProgress getLastProgress() {
		return lastProgress;
	}

	public abstract String getStats();
}
