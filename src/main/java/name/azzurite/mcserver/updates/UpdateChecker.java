package name.azzurite.mcserver.updates;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import name.azzurite.mcserver.config.AppConfig;
import name.azzurite.mcserver.config.Version;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpdateChecker {

	public static final String RELEASES_PAGE = "https://github.com/Azzurite/MinecraftServerSync/releases";
	private static final Logger LOGGER = LoggerFactory.getLogger(UpdateChecker.class);
	private static final String RELEASE_API_URL = "https://api.github.com/repos/azzurite/MinecraftServerSync/releases";
	private final AppConfig appConfig;

	private final Version latestVersion;

	public UpdateChecker(AppConfig appConfig) throws IOException {
		this.appConfig = appConfig;
		latestVersion = retrieveLatestVersion();
	}

	public boolean isNewerVersionAvailable() {
		return appConfig.getAppVersion().compareTo(latestVersion) < 0;
	}

	private Version retrieveLatestVersion() throws IOException {
		InputStream releasesStream = new URL(RELEASE_API_URL).openStream();
		String releasesString = IOUtils.toString(releasesStream, "UTF-8");
		JSONArray releases = new JSONArray(releasesString);
		JSONObject newestRelease = releases.getJSONObject(0);
		String tagName = newestRelease.getString("tag_name");
		Version latestVersion = new Version(tagName.substring(1));
		return latestVersion;
	}

	public Version getLatestVersion() {
		return latestVersion;
	}
}
