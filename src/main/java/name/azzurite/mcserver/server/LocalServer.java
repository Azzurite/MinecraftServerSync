package name.azzurite.mcserver.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import name.azzurite.mcserver.config.AppConfig;
import name.azzurite.mcserver.console.LocalConsole;
import name.azzurite.mcserver.sync.ServerSynchronizer;
import name.azzurite.mcserver.util.LogUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocalServer implements Server {

	private static final Logger LOGGER = LoggerFactory.getLogger(LocalServer.class);

	private final AppConfig appConfig;
	private final ServerSynchronizer sync;
	private final LocalConsole console;
	private final List<Runnable> onCloseCallbacks = new ArrayList<>();
	private Process serverProcess;
	private Thread unexpectedProcessTerminateThread;

	LocalServer(AppConfig appConfig, ServerSynchronizer sync, LocalConsole console) throws IOException {
		this.appConfig = appConfig;
		this.sync = sync;
		this.console = console;

		startLocal();
		listenForUnexpectedTerminate();
	}


	@SuppressWarnings({"OverlyBroadThrowsClause", "UseOfSystemOutOrSystemErr"})
	private void startLocal() throws IOException {
		serverProcess = runServer();
	}

	@Override
	public void close() throws IOException {
		if (serverProcess == null) {
			return;
		}
		Process detached = serverProcess;
		serverProcess = null;

		console.send("stop");
		waitUntilServerTerminated(detached);

		onCloseCallbacks.forEach(Runnable::run);
	}

	@Override
	public boolean isRunning() {
		return serverProcess != null && serverProcess.isAlive();
	}

	@Override
	public void addOnCloseCallback(Runnable onClose) {
		onCloseCallbacks.add(onClose);
	}


	private void waitUntilServerTerminated(Process serverProcess) {
		while (true) {
			try {
				boolean closed = serverProcess.waitFor(1, TimeUnit.SECONDS);
				if (closed) {
					break;
				}
			} catch (InterruptedException ignored) {
				// we should never be interrupted, but even if, we still want to wait for the server to terminate
			}
		}
	}


	private Process runServer() throws IOException {
		ProcessBuilder processBuilder = new ProcessBuilder("cmd", "/c", "java -Xms512M -Xmx" +
				appConfig.getServerMaxMemory() + " -XX:+UseConcMarkSweepGC -jar spigot.jar");
		processBuilder.directory(appConfig.getBaseServerPath().toFile());

		LOGGER.info("Launching server with system command: {}", LogUtil.getCommandString(processBuilder));
		Process serverProcess = processBuilder.start();

		console.addInRedirect(serverProcess.getOutputStream());
		console.addOutput(serverProcess.getInputStream());

		return serverProcess;
	}

	private void listenForUnexpectedTerminate() {
		unexpectedProcessTerminateThread = new Thread(() -> {
			waitUntilServerTerminated(serverProcess);
			try {
				close();
			} catch (IOException e) {
				// should never happen
				LOGGER.error("Error while closing server: {}", e.getMessage());
				LogUtil.stacktrace(LOGGER, e);
			}
		});
		unexpectedProcessTerminateThread.setDaemon(true);
		unexpectedProcessTerminateThread.start();
	}

}
