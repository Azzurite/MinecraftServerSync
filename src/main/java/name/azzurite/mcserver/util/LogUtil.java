package name.azzurite.mcserver.util;

import org.slf4j.Logger;

public final class LogUtil {

    public static void stacktrace(Logger logger, Throwable e) {
        logger.debug("Stacktrace:", e);
    }

    public static String getCommandString(ProcessBuilder processBuilder) {
        StringBuilder sb = new StringBuilder();
        processBuilder.command().stream().forEach((arg) -> {
            sb.append(arg);
            sb.append(' ');
        });
        return sb.toString();
    }

    private LogUtil() {}
}
