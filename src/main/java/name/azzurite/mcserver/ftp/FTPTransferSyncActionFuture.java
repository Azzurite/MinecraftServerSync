package name.azzurite.mcserver.ftp;

import java.util.concurrent.Future;

import name.azzurite.mcserver.sync.SyncActionFuture;
import name.azzurite.mcserver.sync.SyncActionProgress;

public class FTPTransferSyncActionFuture<R> extends SyncActionFuture<R> {

	private final String actionName;

	private final long fileSize;

	private final FTPTransferProgressListener progressListener;

	public FTPTransferSyncActionFuture(Future<R> future, FTPTransferProgressListener progressListener, String actionName, long fileSize) {
		super(future);
		this.progressListener = progressListener;
		this.actionName = actionName;
		this.fileSize = fileSize;
	}

	@Override
	public SyncActionProgress getProgress() {
		return new FTPTransferProgress(actionName, fileSize, progressListener);
	}

}
