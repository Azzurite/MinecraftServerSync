package name.azzurite.mcserver.console;

public class RedirectException extends RuntimeException {

	public RedirectException() {
	}

	public RedirectException(String message) {
		super(message);
	}

	public RedirectException(String message, Throwable cause) {
		super(message, cause);
	}

	public RedirectException(Throwable cause) {
		super(cause);
	}

}
