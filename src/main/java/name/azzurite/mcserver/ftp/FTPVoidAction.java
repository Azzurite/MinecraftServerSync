package name.azzurite.mcserver.ftp;

import java.io.IOException;

@FunctionalInterface
interface FTPVoidAction {

	void perform() throws IOException;
}
