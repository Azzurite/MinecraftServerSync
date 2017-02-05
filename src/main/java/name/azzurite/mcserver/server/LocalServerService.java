package name.azzurite.mcserver.server;

import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import name.azzurite.mcserver.config.AppConfig;
import name.azzurite.mcserver.console.LocalConsole;
import name.azzurite.mcserver.sync.ServerSynchronizer;

import static name.azzurite.mcserver.util.LambdaExceptionUtil.*;

@SuppressWarnings("AssignmentToStaticFieldFromInstanceMethod")
public class LocalServerService extends Service<Void> {

	private final AppConfig appConfig;
	private final ServerSynchronizer sync;
	private final LocalConsole console;

	public LocalServerService(AppConfig appConfig, ServerSynchronizer sync, LocalConsole console) {
		this.appConfig = appConfig;
		this.sync = sync;
		this.console = console;
	}


	@Override
	protected Task<Void> createTask() {
		return new RunServerTask();
	}

	private class RunServerTask extends Task<Void> {

		@Override
		protected Void call() throws Exception {
			SyncingLocalServer localServer = new SyncingLocalServer(appConfig, sync, console);

			listenForUnexpectedShutdown(localServer);

			while (!isCancelled()) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException ignored) {
					// just recheck loop conditions
				}
			}

			console.send("stop");
			localServer.close();

			Platform.runLater(LocalServerService.this::reset);

			return null;
		}

		private void listenForUnexpectedShutdown(SyncingLocalServer localServer) {
			localServer.addOnCloseCallback(() -> {
				Platform.runLater(rethrow(() -> {
					cancel();
				}));
			});
		}
	}
}
