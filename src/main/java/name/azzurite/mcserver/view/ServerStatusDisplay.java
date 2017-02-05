package name.azzurite.mcserver.view;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import name.azzurite.mcserver.sync.ServerInfo;
import name.azzurite.mcserver.sync.ServerInfoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerStatusDisplay extends WithRootFXMLNode {


	private static final Logger LOGGER = LoggerFactory.getLogger(ServerStatusDisplay.class);

	private final ServerInfoService serverInfoService;


	@FXML
	private Label serverStatusLabel;


	public ServerStatusDisplay(ServerInfoService serverInfoService) {
		this.serverInfoService = serverInfoService;

		serverInfoService.addOnSucceeded((e) -> {
			ServerInfo serverInfo = (ServerInfo) e.getSource().getValue();
			LOGGER.info("Server status: {}", serverInfo.getServerStatus());
			switch (serverInfo.getServerStatus()) {
				case REMOTE_ONLINE:
					serverStatusLabel.setTextFill(Color.GREEN);
					serverStatusLabel.setText("Online");
					break;
				case OFFLINE:
					serverStatusLabel.setTextFill(Color.RED);
					serverStatusLabel.setText("Offline");
					break;
				case REMOTE_UPLOADING:
					serverStatusLabel.setTextFill(Color.ORANGE);
					serverStatusLabel.setText("Upload in progress");
					break;
				case LOCALLY_ONLINE:
					serverStatusLabel.setTextFill(Color.GREEN);
					serverStatusLabel.setText("Started locally");
					break;
				case LOCALLY_UPLOADING:
					serverStatusLabel.setTextFill(Color.ORANGE);
					serverStatusLabel.setText("Uploading server files");
					break;
				case LOCALLY_DOWNLOADING:
					serverStatusLabel.setTextFill(Color.ORANGE);
					serverStatusLabel.setText("Downloading server files");
					break;
			}
		});
	}

	@FXML
	private void updateServerStatus(ActionEvent event) {
		checkServerStatus();
	}


	@FXML
	private void initialize() {
		checkServerStatus();
	}

	private void checkServerStatus() {
		LOGGER.info("Checking server status...");
		serverInfoService.recheckServerStatus();
	}

}
