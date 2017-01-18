package name.azzurite.mcserver.minecraft;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.flowpowered.nbt.CompoundMap;
import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.ListTag;
import com.flowpowered.nbt.Tag;
import com.flowpowered.nbt.stream.NBTInputStream;
import com.flowpowered.nbt.stream.NBTOutputStream;
import name.azzurite.mcserver.Server;
import name.azzurite.mcserver.config.AppConfig;
import name.azzurite.mcserver.util.LogUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ServerList {

	private static final Logger LOGGER = LoggerFactory.getLogger(ServerList.class);

	private static final Path SERVER_DAT = AppConfig.getDefaultMinecraftPath().resolve("servers.dat");

	private static final String ROLFSWUDEL_SERVER_NAME = "Rolfswudel";

	private static final String SERVERS_TAG_NAME = "servers";


	private ServerList() {}

	public static void addOrReplace(Server serverToAdd) {
		Collection<ServerListEntry> servers = new ArrayList<>(readServerList());

		Optional<ServerListEntry> existingServer = servers.stream()
				.filter(server -> ROLFSWUDEL_SERVER_NAME.equals(server.getName()))
				.findAny();

		String newIp = serverToAdd.getIp();
		if (existingServer.isPresent()) {
			existingServer.get().setIp(newIp);
		} else {
			servers.add(new ServerListEntry(ROLFSWUDEL_SERVER_NAME, newIp));
		}

		saveServerList(servers);
	}

	private static void saveServerList(Collection<ServerListEntry> servers) {
		Tag rootTag = convertToTagStructure(servers);

		writeServerList(rootTag);
	}

	private static void writeServerList(Tag rootTag) {
		try {
			try (NBTOutputStream nbtStream = new NBTOutputStream(Files.newOutputStream(SERVER_DAT), false)) {
				nbtStream.writeTag(rootTag);
			}
		} catch (IOException e) {
			LOGGER.error("Error while creating new server.dat, skipping server.dat creation: {}", e.getMessage());
			LogUtil.stacktrace(LOGGER, e);
		}
	}

	private static Tag convertToTagStructure(Collection<ServerListEntry> servers) {
		List<CompoundTag> serverTags = servers.stream()
				.map(ServerListEntry::toTag)
				.collect(Collectors.toList());
		ListTag<CompoundTag> serversListTag = new ListTag<CompoundTag>(SERVERS_TAG_NAME, CompoundTag.class, serverTags);
		CompoundMap rootMap = new CompoundMap();
		rootMap.put(SERVERS_TAG_NAME, serversListTag);
		return new CompoundTag("", rootMap);
	}

	private static Collection<ServerListEntry> readServerList() {
		try (NBTInputStream input = new NBTInputStream(Files.newInputStream(SERVER_DAT), false)) {
			CompoundTag rootTag = (CompoundTag) input.readTag();

			Map<String, Tag<?>> rootMap = rootTag.getValue();
			if (rootMap == null) {
				LOGGER.info("Server.dat root tag does not contain a value, creating new server.dat.");
				return Collections.emptyList();
			}
			Tag<List<CompoundTag>> serversTag = (Tag<List<CompoundTag>>) rootMap.get(SERVERS_TAG_NAME);
			if (serversTag == null) {
				LOGGER.info("Server.dat root tag does not contain the server list, creating new server.dat.");
				return Collections.emptyList();
			}

			return serversTag.getValue().stream()
					.map(tag -> (CompoundTag) tag)
					.map(CompoundTag::getValue)
					.map(ServerListEntry::new)
					.collect(Collectors.toList());
		} catch (IOException e) {
			LOGGER.info("Error while parsing server.dat, creating new server.dat: {}", e.getMessage());
			LogUtil.stacktrace(LOGGER, e);
			return Collections.emptyList();
		}
	}
}
