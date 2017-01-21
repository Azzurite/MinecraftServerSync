package name.azzurite.mcserver.ftp;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.Future;

import name.azzurite.mcserver.sync.SyncActionFuture;
import name.azzurite.mcserver.sync.SyncActionProgress;

public class FTPTransferSyncActionFuture<R> extends SyncActionFuture<R> {

	private static final int NUMBER_OF_PROGRESSES_TO_AVERAGE = 10;
	private final String actionName;

	private final long fileSize;

	private final FTPTransferProgressListener progressListener;

	private final long startTotalTransferredBytes;

	private Queue<FTPTransferProgress> lastProgresses = new ArrayDeque<>();

	public FTPTransferSyncActionFuture(Future<R> future, FTPTransferProgressListener progressListener, String actionName, long fileSize) {
		super(future);
		this.progressListener = progressListener;
		this.startTotalTransferredBytes = progressListener.getCurrentTransferredBytes();
		this.actionName = actionName;
		this.fileSize = fileSize;
	}

	@Override
	public SyncActionProgress getProgress() {
		long tranferredBytes = progressListener.getCurrentTransferredBytes() - startTotalTransferredBytes;
		FTPTransferProgress referenceProgress = null;
		if (lastProgresses.size() >= NUMBER_OF_PROGRESSES_TO_AVERAGE) {
			referenceProgress = lastProgresses.poll();
		} else if (!lastProgresses.isEmpty()) {
			referenceProgress = lastProgresses.peek();
		}
		FTPTransferProgress ftpTransferProgress = new FTPTransferProgress(actionName, fileSize, tranferredBytes, referenceProgress);
		lastProgresses.offer(ftpTransferProgress);
		return ftpTransferProgress;
	}

}
