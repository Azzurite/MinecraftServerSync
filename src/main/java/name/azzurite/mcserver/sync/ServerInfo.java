package name.azzurite.mcserver.sync;

import java.util.Optional;

public class ServerInfo {

	private final ServerStatus serverStatus;

	private final String ip;

	public ServerInfo(ServerStatus serverStatus) {
		this.serverStatus = serverStatus;
		ip = null;
	}

	public ServerInfo(ServerStatus serverStatus, String ip) {
		this.serverStatus = serverStatus;
		this.ip = ip;
	}

	public ServerStatus getServerStatus() {
		return serverStatus;
	}

	public Optional<String> getIp() {
		return Optional.ofNullable(ip);
	}
}
