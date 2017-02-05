package name.azzurite.mcserver.view;

import java.io.IOException;
import java.net.URL;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import name.azzurite.mcserver.config.AppConfig;
import name.azzurite.mcserver.console.LocalConsole;
import name.azzurite.mcserver.server.LocalServerService;
import name.azzurite.mcserver.sync.ServerInfoService;
import name.azzurite.mcserver.sync.ServerSynchronizer;
import name.azzurite.mcserver.updates.UpdateChecker;
import name.azzurite.mcserver.util.FXUtil;

public final class MainWindow {

	@FXML
	private BorderPane root;

	@FXML
	private GridPane topBar;

	@FXML
	private VBox menu;

	@FXML
	private VBox overrideMenu;

	public static MainWindow create(Stage primaryStage, AppConfig appConfig, ServerSynchronizer sync, ServerInfoService serverInfoService,
			UpdateChecker updateChecker, LocalServerService serverRunner, LocalConsole console) throws IOException {
		primaryStage.setTitle("MinecraftServerSync");
		primaryStage.getIcons().add(FXUtil.retrieveClassPathImage("icon.png"));

		MainWindow mainWindow = FXUtil.createFXMLController(MainWindow.class);
		createConsoleView(mainWindow, console);
		createUpdateDisplay(mainWindow, appConfig, updateChecker);
		createServerStatusDisplay(mainWindow, serverInfoService);
		createServerButton(mainWindow, serverInfoService, serverRunner);
		createStartMinecraftButton(mainWindow, appConfig, serverInfoService);

		Scene scene = createScene(mainWindow, primaryStage);
		primaryStage.setScene(scene);

		primaryStage.show();
		setDimensions(primaryStage);

		return mainWindow;

	}

	private static void createStartMinecraftButton(MainWindow mainWindow, AppConfig appConfig, ServerInfoService serverInfoService) throws
			IOException {
		StartMinecraftButton minecraftButton = FXUtil.createFXMLController(StartMinecraftButton.class, unused -> new StartMinecraftButton
				(appConfig, serverInfoService));
		mainWindow.addMenuButton(minecraftButton.toNodeRepresentation());
	}

	private static void createServerButton(MainWindow mainWindow, ServerInfoService serverInfoService, LocalServerService serverRunner) throws
			IOException {
		ServerButton serverButton = FXUtil.createFXMLController(ServerButton.class, unused -> new ServerButton(serverInfoService, serverRunner));
		mainWindow.addMenuButton(serverButton.toNodeRepresentation());
	}

	private static void createServerStatusDisplay(MainWindow mainWindow, ServerInfoService serverInfoService) throws IOException {
		ServerStatusDisplay serverStatusDisplay =
				FXUtil.createFXMLController(ServerStatusDisplay.class, unused -> new ServerStatusDisplay(serverInfoService));
		mainWindow.setServerStatusDisplay(serverStatusDisplay);
	}

	private static void createUpdateDisplay(MainWindow mainWindow, AppConfig appConfig, UpdateChecker updateChecker) throws IOException {
		UpdateDisplay updateDisplay = FXUtil.createFXMLController(UpdateDisplay.class, unused -> new UpdateDisplay(appConfig, updateChecker));
		mainWindow.setUpdateChecker(updateDisplay);
	}

	private static void createConsoleView(MainWindow mainWindow, LocalConsole console) throws IOException {
		ConsoleView consoleView = FXUtil.createFXMLController(ConsoleView.class, unused -> new ConsoleView(console));
		mainWindow.setConsole(consoleView);
	}

	private static void setDimensions(Stage primaryStage) {
		primaryStage.setMinWidth(primaryStage.getWidth());
		primaryStage.setMinHeight(primaryStage.getHeight());

		primaryStage.setWidth(800);
		primaryStage.setHeight(600);
	}

	private static Scene createScene(MainWindow mainWindow, Stage primaryStage) {
		Scene scene = new Scene(mainWindow.root);
		addAppCSS(scene);
		return scene;
	}

	private static void addAppCSS(Scene scene) {
		URL cssUrl = MainWindow.class.getClassLoader().getResource("fxml/MinecraftServerSync.css");
		scene.getStylesheets().add(cssUrl.toString());
	}

	private void addMenuButton(Node node) {
		menu.getChildren().add(node);
	}

	private void setServerStatusDisplay(ServerStatusDisplay serverStatusDisplay) {
		topBar.add(serverStatusDisplay.toNodeRepresentation(), 0, 0);
	}

	private void setUpdateChecker(UpdateDisplay updateChecker) {
		topBar.add(updateChecker.toNodeRepresentation(), 1, 0);
	}

	private void setConsole(ConsoleView consoleView) throws IOException {
		root.setCenter(consoleView.toNodeRepresentation());
	}

}
