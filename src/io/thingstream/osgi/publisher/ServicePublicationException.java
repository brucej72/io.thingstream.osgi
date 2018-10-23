package io.thingstream.osgi.publisher;

public class ServicePublicationException extends Exception {

	private static final long serialVersionUID = 7867194312999485567L;

	public ServicePublicationException() {
	}

	public ServicePublicationException(String message) {
		super(message);
	}

	public ServicePublicationException(Throwable cause) {
		super(cause);
	}

	public ServicePublicationException(String message, Throwable cause) {
		super(message, cause);
	}

	public ServicePublicationException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
