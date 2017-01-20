package name.azzurite.mcserver.sync;

import java.text.NumberFormat;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProgressLogger {

	private static final Logger LOGGER = LoggerFactory.getLogger(ProgressLogger.class);

	private static final Executor executor = Executors.newSingleThreadExecutor();
	private static final NumberFormat PERCENT_FORMAT = NumberFormat.getPercentInstance();

	static {
		PERCENT_FORMAT.setMinimumIntegerDigits(2);
		PERCENT_FORMAT.setMinimumFractionDigits(1);
		PERCENT_FORMAT.setMaximumFractionDigits(1);
	}

	private final SyncActionFuture<?> future;

	public ProgressLogger(SyncActionFuture<?> future) {
		this.future = future;
	}

	public void logProgress() {
		executor.execute(() -> {
			SyncActionProgress progress = future.getProgress();
			double percent = progress.getPercent();
			LOGGER.info("{} progress: {}%, {}", progress.getActionName(), PERCENT_FORMAT.format(percent), progress.toHumanReadableString());
		});
	}
}
