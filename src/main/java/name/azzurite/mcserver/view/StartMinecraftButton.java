package name.azzurite.mcserver.view;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import name.azzurite.mcserver.config.AppConfig;
import name.azzurite.mcserver.minecraft.ServerList;
import name.azzurite.mcserver.sync.ServerInfo;
import name.azzurite.mcserver.sync.ServerInfoService;
import name.azzurite.mcserver.util.LogUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StartMinecraftButton extends WithRootFXMLNode {

	private static final Logger LOGGER = LoggerFactory.getLogger(StartMinecraftButton.class);

	private final AppConfig appConfig;
	private final ServerInfoService serverInfoService;


	public StartMinecraftButton(AppConfig appConfig, ServerInfoService serverInfoService) {
		this.appConfig = appConfig;
		this.serverInfoService = serverInfoService;
	}

	@FXML
	private void startMinecraftClicked(ActionEvent event) {
		try {
			ServerInfo serverInfo = serverInfoService.getValue();
			Optional<String> ip = serverInfo.getIp();
			ip.ifPresent(ServerList::addOrReplace);

			startMinecraft();
		} catch (IOException e) {
			LOGGER.info("Error while starting minecraft: {}", e.getMessage());
			LogUtil.stacktrace(LOGGER, e);
		}
	}

	private void startMinecraft() throws IOException {
		Optional<String> minecraftLauncherPath = appConfig.getMinecraftLauncherPath();
		if (minecraftLauncherPath.isPresent()) {
			Path path = Paths.get(minecraftLauncherPath.get());
			ProcessBuilder processBuilder = new ProcessBuilder("cmd", "/C", "start", "\"\"", path.toString());
			processBuilder.directory(path.getParent().toFile());
			LOGGER.info("Starting Minecraft Launcher with system command: {}",
					LogUtil.getCommandString(processBuilder));
			processBuilder.start();
		}
	}

}
