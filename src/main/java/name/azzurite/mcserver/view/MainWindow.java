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
import name.azzurite.mcserver.util.FXUtil;
import name.azzurite.mcserver.view.override.OverrideMenu;

public final class MainWindow {

	@FXML
	private BorderPane root;

	@FXML
	private GridPane topBar;

	@FXML
	private VBox menu;

	@FXML
	private VBox overrideMenu;

	public static MainWindow create(Stage primaryStage, ConsoleView consoleView, UpdateDisplay updateDisplay, ServerStatusDisplay
			serverStatusDisplay, ServerButton serverButton, StartMinecraftButton startMinecraftButton, OverrideMenu overrideMenu) throws
			IOException {
		primaryStage.setTitle("MinecraftServerSync");
		primaryStage.getIcons().add(FXUtil.retrieveClassPathImage("icon.png"));

		MainWindow mainWindow = FXUtil.createFXMLController(MainWindow.class);
		addToMainWindow(mainWindow, consoleView);
		addToMainWindow(mainWindow, updateDisplay);
		addToMainWindow(mainWindow, serverStatusDisplay);
		addToMainWindow(mainWindow, serverButton);
		addToMainWindow(mainWindow, startMinecraftButton);
		addToMainWindow(mainWindow, overrideMenu);

		Scene scene = createScene(mainWindow, primaryStage);
		primaryStage.setScene(scene);

		primaryStage.show();
		setDimensions(primaryStage);

		return mainWindow;

	}

	private static void addToMainWindow(MainWindow mainWindow, OverrideMenu overrideMenuController) {
		mainWindow.overrideMenu.getChildren().add(overrideMenuController.toNodeRepresentation());
	}

	private static void addToMainWindow(MainWindow mainWindow, StartMinecraftButton minecraftButton) {
		mainWindow.addMenuButton(minecraftButton.toNodeRepresentation());
	}

	private static void addToMainWindow(MainWindow mainWindow, ServerButton serverButton) {
		mainWindow.addMenuButton(serverButton.toNodeRepresentation());
	}

	private static void addToMainWindow(MainWindow mainWindow, ServerStatusDisplay serverStatusDisplay) {
		mainWindow.topBar.add(serverStatusDisplay.toNodeRepresentation(), 0, 0);
	}

	private static void addToMainWindow(MainWindow mainWindow, UpdateDisplay updateDisplay) {
		mainWindow.topBar.add(updateDisplay.toNodeRepresentation(), 1, 0);
	}

	private static void addToMainWindow(MainWindow mainWindow, ConsoleView consoleView) {
		mainWindow.root.setCenter(consoleView.toNodeRepresentation());
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

}
