package name.azzurite.mcserver.sync;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import name.azzurite.mcserver.config.Constants;
import name.azzurite.mcserver.util.AsyncUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProgressLogger {

	private static final Logger LOGGER = LoggerFactory.getLogger(ProgressLogger.class);

	private static final Executor executor = Executors.newFixedThreadPool(10);
	private static final int UPDATE_INTERVAL_MILLIS = 1000;
	private static final double SECONDS_IN_MINUTE = 60.0;
	private static final double NANOS_IN_SECOND = 1_000_000_000;

	private final SyncActionFuture<?> future;

	public ProgressLogger(SyncActionFuture<?> future) {
		this.future = future;
	}

	public void logProgress() {
		executor.execute(() -> {
			AsyncUtil.threadSleep(500);
			while (!future.isDone()) {
				SyncActionProgress progress = future.getProgress();
				double percent = progress.getPercent();

				LOGGER.info("{} progress: {}, ETA: {}, stats: {}", progress.getActionName(), Constants.PERCENT_FORMAT.format(percent),
						getETA(progress),
						progress.getStats());

				AsyncUtil.threadSleep(UPDATE_INTERVAL_MILLIS);
			}
		});
	}

	private String getETA(SyncActionProgress currentProgress) {
		SyncActionProgress lastProgress = currentProgress.getLastProgress();
		if (lastProgress == null) {
			return "not available";
		}

		double lastPercent = lastProgress.getPercent();
		double currentPercent = currentProgress.getPercent();
		double percentSinceLastProgress = currentPercent - lastPercent;
		double percentRemaining = 1 - currentPercent;
		double nanosSinceLastProgress = currentProgress.getNanoTime() - lastProgress.getNanoTime();
		double secondsSinceLastProgress = nanosSinceLastProgress / NANOS_IN_SECOND;
		double secondsUntilDone = percentRemaining / percentSinceLastProgress * secondsSinceLastProgress;

		if (secondsUntilDone < SECONDS_IN_MINUTE) {
			return '~' + Constants.NUMBER_FORMAT.format(secondsUntilDone) + "sec";
		} else {
			double minutes = secondsUntilDone / SECONDS_IN_MINUTE;
			return '~' + Constants.NUMBER_FORMAT.format(minutes) + "min";
		}

	}
}
