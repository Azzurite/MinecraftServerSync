package name.azzurite.mcserver.view.override;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import name.azzurite.mcserver.server.LocalServerService;
import name.azzurite.mcserver.sync.ServerStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DownloadFilesOverride extends OverrideButton {

	private static final Logger LOGGER = LoggerFactory.getLogger(DownloadFilesOverride.class);

	private final LocalServerService localServerService;

	public DownloadFilesOverride(LocalServerService localServerService) {
		super(ServerStatus.OFFLINE);

		this.localServerService = localServerService;
	}

	@FXML
	private void onDownloadFiles(ActionEvent event) {
		if (localServerService.isRunning()) {
			LOGGER.info("Still waiting for other action...");
			return;
		}

		LOGGER.info("Download server files...");
		localServerService.reset();
		localServerService.startDownloadServerFiles();
	}

}
