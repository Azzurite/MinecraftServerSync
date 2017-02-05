package name.azzurite.mcserver.ftp;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import name.azzurite.mcserver.config.AppConfig;
import name.azzurite.mcserver.sync.NoProgressSyncActionFuture;
import name.azzurite.mcserver.sync.SyncActionFuture;
import name.azzurite.mcserver.sync.SyncClient;
import name.azzurite.mcserver.util.AsyncUtil;
import name.azzurite.mcserver.util.LogUtil;
import name.azzurite.mcserver.util.OnlyContentZipEntrySource;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.zip.ZipException;
import org.zeroturnaround.zip.ZipUtil;

import static name.azzurite.mcserver.util.LambdaExceptionUtil.*;
import static name.azzurite.mcserver.util.StreamUtil.*;

public class FTPSyncClient implements SyncClient {

	private static final String MD5_FILE = "MinecraftServerSync/md5s.zip";

	private static final Path MD5_TEMP_PATH = Paths.get("MinecraftServerSync/md5s");

	private static final int MAX_RETRIES = 3;

	private static final int BUF_SIZE = 1048576;

	private static final Logger LOGGER = LoggerFactory.getLogger(FTPSyncClient.class);

	private final FTPClient ftpClient = new FTPClient();

	private final AppConfig appConfig;

	private final ExecutorService executor = Executors.newFixedThreadPool(1);

	private final FTPTransferProgressListener progressListener;

	public FTPSyncClient(AppConfig appConfig) {
		ftpClient.setBufferSize(BUF_SIZE);
		this.appConfig = appConfig;
		progressListener = new FTPTransferProgressListener();
		ftpClient.setCopyStreamListener(progressListener);
	}

	private static void logFtpCommand(FTPClient ftp, String commandName, Object... arguments) {
		logCommand(commandName, arguments);
		logResult(ftp);
	}

	private static void logResult(FTPClient ftp) {
		LOGGER.trace("Result: {}", ftp.getReplyString());
	}

	private static void logCommand(String commandName, Object[] arguments) {
		StringBuilder commandLog = new StringBuilder();
		commandLog.append("Executed command: ");
		appendCommandString(commandLog, commandName, arguments);
		LOGGER.trace(commandLog.toString());
	}

	@SuppressWarnings("MagicCharacter")
	private static void appendCommandString(StringBuilder sb, String commandName, Object[] arguments) {
		sb.append(commandName);
		sb.append('(');
		Arrays.stream(arguments).forEach(arg -> {
			if (arg instanceof String) {
				sb.append('"');
			}
			sb.append(arg);
			if (arg instanceof String) {
				sb.append('"');
			}
			sb.append(", ");
		});
		if (arguments.length > 0) {
			sb.delete(sb.length() - 2, sb.length());
		}
		sb.append(')');
	}

	private static FTPException createMissingConfigEx(String property) {
		return new FTPException("The required property " + property + " could not be found.");
	}

	private static String generateMd5FileName(Path file) {
		String md5FileName = file.getFileName() + ".md5";
		Path parent = file.getParent();
		if (parent != null) {
			return toFtpPath(file.getParent().resolve(md5FileName));
		} else {
			return md5FileName;
		}
	}

	private static byte[] computeMd5(Path localPath) throws IOException {
		if (!Files.exists(localPath)) {
			return new byte[0];
		}
		try (InputStream file = Files.newInputStream(localPath)) {
			return DigestUtils.md5(file);
		}
	}

	private static String toFtpPath(Path path) {
		return path.toString().replaceAll("\\\\", "/");
	}

	private void tryConnect() {
		if (ftpClient.isConnected()) {
			return;
		}
		LOGGER.info("Connecting to FTP server...");

		try {
			String hostName = appConfig.getFtpHostName().orElseThrow(() -> createMissingConfigEx("ftp host name"));
			String port = appConfig.getFtpPort().orElseThrow(() -> createMissingConfigEx("ftp port"));
			ftpClient.connect(hostName, Integer.valueOf(port));
			logFtpCommand(ftpClient, "tryConnect", hostName, port);

			ftpClient.enterLocalPassiveMode();
			logFtpCommand(ftpClient, "enterLocalPassiveMode");

			Optional<String> userName = appConfig.getFtpUserName();
			Optional<String> password = appConfig.getFtpPassword();
			if (userName.isPresent() && password.isPresent()) {
				boolean loginSuccessful = ftpClient.login(userName.get(), password.get());
				logFtpCommand(ftpClient, "login", userName.get(), password.get());
				if (loginSuccessful) {
					LOGGER.debug("FTP login successful");
				} else {
					LOGGER.debug("FTP login failed");
				}
			} else {
				LOGGER.warn("No authentication for ftp set!");
			}

			ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
			logFtpCommand(ftpClient, "setFileType");

			String baseDirectory =
					appConfig.getFtpBaseDirectory().orElseThrow(() -> createMissingConfigEx("ftp base directory"));
			ftpClient.changeWorkingDirectory(baseDirectory);
			logFtpCommand(ftpClient, "changeWorkingDirectory", baseDirectory);

			LOGGER.info("Connected to FTP server.");
		} catch (IOException e) {
			throw new FTPException(e);
		}
	}

	@Override
	public SyncActionFuture<String> retrieveFileContents(String file) {
		LOGGER.debug("Retrieving file contents for file '{}'", file);

		return new NoProgressSyncActionFuture<>(file + " content retrieval", perform(() -> {
			try (ByteArrayOutputStream temp = new ByteArrayOutputStream()) {
				ftpClient.retrieveFile(file, temp);
				logFtpCommand(ftpClient, "downloadFile", file);

				String fileContents = temp.toString("UTF-8");

				LOGGER.debug("File contents retrieved: {}", fileContents);

				return fileContents;
			}
		}));
	}

	private byte[] retrieveByteFileContents(String file) throws IOException {
		LOGGER.debug("Retrieving byte file contents for file '{}'", file);

		try (ByteArrayOutputStream temp = new ByteArrayOutputStream()) {
			ftpClient.retrieveFile(file, temp);
			logFtpCommand(ftpClient, "downloadFile", file);

			byte[] bytes = temp.toByteArray();
			LOGGER.debug("File contents retrieved: {}", bytes);

			return bytes;
		}
	}

	@Override
	public SyncActionFuture<Void> setFileContents(String file, String contents) {
		LOGGER.debug("Setting file({}) contents: {}", file, contents);

		return new NoProgressSyncActionFuture<>(file + " set content", perform(() -> {
			ftpStoreFile(file, IOUtils.toInputStream(contents, "UTF-8"));
		}));
	}

	@Override
	public SyncActionFuture<Void> deleteFile(String file) {
		LOGGER.debug("Deleting file: {}", file);

		return new NoProgressSyncActionFuture<>(file + " deletion", perform(() -> {
			ftpClient.deleteFile(file);
			logFtpCommand(ftpClient, "deleteFile", file);
		}));
	}

	@Override
	public SyncActionFuture<Void> uploadFiles(Collection<Path> filePaths) {
		LOGGER.debug("Uploading files: {}", filePaths);

		Map<String, byte[]> md5s = retrieveMd5s();

		List<Path> filesToUpload = filePaths.stream()
				.filter(filePath -> {
					Path relativeToBase = appConfig.getSyncPath().relativize(filePath);
					byte[] remoteMd5 = md5s.get(relativeToBase.toString());
					return hasEqualMd5(filePath, remoteMd5);
				})
				.collect(Collectors.toList());

		long totalFileSize = getTotalFileSize(filesToUpload);

		Future<Void> uploadResult = perform(() -> {

			filesToUpload.forEach(rethrow(this::uploadFile));

			storeMd5s(filePaths);
		});


		return new FTPTransferSyncActionFuture<>(uploadResult, progressListener, "server file upload", totalFileSize);
	}

	private long getTotalFileSize(Collection<Path> filePaths) {
		return filePaths.stream()
				.mapToLong(filePath -> {
					try {
						return Files.size(filePath);
					} catch (IOException e) {
						LOGGER.warn("Error while calculating file sizes. Progress may not be accurate.");
						LogUtil.stacktrace(LOGGER, e);
						return 0L;
					}
				})
				.sum();
	}

	private void uploadFile(Path filePath) throws IOException {
		LOGGER.debug("Uploading file {}...", filePath);
		Path relativeToBase = appConfig.getBaseServerPath().relativize(filePath);
		String ftpFileName = toFtpPath(relativeToBase);

		try (InputStream zipFile = Files.newInputStream(filePath)) {
			ftpStoreFile(ftpFileName, zipFile);
		}
		LOGGER.debug("Uploaded file: {}", ftpFileName);
	}

	@Override
	public SyncActionFuture<Boolean> doesFileExist(String file) {
		LOGGER.debug("Checking file existence: {}", file);

		return new NoProgressSyncActionFuture<>(file + " existance check", perform(() -> ftpFileExists(file)));
	}

	private boolean ftpFileExists(String file) throws IOException {
		FTPFile[] ftpFiles = ftpClient.listFiles(file);
		logFtpCommand(ftpClient, "listFiles", file);

		return ftpFiles.length > 0;
	}

	@Override
	public SyncActionFuture<List<Path>> downloadFiles(Collection<String> fileNames) {
		LOGGER.debug("Downloading files {}", fileNames);

		Map<String, byte[]> md5s = retrieveMd5s();

		List<String> filesToDownload = fileNames.stream()
				.filter(remoteFileName -> {
					Path localPath = toLocalPath(remoteFileName);
					Path relativeToBase = appConfig.getSyncPath().relativize(localPath);
					byte[] remoteMd5 = md5s.get(relativeToBase.toString());
					return hasEqualMd5(localPath, remoteMd5);
				})
				.collect(Collectors.toList());

		LOGGER.debug("Retrieving total file size of remote files ");
		long totalFileSize = getTotalRemoteFileSize(filesToDownload);
		LOGGER.debug("Total file size of remote files: {}", totalFileSize);

		Future<List<Path>> downloadResult = perform(() -> {
			filesToDownload.stream()
					.map(rethrow(this::downloadFile))
					.collect(Collectors.toList());

			return fileNames.stream()
					.map(this::toLocalPath)
					.collect(Collectors.toList());
		});

		return new FTPTransferSyncActionFuture<>(downloadResult, progressListener, "server file download", totalFileSize);
	}

	private boolean hasEqualMd5(Path localFile, byte[] md5) {
		try {
			Path relativeToBase = appConfig.getSyncPath().relativize(localFile);
			LOGGER.debug("Comparing MD5s for file '{}'...", localFile);
			byte[] localMd5 = computeMd5(localFile);
			if (Arrays.equals(md5, localMd5)) {
				LOGGER.debug("Equal MD5s, skipping upload/download.");
				return false;
			}
		} catch (IOException e) {
			LOGGER.debug("Error while computing MD5...");
			LogUtil.stacktrace(LOGGER, e);
		}
		LOGGER.debug("Differing MD5s, uploading/downloading...");
		return true;
	}

	private Path toLocalPath(String remoteFileName) {
		return Paths.get(appConfig.getBaseServerPath().toString(), remoteFileName.replaceAll("/", "\\\\"));
	}

	private long getTotalRemoteFileSize(Collection<String> fileNames) {
		try {
			Future<Long> fileSizeResult = perform(() -> {
				return fileNames.stream()
						.map(rethrow((String fileName) -> {
							FTPFile[] ftpFiles = ftpClient.listFiles(fileName);
							if (ftpFiles.length != 1 || !ftpFiles[0].isFile()) {
								return 0L;
							} else {
								return ftpFiles[0].getSize();
							}
						}))
						.mapToLong(Long::valueOf)
						.sum();
			});
			return AsyncUtil.getResult(fileSizeResult);
		} catch (ExecutionException e) {
			LOGGER.warn("Error while calculating download file size, progress will not be available.");
			LogUtil.stacktrace(LOGGER, e);
			return 0;
		}
	}

	private Map<String, byte[]> retrieveMd5s() {
		try {
			Future<Path> md5FileFuture = perform(() -> downloadFile(MD5_FILE));

			Path md5File = AsyncUtil.getResult(md5FileFuture);
			Path md5TempPath = appConfig.getBaseServerPath().resolve(MD5_TEMP_PATH);

			ZipUtil.unpack(md5File.toFile(), md5TempPath.toFile());

			return Files.walk(md5TempPath)
					.collect(
							HashMap<String, byte[]>::new,
							(map, path) -> {
								try {
									Path relativeToBase = md5TempPath.relativize(path);
									String fileName = relativeToBase.getFileName().toString().replaceAll("\\.md5$", "");
									if (relativeToBase.getParent() != null) {
										fileName = relativeToBase.getParent().resolve(fileName).toString();
									}
									byte[] md5 = Files.readAllBytes(path);
									map.put(fileName, md5);
								} catch (IOException ignored) {
								}
							},
							HashMap::putAll
					);
		} catch (IOException | ExecutionException | ZipException e) {
			LOGGER.warn("Error while creating MD5 table");
			LogUtil.stacktrace(LOGGER, e);
			return Collections.emptyMap();
		}
	}


	private void storeMd5s(Collection<Path> filePaths) throws IOException {
		Path md5TempPath = appConfig.getBaseServerPath().resolve(MD5_TEMP_PATH);
		filePaths.forEach(filePath -> {
			try {
				Path dirRelativeToBase = appConfig.getSyncPath().relativize(filePath.getParent());
				Path md5FilePath = md5TempPath.resolve(dirRelativeToBase).resolve(filePath.getFileName() + ".md5");

				Files.createDirectories(md5FilePath.getParent());

				Files.write(md5FilePath, computeMd5(filePath));
			} catch (IOException e) {
				LOGGER.warn("Error while computing md5.");
				LogUtil.stacktrace(LOGGER, e);
			}
		});

		OnlyContentZipEntrySource[] zipEntries = Files.walk(md5TempPath)
				.filter(not(Files::isDirectory))
				.map(file -> {
					Path relativeToBase = md5TempPath.relativize(file);
					return new OnlyContentZipEntrySource(relativeToBase.toString(), file.toFile());
				})
				.toArray(OnlyContentZipEntrySource[]::new);

		Path md5File = appConfig.getBaseServerPath().resolve(MD5_FILE);
		ZipUtil.pack(zipEntries, md5File.toFile());

		ftpStoreFile(MD5_FILE, Files.newInputStream(md5File));
	}


	private Path downloadFile(String remoteFileName) throws IOException {
		Path localPath = toLocalPath(remoteFileName);

		ftpRetrieveFile(remoteFileName, localPath);

		return localPath;
	}

	private void ftpRetrieveFile(String remoteFileName, Path path) throws IOException {
		Path parent = path.getParent();
		if (parent != null) {
			Files.createDirectories(parent);
		}
		try (FileOutputStream fileStream = new FileOutputStream(path.toFile())) {
			ftpClient.retrieveFile(remoteFileName, fileStream);
			logFtpCommand(ftpClient, "retrieveFile", remoteFileName);
		}
	}

	@Override
	public SyncActionFuture<Set<String>> findServerFiles() {
		Future<Set<String>> findServerFilesResult = perform(() -> {
			String serverFilesDirectory = toFtpPath(appConfig.getBaseServerPath().relativize(appConfig.getSyncPath()));
			return listFilesRecursively(serverFilesDirectory).keySet();
		});

		return new NoProgressSyncActionFuture<>("list server files", findServerFilesResult);
	}

	private void ftpStoreFile(String file, InputStream is) throws IOException {
		Path filePath = Paths.get(file);
		ftpCreateParentDirs(filePath);

		ftpClient.storeFile(file, is);
		logFtpCommand(ftpClient, "ftpStoreFile", file);
	}

	private void ftpCreateParentDirs(Path filePath) throws IOException {
		Path parent = filePath.getParent();
		if (parent != null) {
			if (!ftpFileExists(toFtpPath(parent))) {
				ftpCreateParentDirs(parent);
				ftpCreateDir(parent);
			}
		}
	}

	private void ftpCreateDir(Path dir) throws IOException {
		ftpClient.makeDirectory(toFtpPath(dir));
		logFtpCommand(ftpClient, "makeDirectory", dir);
	}

	private Map<String, FTPFile> listFilesRecursively(String directory) throws IOException {
		FTPFile[] listFiles = ftpClient.listFiles(directory);
		List<FTPFile> ftpFiles = Arrays.asList(listFiles);

		Map<String, FTPFile> fileMap = new HashMap<>();

		ftpFiles.stream()
				.filter(FTPFile::isFile)
				.filter(file -> !file.getName().endsWith(".md5"))
				.forEach(file -> {
					fileMap.put(directory + '/' + file.getName(), file);
				});

		ftpFiles.stream()
				.filter(FTPFile::isDirectory)
				.map(rethrow((FTPFile dir) -> listFilesRecursively(directory + '/' + dir.getName())))
				.forEach(fileMap::putAll);

		return fileMap;
	}

	private <R> Future<R> retry(FTPAction<R> action) {
		return executor.submit(() -> {
			for (int i = 1; i <= MAX_RETRIES; ++i) {
				try {
					if (i > 1) {
						LOGGER.debug("Trying action again after failure...");
					}
					return action.perform();
				} catch (IOException e) {
					LOGGER.warn("Error during FTP action: {}", e.getMessage());
					LogUtil.stacktrace(LOGGER, e);
					try {
						ftpClient.disconnect();
					} catch (IOException e1) {
						LOGGER.warn("Error while disconnecting: {}", e1.getMessage());
						LogUtil.stacktrace(LOGGER, e1);
					}

					tryConnect();
				}
			}

			LOGGER.error("Could not perform FTP action, 3 retries all failed.");
			throw new FTPException("Could not perform FTP action.");
		});
	}

	private Future<Void> retry(FTPVoidAction action) {
		return retry(() -> {
			action.perform();
			return (Void) null;
		});
	}

	private <R> Future<R> perform(FTPAction<R> action) {
		tryConnect();

		return retry(action);
	}

	private Future<Void> perform(FTPVoidAction action) {
		tryConnect();

		return retry(action);
	}

	private long getFileSize(String fileName) {
		Future<Long> fileSizeResult = perform(() -> {
			long fileSize = 0;
			FTPFile[] files = ftpClient.listFiles(fileName);
			if (files.length != 1 || !files[0].isFile()) {
				throw new FTPException("Could not get size of file " + fileName);
			}
			return files[0].getSize();
		});

		try {
			return AsyncUtil.getResult(fileSizeResult);
		} catch (ExecutionException e) {
			throw new FTPException(e);
		}

	}

}
