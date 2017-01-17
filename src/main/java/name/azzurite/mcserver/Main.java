package name.azzurite.mcserver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import name.azzurite.mcserver.config.AppConfig;
import name.azzurite.mcserver.ftp.FTPServerSynchronizer;
import name.azzurite.mcserver.minecraft.ServerList;
import name.azzurite.mcserver.util.LogUtil;

@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws IOException {

        //noinspection OverlyBroadCatchBlock
        try {

            AppConfig appConfig = AppConfig.readConfig();

            createNecessaryDirectories(appConfig);

            FTPServerSynchronizer sync = new FTPServerSynchronizer(appConfig);

            try (Server server = new Server(appConfig, sync)) {
                ServerList.addOrReplace(server);
                startMinecraft(appConfig);
            }
            System.out.print("Shutting down in 3... ");
            waitForExit();
        } catch (Exception e) {
            LOGGER.error("Uncaught exception", e);
        }
    }

    private static void waitForExit() {
        try {
            Thread.sleep(1000);
            System.out.print("2... ");
            Thread.sleep(1000);
            System.out.print("1... ");
            Thread.sleep(1000);
        } catch (InterruptedException ignored) {
        }
    }


    private static void createNecessaryDirectories(AppConfig appConfig) throws IOException {
        Files.createDirectories(appConfig.getBaseServerPath());
        Files.createDirectories(appConfig.getSyncPath());
    }

    private static void startMinecraft(AppConfig appConfig) throws IOException {
        Optional<String> minecraftLauncherPath = appConfig.getMinecraftLauncherPath();
        if (minecraftLauncherPath.isPresent()) {
            Path path = Paths.get(minecraftLauncherPath.get());
            ProcessBuilder processBuilder = new ProcessBuilder(path.toString());
            processBuilder.directory(path.getParent().toFile());
            LOGGER.info("Starting Minecraft Launcher with system command: {}",
                    LogUtil.getCommandString(processBuilder));
            processBuilder.start();
        }
    }

    private Main() {}
}
