package name.azzurite.mcserver.view.override;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import name.azzurite.mcserver.server.LocalServerService;
import name.azzurite.mcserver.sync.ServerStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UploadInProgressOverride extends OverrideButton {

	private static final Logger LOGGER = LoggerFactory.getLogger(UploadInProgressOverride.class);

	private final LocalServerService localServerService;

	public UploadInProgressOverride(LocalServerService localServerService) {
		super(ServerStatus.REMOTE_UPLOADING);

		this.localServerService = localServerService;
	}

	@FXML
	private void onRemoveUploadInProgress(ActionEvent event) {
		if (localServerService.isRunning()) {
			LOGGER.info("Still waiting for other action...");
			return;
		}

		LOGGER.info("Removing upload in progress flag...");
		localServerService.reset();
		localServerService.startRemoveUploadInProgressFlag();
	}

}
