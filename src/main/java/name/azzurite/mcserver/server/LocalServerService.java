package name.azzurite.mcserver.server;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import name.azzurite.mcserver.config.AppConfig;
import name.azzurite.mcserver.console.LocalConsole;
import name.azzurite.mcserver.sync.ServerSynchronizer;
import name.azzurite.mcserver.util.LogUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static name.azzurite.mcserver.util.LambdaExceptionUtil.*;

@SuppressWarnings("AssignmentToStaticFieldFromInstanceMethod")
public class LocalServerService extends Service<Void> {

	private static final Logger LOGGER = LoggerFactory.getLogger(LocalServerService.class);

	private final AppConfig appConfig;
	private final ServerSynchronizer sync;
	private final LocalConsole console;

	private Supplier<Task<Void>> currentTask;
	private SyncingLocalServer localServer;
	private ObjectProperty<SyncingLocalServer.SyncingLocalServerStatus> localServerStatus = new SimpleObjectProperty<>(
			SyncingLocalServer.SyncingLocalServerStatus.OFFLINE);
	private boolean hasShutdown = false;
	private boolean shouldShutdown = false;

	public LocalServerService(AppConfig appConfig, ServerSynchronizer sync, LocalConsole console) {
		this.appConfig = appConfig;
		this.sync = sync;
		this.console = console;

		setOnFailed((e) -> {
			Alert alert = new Alert(Alert.AlertType.ERROR);
			alert.setHeaderText("Error while performing server action");
			alert.setContentText(getException().getMessage());
			alert.show();
			reset();
		});
	}

	public SyncingLocalServer.SyncingLocalServerStatus getLocalServerStatus() {
		return localServerStatus.get();
	}

	public ObjectProperty<SyncingLocalServer.SyncingLocalServerStatus> localServerStatusProperty() {
		return localServerStatus;
	}

	public void startDeleteServerIp() {
		startNewTask(() -> new ServerSyncTask(ServerSynchronizer::deleteServerIp, SyncingLocalServer.SyncingLocalServerStatus.RETRIEVING_FILES));
	}

	public void startDownloadServerFiles() {
		startNewTask(() -> new ServerSyncTask(ServerSynchronizer::retrieveFiles, SyncingLocalServer.SyncingLocalServerStatus.RETRIEVING_FILES));
	}

	public void startUploadServerFiles() {
		startNewTask(() -> new ServerSyncTask(ServerSynchronizer::saveFiles, SyncingLocalServer.SyncingLocalServerStatus.SAVING_FILES));
	}

	public void startRemoveUploadInProgressFlag() {
		startNewTask(() -> new ServerSyncTask(ServerSynchronizer::removeUploadInProgressFlag, SyncingLocalServer.SyncingLocalServerStatus
				.RETRIEVING_FILES));
	}

	private void startNewTask(Supplier<Task<Void>> taskSupplier) {
		currentTask = taskSupplier;
		super.start();
	}

	@Override
	public void start() {
		startNewTask(RunServerTask::new);
	}

	@Override
	protected Task<Void> createTask() {
		return currentTask.get();
	}

	public void requestShutdown() {
		shouldShutdown = true;
	}

	private class ServerSyncTask extends Task<Void> {

		private final Consumer_WithExceptions<ServerSynchronizer, Exception> serverSyncAction;
		private final SyncingLocalServer.SyncingLocalServerStatus statusUpdate;

		private ServerSyncTask(Consumer_WithExceptions<ServerSynchronizer, Exception> serverSyncAction,
				SyncingLocalServer.SyncingLocalServerStatus statusUpdate) {
			this.serverSyncAction = serverSyncAction;
			this.statusUpdate = statusUpdate;
		}

		@Override
		protected Void call() throws Exception {
			try {
				localServerStatus.set(statusUpdate);
				serverSyncAction.accept(sync);
			} catch (Exception e) {
				LOGGER.error("Error while performing sync action: {}", e.getMessage());
				LogUtil.stacktrace(LOGGER, e);
				//noinspection ProhibitedExceptionThrown
				throw e;
			} finally {
				localServerStatus.set(SyncingLocalServer.SyncingLocalServerStatus.OFFLINE);
			}

			return null;
		}
	}

	private class RunServerTask extends Task<Void> {

		@Override
		protected Void call() throws Exception {
			hasShutdown = false;
			shouldShutdown = false;

			localServerStatus.set(SyncingLocalServer.SyncingLocalServerStatus.RETRIEVING_FILES);

			try {
				localServer = new SyncingLocalServer(appConfig, sync, console);

				localServerStatus.bind(localServer.statusProperty());

				listenForUnexpectedShutdown(localServer);

				while (!hasShutdown && !shouldShutdown) {
					try {
						Thread.sleep(200);
					} catch (InterruptedException ignored) {
						// just recheck loop conditions
					}
				}

				localServer.close();
			} catch (IOException | ExecutionException e) {
				LOGGER.error("Error while running server: {}", e.getMessage());
				LogUtil.stacktrace(LOGGER, e);
			} finally {
				localServerStatus.unbind();
				localServerStatus.set(SyncingLocalServer.SyncingLocalServerStatus.OFFLINE);
			}

			return null;
		}

		private void listenForUnexpectedShutdown(SyncingLocalServer localServer) {
			localServer.addOnCloseCallback(() -> {
				hasShutdown = true;
			});
		}
	}
}
