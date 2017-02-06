package name.azzurite.mcserver.sync;

import name.azzurite.mcserver.server.SyncingLocalServer;

public enum ServerStatus {
	OFFLINE,
	REMOTE_ONLINE,
	REMOTE_UPLOADING,
	LOCALLY_ONLINE,
	LOCALLY_UPLOADING,
	LOCALLY_DOWNLOADING;

	public static ServerStatus fromLocalServerStatus(SyncingLocalServer.SyncingLocalServerStatus localServerStatus) {
		switch (localServerStatus) {
			case RUNNING:
				return LOCALLY_ONLINE;
			case RETRIEVING_FILES:
				return LOCALLY_DOWNLOADING;
			case SAVING_FILES:
				return LOCALLY_UPLOADING;
			case OFFLINE:
			default:
				return OFFLINE;
		}
	}
}
