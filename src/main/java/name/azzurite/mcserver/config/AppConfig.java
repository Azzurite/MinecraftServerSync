package name.azzurite.mcserver.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import name.azzurite.mcserver.util.LogUtil;

public final class AppConfig {

    @SuppressWarnings("AccessOfSystemProperties")
    private static final Path CONFIG_PATH = Paths.get(System.getProperty("user.dir"), "config.properties");

    private static final String BASE_PATH_PROPERTY = "base.path";

    private static final String LAUNCHER_PATH_PROPERTY = "launcher.path";

    private static final Logger LOGGER = LoggerFactory.getLogger(AppConfig.class);

    private static final String FTP_HOSTNAME_PROPERTY = "ftp.hostname";

    private static final String FTP_PORT_PROPERTY = "ftp.port";

    private static final String FTP_USERNAME_PROPERTY = "ftp.username";

    private static final String FTP_PASSWORD_PROPERTY = "ftp.password";

    private static final String FTP_BASE_DIRECTORY_PROPERTY = "ftp.base.directory";

    private final Properties config;

    public static AppConfig readConfig() {
        Properties config = new Properties(createDefaultConfig());
        try (BufferedReader reader = Files.newBufferedReader(CONFIG_PATH)) {
            config.load(reader);
            return new AppConfig(config);
        } catch (IOException e) {
            LOGGER.warn("Config file could not be loaded from path {}", CONFIG_PATH);
            LogUtil.stacktrace(LOGGER, e);
            return new AppConfig();
        }
    }

    private static Properties createDefaultConfig() {
        Properties config = new Properties();
        Path defaultBasePath = getDefaultMinecraftPath().resolve("server");
        config.setProperty(BASE_PATH_PROPERTY, defaultBasePath.toString());
        return config;
    }

    public static Path getDefaultMinecraftPath() {
        return Paths.get(System.getenv("APPDATA"), ".minecraft");
    }

    private AppConfig() {
        config = createDefaultConfig();
    }

    private AppConfig(Properties properties) {
        config = new Properties(properties);
    }

    public Path getBaseServerPath() {
        return Paths.get(config.getProperty(BASE_PATH_PROPERTY));
    }

    public Path getSyncPath() {
        return getBaseServerPath().resolve("sync");
    }

    public Optional<String> getMinecraftLauncherPath() {
        return Optional.ofNullable(config.getProperty(LAUNCHER_PATH_PROPERTY));
    }

    public Optional<String> getFtpHostName() {
        return getStringProperty(FTP_HOSTNAME_PROPERTY);
    }

    private Optional<String> getStringProperty(String propertyName) {
        return Optional.ofNullable(config.getProperty(propertyName));
    }

    public Optional<String> getFtpPort() {
        return getStringProperty(FTP_PORT_PROPERTY);
    }

    public Optional<String> getFtpUserName() {
        return getStringProperty(FTP_USERNAME_PROPERTY);
    }

    public Optional<String> getFtpPassword() {
        return getStringProperty(FTP_PASSWORD_PROPERTY);
    }

    public Optional<String> getFtpBaseDirectory() {
        return getStringProperty(FTP_BASE_DIRECTORY_PROPERTY);
    }

    public String getServerMaxMemory() {
        String memory = config.getProperty("max.memory");
        return (memory == null) ? "1408M" : memory;
    }
}
