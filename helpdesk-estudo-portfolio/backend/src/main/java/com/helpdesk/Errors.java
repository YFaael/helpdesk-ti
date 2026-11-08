package com.helpdesk;

import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.dao.DataIntegrityViolationException;

@RestControllerAdvice
class Errors {
    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<Map<String,String>> credentials(){return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message","E-mail ou senha inválidos."));}
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String,String>> invalid(MethodArgumentNotValidException ex){
        String message=ex.getBindingResult().getFieldErrors().stream().map(e->e.getField()+": "+e.getDefaultMessage()).limit(3).collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(Map.of("message",message.isBlank()?"Verifique os campos.":message));
    }
    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<Map<String,String>> expected(ResponseStatusException ex){
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of("message",ex.getReason()==null?"Requisição inválida.":ex.getReason()));
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<Map<String,String>> conflict(){return ResponseEntity.status(409).body(Map.of("message","Os dados informados já existem ou violam uma restrição."));}
}
