package name.azzurite.mcserver.util;

import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.util.List;

public interface SaveFileListVisitor extends FileVisitor<Path> {
	List<Path> getSavedFiles();
}
