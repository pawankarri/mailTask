package com.broadcastMail.exception;

public class FolderDoesNotExistsException extends RuntimeException{
    public FolderDoesNotExistsException(String  message){
        super(message);
    }
}
