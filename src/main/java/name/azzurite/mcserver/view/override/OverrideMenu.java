package name.azzurite.mcserver.view.override;

import java.util.ArrayList;
import java.util.List;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import name.azzurite.mcserver.sync.ServerInfo;
import name.azzurite.mcserver.sync.ServerInfoService;
import name.azzurite.mcserver.sync.ServerStatus;
import name.azzurite.mcserver.view.NodeRepresentation;

public class OverrideMenu implements NodeRepresentation {

	private final List<OverrideButton> overrideButtons = new ArrayList<>();

	@FXML
	private VBox overrideMenu;

	@FXML
	private Label noOverrides;

	private ServerStatus serverStatus;

	public OverrideMenu(ServerInfoService serverInfoService) {
		serverInfoService.addOnSucceeded(event -> {
			ServerInfo serverInfo = (ServerInfo) event.getSource().getValue();
			serverStatus = serverInfo.getServerStatus();
			reCheckOverrideButtonsActive();
		});
	}

	private void reCheckOverrideButtonsActive() {
		if (serverStatus == null) {
			return;
		}

		overrideMenu.getChildren().clear();

		overrideButtons.stream()
				.filter(button -> button.isActive(serverStatus))
				.map(OverrideButton::toNodeRepresentation)
				.forEach(overrideMenu.getChildren()::add);

		if (overrideMenu.getChildren().isEmpty()) {
			overrideMenu.getChildren().add(noOverrides);
		}
	}

	public void addOverrideButton(OverrideButton overrideButton) {
		overrideButtons.add(overrideButton);
		reCheckOverrideButtonsActive();
	}

	@Override
	public Node toNodeRepresentation() {
		return overrideMenu;
	}
}
