package name.azzurite.mcserver;

import java.io.IOException;
import java.util.Optional;

public interface ServerSynchronizer {

    void setServerIp(String ip) throws IOException;

    Optional<String> getServerIp() throws IOException;

    void retrieveFiles() throws IOException;

    void saveFiles() throws IOException;

    void deleteServerIp() throws IOException;

}
