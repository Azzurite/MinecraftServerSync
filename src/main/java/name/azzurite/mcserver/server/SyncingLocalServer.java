package name.azzurite.mcserver.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import name.azzurite.mcserver.config.AppConfig;
import name.azzurite.mcserver.console.LocalConsole;
import name.azzurite.mcserver.sync.ServerSynchronizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static name.azzurite.mcserver.util.LambdaExceptionUtil.*;

public class SyncingLocalServer implements Server {

	private static final Logger LOGGER = LoggerFactory.getLogger(SyncingLocalServer.class);

	private final AppConfig appConfig;

	private final ServerSynchronizer sync;

	private final LocalServer localServer;

	private final LocalConsole console;

	private final List<Runnable> onCloseCallbacks = new ArrayList<>();

	private boolean running;

	public SyncingLocalServer(AppConfig appConfig, ServerSynchronizer sync, LocalConsole console) throws IOException, ExecutionException {
		this.appConfig = appConfig;
		this.sync = sync;
		this.console = console;
		localServer = createLocalServer();
		localServer.addOnCloseCallback(rethrow(this::close));
		running = true;
	}

	private static String retrieveExternalIP() throws MalformedURLException {
		URL checkIp = new URL("http://checkip.amazonaws.com");
		while (true) {
			//noinspection OverlyBroadCatchBlock
			try (BufferedReader in = new BufferedReader(new InputStreamReader(
					checkIp.openStream(), "UTF-8"))) {

				return in.readLine(); //you get the IP as a String
			} catch (IOException e) {
				LOGGER.error("Error retrieving external IP address, trying again.", e);
			}
		}

	}

	@Override
	public boolean isRunning() {
		return running;
	}

	@Override
	public void addOnCloseCallback(Runnable onClose) {
		onCloseCallbacks.add(onClose);
	}

	@Override
	public void close() throws IOException {
		if (!running) {
			return;
		}

		localServer.close();

		try {
			sync.saveFiles();
			sync.deleteServerIp();
		} catch (ExecutionException e) {
			throw new IOException(e);
		} finally {
			running = false;
			onCloseCallbacks.forEach(Runnable::run);
		}
	}

	@SuppressWarnings("UseOfSystemOutOrSystemErr")
	private LocalServer createLocalServer() throws IOException, ExecutionException {
		sync.retrieveFiles();
		sync.setServerIp(retrieveExternalIP());

		return new LocalServer(appConfig, sync, console);
	}

}
