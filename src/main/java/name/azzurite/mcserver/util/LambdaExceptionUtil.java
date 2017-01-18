package name.azzurite.mcserver.util;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

@SuppressWarnings("ALL")
public final class LambdaExceptionUtil {

	private LambdaExceptionUtil() {}

	public static <T, E extends Exception> Consumer<T> rethrow(Consumer_WithExceptions<T, E> consumer) {
		return t -> {
			try {
				consumer.accept(t);
			} catch (Exception exception) {
				throwAsUnchecked(exception);
			}
		};
	}

	public static <T, U, E extends Exception> BiConsumer<T, U> rethrow(
			BiConsumer_WithExceptions<T, U, E> biConsumer) {
		return (t, u) -> {
			try {
				biConsumer.accept(t, u);
			} catch (Exception exception) {
				throwAsUnchecked(exception);
			}
		};
	}

	public static <T, R, E extends Exception> Function<T, R> rethrow(
			Function_WithExceptions<T, R, E> function) {
		return t -> {
			try {
				return function.apply(t);
			} catch (Exception exception) {
				throwAsUnchecked(exception);
				return null;
			}
		};
	}

	public static <T, E extends Exception> Supplier<T> rethrow(Supplier_WithExceptions<T, E> function) {
		return () -> {
			try {
				return function.get();
			} catch (Exception exception) {
				throwAsUnchecked(exception);
				return null;
			}
		};
	}

	public static <T, E extends Exception> Predicate<T> rethrow(
			Predicate_WithExceptions<T, E> predicate) {
		return t -> {
			try {
				return predicate.test(t);
			} catch (Exception exception) {
				throwAsUnchecked(exception);
				return false;
			}
		};
	}

	@SuppressWarnings("unchecked")
	private static <E extends Throwable> void throwAsUnchecked(Exception exception) throws E { throw (E) exception; }

	@FunctionalInterface
	public interface Consumer_WithExceptions<T, E extends Exception> {

		void accept(T t) throws E;
	}

	@FunctionalInterface
	public interface BiConsumer_WithExceptions<T, U, E extends Exception> {

		void accept(T t, U u) throws E;
	}

	@FunctionalInterface
	public interface Function_WithExceptions<T, R, E extends Exception> {

		R apply(T t) throws E;
	}

	@FunctionalInterface
	public interface Supplier_WithExceptions<T, E extends Exception> {

		T get() throws E;
	}

	@FunctionalInterface
	public interface Runnable_WithExceptions<E extends Exception> {

		void run() throws E;
	}

	@FunctionalInterface
	public interface Predicate_WithExceptions<T, E extends Exception> {

		boolean test(T t) throws E;
	}
}