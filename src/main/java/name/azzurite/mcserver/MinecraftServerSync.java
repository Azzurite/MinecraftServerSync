package name.azzurite.mcserver;

import java.io.IOException;
import java.nio.file.Files;

import javafx.application.Application;
import javafx.concurrent.Worker;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import name.azzurite.mcserver.config.AppConfig;
import name.azzurite.mcserver.console.LocalConsole;
import name.azzurite.mcserver.ftp.FTPSyncClient;
import name.azzurite.mcserver.server.LocalServerService;
import name.azzurite.mcserver.sync.ServerInfoService;
import name.azzurite.mcserver.sync.ServerSynchronizer;
import name.azzurite.mcserver.sync.SyncClient;
import name.azzurite.mcserver.updates.UpdateChecker;
import name.azzurite.mcserver.util.LogUtil;
import name.azzurite.mcserver.view.MainWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class MinecraftServerSync extends Application {

	private static final Logger LOGGER = LoggerFactory.getLogger(MinecraftServerSync.class);
	private LocalServerService serverService;

	public MinecraftServerSync() {}

	public static void main(String[] args) throws IOException {
		try {
			Application.launch(args);
			System.exit(0);
		} catch (Exception e) {
			LOGGER.error("Uncaught exception", e);
			System.exit(1);
		}
	}

	private static void createNecessaryDirectories(AppConfig appConfig) throws IOException {
		Files.createDirectories(appConfig.getBaseServerPath());
		Files.createDirectories(appConfig.getSyncPath());
		Files.createDirectories(appConfig.getBackupPath());
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		AppConfig appConfig = AppConfig.readConfig();

		createNecessaryDirectories(appConfig);

		LocalConsole console = new LocalConsole();

		UpdateChecker updateChecker = null;
		try {
			updateChecker = new UpdateChecker(appConfig);
		} catch (IOException e) {
			LOGGER.warn("Error while checking version: {}", e.getMessage());
			LogUtil.stacktrace(LOGGER, e);
			LOGGER.info("Skipping version check.");
		}

		SyncClient syncClient = new FTPSyncClient(appConfig);
		ServerSynchronizer sync = new ServerSynchronizer(appConfig, syncClient);
		serverService = new LocalServerService(appConfig, sync, console);

		ServerInfoService serverInfoService = new ServerInfoService(sync, serverService);

		MainWindow.create(primaryStage, appConfig, sync, serverInfoService, updateChecker, serverService, console);

		primaryStage.setOnCloseRequest(event -> {
			if (serverService.getState() != Worker.State.READY
					&& serverService.getState() != Worker.State.FAILED) {
				event.consume();
				serverService.cancel();
				Alert alert = new Alert(Alert.AlertType.WARNING);
				alert.setHeaderText("Sync in progress");
				alert.setContentText("Please wait until the server has closed and synchronization is complete. If you ignore this warning, " +
						"your Minecraft world may be corrupted!");
				alert.show();
			}
		});
	}

}
