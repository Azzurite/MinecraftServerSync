package name.azzurite.mcserver.view;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import name.azzurite.mcserver.server.LocalServerService;
import name.azzurite.mcserver.sync.ServerInfo;
import name.azzurite.mcserver.sync.ServerInfoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerButton implements NodeRepresentation {

	private static final Logger LOGGER = LoggerFactory.getLogger(ServerButton.class);
	private final ServerInfoService serverInfoService;
	private final LocalServerService serverService;

	@FXML
	private Button serverButton;

	public ServerButton(ServerInfoService serverInfoService, LocalServerService serverService) {
		this.serverInfoService = serverInfoService;
		this.serverService = serverService;
		serverInfoService.addOnSucceeded(this::handleServerStatusChange);
	}

	private void handleServerStatusChange(WorkerStateEvent event) {
		ServerInfo serverInfo = (ServerInfo) event.getSource().getValue();

		switch (serverInfo.getServerStatus()) {
			case REMOTE_ONLINE:
				serverButton.setDisable(true);
				break;
			case OFFLINE:
				serverButton.setText("Start server");
				serverButton.setDisable(false);
				break;
			case REMOTE_UPLOADING:
				serverButton.setDisable(true);
				break;
			case LOCALLY_ONLINE:
				serverButton.setText("Stop server");
				serverButton.setDisable(false);
				break;
			case LOCALLY_UPLOADING:
				serverButton.setDisable(true);
				break;
			case LOCALLY_DOWNLOADING:
				serverButton.setDisable(true);
				break;
		}
	}

	@FXML
	private void serverButtonClicked(ActionEvent event) throws IOException, ExecutionException {

		ServerInfo serverInfo = serverInfoService.getValue();
		switch (serverInfo.getServerStatus()) {
			case LOCALLY_ONLINE:
				serverService.requestShutdown();
				break;
			case OFFLINE:
				startServer();
				break;
			case REMOTE_ONLINE:
			case REMOTE_UPLOADING:
			case LOCALLY_UPLOADING:
			case LOCALLY_DOWNLOADING:
				// do nothing

		}
	}

	private void startServer() {
		if (serverService.isRunning()) {
			LOGGER.info("Still waiting for other action...");
			return;
		}

		serverService.reset();
		serverService.start();
	}

	@Override
	public Node toNodeRepresentation() {
		return serverButton;
	}
}
