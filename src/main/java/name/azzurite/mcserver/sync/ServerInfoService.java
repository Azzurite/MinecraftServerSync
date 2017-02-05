package name.azzurite.mcserver.sync;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import name.azzurite.mcserver.server.LocalServerService;
import name.azzurite.mcserver.util.EventHandlerChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerInfoService extends Service<ServerInfo> {

	private static final Logger LOGGER = LoggerFactory.getLogger(ServerInfoService.class);

	private final ServerSynchronizer sync;
	private final LocalServerService serverService;
	private final EventHandlerChain<WorkerStateEvent> succeededChain;

	public ServerInfoService(ServerSynchronizer sync, LocalServerService serverService) {
		this.sync = sync;

		succeededChain = new EventHandlerChain<>();
		setOnSucceeded(succeededChain);

		this.serverService = serverService;
		serverService.stateProperty().addListener((event) -> {
			recheckServerStatus();
		});
	}

	@Override
	protected Task<ServerInfo> createTask() {
		boolean localServerRunning = serverService.isRunning() || serverService.getState() == State.CANCELLED;
		return new ServerInfoTask(localServerRunning);
	}

	public void addOnSucceeded(EventHandler<WorkerStateEvent> eventHandler) {
		succeededChain.addEventHandler(eventHandler);
	}

	public void recheckServerStatus() {
		if (isRunning()) {
			cancel();
		}
		reset();
		start();
	}

	private class ServerInfoTask extends Task<ServerInfo> {

		private final boolean localServerRunning;

		public ServerInfoTask(boolean localServerRunning) {
			this.localServerRunning = localServerRunning;
		}

		@Override
		protected ServerInfo call() throws ExecutionException {
			if (localServerRunning) {
				return new ServerInfo(ServerStatus.STARTED_LOCALLY, "localhost");
			}

			Optional<String> onlineServerIp = sync.getServerIp();
			if (onlineServerIp.isPresent()) {
				return new ServerInfo(ServerStatus.ONLINE, onlineServerIp.get());
			} else if (sync.isUploadInProgress()) {
				return new ServerInfo(ServerStatus.UPLOAD_IN_PROGRESS);
			} else {
				return new ServerInfo(ServerStatus.OFFLINE);
			}
		}
	}
}
