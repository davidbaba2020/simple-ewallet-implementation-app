package com.decadave.ewalletapp.shared.exceptions;

public class kycUpdatedAlreadyException extends RuntimeException {
    public kycUpdatedAlreadyException (String message)
    {
        super(message);
    }
}
