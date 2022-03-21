package com.decadave.ewalletapp.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TopUpDto
{
    private Double amount;
    private String transactionSummary;
    private String transactionPin;
}
