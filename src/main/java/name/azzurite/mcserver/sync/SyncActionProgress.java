package name.azzurite.mcserver.sync;

import java.util.Map;

public abstract class SyncActionProgress {

	private final String  actionName;

	private final long nanos = System.nanoTime();

	protected SyncActionProgress(String actionName) {
		this.actionName = actionName;
	}

	public abstract double getPercent();

	public abstract Map<String, String> getAdditionalData();

	public String getActionName() {
		return actionName;
	}

	public long getNanoTime() {
		return nanos;
	}

	public abstract String getStats();
}
