package com.codesync.auth.exceptionhandler;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
	
	 @ExceptionHandler(EmailAlreadyExistsException.class)
	    public ResponseEntity<?> handleEmail(EmailAlreadyExistsException ex) {
	        return ResponseEntity
	                .status(HttpStatus.CONFLICT)
	                .body(Map.of("error", ex.getMessage()));
	    }

	    @ExceptionHandler(UsernameAlreadyExistsException.class)
	    public ResponseEntity<?> handleUsername(UsernameAlreadyExistsException ex) {
	        return ResponseEntity
	                .status(HttpStatus.CONFLICT)
	                .body(Map.of("error", ex.getMessage()));
	    }
	    
	    @ExceptionHandler(UserNotFoundException.class)
	    public ResponseEntity<?> handleUser(UserNotFoundException ex){
	    	return ResponseEntity
	    			.status(HttpStatus.CONFLICT)
	    			.body(Map.of("error",ex.getMessage()));
	    }

}
