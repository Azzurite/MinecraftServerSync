package name.azzurite.mcserver.ftp;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.zip.ZipEntrySource;
import org.zeroturnaround.zip.ZipUtil;

import name.azzurite.mcserver.ServerSynchronizer;
import name.azzurite.mcserver.config.AppConfig;
import name.azzurite.mcserver.util.LogUtil;

import static name.azzurite.mcserver.util.LambdaExceptionUtil.*;
import static name.azzurite.mcserver.util.StreamUtil.*;

public class FTPServerSynchronizer implements ServerSynchronizer {

    private static class OnlyContentZipEntrySource implements ZipEntrySource {

        private final String path;

        private final File file;

        OnlyContentZipEntrySource(String path, File file) {
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

    private static final Logger LOGGER = LoggerFactory.getLogger(FTPServerSynchronizer.class);

    private static final String SERVER_IP_FILE = "runningServer";

    private static final Map<String, List<String>> ZIP_FILES = new HashMap<>();

    private static final String UPLOAD_IN_PROGRESS_FILE_NAME = "uploadInProgress";

    private static final long SECONDS_10 = 10000L;

    static {
        ZIP_FILES.put("map.zip", Arrays.asList("rolfswudel", "rolfswudel_nether", "rolfswudel_the_end"));
        ZIP_FILES.put("server.zip", Arrays.asList("spigot.jar", "plugins"));
        ZIP_FILES.put("config.zip",
                Arrays.asList("banned-ips.json", "banned-players.json", "ops.json", "usercache.json", "whitelist.json",
                        "server.properties", "eula.txt", "run.bat", "bukkit.yml", "commands.yml", "help.yml",
                        "permissions.yml", "spigot.yml"));
    }


    private final AppConfig appConfig;

    private final AutoFTPClient ftpClient;

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

    public FTPServerSynchronizer(AppConfig appConfig) {
        this.appConfig = appConfig;
        ftpClient = new AutoFTPClient(appConfig);
    }

    private void extractFile(Path file) {
        LOGGER.info("Extracting file: {}", file);
        ZipUtil.unpack(file.toFile(), appConfig.getBaseServerPath().toFile());
        LOGGER.info("Extract finished");
    }

    @Override
    public Optional<String> getServerIp() throws IOException {
        String serverIp = ftpClient.retrieveFileContents(SERVER_IP_FILE);

        return StringUtils.isEmpty(serverIp) ? Optional.empty() : Optional.of(serverIp);
    }

    @Override
    public void setServerIp(String ip) throws IOException {
        LOGGER.info("Publishing server IP {}", ip);
        ftpClient.setFileContents(SERVER_IP_FILE, ip);
    }

    @Override
    public void deleteServerIp() throws IOException {
        LOGGER.info("Depublishing server IP");
        ftpClient.deleteFile(SERVER_IP_FILE);
    }

    @Override
    public void retrieveFiles() throws IOException {
        while (isUploadInProgress()) {
            LOGGER.info("Someone is still uploading the server files. Waiting for completion...");
            try {
                //noinspection BusyWait
                Thread.sleep(SECONDS_10);
            } catch (InterruptedException ignored) {
            }

        }


        Set<String> existingZipFiles = ZIP_FILES.keySet().stream()
                .filter(ftpClient::doesFileExist)
                .collect(Collectors.toSet());
        LOGGER.info("Backing up previous data");
        backupCurrentFiles(existingZipFiles);
        LOGGER.info("Backed up previous data");
        LOGGER.info("Deleting previous data");
        existingZipFiles.stream()
                .map(ZIP_FILES::get)
                .flatMap(Collection::stream)
                .forEach(this::deleteFile);
        LOGGER.info("Deleted previous data");
        LOGGER.info("Downloading new server files");
        existingZipFiles.stream()
                .filter(ftpClient::doesFileExist)
                .map(this::downloadFile)
                .forEach(this::extractFile);
        LOGGER.info("Downloaded new server files");
    }

    private void deleteFile(String fileToDelete) {
        Path pathToDelete = appConfig.getBaseServerPath().resolve(fileToDelete);
        if (!Files.exists(pathToDelete, LinkOption.NOFOLLOW_LINKS)) {
            return;
        }

        if (Files.isDirectory(pathToDelete)) {
            deleteDirectory(pathToDelete);
        } else {
            try {
                Files.delete(pathToDelete);
            } catch (IOException e) {
                LOGGER.warn("Could not delete file {}", fileToDelete);
                LogUtil.stacktrace(LOGGER, e);
            }
        }
    }

    private void backupCurrentFiles(Iterable<String> zippedFiles) throws IOException {
        Map<String, List<String>> zipFileMap = new HashMap<>();
        zippedFiles.forEach((zippedFile) -> {
            List<String> filesToBackup = ZIP_FILES.get(zippedFile);
            Optional<Path> missingFile = filesToBackup.stream()
                    .map(appConfig.getBaseServerPath()::resolve)
                    .filter(not(Files::exists))
                    .findAny();
            if (missingFile.isPresent()) {
                LOGGER.warn("Can not backup {}, required files are missing.", zippedFile);
            } else {
                zipFileMap.put("backup_" + zippedFile, filesToBackup);
            }
        });

        zipFiles(zipFileMap);
    }

    private boolean isUploadInProgress() {
        return Boolean.parseBoolean(ftpClient.retrieveFileContents(UPLOAD_IN_PROGRESS_FILE_NAME));
    }

    private Path downloadFile(String fileName) {
        LOGGER.info("Downloading file '{}'...", fileName);
        Path downloadedFile = ftpClient.downloadFile(fileName);
        LOGGER.info("Download finished");
        return downloadedFile;
    }

    @Override
    public void saveFiles() throws IOException {
        List<Path> paths = zipFiles(ZIP_FILES);
        uploadFiles(paths);
        LOGGER.info("All server files synchronized!");
    }

    private List<Path> zipFiles(Map<String, List<String>> zippedFiles) throws IOException {
        return zippedFiles.entrySet().stream()
                .map(rethrow(this::zipFile))
                .collect(Collectors.toList());
    }

    private Path zipFile(Map.Entry<String, List<String>> zipFileDescriptor) throws IOException {
        Path zipFile = appConfig.getSyncPath().resolve(zipFileDescriptor.getKey());

        ZipEntrySource[] filesToZip = zipFileDescriptor.getValue().stream()
                .map(appConfig.getBaseServerPath()::resolve)
                .map(rethrow(FTPServerSynchronizer::pathToFileList))
                .flatMap(List::stream)
                .map(path -> {
                    Path relativePath = appConfig.getBaseServerPath().relativize(path);
                    return new OnlyContentZipEntrySource(relativePath.toString(), path.toFile());
                })
                .toArray(ZipEntrySource[]::new);


        LOGGER.info("Zipping file: {}", zipFile);
        ZipUtil.pack(filesToZip, zipFile.toFile());
        LOGGER.info("File zipped: {}", zipFile);

        return zipFile;
    }

    private void uploadFiles(Iterable<Path> paths) {
        ftpClient.setFileContents(UPLOAD_IN_PROGRESS_FILE_NAME, String.valueOf(true));
        paths.forEach((path) -> {
            LOGGER.info("Uploading file '{}' to server", path);
            LOGGER.warn("WAIT FOR THIS TO FINISH OR ELSE THE SERVER FILES WILL BE CORRUPTED!");
            ftpClient.uploadFile(path);
            LOGGER.info("Upload finished");
        });
        ftpClient.setFileContents(UPLOAD_IN_PROGRESS_FILE_NAME, String.valueOf(false));
    }

}
