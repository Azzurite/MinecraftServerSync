package name.azzurite.mcserver.view;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import name.azzurite.mcserver.sync.ServerInfo;
import name.azzurite.mcserver.sync.ServerInfoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerStatusDisplay implements NodeRepresentation {


	private static final Logger LOGGER = LoggerFactory.getLogger(ServerStatusDisplay.class);

	private final ServerInfoService serverInfoService;

	@FXML
	private HBox root;

	@FXML
	private Label serverStatusLabel;


	public ServerStatusDisplay(ServerInfoService serverInfoService) {
		this.serverInfoService = serverInfoService;

		serverInfoService.addOnSucceeded((e) -> {
			ServerInfo serverInfo = (ServerInfo) e.getSource().getValue();
			LOGGER.info("Server status: {}", serverInfo.getServerStatus());
			switch (serverInfo.getServerStatus()) {
				case ONLINE:
					serverStatusLabel.setTextFill(Color.GREEN);
					serverStatusLabel.setText("Online");
					break;
				case OFFLINE:
					serverStatusLabel.setTextFill(Color.RED);
					serverStatusLabel.setText("Offline");
					break;
				case UPLOAD_IN_PROGRESS:
					serverStatusLabel.setTextFill(Color.ORANGE);
					serverStatusLabel.setText("Upload in progress");
					break;
				case STARTED_LOCALLY:
					serverStatusLabel.setTextFill(Color.GREEN);
					serverStatusLabel.setText("Started locally");
					break;
			}
		});
	}

	@FXML
	private void updateServerStatus(ActionEvent event) {
		checkServerStatus();
	}

	@Override
	public Node toNodeRepresentation() {
		return root;
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
