package com.decadave.ewalletapp.shared.exceptions;

public class KycNotFoundException extends RuntimeException {
    public KycNotFoundException(String message)
    {
        super(message);
    }
}
