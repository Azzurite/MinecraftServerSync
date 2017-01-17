package name.azzurite.mcserver.minecraft;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.jnbt.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import name.azzurite.mcserver.Server;
import name.azzurite.mcserver.config.AppConfig;
import name.azzurite.mcserver.util.LogUtil;

public final class ServerList {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerList.class);

    private static final Path SERVER_DAT = AppConfig.getDefaultMinecraftPath().resolve("servers.dat");

    private static final String ROLFSWUDEL_SERVER_NAME = "Rolfswudel";

    private static final String SERVERS_TAG_NAME = "servers";


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
            ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
            try (NBTOutputStream nbtStream = new NBTOutputStream(byteOutputStream)) {
                nbtStream.writeTag(rootTag);
            }
            byte[] gzippedServerDat = byteOutputStream.toByteArray();

            try (ByteArrayInputStream byteInputStream = new ByteArrayInputStream(gzippedServerDat);
                 GZIPInputStream gzipStream = new GZIPInputStream(byteInputStream)) {
                Files.copy(gzipStream, SERVER_DAT, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            LOGGER.error("Error while creating new server.dat, skipping server.dat creation: {}", e.getMessage());
            LogUtil.stacktrace(LOGGER, e);
        }
    }

    private static Tag convertToTagStructure(Collection<ServerListEntry> servers) {
        List<Tag> serverTags = servers.stream()
                .map(ServerListEntry::toTag)
                .collect(Collectors.toList());
        ListTag serversListTag = new ListTag(SERVERS_TAG_NAME, CompoundTag.class, serverTags);
        HashMap<String, Tag> rootMap = new HashMap<>();
        rootMap.put(SERVERS_TAG_NAME, serversListTag);
        return new CompoundTag("", rootMap);
    }

    private static Collection<ServerListEntry> readServerList() {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        try (GZIPOutputStream gzippedServerDat = new GZIPOutputStream(byteOut)) {
            Files.copy(SERVER_DAT, gzippedServerDat);
        } catch (IOException e) {
            LOGGER.info("Error while opening server.dat, creating new file: {}", e.getMessage());
            LogUtil.stacktrace(LOGGER, e);
            return Collections.emptyList();
        }
        byte[] serverDat = byteOut.toByteArray();

        try (NBTInputStream input = new NBTInputStream(new ByteArrayInputStream(serverDat))) {
            CompoundTag rootTag = (CompoundTag) input.readTag();

            Map<String, Tag> rootMap = rootTag.getValue();
            if (rootMap == null) {
                LOGGER.info("Server.dat root tag does not contain a value, creating new server.dat.");
                return Collections.emptyList();
            }
            ListTag serversTag = (ListTag) rootMap.get(SERVERS_TAG_NAME);
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

    private ServerList() {}
}
