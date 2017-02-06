package name.azzurite.mcserver.view.override;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import name.azzurite.mcserver.server.LocalServerService;
import name.azzurite.mcserver.sync.ServerStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SetOfflineOverride extends OverrideButton {

	private static final Logger LOGGER = LoggerFactory.getLogger(SetOfflineOverride.class);

	private final LocalServerService localServerService;

	public SetOfflineOverride(LocalServerService localServerService) {
		super(ServerStatus.REMOTE_ONLINE);
		this.localServerService = localServerService;
	}

	@FXML
	private void onSetOffline(ActionEvent event) {
		if (localServerService.isRunning()) {
			LOGGER.info("Still waiting for other action...");
			return;
		}

		LOGGER.info("Setting server offline...");
		localServerService.reset();
		localServerService.startDeleteServerIp();
	}
}
