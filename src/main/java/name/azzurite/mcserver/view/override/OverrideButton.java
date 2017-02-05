package name.azzurite.mcserver.view.override;

import name.azzurite.mcserver.sync.ServerStatus;
import name.azzurite.mcserver.view.WithRootFXMLNode;

public abstract class OverrideButton extends WithRootFXMLNode {

	private final ServerStatus activeStatus;

	public OverrideButton(ServerStatus activeStatus) {
		this.activeStatus = activeStatus;
	}

	public boolean isActive(ServerStatus serverStatus) {
		return activeStatus == serverStatus;
	}
}
