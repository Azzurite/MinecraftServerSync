package name.azzurite.mcserver.ftp;

import java.util.Collections;
import java.util.Map;

import name.azzurite.mcserver.sync.SyncActionProgress;

public class FTPTransferProgress extends SyncActionProgress {

	private final long fileSizeTotalBytes;

	private final FTPTransferProgressListener listener;

	public FTPTransferProgress(String actionName, long fileSizeTotalBytes, FTPTransferProgressListener listener) {
		super(actionName);
		this.fileSizeTotalBytes = fileSizeTotalBytes;
		this.listener = listener;
	}

	@Override
	public double getPercent() {
		return (double) listener.getCurrentTransferTotalBytes() / fileSizeTotalBytes;
	}

	@Override
	public Map<String, String> getAdditionalData() {
		return Collections.emptyMap();
	}

	@Override
	public String toHumanReadableString() {
		return "";
	}
}
