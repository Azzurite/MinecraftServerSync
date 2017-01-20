package name.azzurite.mcserver.sync;

import java.nio.file.Path;
import java.util.concurrent.Future;

public interface SyncClient {

	Future<String> retrieveFileContents(String file);

	Future<Void> setFileContents(String file, String contents);

	Future<Void> deleteFile(String file);

	Future<Void> uploadFile(Path path);

	Future<Boolean> doesFileExist(String file);

	Future<Path> downloadFile(String fileName);
}
