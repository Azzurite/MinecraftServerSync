package name.azzurite.mcserver.sync;

import java.nio.file.Path;
import java.util.List;

public interface SyncClient {

	SyncActionFuture<String> retrieveFileContents(String file);

	SyncActionFuture<Void> setFileContents(String file, String contents);

	SyncActionFuture<Void> deleteFile(String file);

	SyncActionFuture<Void> uploadFile(Path path);

	SyncActionFuture<Boolean> doesFileExist(String file);

	SyncActionFuture<Path> downloadFile(String fileName);

	SyncActionFuture<List<String>> findServerFiles();
}
