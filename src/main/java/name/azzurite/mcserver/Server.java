package name.azzurite.mcserver;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;

import name.azzurite.mcserver.config.AppConfig;
import name.azzurite.mcserver.util.LogUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Server implements Closeable {

	private static final Logger LOGGER = LoggerFactory.getLogger(Server.class);

	private static final String LOCALHOST = "localhost";


	private final AppConfig appConfig;

	private final ServerSynchronizer sync;

	private final String ip;

	private Process serverProcess;

	Server(AppConfig appConfig, ServerSynchronizer sync) throws IOException {
		this.appConfig = appConfig;
		this.sync = sync;

		Optional<String> serverIp = sync.getServerIp();
		if (serverIp.isPresent()) {
			ip = serverIp.get();
			LOGGER.info("Server is already hosted at {}", ip);
			LOGGER.info("No need to launch a local server, will launch Minecraft and then shut down");
		} else {
			LOGGER.info("No server hosted yet, starting local instance");
			startLocal();
			ip = LOCALHOST;
		}
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

	@SuppressWarnings({"OverlyBroadThrowsClause", "UseOfSystemOutOrSystemErr"})
	private void startLocal() throws IOException {
		System.out.println('\n' +
				"###############################################\n" +
				"##                                           ##\n" +
				"##         This is the sync window,          ##\n" +
				"##        do not close it manually!!         ##\n" +
				"##                                           ##\n" +
				"###############################################\n");
		sync.retrieveFiles();
		serverProcess = runServer();
		sync.setServerIp(retrieveExternalIP());
	}

	@Override
	public void close() throws IOException {
		if (serverProcess == null) {
			return;
		}

		waitUntilServerTerminated();
		sync.deleteServerIp();
		sync.saveFiles();
	}

	@SuppressWarnings("UseOfSystemOutOrSystemErr")
	private void waitUntilServerTerminated() {
		Thread thread = new Thread(() -> {
			try {
				Thread.sleep(10000);
			} catch (InterruptedException ignored) {
			}
			System.out.println();
			LOGGER.info("Enter 'stop' to exit the server and start syncing files");
			System.out.print(">");
		});
		thread.start();
		while (true) {
			try {
				serverProcess.waitFor();
				break;
			} catch (InterruptedException ignored) {
				// we should never be interrupted, but even if, we still want to wait for the server to terminate
			}
		}
	}

	public String getIp() {
		return ip;
	}

	private Process runServer() throws IOException {
		ProcessBuilder processBuilder = new ProcessBuilder("cmd", "/c", "java -Xms512M -Xmx" +
				appConfig.getServerMaxMemory() + " -XX:+UseConcMarkSweepGC -jar spigot.jar");
		processBuilder.directory(appConfig.getBaseServerPath().toFile());
		processBuilder.inheritIO();
		LOGGER.info("Launching server with system command: {}", LogUtil.getCommandString(processBuilder));
		return processBuilder.start();
	}

}
