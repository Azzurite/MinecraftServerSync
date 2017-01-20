package name.azzurite.mcserver;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import name.azzurite.mcserver.config.AppConfig;
import name.azzurite.mcserver.ftp.FTPServerSynchronizer;
import name.azzurite.mcserver.minecraft.ServerList;
import name.azzurite.mcserver.updates.UpdateChecker;
import name.azzurite.mcserver.util.LogUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class Main {

	private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

	private Main() {}

	public static void main(String[] args) throws IOException {

		//noinspection OverlyBroadCatchBlock
		try {
			AppConfig appConfig = AppConfig.readConfig();

			updateApplication(appConfig);

			createNecessaryDirectories(appConfig);

			FTPServerSynchronizer sync = new FTPServerSynchronizer(appConfig);

			try (Server server = new Server(appConfig, sync)) {
				ServerList.addOrReplace(server);
				startMinecraft(appConfig);
			}
			waitAndExit(0);
		} catch (Exception e) {
			LOGGER.error("Uncaught exception", e);
			waitAndExit(1);
		}
	}

	private static void updateApplication(AppConfig appConfig) throws URISyntaxException {
		try {
			UpdateChecker updateChecker = new UpdateChecker(appConfig);
				LOGGER.info("Checking for updates.");
			if (updateChecker.isNewerVersionAvailable()) {
				LOGGER.info("Update available!");
				if (updateChecker.shouldUpdate()) {
					openReleasesPage();
					LOGGER.info("Please download the latest release from the page opened in your browser.");
					waitAndExit(0);
				} else {
					LOGGER.info("Choosing not to update. Be careful!");
				}
			} else {
				LOGGER.info("You already have the latest version.");
			}
		} catch (IOException e) {
			LOGGER.warn("Error while checking version: {}", e.getMessage());
			LogUtil.stacktrace(LOGGER, e);
			LOGGER.info("Skipping version check.");
		}
	}

	private static void openReleasesPage() throws URISyntaxException, IOException {
		if (Desktop.isDesktopSupported()) {
			Desktop.getDesktop().browse(new URI(UpdateChecker.RELEASES_PAGE));
		} else {
			LOGGER.warn("Could not open browser automatically. Please navigate to the page {} manually.", UpdateChecker.RELEASES_PAGE);
			try {
				Thread.sleep(5000);
			} catch (InterruptedException ignored) {
			}
		}
	}


	private static void waitAndExit(int status) {
		try {
			System.out.print("Shutting down in 3... ");
			Thread.sleep(1000);
			System.out.print("2... ");
			Thread.sleep(1000);
			System.out.print("1... ");
			Thread.sleep(1000);
		} catch (InterruptedException ignored) {
		}
		System.exit(status);
	}

	private static void createNecessaryDirectories(AppConfig appConfig) throws IOException {
		Files.createDirectories(appConfig.getBaseServerPath());
		Files.createDirectories(appConfig.getSyncPath());
	}

	private static void startMinecraft(AppConfig appConfig) throws IOException {
		Optional<String> minecraftLauncherPath = appConfig.getMinecraftLauncherPath();
		if (minecraftLauncherPath.isPresent()) {
			Path path = Paths.get(minecraftLauncherPath.get());
			ProcessBuilder processBuilder = new ProcessBuilder(path.toString());
			processBuilder.directory(path.getParent().toFile());
			LOGGER.info("Starting Minecraft Launcher with system command: {}",
					LogUtil.getCommandString(processBuilder));
			processBuilder.start();
		}
	}
}
