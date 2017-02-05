package name.azzurite.mcserver;

import java.io.IOException;
import java.nio.file.Files;

import javafx.application.Application;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import name.azzurite.mcserver.config.AppConfig;
import name.azzurite.mcserver.console.LocalConsole;
import name.azzurite.mcserver.ftp.FTPSyncClient;
import name.azzurite.mcserver.server.LocalServerService;
import name.azzurite.mcserver.server.SyncingLocalServer;
import name.azzurite.mcserver.sync.ServerInfoService;
import name.azzurite.mcserver.sync.ServerSynchronizer;
import name.azzurite.mcserver.sync.SyncClient;
import name.azzurite.mcserver.updates.UpdateChecker;
import name.azzurite.mcserver.util.FXUtil;
import name.azzurite.mcserver.util.LogUtil;
import name.azzurite.mcserver.view.ConsoleView;
import name.azzurite.mcserver.view.MainWindow;
import name.azzurite.mcserver.view.ServerButton;
import name.azzurite.mcserver.view.ServerStatusDisplay;
import name.azzurite.mcserver.view.StartMinecraftButton;
import name.azzurite.mcserver.view.UpdateDisplay;
import name.azzurite.mcserver.view.override.DownloadFilesOverride;
import name.azzurite.mcserver.view.override.OverrideMenu;
import name.azzurite.mcserver.view.override.SetOfflineOverride;
import name.azzurite.mcserver.view.override.UploadFilesOverride;
import name.azzurite.mcserver.view.override.UploadInProgressOverride;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class MinecraftServerSync extends Application {

	private static final Logger LOGGER = LoggerFactory.getLogger(MinecraftServerSync.class);
	private LocalServerService serverService;
	private ServerSynchronizer sync;
	private UpdateChecker updateChecker;
	private ServerInfoService serverInfoService;
	private AppConfig appConfig;
	private Stage primaryStage;
	private LocalConsole console;

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

	private static OverrideMenu createOverrideMenu(ServerInfoService serverInfoService,
			LocalServerService serverService) throws
			IOException {
		OverrideMenu overrideMenu = FXUtil.createFXMLController(OverrideMenu.class, unused -> new OverrideMenu(serverInfoService));

		SetOfflineOverride setOfflineOverride =
				FXUtil.createFXMLController(SetOfflineOverride.class, unused -> new SetOfflineOverride(serverService));
		overrideMenu.addOverrideButton(setOfflineOverride);

		DownloadFilesOverride downloadFilesOverride =
				FXUtil.createFXMLController(DownloadFilesOverride.class, unused -> new DownloadFilesOverride(serverService));
		overrideMenu.addOverrideButton(downloadFilesOverride);

		UploadFilesOverride uploadFilesOverride =
				FXUtil.createFXMLController(UploadFilesOverride.class, unused -> new UploadFilesOverride(serverService));
		overrideMenu.addOverrideButton(uploadFilesOverride);

		UploadInProgressOverride uploadInProgressOverride =
				FXUtil.createFXMLController(UploadInProgressOverride.class, unused -> new UploadInProgressOverride(serverService));
		overrideMenu.addOverrideButton(uploadInProgressOverride);

		return overrideMenu;
	}

	private static StartMinecraftButton createStartMinecraftButton(AppConfig appConfig, ServerInfoService serverInfoService) throws
			IOException {
		return FXUtil.createFXMLController(StartMinecraftButton.class, unused -> new StartMinecraftButton
				(appConfig, serverInfoService));
	}

	private static ServerButton createServerButton(ServerInfoService serverInfoService, LocalServerService serverRunner) throws
			IOException {
		return FXUtil.createFXMLController(ServerButton.class, unused -> new ServerButton(serverInfoService, serverRunner));
	}

	private static ServerStatusDisplay createServerStatusDisplay(ServerInfoService serverInfoService) throws IOException {
		return
				FXUtil.createFXMLController(ServerStatusDisplay.class, unused -> new ServerStatusDisplay(serverInfoService));
	}

	private static UpdateDisplay createUpdateDisplay(AppConfig appConfig, UpdateChecker updateChecker) throws IOException {
		return FXUtil.createFXMLController(UpdateDisplay.class, unused -> new UpdateDisplay(appConfig, updateChecker));
	}

	private static ConsoleView createConsoleView(LocalConsole console) throws IOException {
		return FXUtil.createFXMLController(ConsoleView.class, unused -> new ConsoleView(console));
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		this.primaryStage = primaryStage;

		appConfig = AppConfig.readConfig();
		createNecessaryDirectories(appConfig);

		console = new LocalConsole();

		try {
			updateChecker = new UpdateChecker(appConfig);
		} catch (IOException e) {
			LOGGER.warn("Error while checking version: {}", e.getMessage());
			LogUtil.stacktrace(LOGGER, e);
			LOGGER.info("Skipping version check.");
		}

		SyncClient syncClient = new FTPSyncClient(appConfig);
		sync = new ServerSynchronizer(appConfig, syncClient);

		serverService = new LocalServerService(appConfig, sync, console);
		primaryStage.setOnCloseRequest(this::preventCloseUntilServerShutdown);

		serverInfoService = new ServerInfoService(sync, serverService);

		createGui();
	}

	private void preventCloseUntilServerShutdown(WindowEvent event) {
		if (serverService.getLocalServerStatus() != SyncingLocalServer.SyncingLocalServerStatus.OFFLINE) {
			event.consume();
			serverService.cancel();
			Alert alert = new Alert(Alert.AlertType.WARNING);
			alert.setHeaderText("Sync in progress");
			alert.setContentText("Please wait until the server has closed and synchronization is complete. If you ignore this warning, " +
					"your Minecraft world may be corrupted!");
			alert.show();
		}
	}

	private void createGui() throws
			IOException {
		ConsoleView consoleView = createConsoleView(console);
		UpdateDisplay updateDisplay = createUpdateDisplay(appConfig, updateChecker);
		ServerStatusDisplay serverStatusDisplay = createServerStatusDisplay(serverInfoService);
		ServerButton serverButton = createServerButton(serverInfoService, serverService);
		StartMinecraftButton startMinecraftButton = createStartMinecraftButton(appConfig, serverInfoService);
		OverrideMenu overrideMenu = createOverrideMenu(serverInfoService, serverService);

		MainWindow.create(primaryStage, consoleView, updateDisplay, serverStatusDisplay, serverButton, startMinecraftButton, overrideMenu);
	}

}
