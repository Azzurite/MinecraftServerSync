package name.azzurite.mcserver.view;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import name.azzurite.mcserver.server.LocalServerService;
import name.azzurite.mcserver.sync.ServerInfo;
import name.azzurite.mcserver.sync.ServerInfoService;

public class ServerButton implements NodeRepresentation {

	private final ServerInfoService serverInfoService;
	private final LocalServerService serverService;

	@FXML
	private Button serverButton;

	public ServerButton(ServerInfoService serverInfoService, LocalServerService serverService) {
		this.serverInfoService = serverInfoService;
		this.serverService = serverService;
		serverInfoService.addOnSucceeded(this::handleServerStatusChange);

		serverService.setOnFailed((e) -> {
			Alert alert = new Alert(Alert.AlertType.ERROR);
			alert.setHeaderText("Error while starting server");
			alert.setContentText(serverService.getException().getMessage());
			alert.show();
			serverService.reset();
		});

	}

	private void handleServerStatusChange(WorkerStateEvent event) {
		ServerInfo serverInfo = (ServerInfo) event.getSource().getValue();

		switch (serverInfo.getServerStatus()) {
			case ONLINE:
				serverButton.setDisable(true);
				break;
			case OFFLINE:
				serverButton.setText("Start server");
				serverButton.setDisable(false);
				break;
			case UPLOAD_IN_PROGRESS:
				serverButton.setDisable(true);
				break;
			case STARTED_LOCALLY:
				serverButton.setText("Stop server");
				serverButton.setDisable(false);
				break;
		}
	}

	@FXML
	private void serverButtonClicked(ActionEvent event) throws IOException, ExecutionException {
		ServerInfo serverInfo = serverInfoService.getValue();
		switch (serverInfo.getServerStatus()) {
			case STARTED_LOCALLY:
				serverService.cancel();
				break;
			case OFFLINE:
				serverService.start();
				break;
			case ONLINE:
			case UPLOAD_IN_PROGRESS:
				// do nothing
		}
	}

	@Override
	public Node toNodeRepresentation() {
		return serverButton;
	}
}
