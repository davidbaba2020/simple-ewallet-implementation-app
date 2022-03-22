package com.decadave.ewalletapp.KYC;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class KycDto {
    private String bVN;
    private String driverLicence;
    private String passportUrl;
}
