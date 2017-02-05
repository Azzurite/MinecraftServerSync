package name.azzurite.mcserver.server;

import java.io.Closeable;
import java.io.IOException;

public interface Server extends Closeable {

	@Override
	void close() throws IOException;

	boolean isRunning();

	void addOnCloseCallback(Runnable onClose);
}
