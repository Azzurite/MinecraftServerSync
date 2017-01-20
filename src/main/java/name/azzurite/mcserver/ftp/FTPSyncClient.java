package name.azzurite.mcserver.ftp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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

public class FTPSyncClient implements SyncClient {

	private static final int MAX_RETRIES = 3;

	private static final int BUF_SIZE = 1048576;

	private static final Logger LOGGER = LoggerFactory.getLogger(FTPSyncClient.class);

	private final FTPClient ftpClient = new FTPClient();

	private final AppConfig appConfig;

	private final ExecutorService executor = Executors.newSingleThreadExecutor();

	private final FTPTransferProgressListener progressListener;

	public FTPSyncClient(AppConfig appConfig) {
		ftpClient.setBufferSize(BUF_SIZE);
		this.appConfig = appConfig;
		progressListener = new FTPTransferProgressListener();
		ftpClient.setCopyStreamListener(progressListener);
	}

	public static void main(String[] args) {
		AppConfig appConfig = AppConfig.readConfig();
		SyncClient ftpClient = new FTPSyncClient(appConfig);
		ftpClient.downloadFile("map.zip");
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

	private static String generateMd5FileName(String fileName) {
		return fileName + ".md5";
	}

	private static byte[] computeMd5(Path localPath) throws IOException {
		if (!Files.exists(localPath)) {
			return new byte[0];
		}
		try (InputStream file = Files.newInputStream(localPath)) {
			return DigestUtils.md5(file);
		}
	}

	private void tryConnect() {
		if (ftpClient.isConnected()) {
			return;
		}
		LOGGER.debug("Connecting...");

		retry(() -> {
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
		});
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

	private Future<byte[]> retrieveByteFileContents(String file) {
		LOGGER.debug("Retrieving byte file contents for file '{}'", file);

		return perform(() -> {
			try (ByteArrayOutputStream temp = new ByteArrayOutputStream()) {
				ftpClient.retrieveFile(file, temp);
				logFtpCommand(ftpClient, "downloadFile", file);

				byte[] bytes = temp.toByteArray();
				LOGGER.debug("File contents retrieved: {}", bytes);

				return bytes;
			}
		});
	}

	@Override
	public SyncActionFuture<Void> setFileContents(String file, String contents) {
		LOGGER.debug("Setting file({}) contents: {}", file, contents);

		return new NoProgressSyncActionFuture<>(file + " set content", perform(() -> {
			ftpClient.storeFile(file, IOUtils.toInputStream(contents, "UTF-8"));
			logFtpCommand(ftpClient, "storeFile", file, contents);
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

		String fileName = path.getFileName().toString();
		String md5FileName = generateMd5FileName(fileName);

		Future<Void> uploadResult = perform(() -> {
			try {
				byte[] remoteMd5 = new byte[0];
				remoteMd5 = AsyncUtil.getResult(retrieveByteFileContents(md5FileName));
				byte[] localMd5 = computeMd5(path);
				if (Arrays.equals(localMd5, remoteMd5)) {
					LOGGER.debug("Equal MD5, skipping upload.");
				}

				try (InputStream zipFile = Files.newInputStream(path);
					 InputStream md5File = new ByteArrayInputStream(localMd5)) {
					ftpClient.storeFile(fileName, zipFile);
					logFtpCommand(ftpClient, "storeFile", fileName);
					ftpClient.storeFile(md5FileName, md5File);
					logFtpCommand(ftpClient, "storeFile", md5FileName);

					LOGGER.debug("Uploaded file: {}", path);
				}
			} catch (ExecutionException e) {
				throw new FTPException(e);
			}
		});
		long fileSize = getFileSize(fileName);
		return new FTPTransferSyncActionFuture<>(uploadResult, progressListener, fileName + " upload", fileSize);
	}

	@Override
	public SyncActionFuture<Boolean> doesFileExist(String file) {
		LOGGER.debug("Checking file existence: {}", file);

		return new NoProgressSyncActionFuture<>(file + " existance check", perform(() -> {
			FTPFile[] ftpFiles = ftpClient.listFiles(file);
			logFtpCommand(ftpClient, "listFiles", file);

			return ftpFiles.length > 0;
		}));
	}

	@Override
	public SyncActionFuture<Path> downloadFile(String fileName) {
		LOGGER.debug("Downloading file {}", fileName);

		Future<Path> downloadResult = perform(() -> {
			Path localPath = appConfig.getSyncPath().resolve(fileName);

			try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream()) {
				String md5FileName = generateMd5FileName(fileName);

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
		long fileSize = getFileSize(fileName);
		return new FTPTransferSyncActionFuture<>(downloadResult, progressListener, fileName + " download", fileSize);
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
