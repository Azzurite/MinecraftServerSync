package name.azzurite.mcserver.view.override;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import name.azzurite.mcserver.server.LocalServerService;
import name.azzurite.mcserver.sync.ServerStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UploadFilesOverride extends OverrideButton {

	private static final Logger LOGGER = LoggerFactory.getLogger(UploadFilesOverride.class);

	private final LocalServerService localServerService;

	public UploadFilesOverride(LocalServerService localServerService) {
		super(ServerStatus.OFFLINE);

		this.localServerService = localServerService;
	}

	@FXML
	private void onUploadFiles(ActionEvent event) {
		if (localServerService.isRunning()) {
			LOGGER.info("Still waiting for other action...");
			return;
		}

		LOGGER.info("Uploading server files...");
		localServerService.reset();
		localServerService.startUploadServerFiles();
	}

}
