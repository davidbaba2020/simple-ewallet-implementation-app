package com.decadave.ewalletapp.accountUser;

import com.decadave.ewalletapp.KYC.KycDto;
import com.decadave.ewalletapp.role.RoleDto;
import com.decadave.ewalletapp.shared.dto.ChangeTransactionPinDto;
import com.decadave.ewalletapp.shared.dto.TopUpDto;
import com.decadave.ewalletapp.shared.dto.TransferDto;
import com.decadave.ewalletapp.shared.dto.WithdrawalDto;
import com.decadave.ewalletapp.shared.responses.HttpResponse;
import com.decadave.ewalletapp.transaction.Transaction;
import com.decadave.ewalletapp.transaction.TransactionDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class AccountUserController
{
    @Autowired
    private  final UserService userService;

    public AccountUserController(UserService userService)
    {
        this.userService = userService;
    }


    @PostMapping("/user/create-account")
    public ResponseEntity<HttpResponse> saveUsers (@RequestBody AccountUserDto accountUserDto)
    {
        return response(HttpStatus.OK, userService.createAccountUser(accountUserDto));
    }

    @PutMapping("/user/top-up-wallet-balance")
    public ResponseEntity<Transaction> topUpAccount (@RequestBody TopUpDto topUpDto)
    {
        return new ResponseEntity<>(userService.topUpWalletBalance(topUpDto), HttpStatus.OK);
    }

    @PutMapping("/user/withdrawal-from-wallet")
    public ResponseEntity<Transaction> withdrawal (@RequestBody WithdrawalDto withdrawalDto)
    {
        return new ResponseEntity<>(userService.withdrawal(withdrawalDto), HttpStatus.OK);
    }

    @PutMapping("/user/transfer-from-wallet")
    public ResponseEntity<TransactionDto> transfer (@RequestBody TransferDto transferDto)
    {
        return new ResponseEntity<>(userService.transferMoney(transferDto), HttpStatus.OK);
    }


    @PutMapping("/user/change-transaction-pin")
    public  ResponseEntity<HttpResponse> changeTransactionPin (@RequestBody ChangeTransactionPinDto changeTransactionPinDto)
    {
        return response(HttpStatus.CREATED, userService.changeTransactionPin(changeTransactionPinDto));
    }

    @PutMapping("/user/kyc-upgrade-level")
    public ResponseEntity<String> doYourKycAndUpgradeLevel (@RequestBody KycDto kycDto)
    {
        return new ResponseEntity<>(userService.doKycDocumentation(kycDto), HttpStatus.OK);
    }

    @GetMapping("/my-account-summary")
    public ResponseEntity<AccountDetailsForUserDto> getAccountSummary ()
    {
        return new ResponseEntity<>(userService.getMyAccountDetails(), HttpStatus.OK);
    }


    public static ResponseEntity<HttpResponse> response(HttpStatus httpStatus, String message)
    {
        return new  ResponseEntity<>(new HttpResponse(httpStatus.value(),
                httpStatus, httpStatus.getReasonPhrase().toUpperCase(),
                message.toUpperCase()), httpStatus );
    }

}
