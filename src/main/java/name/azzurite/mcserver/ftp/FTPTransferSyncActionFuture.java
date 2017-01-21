package name.azzurite.mcserver.ftp;

import java.util.concurrent.Future;

import name.azzurite.mcserver.sync.SyncActionFuture;
import name.azzurite.mcserver.sync.SyncActionProgress;

public class FTPTransferSyncActionFuture<R> extends SyncActionFuture<R> {

	private final String actionName;

	private final long fileSize;

	private final FTPTransferProgressListener progressListener;

	private final long startTotalTransferredBytes;

	private FTPTransferProgress lastProgress;

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
		FTPTransferProgress ftpTransferProgress = new FTPTransferProgress(actionName, fileSize, tranferredBytes, lastProgress);
		lastProgress = ftpTransferProgress;
		return ftpTransferProgress;
	}

}
