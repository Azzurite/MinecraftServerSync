package name.azzurite.mcserver.sync;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import com.jakewharton.byteunits.BinaryByteUnit;
import name.azzurite.mcserver.config.AppConfig;
import name.azzurite.mcserver.util.AsyncUtil;
import name.azzurite.mcserver.util.FilteringSaveFileListVisitor;
import name.azzurite.mcserver.util.LogUtil;
import name.azzurite.mcserver.util.OnlyContentZipEntrySource;
import name.azzurite.mcserver.util.SaveFileListVisitor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.zip.ZipEntrySource;
import org.zeroturnaround.zip.ZipUtil;

import static name.azzurite.mcserver.util.LambdaExceptionUtil.*;
import static name.azzurite.mcserver.util.StreamUtil.*;

public class ServerSynchronizer {

	static final Set<String> IGNORE_FILES = new HashSet<>();
	private static final Logger LOGGER = LoggerFactory.getLogger(ServerSynchronizer.class);
	private static final String SERVER_IP_FILE = "MinecraftServerSync/runningServer";
	private static final String SAVE_IN_PROGRESS_FILE_NAME = "MinecraftServerSync/uploadInProgress";
	private static final long SECONDS_10 = 10000L;
	private static final Path BACKUP_FILE_ZIP = Paths.get("backup.zip");
	private static final Path SMALL_FILES_ZIP = Paths.get("smallFilesSync.zip");
	private static final int SMALL_FILE_THRESHOLD_KB = 20;

	static {
		IGNORE_FILES.addAll(Arrays.asList("MinecraftServerSync", "logs"));
	}

	private final AppConfig appConfig;
	private final SyncClient syncClient;

	public ServerSynchronizer(AppConfig appConfig, SyncClient syncClient) {
		this.appConfig = appConfig;
		this.syncClient = syncClient;
	}

	private static List<Path> pathToFileList(Path path) throws IOException {
		if (Files.isDirectory(path)) {
			return Files.walk(path)
					.filter(Files::isRegularFile)
					.collect(Collectors.toList());
		} else {
			return Collections.singletonList(path);
		}
	}

	private static void deleteDirectory(Path dirToDelete) {
		try {
			Files.walkFileTree(dirToDelete, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					Files.delete(file);
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
					Files.delete(dir);
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (IOException e) {
			LOGGER.warn("Could not delete directory {}", dirToDelete);
			LogUtil.stacktrace(LOGGER, e);
		}
	}

	private static void deleteFile(Path pathToDelete) {
		if (!Files.exists(pathToDelete, LinkOption.NOFOLLOW_LINKS)) {
			return;
		}

		if (Files.isDirectory(pathToDelete)) {
			deleteDirectory(pathToDelete);
		} else {
			try {
				Files.delete(pathToDelete);
			} catch (IOException e) {
				LOGGER.warn("Could not delete file {}", pathToDelete);
				LogUtil.stacktrace(LOGGER, e);
			}
		}
	}

	private static boolean isSmallFile(Path file) {
		try {
			return BinaryByteUnit.BYTES.toKibibytes(Files.size(file)) < SMALL_FILE_THRESHOLD_KB;
		} catch (IOException e) {
			LOGGER.warn("Error while calculating file size: {}", e.getMessage());
			LogUtil.stacktrace(LOGGER, e);
			return false;
		}
	}

	private static List<Path> filterForBigFiles(Collection<Path> localServerFiles) {
		return localServerFiles.stream()
				.filter(not(ServerSynchronizer::isSmallFile))
				.collect(Collectors.toList());
	}

	private static List<Path> filterForSmallFiles(Collection<Path> localServerFiles) {
		return localServerFiles.stream()
				.filter(ServerSynchronizer::isSmallFile)
				.collect(Collectors.toList());
	}

	private void extractFile(Path file) {
		LOGGER.debug("Extracting file: {}", file);
		Path relativePath = appConfig.getSyncPath().relativize(file);
		Path parentBaseDir = appConfig.getBaseServerPath().resolve(relativePath).getParent();
		ZipUtil.unpack(file.toFile(), parentBaseDir.toFile());
		LOGGER.debug("Extract finished");
	}

	public Optional<String> getServerIp() throws ExecutionException {
		String serverIp = AsyncUtil.getResult(syncClient.retrieveFileContents(SERVER_IP_FILE));

		return StringUtils.isEmpty(serverIp) ? Optional.empty() : Optional.of(serverIp);
	}

	public void setServerIp(String ip) throws IOException, ExecutionException {
		LOGGER.info("Publishing server IP {}", ip);
		AsyncUtil.getResult(syncClient.setFileContents(SERVER_IP_FILE, ip));
	}

	public void deleteServerIp() throws IOException, ExecutionException {
		LOGGER.info("Depublishing server IP");
		AsyncUtil.getResult(syncClient.deleteFile(SERVER_IP_FILE));
	}

	public void retrieveFiles() throws ExecutionException, IOException {
		while (isUploadInProgress()) {
			LOGGER.info("Someone is still uploading the server files. Waiting for completion...");
			AsyncUtil.threadSleep(SECONDS_10);
		}

		backupCurrentFiles();

		Set<String> serverFiles = searchOnlineServerFiles();

		if (!serverFiles.isEmpty()) {
			deleteLocalServerFiles();

			List<Path> downloadedFiles = downloadFiles(serverFiles);

			extractDownloadedServerFiles(downloadedFiles);
		}
	}

	private void extractDownloadedServerFiles(Iterable<Path> downloadedFiles) {
		LOGGER.info("Extracting server files...");
		downloadedFiles.forEach(rethrow(this::extractFile));
		LOGGER.info("Server files extracted.");
	}

	private void deleteLocalServerFiles() throws IOException {
		LOGGER.info("Deleting previous data...");
		findLocalServerFiles().forEach(ServerSynchronizer::deleteFile);
		LOGGER.info("Deleted previous data.");
	}

	private Set<String> searchOnlineServerFiles() throws ExecutionException {
		LOGGER.info("Searching for online server files...");
		SyncActionFuture<Set<String>> serverFilesFuture = syncClient.findServerFiles();
		Set<String> serverFiles = AsyncUtil.getResult(serverFilesFuture);
		LOGGER.info("Server files found: {}", serverFiles);
		return serverFiles;
	}

	private void backupCurrentFiles() throws IOException {
		LOGGER.info("Backing up previous data...");

		OnlyContentZipEntrySource[] zipEntries = findLocalServerFiles().stream()
				.map(file -> {
					Path relativeToBase = appConfig.getBaseServerPath().relativize(file);
					return new OnlyContentZipEntrySource(relativeToBase.toString(), file.toFile());
				})
				.toArray(OnlyContentZipEntrySource[]::new);
		Path backupZip = appConfig.getBackupPath().resolve(BACKUP_FILE_ZIP);
		ZipUtil.pack(zipEntries, backupZip.toFile());

		LOGGER.info("Backed up previous data at '{}'.", backupZip);
	}

	boolean isUploadInProgress() throws ExecutionException {
		Future<String> content = syncClient.retrieveFileContents(SAVE_IN_PROGRESS_FILE_NAME);
		return Boolean.parseBoolean(AsyncUtil.getResult(content));
	}

	private List<Path> downloadFiles(Collection<String> fileNames) throws ExecutionException {
		LOGGER.info("Downloading new server files...");

		SyncActionFuture<List<Path>> downloadedFileResult = syncClient.downloadFiles(fileNames);
		new ProgressLogger(downloadedFileResult).logProgress();
		List<Path> newServerFiles = AsyncUtil.getResult(downloadedFileResult);

		LOGGER.info("Downloaded new server files.");
		return newServerFiles;
	}

	public void saveFiles() throws IOException, ExecutionException {
		setUploadInProgressFlag();

		List<Path> localServerFiles = findLocalServerFiles();

		List<Path> filesToUpload = zipServerFiles(localServerFiles);

		uploadFiles(filesToUpload);

		removeUploadInProgressFlag();

		LOGGER.info("All server files synchronized!");
	}

	public void removeUploadInProgressFlag() throws ExecutionException {
		AsyncUtil.getResult(syncClient.setFileContents(SAVE_IN_PROGRESS_FILE_NAME, String.valueOf(false)));
	}

	private void setUploadInProgressFlag() throws ExecutionException {
		AsyncUtil.getResult(syncClient.setFileContents(SAVE_IN_PROGRESS_FILE_NAME, String.valueOf(true)));
	}

	private List<Path> zipServerFiles(Collection<Path> localServerFiles) {
		LOGGER.info("Zipping local server files...");
		List<Path> bigFileZips = zipBigFiles(localServerFiles);
		Path smallFilesZip = zipSmallFiles(localServerFiles);
		List<Path> zippedServerFiles = new ArrayList<>(bigFileZips);
		zippedServerFiles.add(smallFilesZip);
		LOGGER.info("Local server files zipped.");
		return zippedServerFiles;
	}

	private List<Path> zipBigFiles(Collection<Path> localServerFiles) {
		List<Path> bigFiles = filterForBigFiles(localServerFiles);
		return bigFiles.stream()
				.map(rethrow(this::zipFile))
				.collect(Collectors.toList());
	}

	private Path zipSmallFiles(Collection<Path> localServerFiles) {
		List<Path> smallFiles = filterForSmallFiles(localServerFiles);
		OnlyContentZipEntrySource[] smallFileZipEntries = smallFiles.stream()
				.map(smallFile -> {
					Path relativeToBase = appConfig.getBaseServerPath().relativize(smallFile);
					return new OnlyContentZipEntrySource(relativeToBase.toString(), smallFile.toFile());
				})
				.toArray(OnlyContentZipEntrySource[]::new);
		Path smallFilesZip = appConfig.getSyncPath().resolve(SMALL_FILES_ZIP);
		ZipUtil.pack(smallFileZipEntries, smallFilesZip.toFile());
		return smallFilesZip;
	}

	private Path zipFile(Path fileToZip) throws IOException {
		LOGGER.debug("Zipping file: {}", fileToZip);

		Path fileRelativeToBase = appConfig.getBaseServerPath().relativize(fileToZip);
		String fileName = fileToZip.getFileName().toString();
		Path zipFilePath = appConfig.getSyncPath();
		if (fileRelativeToBase.getParent() != null) {
			zipFilePath = zipFilePath.resolve(fileRelativeToBase.getParent());
		}
		Files.createDirectories(zipFilePath);
		Path zipFile = zipFilePath.resolve(fileName + ".zip");

		ZipEntrySource[] filesToZip = {new OnlyContentZipEntrySource(fileName, fileToZip.toFile())};

		ZipUtil.pack(filesToZip, zipFile.toFile());

		LOGGER.debug("File zipped: {}", zipFile);

		return zipFile;
	}

	private void uploadFiles(Collection<Path> paths) throws ExecutionException {
		LOGGER.info("Uploading server files to server");
		SyncActionFuture<Void> future = syncClient.uploadFiles(paths);
		new ProgressLogger(future).logProgress();
		AsyncUtil.getResult(future);
		LOGGER.info("Upload finished");
	}

	private List<Path> findLocalServerFiles() throws IOException {
		SaveFileListVisitor saveVisitor = new FilteringSaveFileListVisitor(appConfig.getBaseServerPath(), IGNORE_FILES);
		Files.walkFileTree(appConfig.getBaseServerPath(), saveVisitor);
		return saveVisitor.getSavedFiles();
	}

}
