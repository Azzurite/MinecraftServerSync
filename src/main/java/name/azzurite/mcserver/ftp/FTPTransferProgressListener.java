package name.azzurite.mcserver.ftp;

import org.apache.commons.net.io.CopyStreamEvent;
import org.apache.commons.net.io.CopyStreamListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class FTPTransferProgressListener implements CopyStreamListener {
	private static final Logger LOGGER = LoggerFactory.getLogger(FTPTransferProgressListener.class);

	private long currentTotal;

	public long getCurrentTransferTotalBytes() {
		return currentTotal;
	}

	@Override
	public void bytesTransferred(CopyStreamEvent event) {
		bytesTransferred(event.getTotalBytesTransferred(), event.getBytesTransferred(), event.getStreamSize());
	}

	@Override
	public void bytesTransferred(long totalBytesTransferred, int bytesTransferred, long streamSize) {
		currentTotal = totalBytesTransferred;
	}
}
