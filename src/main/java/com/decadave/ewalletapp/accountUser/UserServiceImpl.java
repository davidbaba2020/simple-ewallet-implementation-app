package com.decadave.ewalletapp.accountUser;

import com.decadave.ewalletapp.KYC.*;
import com.decadave.ewalletapp.account.Account;
import com.decadave.ewalletapp.account.AccountRepository;
import com.decadave.ewalletapp.role.Role;
import com.decadave.ewalletapp.role.RoleDto;
import com.decadave.ewalletapp.role.RoleRepository;
import com.decadave.ewalletapp.shared.configuration.mail_sender.MailSenderImpl;
import com.decadave.ewalletapp.shared.dto.TransferDto;
import com.decadave.ewalletapp.shared.dto.ChangeTransactionPinDto;
import com.decadave.ewalletapp.shared.dto.TopUpDto;
import com.decadave.ewalletapp.shared.dto.WithdrawalDto;
import com.decadave.ewalletapp.shared.enums.TransactionLevel;
import com.decadave.ewalletapp.shared.enums.TransactionStatus;
import com.decadave.ewalletapp.shared.enums.TransactionType;
import com.decadave.ewalletapp.shared.exceptions.*;
import com.decadave.ewalletapp.transaction.Transaction;
import com.decadave.ewalletapp.transaction.TransactionDto;
import com.decadave.ewalletapp.transaction.TransactionRepository;
import com.decadave.ewalletapp.wallet.Wallet;
import com.decadave.ewalletapp.wallet.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import javax.transaction.Transactional;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UserServiceImpl implements UserService
{

    private final AccountUserRepository userRepository;
    private final AccountRepository accountRepository;
    private final WalletRepository walletRepository;
    private final RoleRepository roleRepository;
    private final KycRepository kycRepository;
    private final KYCEntityRepository kycEntityRepository;
    private final TransactionRepository transactionRepository;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper mapper;
    private final MailSenderImpl mailSender;

    @Override
    public String createAccountUser(AccountUserDto userDto)
    {
        log.info("Saving new account user, {}", userDto.getEmail());
        Optional<AccountUser> user = userRepository.findByEmail(userDto.getEmail());
        Optional<Role> role = Optional.ofNullable(roleRepository.findByName("USER").orElseThrow(() -> new RoleNotFountException("Role specified not found")));
        if(user.isEmpty())
        {
            userDto.setPassword(passwordEncoder.encode(userDto.getPassword()));
            AccountUser userToSave = mapper.map(userDto, AccountUser.class);
            userToSave.setTransactionLevel(TransactionLevel.LEVEL_ONE_ALL);
            userToSave.setAccountVerified(true);
            Set<Role> roles = new HashSet<>();
            roles.add(role.get());
            userToSave.setRoles(roles);
            userRepository.save(userToSave);
            System.out.println(userToSave.getId());

            Account userAccountCreation = createAccountForUser(userToSave);

            KYC userKyc = createAKycDirectoryForUser(userToSave);

            createWalletForUser(userAccountCreation, userKyc, userToSave.getEmail());

        } else
        {
            throw new UserWithEmailAlreadyExist("Email: " + userDto.getEmail() + " already exist");
        }
        return "Account user created successfully";
    }

    private void createWalletForUser(Account userAccountCreation, KYC userKyc, String userEmail)
    {
        Wallet userWallet = Wallet.builder()
                .walletBalance(0.00)
                .walletAccountNumber(generateRandomAccountNumber())
                .accountHolderId(userAccountCreation.getAccountHolderId())
                .transactionPin("0000")
                .kycId(userKyc.getId())
                .accountHolderEmail(userEmail)
                .build();

        userAccountCreation.setWallet(userWallet);
        userWallet.setAccountId(userAccountCreation.getId());

        walletRepository.save(userWallet);
        String toSent = "A new account wallet with the following information has been created for you:"+
                "Wallet Account Name: "+userAccountCreation.getAccountName()+""+
                "Wallet Account Number: "+userWallet.getWalletAccountNumber()+""+
                "Wallet Transaction pin: "+userWallet.getTransactionPin()+""+
                "Wallet Account Email: "+userWallet.getAccountHolderEmail()+""+
                "Wallet Account Opening Balance: "+userWallet.getWalletBalance()+""+
                "Date Created: "+userWallet.getCreatedAt()+""+
                "Thanks For Patronizing us";
        mailSender.sendMail(
                "davawallet@davacom.com",
                userWallet.getAccountHolderEmail(),
                "Account creation",
                toSent
        );
    }

    private KYC createAKycDirectoryForUser(AccountUser userAccountCreation)
    {
        KYC userKyc = KYC.builder()
                .accountHolderId(userAccountCreation.getId())
                .bVN("")
                .driverLicence("")
                .passportUrl("")
                .build();
        kycRepository.save(userKyc);
        return userKyc;
    }

    private Account createAccountForUser(AccountUser userToSave)
    {
        Account userAccountCreation = Account.builder()
                .accountHolderId(userToSave.getId())
                .accountName(userToSave.getFirstName() +" "+ userToSave.getLastName())
                .build();
        accountRepository.save(userAccountCreation);
        return userAccountCreation;
    }

    @Override
    public String saveRole(RoleDto roleDto)
    {
        log.info("Saving new user role, {}", roleDto.getName());
        Optional<Role> role = roleRepository.findByName(roleDto.getName().toUpperCase());
        if(role.isEmpty())
        {
            RoleDto roleDtoCapitalized = new RoleDto();
            roleDtoCapitalized.setName(roleDto.getName().toUpperCase());
            Role roleToSave = mapper.map(roleDtoCapitalized, Role.class);
            roleRepository.save(roleToSave);
        } else
        {
            throw new UserWithEmailAlreadyExist("Role already exist already exist");
        }
        return "Role saved successfully";

    }

    @Override
    @CachePut(cacheNames = "add-role")
    public String addROleTOUser(AddRoleToUserDto addRoleToUserDto)
    {
        log.info("Adding new role to a user");
        AccountUser user = userRepository.findByEmail(addRoleToUserDto.getUserEmail())
                .orElseThrow(() ->new UserWithEmailNotFound("User with email "+ addRoleToUserDto.getUserEmail()+ "was not found"));
        addRoleToUserDto.getRoleName().toUpperCase();
        Role role = roleRepository.findByName(addRoleToUserDto.getRoleName())
                .orElseThrow(() ->new RoleNotFoundException("Role was not found"));
        if(user.getRoles().contains(role))
        {
            throw new RoleNotFoundException("User already has this role");
        }  else
        {
            Set<Role> roles;
            if(user.getRoles() == null || user.getRoles().isEmpty())
            {
                roles = new HashSet<>();
            } else
            {
                roles = user.getRoles();
            }
            roles.add(role);
            user.setRoles(roles);
        }
        return "Role added successfully";
    }

    @Override
    @Cacheable(cacheNames = "user")
    public AccountUserDto getUser(String userEmail)
    {
        log.info("Fetching a particular user by email, {}", userEmail);
        AccountUser user = userRepository.findByEmail(userEmail)
                .orElseThrow(() ->new UserWithEmailNotFound("User with email "+userEmail+ "was not found"));
        return mapper.map(user, AccountUserDto.class);
    }

    @Override
    @Cacheable(cacheNames = "users")
    public List<AccountUser> AccountUsers()
    {
        log.info("Getting all registered account users");
        return userRepository.findAll();
    }

    @Override
    @CachePut(cacheNames = "wallet-top-up")
    public Transaction topUpWalletBalance (TopUpDto topUpDto)
    {
        Transaction userTransaction;
    UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication()
            .getPrincipal();
    String email = userDetails.getUsername();
    AccountUser user = userRepository.findByEmail(email).orElseThrow(() ->
            new UserWithEmailNotFound("USER_NOT_FOUND"));
        log.info("Topping up my wallet account");
        Optional<Wallet> walletToTopUp = Optional.ofNullable(walletRepository.findByAccountHolderId(user.getId())
                .orElseThrow(() -> new UserWithEmailNotFound("Wallet was not found")));

        TopUpDto transactionDone = new TopUpDto();
        if(Objects.equals(walletToTopUp.get().getTransactionPin(), "0000"))
        {
            throw new WrongTransactionPin("For your security, you need to change your transaction pin before any transaction.");
        }
        if(walletToTopUp.get().getTransactionPin().equals(topUpDto.getTransactionPin()))
        {
            TransactionLevel userLevel = user.getTransactionLevel();
            if (userLevel.equals(TransactionLevel.LEVEL_ONE_ALL))
            {
                if(topUpDto.getAmount()>=50 && topUpDto.getAmount()<=50000)
                {
                    createTransactionSummary(walletToTopUp, topUpDto, transactionDone);
                    SendTransactionEmail(topUpDto, user);
                } else
                {
                    throw new AmountTooSmallOrBiggerException("Amount to topUp cannot be less than N50 and Not more than 50000 ");
                }
            } else if (userLevel.equals(TransactionLevel.LEVEL_TWO_SILVER))
            {
                if(topUpDto.getAmount()>=50 && topUpDto.getAmount()<=1000000)
                {
                    createTransactionSummary(walletToTopUp, topUpDto, transactionDone);
                    SendTransactionEmail(topUpDto, user);
                } else
                {
                    throw new AmountTooSmallOrBiggerException("Amount to topUp cannot be less than N50 and Not more than 1000000 ");
                }
            } else if (userLevel.equals(TransactionLevel.LEVEL_THREE_GOLD))
            {
                if(topUpDto.getAmount()>=50 && topUpDto.getAmount()<=1000000000)
                {
                    createTransactionSummary(walletToTopUp, topUpDto, transactionDone);
                    SendTransactionEmail(topUpDto, user);
                }
            } else
            {
                throw new AmountTooSmallOrBiggerException("Amount to topUp cannot be less than N50 and Not more than 1000000000 ");
            }
            String date = setDateAndTimeForTransaction();
            userTransaction = generateTransactionSummary(topUpDto, walletToTopUp.get(), date);

        } else
        {
            throw new WrongTransactionPin("You have entered a wrong transaction pin! ");
        }

        return userTransaction;
    }

    private void SendTransactionEmail(TopUpDto topUpDto, AccountUser user) {
        mailSender.sendMail(
                "davawallet@davacom.com",
                user.getEmail(),
                "Wallet Account Topup",
                "Your Account Has been credited up with: " + topUpDto.getAmount()+
                        "On: " + new Date()+
                        "Summary: "+ topUpDto.getTransactionSummary()
        );
    }


    @Override
    @CachePut(cacheNames = "withdrawal")
    public Transaction withdrawal(WithdrawalDto withdrawalDto)
    {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication()
                .getPrincipal();
        String email = userDetails.getUsername();
        AccountUser user = userRepository.findByEmail(email).orElseThrow(() ->
                new UserWithEmailNotFound("USER_NOT_FOUND"));
        log.info("Withdrawing of currency");
        Optional<Wallet> sendersWallet = Optional.ofNullable(walletRepository
                .findByAccountHolderId(user.getId())
                .orElseThrow(() -> new UserWithEmailNotFound("Wallet was not found")));

        Transaction transactionToBeDone = new Transaction();
        Transaction transactionSummary = new Transaction();
        Double amountToWithdraw = withdrawalDto.getAmount();
        Double amountHaving = sendersWallet.get().getWalletBalance();
        if(Objects.equals(withdrawalDto.getTransactionPin(), "0000"))
        {
            throw new AmountTooSmallOrBiggerException("For your security, you need to change your transaction pin before any transaction.");
        }
        if(sendersWallet.get().getTransactionPin().equals(withdrawalDto.getTransactionPin()))
        {
        if(amountHaving < 999 || amountToWithdraw > amountHaving)
        {
            throw new InsufficientFundsException("Amount initiated is below your wallet balance");
        }
            TransactionLevel userLevel = user.getTransactionLevel();
            if (userLevel.equals(TransactionLevel.LEVEL_ONE_ALL))
            {
                if(withdrawalDto.getAmount()>=1000 && withdrawalDto.getAmount()<=20000)
                {
                    transactionToBeDone = getWithdrawalSummary(sendersWallet, transactionSummary, withdrawalDto);
                    sendWithdrawalEmail(withdrawalDto, user);
                } else
                {
                    throw new AmountTooSmallOrBiggerException("Amount to Withdraw cannot be less than N1000 and Not more than 20000");
                }
            } else if (userLevel.equals(TransactionLevel.LEVEL_TWO_SILVER))
            {
                if(withdrawalDto.getAmount()>=1000 && withdrawalDto.getAmount()<=700000)
                {
                    transactionToBeDone = getWithdrawalSummary(sendersWallet, transactionSummary, withdrawalDto);
                    sendWithdrawalEmail(withdrawalDto, user);
                } else
                {
                    throw new AmountTooSmallOrBiggerException("Amount to Withdraw cannot be less than N1000 and Not more than 700000 ");
                }
            } else if (userLevel.equals(TransactionLevel.LEVEL_THREE_GOLD))
            {
                if(withdrawalDto.getAmount()>=50 && withdrawalDto.getAmount()<=20000000)
                {
                    transactionToBeDone = getWithdrawalSummary(sendersWallet, transactionSummary, withdrawalDto);
                    sendWithdrawalEmail(withdrawalDto, user);
                }
            } else {
                throw new AmountTooSmallOrBiggerException("Amount to Withdraw cannot be less than N1000 and Not more than 20000000");
            }
    } else
        {
            throw new WrongTransactionPin("You have entered a wrong transaction pin! ");

        }
        return transactionToBeDone;
    }

    private void sendWithdrawalEmail(WithdrawalDto withdrawalDto, AccountUser user) {
        mailSender.sendMail(
                "davawallet@davacom.com",
                user.getEmail(),
                "Wallet Account Topup",
                "Your Account Has been credited up with: " + withdrawalDto.getAmount()+
                        "On: " + new Date()+
                        "Summary: "+ withdrawalDto.getTransactionSummary()
        );
    }

    @Override
    @CachePut(cacheNames = "transfer-money")
    public Transaction transferMoney(TransferDto transferDto)
    {
        log.info("Transfer of currency");
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication()
                .getPrincipal();
        String email = userDetails.getUsername();
        AccountUser sender = userRepository.findByEmail(email).orElseThrow(() ->
                new UserWithEmailNotFound("USER_NOT_FOUND"));
        Optional<Wallet> sendersWallet = Optional.ofNullable(walletRepository
                .findByAccountHolderId(sender.getId())
                .orElseThrow(() -> new UserWithEmailNotFound("Wallet was not found")));

        Optional<Wallet> receiversWallet = Optional.ofNullable(walletRepository
                .findByAccountHolderEmailOrWalletAccountNumber(transferDto.getReceiversEmail(), transferDto.getReceiversAccountNumber())
                .orElseThrow(() -> new UserWithEmailNotFound("Wallet was not found")));

        Double amountToTransfer = transferDto.getAmount();
        TransactionDto transactionDto = new TransactionDto();
        Double amountHaving = sendersWallet.get().getWalletBalance();
        if(Objects.equals(transferDto.getTransactionPin(), "0000"))
        {
            throw new AmountTooSmallOrBiggerException("For your security, you need to change your transaction pin before any transaction.");
        }
        if(sendersWallet.get().getTransactionPin().equals(transferDto.getTransactionPin()))
        {
            if(amountHaving < 999 || amountToTransfer > amountHaving)
            {
                throw new InsufficientFundsException("Amount initiated is below your wallet balance");
            }
            TransactionLevel userLevel = sender.getTransactionLevel();
            if (userLevel.equals(TransactionLevel.LEVEL_ONE_ALL))
            {
                if(transferDto.getAmount()>=1000 && transferDto.getAmount()<=20000)
                {
                    transferMethod(transferDto, sendersWallet, receiversWallet);

                    transactionDto = getTransactionDto(transferDto, transactionDto, sendersWallet);
                } else
                {
                    throw new AmountTooSmallOrBiggerException("Amount to Withdraw cannot be less than N1000 and Not more than 20000");
                }
            } else if (userLevel.equals(TransactionLevel.LEVEL_TWO_SILVER))
            {
                if(transferDto.getAmount()>=1000 && transferDto.getAmount()<=700000)
                {
                    transferMethod(transferDto, sendersWallet, receiversWallet);

                    transactionDto = getTransactionDto(transferDto, transactionDto, sendersWallet);
                } else
                {
                    throw new AmountTooSmallOrBiggerException("Amount to Withdraw cannot be less than N1000 and Not more than 700000 ");
                }
            } else if (userLevel.equals(TransactionLevel.LEVEL_THREE_GOLD))
            {
                if(transferDto.getAmount()>=50 && transferDto.getAmount()<=20000000)
                {
                    transferMethod(transferDto, sendersWallet, receiversWallet);

                    transactionDto = getTransactionDto(transferDto, transactionDto, sendersWallet);
                }
            } else {
                throw new AmountTooSmallOrBiggerException("Amount to Withdraw cannot be less than N1000 and Not more than 20000000");
            }
        } else
        {
            throw new WrongTransactionPin("You have entered a wrong transaction pin! ");

        }
        Transaction transactionSum = mapper.map(transactionDto, Transaction.class);
        transactionSum.setUserId(sender.getId());
        transactionSum.setWalletId(sendersWallet.get().getId());
        transactionSum.setTransactionAmount(transferDto.getAmount());
        transactionRepository.save(transactionSum);


        return transactionSum;
    }

    private TransactionDto getTransactionDto(TransferDto transferDto, TransactionDto transactionDto, Optional<Wallet> sendersWallet)
    {
        String date = setDateAndTimeForTransaction();
        StringBuilder receiverId = new StringBuilder();
        if(transferDto.getReceiversAccountNumber()!=null)
        {
            receiverId.append(transferDto.getReceiversAccountNumber());
        } else
        {
            receiverId.append(transferDto.getReceiversEmail());
        }
        transactionDto = TransactionDto.builder()
                .senderOrTransfer(sendersWallet.get().getAccountHolderEmail())
                .receiver(receiverId.toString())
                .transactionType(TransactionType.TRANSFER)
                .transactionStatus(TransactionStatus.SUCCESSFUL)
                .dateAndTimeForTransaction(date)
                .summary(transferDto.getTransactionSummary())
                .build();
        return transactionDto;
    }

    private void transferMethod(TransferDto transferDto, Optional<Wallet> sendersWallet, Optional<Wallet> receiversWallet)
    {
        Double amountToTransferTo = transferDto.getAmount();
        Double senderBalance = sendersWallet.get().getWalletBalance();
        Double receiversBalance = receiversWallet.get().getWalletBalance();
        Double newSenderBalance = senderBalance-amountToTransferTo;
        Double newReceiversBalance = receiversBalance+amountToTransferTo;

        sendersWallet.get().setWalletBalance(newSenderBalance);
        walletRepository.save(sendersWallet.get());
        receiversWallet.get().setWalletBalance(newReceiversBalance);
        walletRepository.save(receiversWallet.get());
    }

    private Transaction getWithdrawalSummary(Optional<Wallet> sendersWallet, Transaction transaction, WithdrawalDto transactionDone)
    {
        deductionMethod(sendersWallet, transactionDone.getAmount());
        String date = setDateAndTimeForTransaction();

        Transaction userWithdrawalTransaction = Transaction.builder()
                .transactionAmount(transactionDone.getAmount())
                .transactionStatus(TransactionStatus.SUCCESSFUL)
                .transactionType(TransactionType.WITHDRAWALS)
                .userId(sendersWallet.get().getAccountHolderId())
                .walletId(sendersWallet.get().getId())
                .dateAndTimeForTransaction(date)
                .Summary(transactionDone.getTransactionSummary())
                .build();
        transactionRepository.save(userWithdrawalTransaction);

        transactionDone = mapper.map(userWithdrawalTransaction, WithdrawalDto.class);
        return userWithdrawalTransaction;
    }

    private void createTransactionSummary(Optional<Wallet> walletToTopUp, TopUpDto topUpDto, TopUpDto transactionDone)
    {
        topUpMethod(walletToTopUp, topUpDto.getAmount());
        String date = setDateAndTimeForTransaction();
        Transaction userTransaction = generateTransactionSummary(topUpDto, walletToTopUp.get(), date);
        transactionDone = mapper.map(userTransaction, TopUpDto.class);
    }

    @Override
    public String changeTransactionPin(ChangeTransactionPinDto changeTransactionPinDto)
    {
        log.info("Changing my wallet transaction pin");
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication()
                .getPrincipal();
        String email = userDetails.getUsername();
        AccountUser user = userRepository.findByEmail(email)
                .orElseThrow(() ->new UserWithEmailNotFound("Use was not found"));

        Wallet walletToChangeTransactionPin = (walletRepository.findByAccountHolderId(user.getId())
                .orElseThrow(() -> new UserWithEmailNotFound("Wallet was not found")));

        walletToChangeTransactionPin.setTransactionPin(changeTransactionPinDto.getNewPin());
        walletRepository.save(walletToChangeTransactionPin);

        return "Pin changed Successfully";
    }

    @Override
    @Transactional
    public String doKycDocumentation(KycDto kycDto) {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication()
                .getPrincipal();
        String email = userDetails.getUsername();
        AccountUser user = userRepository.findByEmail(email).orElseThrow(() ->
                new UserWithEmailNotFound("User with email not found"));
        {
            KYC userKyc = kycRepository.findByAccountHolderId(user.getId());

            if (Objects.equals(userKyc.getBVN(), ""))
            {
                userKyc.setBVN(kycDto.getBVN());
            }

            if(Objects.equals(userKyc.getDriverLicence(), ""))
            {
                userKyc.setDriverLicence(kycDto.getDriverLicence());
            }

            if(Objects.equals(userKyc.getPassportUrl(), ""))
            {
                userKyc.setPassportUrl(kycDto.getPassportUrl());
            }

                kycRepository.save(userKyc);
            }
            return "KYC completed, wait for verification, validation and approval";
    }

    @Override
    public String kycApprovalByAdmin(KycAdminVerificationDto kycAdminVerificationDto)
    {
        AccountUser userToUpgradeAndVerify = userRepository.findById(kycAdminVerificationDto.getAccountHolderId()).orElseThrow(() ->
                new UserWithEmailNotFound("User with email not found"));
        KYC userKyc = kycRepository.findByAccountHolderId(userToUpgradeAndVerify.getId());
        int levelApproved = kycAdminVerificationDto.getLevelApproved();
        StringBuilder levelUpdated = new StringBuilder();
        if(levelApproved==2)
        {
            userToUpgradeAndVerify.setTransactionLevel(TransactionLevel.LEVEL_TWO_SILVER);
            userKyc.setApprovedLevel(TransactionLevel.LEVEL_TWO_SILVER.name());
            userRepository.save(userToUpgradeAndVerify);
            kycRepository.save(userKyc);
            levelUpdated.append(userToUpgradeAndVerify.getTransactionLevel().name());
        }
        else if(levelApproved==3)
        {
            userToUpgradeAndVerify.setTransactionLevel(TransactionLevel.LEVEL_THREE_GOLD);
            userKyc.setApprovedLevel(TransactionLevel.LEVEL_THREE_GOLD.name());
            userRepository.save(userToUpgradeAndVerify);
            kycRepository.save(userKyc);
            levelUpdated.append(userToUpgradeAndVerify.getTransactionLevel().name());
        }
        return levelUpdated.toString();
    }

    @Override
    public AccountDetailsForUserDto getMyAccountDetails() {
        log.info("Fetching my account details");
        AccountDetailsForUserDto myDetails = new AccountDetailsForUserDto();
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication()
                .getPrincipal();
        String email = userDetails.getUsername();
        AccountUser user = userRepository.findByEmail(email).orElseThrow(() -> new UserWithEmailNotFound("USER_NOT_FOUND"));
        Optional<Wallet> userWallet = Optional.ofNullable(walletRepository.findByAccountHolderId(user.getId())
                .orElseThrow(() -> new UserWithEmailNotFound("Wallet was not found")));
        KYC userKyc = kycRepository.findByAccountHolderId(user.getId());

        if (userKyc.getId() == null) {
            throw new KycNotFoundException("User KYC was not found pin! ");
        } else {

             myDetails = AccountDetailsForUserDto.builder()
                    .AccountHolderFullName(user.getFirstName()+" " +user.getLastName())
                    .email(user.getEmail())
                    .walletAccountNumber(userWallet.get().getWalletAccountNumber())
                    .walletBalance(""+userWallet.get().getWalletBalance())
                    .bVN(userKyc.getBVN())
                    .driverKYCLicence(userKyc.getDriverLicence())
                    .passportKYCUrl(userKyc.getPassportUrl())
                    .approvedLevel(user.getTransactionLevel().name())
                    .build();
        }
        return myDetails;
    }

    private Transaction generateTransactionSummary(TopUpDto topUpDto, Wallet walletToTopUp, String date) {
        Transaction userTransaction = Transaction.builder()
                .transactionAmount(topUpDto.getAmount())
                .transactionStatus(TransactionStatus.SUCCESSFUL)
                .transactionType(TransactionType.DEPOSIT)
                .walletId(walletToTopUp.getId())
                .dateAndTimeForTransaction(date)
                .userId(walletToTopUp.getAccountHolderId())
                .Summary(topUpDto.getTransactionSummary())
                .build();
        transactionRepository.save(userTransaction);
        return userTransaction;
    }

    private String setDateAndTimeForTransaction() {
        String pattern = "dd MMMMM yyyy HH:mm:ss.zzz";
        SimpleDateFormat simpleDateFormat =new SimpleDateFormat(pattern, new Locale("en", "UK"));
        String date = simpleDateFormat.format(new Date());
        return date;
    }

    private String generateRandomAccountNumber ()
    {
        StringBuilder accNum = new StringBuilder();
        Random rand = new Random();
        for (int i = 1; i <= 5; i++) {
            int resRandom = rand.nextInt((99 - 10) + 1) + 10;
            accNum.append(resRandom);
        }
        return accNum.toString();
    }

    private  void topUpMethod (Optional<Wallet> wallet, Double amount)
    {
        Double amountToppedUp = wallet.get().getWalletBalance()+amount;
                wallet.get().setWalletBalance(amountToppedUp);
                walletRepository.save(wallet.get());
    }

    private  void deductionMethod (Optional<Wallet> wallet, Double amount)
    {
        Double amountTopDeduct = wallet.get().getWalletBalance()-amount;
        wallet.get().setWalletBalance(amountTopDeduct);
        walletRepository.save(wallet.get());
    }
}
