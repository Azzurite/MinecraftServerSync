package name.azzurite.mcserver.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Properties;

import name.azzurite.mcserver.util.LogUtil;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	private static final String VERSION_RESOURCE_NAME = "version.txt";
	private static final String VERSION_PROPERTY = "version";

	private final Properties config;

	private AppConfig() {
		this(createDefaultConfig());
	}

	private AppConfig(Properties properties) {
		config = new Properties(properties);
		config.setProperty(VERSION_PROPERTY, readVersion());
	}

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

	private String readVersion() {
		try {
			InputStream versionInputStream = getClass().getClassLoader().getResourceAsStream(VERSION_RESOURCE_NAME);
			return IOUtils.toString(versionInputStream, "UTF-8");
		} catch (IOException e) {
			LOGGER.error("Problem reading version from classpath.");
			throw new IllegalStateException(e);
		}
	}

	public Version getAppVersion() {
		return new Version(config.getProperty(VERSION_PROPERTY));
	}

	public Path getBaseServerPath() {
		return Paths.get(config.getProperty(BASE_PATH_PROPERTY));
	}

	public Path getSyncPath() {
		return getBaseServerPath().resolve("MinecraftServerSync/sync");
	}
	public Path getBackupPath() {
		return getBaseServerPath().resolve("MinecraftServerSync/backup");
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
