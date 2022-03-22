package com.decadave.ewalletapp.shared.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TransferDto
{
    private Double amount;
    private String transactionSummary;
    private String transactionPin;
    private String receiversEmail;
    private String receiversAccountNumber;
}
