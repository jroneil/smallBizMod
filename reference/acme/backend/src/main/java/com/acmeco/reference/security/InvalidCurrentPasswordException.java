package com.acmeco.reference.security;

public class InvalidCurrentPasswordException extends RuntimeException {

	public InvalidCurrentPasswordException() {
		super("Current password is incorrect");
	}
}
