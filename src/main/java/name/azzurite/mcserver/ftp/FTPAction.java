package name.azzurite.mcserver.ftp;

import java.io.IOException;

@FunctionalInterface
interface FTPAction<R> {

	R perform() throws IOException;
}
