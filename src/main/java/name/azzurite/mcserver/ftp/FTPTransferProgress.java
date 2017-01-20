package name.azzurite.mcserver.ftp;

import java.util.Collections;
import java.util.Map;

import com.jakewharton.byteunits.BinaryByteUnit;
import name.azzurite.mcserver.config.Constants;
import name.azzurite.mcserver.sync.SyncActionProgress;

public class FTPTransferProgress extends SyncActionProgress {

	private static final double BYTES_IN_GIBIBYTES = 1_000_000_000d;
	private static final double BYTES_IN_MEBIBYTES = 1_000_000d;
	private static final double BYTES_IN_KIBIBYTES = 1_000d;
	private static final double UNIT_READABLE_THRESHOLD = 1.5;
	private static final double NANOS_IN_SECOND = 1_000_000_000;
	private final long transferredBytes;
	private final long totalBytes;

	private FTPTransferProgress lastProgress;

	FTPTransferProgress(String actionName, long totalBytes, long transferredBytes, FTPTransferProgress lastProgress) {
		super(actionName);
		this.totalBytes = totalBytes;
		this.transferredBytes = transferredBytes;
		this.lastProgress = lastProgress;
	}

	@Override
	public double getPercent() {
		if (totalBytes == 0) {
			return Double.NaN;
		}
		return (double) transferredBytes / totalBytes;
	}

	@Override
	public Map<String, String> getAdditionalData() {
		return Collections.emptyMap();
	}

	@Override
	public String getStats() {
		if (lastProgress == null) {
			return "not available";
		}


		long nanosSinceLastProgress = getNanoTime() - lastProgress.getNanoTime();
		double secondsSinceLastProgress = nanosSinceLastProgress / NANOS_IN_SECOND;
		long bytesSinceLastProgress = transferredBytes - lastProgress.transferredBytes;

		String perSec = byteToReadableString((long) (bytesSinceLastProgress / secondsSinceLastProgress));
		String transferred = byteToReadableString(transferredBytes);
		String total = byteToReadableString(totalBytes);

		return perSec + "/s, transferred: " + transferred + ", total: " + total;
	}

	private BinaryByteUnit calcBestUnit(long bytes) {
		if (bytes / BYTES_IN_GIBIBYTES > UNIT_READABLE_THRESHOLD) {
			return BinaryByteUnit.GIBIBYTES;
		} else if (bytes / BYTES_IN_MEBIBYTES > UNIT_READABLE_THRESHOLD) {
			return BinaryByteUnit.MEBIBYTES;
		} else if (bytes / BYTES_IN_KIBIBYTES > UNIT_READABLE_THRESHOLD) {
			return BinaryByteUnit.KIBIBYTES;
		} else {
			return BinaryByteUnit.BYTES;
		}
	}

	private String byteToReadableString(long bytes) {
		BinaryByteUnit bestUnit = calcBestUnit(bytes);
		switch (bestUnit) {
			case KIBIBYTES:
				return Constants.NUMBER_FORMAT.format(bytes / BYTES_IN_KIBIBYTES) + "KiB";
			case MEBIBYTES:
				return Constants.NUMBER_FORMAT.format(bytes / BYTES_IN_MEBIBYTES) + "MiB";
			case GIBIBYTES:
				return Constants.NUMBER_FORMAT.format(bytes / BYTES_IN_GIBIBYTES) + "GiB";
			default:
				return Constants.NUMBER_FORMAT.format(bytes) + "B";
		}
	}

}
