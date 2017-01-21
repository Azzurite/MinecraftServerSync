package name.azzurite.mcserver.ftp;

import org.apache.commons.net.io.CopyStreamEvent;
import org.apache.commons.net.io.CopyStreamListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class FTPTransferProgressListener implements CopyStreamListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(FTPTransferProgressListener.class);

	private long currentTransferredBytes = Long.MIN_VALUE;

	/**
	 * Only valid when used as a difference of different invocations, to get the amount of bytes transfered between two different points in time.
	 *
	 * @return
	 */
	public long getCurrentTransferredBytes() {
		return currentTransferredBytes;
	}

	@Override
	public void bytesTransferred(CopyStreamEvent event) {
		bytesTransferred(event.getTotalBytesTransferred(), event.getBytesTransferred(), event.getStreamSize());
	}

	@Override
	public void bytesTransferred(long totalBytesTransferred, int bytesTransferred, long streamSize) {
		currentTransferredBytes += bytesTransferred;
	}
}
