package com.codesync.auth.exceptionhandler;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final AuthenticationManager authenticationManager;

    GlobalExceptionHandler(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }
	
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {

        Map<String, Object> errors = new HashMap<>();
        errors.put("status", HttpStatus.BAD_REQUEST.value());
        errors.put("error", "Validation Failed");

        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }
        errors.put("fieldErrors", fieldErrors);

        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }
    
	@ExceptionHandler(RuntimeException.class)
	public ResponseEntity<?> handleRuntimeException(RuntimeException ex){
		return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
	}
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
