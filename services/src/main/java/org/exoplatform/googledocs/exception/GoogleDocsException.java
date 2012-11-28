package org.exoplatform.googledocs.exception;

public class GoogleDocsException extends Exception {

	private static final long serialVersionUID = 1L;
	
	private String messageKey;

	public GoogleDocsException(String messageKey, String message) {
		super(message);
		this.messageKey = messageKey;
	}
	
	public GoogleDocsException(String messageKey, String message, Throwable throwable) {
		super(message, throwable);
		this.messageKey = messageKey;
	}

	public String getMessageKey() {
		return messageKey;
	}

}
