package name.azzurite.mcserver.ftp;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPConnectionClosedException;
import org.apache.commons.net.ftp.FTPFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import name.azzurite.mcserver.config.AppConfig;
import name.azzurite.mcserver.util.LogUtil;

class AutoFTPClient {

    private static final int MAX_RETRIES = 3;

    private static final int BUF_SIZE = 1048576;

    private static final Logger LOGGER = LoggerFactory.getLogger(AutoFTPClient.class);

    private final FTPClient ftpClient = new FTPClient();

    private final AppConfig appConfig;

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

    AutoFTPClient(AppConfig appConfig) {
        ftpClient.setBufferSize(BUF_SIZE);
        this.appConfig = appConfig;
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
        }, ftpClient::disconnect);
    }

    public String retrieveFileContents(String file) {
        LOGGER.debug("Retrieving file contents for file '{}'", file);

        return perform(() -> {
            try (ByteArrayOutputStream temp = new ByteArrayOutputStream()) {
                ftpClient.retrieveFile(file, temp);
                logFtpCommand(ftpClient, "downloadFile", file);

                String fileContents = temp.toString("UTF-8");

                LOGGER.debug("File contents retrieved: {}", fileContents);

                return fileContents;
            }
        });
    }

    private byte[] retrieveByteFileContents(String file) {
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

    public void setFileContents(String file, String contents) {
        LOGGER.debug("Setting file({}) contents: {}", file, contents);

        perform(() -> {
            ftpClient.storeFile(file, IOUtils.toInputStream(contents, "UTF-8"));
            logFtpCommand(ftpClient, "storeFile", file, contents);
        });
    }

    public void deleteFile(String file) {
        LOGGER.debug("Deleting file: {}", file);

        perform(() -> {
            ftpClient.deleteFile(file);
            logFtpCommand(ftpClient, "deleteFile", file);
        });
    }

    public void uploadFile(Path path) {
        LOGGER.debug("Uploading file: {}", path);

        String fileName = path.getFileName().toString();
        String md5FileName = generateMd5FileName(fileName);

        perform(() -> {

            byte[] remoteMd5 = retrieveByteFileContents(md5FileName);
            byte[] localMd5 = computeMd5(path);
            if (Arrays.equals(localMd5, remoteMd5)) {
                LOGGER.debug("Equal MD5, skipping upload.");
                return;
            }

            try (InputStream zipFile = Files.newInputStream(path);
                 InputStream md5File = new ByteArrayInputStream(localMd5)) {
                ftpClient.storeFile(fileName, zipFile);
                logFtpCommand(ftpClient, "storeFile", fileName);
                ftpClient.storeFile(md5FileName, md5File);
                logFtpCommand(ftpClient, "storeFile", md5FileName);

                LOGGER.debug("Uploaded file: {}", path);
            }
        });
    }

    private static String generateMd5FileName(String fileName) {
        return fileName + ".md5";
    }

    public boolean doesFileExist(String file) {
        LOGGER.debug("Checking file existence: {}", file);

        return perform(() -> {
            FTPFile[] ftpFiles = ftpClient.listFiles(file);
            logFtpCommand(ftpClient, "listFiles", file);

            return ftpFiles.length > 0;
        });
    }

    public Path downloadFile(String fileName) {
        LOGGER.debug("Downloading file {}", fileName);

        return perform(() -> {
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
    }

    private static byte[] computeMd5(Path localPath) throws IOException {
        if (!Files.exists(localPath)) {
            return new byte[0];
        }
        try (InputStream file = Files.newInputStream(localPath)) {
            return DigestUtils.md5(file);
        }
    }

    private <R> R retry(FTPAction<R> action, FTPVoidAction onFail) {
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
    }

    @SuppressWarnings("ReturnOfNull")
    private void retry(FTPVoidAction action, FTPVoidAction onFail) {
        retry(() -> {
            action.perform();
            return null;
        }, onFail);
    }

    private <R> R perform(FTPAction<R> action, FTPVoidAction onFail) {
        tryConnect();

        return retry(action, onFail);
    }

    private <R> R perform(FTPAction<R> action) {
        return perform(action, null);
    }

    @SuppressWarnings("ReturnOfNull")
    private void perform(FTPVoidAction action, FTPVoidAction onFail) {
        perform(() -> {
            action.perform();
            return null;
        }, onFail);
    }


    private void perform(FTPVoidAction action) {
        perform(action, null);
    }
}
