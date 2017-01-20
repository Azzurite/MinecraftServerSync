package name.azzurite.mcserver.sync;

import java.nio.file.Path;

public interface SyncClient {

	String retrieveFileContents(String file);

	void setFileContents(String file, String contents);

	void deleteFile(String file);

	void uploadFile(Path path);

	boolean doesFileExist(String file);

	Path downloadFile(String fileName);
}
