package name.azzurite.mcserver.view;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import name.azzurite.mcserver.config.AppConfig;
import name.azzurite.mcserver.updates.UpdateChecker;
import name.azzurite.mcserver.util.AsyncUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpdateDisplay extends WithRootFXMLNode {


	private static final Logger LOGGER = LoggerFactory.getLogger(UpdateDisplay.class);

	private static final String INFO_TEMPLATE =
			"Your current version of MinecraftServerSync (v%s) is out of date. The latest version is v%s. If there was a major version bump (e.g. " +
					"v1.x.x to v2.x.x) and you choose not to update, MinecraftServerSync will misbehave and corrupt your server files!";

	private final UpdateChecker updateChecker;

	private final AppConfig appConfig;


	@FXML
	private StackPane updatesPane;

	@FXML
	private StackPane tooltipArea;

	@FXML
	private HBox checkFailed;

	@FXML
	private HBox newVersion;

	@FXML
	private Label currentVersion;

	public UpdateDisplay(AppConfig appConfig, UpdateChecker updateChecker) {
		this.appConfig = appConfig;
		this.updateChecker = updateChecker;
	}

	@FXML
	void onUpdateClicked(ActionEvent event) {
		if (Desktop.isDesktopSupported()) {
			try {
				Desktop.getDesktop().browse(new URI(UpdateChecker.RELEASES_PAGE));
				return;
			} catch (URISyntaxException | IOException e) {
				LOGGER.debug("Error while opening browser", e);
			}
		}

		LOGGER.warn("Could not open browser automatically. Please navigate to the page {} manually.", UpdateChecker.RELEASES_PAGE);
		AsyncUtil.threadSleep(5000);
	}

	@FXML
	private void initialize() {
		setCurrentVersion();

		setCorrectUpdateStatus();
	}

	private void setCorrectUpdateStatus() {
		updatesPane.getChildren().clear();

		LOGGER.info("Checking for updates...");

		if (updateChecker == null) {
			LOGGER.info("Error while checking for updates.");

			updatesPane.getChildren().add(checkFailed);
		} else if (updateChecker.isNewerVersionAvailable()) {
			LOGGER.info("Newer version available: v{}. Current version: v{}.", updateChecker.getLatestVersion(), appConfig.getAppVersion());

			updatesPane.getChildren().add(newVersion);
			tooltipArea.setOnMouseClicked((e) -> {
				Alert alert = new Alert(Alert.AlertType.INFORMATION);
				alert.setContentText(String.format(INFO_TEMPLATE, appConfig.getAppVersion(), updateChecker.getLatestVersion()));
				alert.show();
			});
		}
	}

	private void setCurrentVersion() {
		LOGGER.info("Current version: {}", appConfig.getAppVersion());
		currentVersion.setText(appConfig.getAppVersion().toString());
	}

}
