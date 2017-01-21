package name.azzurite.mcserver.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;

import org.zeroturnaround.zip.ZipEntrySource;

public class OnlyContentZipEntrySource implements ZipEntrySource {

	private final String path;

	private final File file;

	public OnlyContentZipEntrySource(String path, File file) {
		this.path = path;
		this.file = file;
	}

	@Override
	public String getPath() {
		return path;
	}

	@Override
	public ZipEntry getEntry() {
		ZipEntry zipEntry = new ZipEntry(path);
		if (!file.isDirectory()) {
			zipEntry.setSize(file.length());
		}
		zipEntry.setTime(0L);

		return zipEntry;
	}

	@SuppressWarnings({"OverlyBroadThrowsClause", "ReturnOfNull"})
	@Override
	public InputStream getInputStream() throws IOException {
		return file.isDirectory() ? null : new BufferedInputStream(new FileInputStream(file));
	}
}
