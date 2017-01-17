package name.azzurite.mcserver.util;

import java.util.function.Predicate;

public final class StreamUtil {

    public static <T> Predicate<T> not(Predicate<T> t) {
        return t.negate();
    }

    private StreamUtil() {}
}
