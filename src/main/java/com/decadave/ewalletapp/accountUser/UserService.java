package com.decadave.ewalletapp.accountUser;


import com.decadave.ewalletapp.KYC.KycAdminVerificationDto;
import com.decadave.ewalletapp.KYC.KycDto;
import com.decadave.ewalletapp.role.RoleDto;
import com.decadave.ewalletapp.shared.dto.TransferDto;
import com.decadave.ewalletapp.shared.dto.TopUpDto;
import com.decadave.ewalletapp.shared.dto.ChangeTransactionPinDto;
import com.decadave.ewalletapp.shared.dto.WithdrawalDto;
import com.decadave.ewalletapp.transaction.Transaction;
import com.decadave.ewalletapp.transaction.TransactionDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface UserService
{
    String createAccountUser(AccountUserDto userDto);
    String saveRole (RoleDto roleDto);
    String addROleTOUser(AddRoleToUserDto addRoleToUserDto);
    AccountUserDto getUser(String userId);
    List<AccountUser> AccountUsers();
    Transaction topUpWalletBalance(TopUpDto topUpDto);
    Transaction withdrawal(WithdrawalDto withdrawalDto);
    Transaction transferMoney (TransferDto transferDto);
    String changeTransactionPin (ChangeTransactionPinDto changeTransactionPinDto);
    String doKycDocumentation (KycDto kycDto);
    String kycApprovalByAdmin (KycAdminVerificationDto kycAdminVerificationDto);
    AccountDetailsForUserDto getMyAccountDetails();
}
