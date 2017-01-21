package name.azzurite.mcserver.sync;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface SyncClient {

	SyncActionFuture<String> retrieveFileContents(String file);

	SyncActionFuture<Void> setFileContents(String file, String contents);

	SyncActionFuture<Void> deleteFile(String file);

	SyncActionFuture<Void> uploadFiles(Collection<Path> filePaths);

	SyncActionFuture<Boolean> doesFileExist(String file);

	SyncActionFuture<List<Path>> downloadFiles(Collection<String> fileNames);

	SyncActionFuture<Set<String>> findServerFiles();
}
