package name.azzurite.mcserver.util;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class FilteringSaveFileListVisitor extends SimpleFileVisitor<Path> implements SaveFileListVisitor {

	private final List<Path> savedFiles = new ArrayList<>();
	private final Collection<String> filesToIgnore = new HashSet<>();
	private final Path basePath;

	public FilteringSaveFileListVisitor(Path basePath, Collection<String> filesToIgnore) {
		this.basePath = basePath;
		this.filesToIgnore.addAll(filesToIgnore);
	}

	@Override
	public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
		Path relativePath = basePath.relativize(dir);
		if (filesToIgnore.contains(relativePath.toString())) {
			return FileVisitResult.SKIP_SUBTREE;
		} else {
			return FileVisitResult.CONTINUE;
		}
	}

	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
		Path relativePath = basePath.relativize(file);
		if (!filesToIgnore.contains(relativePath.toString())) {
			savedFiles.add(file);
		}
		return FileVisitResult.CONTINUE;
	}

	@Override
	public List<Path> getSavedFiles() {
		return Collections.unmodifiableList(savedFiles);
	}
}
