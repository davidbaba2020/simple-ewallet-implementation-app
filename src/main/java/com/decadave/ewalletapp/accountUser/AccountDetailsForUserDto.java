package com.decadave.ewalletapp.accountUser;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AccountDetailsForUserDto {
    private String AccountHolderFullName;
    private String email;
    private String walletAccountNumber;
    private String walletBalance;
    private String bVN;
    private String driverKYCLicence;
    private String passportKYCUrl;
    private String approvedLevel;
}
