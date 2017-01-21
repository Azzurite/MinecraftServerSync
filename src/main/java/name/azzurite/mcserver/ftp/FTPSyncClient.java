package name.azzurite.mcserver.ftp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
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
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPConnectionClosedException;
import org.apache.commons.net.ftp.FTPFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static name.azzurite.mcserver.util.LambdaExceptionUtil.*;

public class FTPSyncClient implements SyncClient {

	private static final int MAX_RETRIES = 3;

	private static final int BUF_SIZE = 1048576;

	private static final Logger LOGGER = LoggerFactory.getLogger(FTPSyncClient.class);

	private final FTPClient ftpClient = new FTPClient();

	private final AppConfig appConfig;

	private final ExecutorService executor = Executors.newFixedThreadPool(10);

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
		LOGGER.debug("Connecting...");

		try {
			AsyncUtil.getResult(retry(() -> {
				String hostName = appConfig.getFtpHostName().orElseThrow(() -> createMissingConfigEx("ftp host name"));
				String port = appConfig.getFtpPort().orElseThrow(() -> createMissingConfigEx("ftp port"));
				ftpClient.connect(hostName, Integer.valueOf(port));
				logFtpCommand(ftpClient, "tryConnect", hostName, port);

				ftpClient.enterLocalPassiveMode();
				logFtpCommand(ftpClient, "enterLocalPassiveMode");

				Optional<String> userName = appConfig.getFtpUserName();
				Optional<String> password = appConfig.getFtpPassword();
				if (userName.isPresent() && password.isPresent()) {
					ftpClient.login(userName.get(), password.get());
					logFtpCommand(ftpClient, "login", userName.get(), password.get());
				} else {
					LOGGER.warn("No authentication for ftp set!");
				}

				ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
				logFtpCommand(ftpClient, "setFileType");

				String baseDirectory =
						appConfig.getFtpBaseDirectory().orElseThrow(() -> createMissingConfigEx("ftp base directory"));
				ftpClient.changeWorkingDirectory(baseDirectory);
				logFtpCommand(ftpClient, "changeWorkingDirectory", baseDirectory);
			}, () -> {
				ftpClient.disconnect();
			}));
		} catch (ExecutionException e) {
			new FTPException(e);
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
	public SyncActionFuture<Void> uploadFile(Path path) {
		LOGGER.debug("Uploading file: {}", path);

		try {
			Path relativeToBase = appConfig.getBaseServerPath().relativize(path);
			String ftpFileName = toFtpPath(relativeToBase);
			String ftpMd5FileName = generateMd5FileName(relativeToBase).replaceAll("\\\\", "/");

			long fileSize = Files.size(path);

			Future<Void> uploadResult = perform(() -> {
				byte[] remoteMd5 = new byte[0];
				remoteMd5 = retrieveByteFileContents(ftpMd5FileName);
				byte[] localMd5 = computeMd5(path);
				if (Arrays.equals(localMd5, remoteMd5)) {
					LOGGER.debug("Equal MD5, skipping upload.");
					return;
				}

				try (InputStream zipFile = Files.newInputStream(path);
					 InputStream md5File = new ByteArrayInputStream(localMd5)) {
					ftpStoreFile(ftpFileName, zipFile);
					ftpStoreFile(ftpMd5FileName, md5File);

					LOGGER.debug("Uploaded file: {}", path);
				}
			});

			return new FTPTransferSyncActionFuture<>(uploadResult, progressListener, ftpFileName + " upload", fileSize);
		} catch (IOException e) {
			throw new FTPException(e);
		}
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
	public SyncActionFuture<Path> downloadFile(String fileName) {
		LOGGER.debug("Downloading file {}", fileName);

		long fileSize = getFileSize(fileName);

		Future<Path> downloadResult = perform(() -> {
			Path localPath = appConfig.getBaseServerPath().resolve(fileName);

			try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream()) {
				String md5FileName = generateMd5FileName(Paths.get(fileName));

				ftpClient.retrieveFile(md5FileName, byteStream);
				logFtpCommand(ftpClient, "retrieveFile", fileName);

				byte[] remoteMd5 = byteStream.toByteArray();
				byte[] localMd5 = computeMd5(localPath);
				if (Arrays.equals(remoteMd5, localMd5)) {
					LOGGER.debug("Equal MD5, skipping download.");
				} else {
					try (FileOutputStream fileStream = new FileOutputStream(localPath.toFile())) {
						ftpClient.retrieveFile(fileName, fileStream);
						logFtpCommand(ftpClient, "retrieveFile", fileName);
					}
				}
			}

			return localPath;
		});

		return new FTPTransferSyncActionFuture<>(downloadResult, progressListener, fileName + " download", fileSize);
	}

	@Override
	public SyncActionFuture<List<String>> findServerFiles() {
		Future<List<String>> findServerFilesResult = perform(() -> {
			return listFilesRecursively(toFtpPath(appConfig.getBaseServerPath().relativize(appConfig.getSyncPath())));
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

	private List<String> listFilesRecursively(String directory) throws IOException {
		FTPFile[] listFiles = ftpClient.listFiles(directory);
		List<FTPFile> ftpFiles = Arrays.asList(listFiles);

		List<String> curFiles = ftpFiles.stream()
				.filter(FTPFile::isFile)
				.filter(file -> !file.getName().endsWith(".md5"))
				.map(file -> directory + '/' + file.getName())
				.collect(Collectors.toList());

		List<String> subDirectoryFiles = ftpFiles.stream()
				.filter(FTPFile::isDirectory)
				.map(rethrow((FTPFile dir) -> listFilesRecursively(directory + '/' + dir.getName())))
				.flatMap(List::stream)
				.collect(Collectors.toList());

		curFiles.addAll(subDirectoryFiles);

		return curFiles;
	}

	private <R> Future<R> retry(FTPAction<R> action, FTPVoidAction onFail) {
		return executor.submit(() -> {
			for (int i = 1; i <= MAX_RETRIES; ++i) {
				try {
					if (i > 1) {
						LOGGER.debug("Trying action again after failure...");
					}
					return action.perform();
				} catch (FTPConnectionClosedException e) {
					LOGGER.debug("Connection lost, reconnecting...");
					LogUtil.stacktrace(LOGGER, e);
					try {
						ftpClient.disconnect();
					} catch (IOException ignore) {
					}

					tryConnect();
				} catch (IOException e) {
					LOGGER.warn("Error during FTP action");
					LogUtil.stacktrace(LOGGER, e);

					if (onFail != null) {
						try {
							onFail.perform();
						} catch (IOException e1) {
							LOGGER.warn("Error while performing onFail action");
							LogUtil.stacktrace(LOGGER, e1);
						}
					}
				}
			}
			throw new FTPException("Could not perform FTP action.");
		});
	}

	private Future<Void> retry(FTPVoidAction action, FTPVoidAction onFail) {
		return retry(() -> {
			action.perform();
			return (Void) null;
		}, onFail);
	}

	private <R> Future<R> perform(FTPAction<R> action, FTPVoidAction onFail) {
		tryConnect();

		return retry(action, onFail);
	}

	private Future<Void> perform(FTPVoidAction action, FTPVoidAction onFail) {
		tryConnect();

		return retry(() -> {
			action.perform();
			return (Void) null;
		}, onFail);
	}

	private <R> Future<R> perform(FTPAction<R> action) {
		return perform(action, null);
	}

	private <R> Future<Void> perform(FTPVoidAction action) {
		return perform(() -> {
			action.perform();
			return (Void) null;
		});
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
