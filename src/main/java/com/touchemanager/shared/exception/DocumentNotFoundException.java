package com.touchemanager.shared.exception;

public class DocumentNotFoundException extends RuntimeException {
    public DocumentNotFoundException(Long id) {
        super("Document not found with ID: " + id);
    }
}
