package name.azzurite.mcserver.ftp;

@SuppressWarnings("WeakerAccess")
public class FTPException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public FTPException() {
	}

	public FTPException(String message) {
		super(message);
	}

	public FTPException(String message, Throwable cause) {
		super(message, cause);
	}

	public FTPException(Throwable cause) {
		super(cause);
	}

	@SuppressWarnings("BooleanParameter")
	public FTPException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}


}
