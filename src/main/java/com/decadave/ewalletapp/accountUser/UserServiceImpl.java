package com.decadave.ewalletapp.accountUser;

import com.decadave.ewalletapp.KYC.KYC;
import com.decadave.ewalletapp.KYC.KycRepository;
import com.decadave.ewalletapp.account.Account;
import com.decadave.ewalletapp.account.AccountRepository;
import com.decadave.ewalletapp.role.Role;
import com.decadave.ewalletapp.role.RoleDto;
import com.decadave.ewalletapp.role.RoleRepository;
import com.decadave.ewalletapp.shared.dto.ChangeTransactionPinDto;
import com.decadave.ewalletapp.shared.dto.TopUpDto;
import com.decadave.ewalletapp.shared.dto.WithdrawalOrTransferDto;
import com.decadave.ewalletapp.shared.enums.TransactionLevel;
import com.decadave.ewalletapp.shared.enums.TransactionStatus;
import com.decadave.ewalletapp.shared.enums.TransactionType;
import com.decadave.ewalletapp.shared.exceptions.*;
import com.decadave.ewalletapp.transaction.Transaction;
import com.decadave.ewalletapp.transaction.TransactionRepository;
import com.decadave.ewalletapp.wallet.Wallet;
import com.decadave.ewalletapp.wallet.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.Cacheable;
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
    private final TransactionRepository transactionRepository;
    private  final ModelMapper mapper;

    @Override
    public String createAccountUser(AccountUserDto userDto)
    {
        log.info("Saving new account user, {}", userDto.getEmail());
        Optional<AccountUser> user = userRepository.findByEmail(userDto.getEmail());
        if(user.isEmpty())
        {
            AccountUser userToSave = mapper.map(userDto, AccountUser.class);
//            userToSave.setTransactionLevel(TransactionLevel.LEVEL_ONE_ALL);
            userToSave.setTransactionLevel(TransactionLevel.LEVEL_TWO_SILVER);
            userRepository.save(userToSave);
            System.out.println(userToSave.getId());

            Account userAccountCreation = createAccountForUser(userToSave);

            KYC userKyc = createAKycDirectoryForUser(userAccountCreation);

            createWalletForUser(userAccountCreation, userKyc, userToSave.getEmail());

        } else
        {
            throw new UserWithEmailAlreadyExist("Email: " + userDto.getEmail() + " already exist");
        }
        return "Account user created successfully";
    }

    private void createWalletForUser(Account userAccountCreation, KYC userKyc, String userEmail) {
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
    }

    private KYC createAKycDirectoryForUser(Account userAccountCreation) {
        KYC userKyc = KYC.builder()
                .accountHolderId(userAccountCreation.getId())
                .build();
        kycRepository.save(userKyc);
        return userKyc;
    }

    private Account createAccountForUser(AccountUser userToSave) {
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
        log.info("Saving new user role", roleDto.getName());
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
    public void addROleTOUser(AddRoleToUserDto addRoleToUserDto)
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
            user.getRoles().add(role);
        }
    }

    @Override
    @Cacheable("user")
    public AccountUserDto getUser(String userEmail)
    {
        log.info("Fetching a particular user by email, {}", userEmail);
        AccountUser user = userRepository.findByEmail(userEmail)
                .orElseThrow(() ->new UserWithEmailNotFound("User with email "+userEmail+ "was not found"));
        AccountUserDto userFound = mapper.map(user, AccountUserDto.class);
        return userFound;
    }

    @Override
    @Cacheable("users")
    public List<AccountUser> AccountUsers()
    {
        log.info("Getting all registered account users");
        return userRepository.findAll();
    }

    @Override
    public TopUpDto topUpWalletBalance (TopUpDto topUpDto)
    {
        log.info("Topping up my wallet account");
        Optional<Wallet> walletToTopUp = Optional.ofNullable(walletRepository.findByAccountHolderId(topUpDto.getAccountHolderId())
                .orElseThrow(() -> new UserWithEmailNotFound("Wallet was not found")));
        Optional<AccountUser> user = userRepository.findById(walletToTopUp.get().getAccountHolderId());

        TopUpDto transactionDone = new TopUpDto();
        if(Objects.equals(walletToTopUp.get().getTransactionPin(), "0000"))
        {
            throw new AmountTooSmallOrBiggerException("For your security, you need to change your transaction pin before any transaction.");
        }
        if(walletToTopUp.get().getTransactionPin().equals(topUpDto.getTransactionPin()))
        {
            TransactionLevel userLevel = user.get().getTransactionLevel();
            if (userLevel.equals(TransactionLevel.LEVEL_ONE_ALL))
            {
                if(topUpDto.getAmount()>=50 && topUpDto.getAmount()<=50000)
                {
                    createTransactionSummary(walletToTopUp, topUpDto, transactionDone);
                } else
                {
                    throw new AmountTooSmallOrBiggerException("Amount to topUp cannot be less than N50 and Not more than 50000 ");
                }
            } else if (userLevel.equals(TransactionLevel.LEVEL_TWO_SILVER))
            {
                if(topUpDto.getAmount()>=50 && topUpDto.getAmount()<=1000000)
                {
                    createTransactionSummary(walletToTopUp, topUpDto, transactionDone);
                } else
                {
                    throw new AmountTooSmallOrBiggerException("Amount to topUp cannot be less than N50 and Not more than 1000000 ");
                }
            } else if (userLevel.equals(TransactionLevel.LEVEL_THREE_GOLD))
            {
                if(topUpDto.getAmount()>=50 && topUpDto.getAmount()<=1000000000);
                createTransactionSummary(walletToTopUp, topUpDto, transactionDone);
            } else
            {
                throw new AmountTooSmallOrBiggerException("Amount to topUp cannot be less than N50 and Not more than 1000000000 ");
            }
            String date = setDateAndTimeForTransaction();
            Transaction userTransaction = generateTransactionSummary(topUpDto, walletToTopUp.get(), date);
            transactionDone = mapper.map(userTransaction, TopUpDto.class);
        } else
        {
            throw new WrongTransactionPin("You have entered a wrong transaction pin! ");
        }

        return transactionDone;
    }


    @Override
    public Transaction withdrawal(WithdrawalOrTransferDto withdrawalOrTransferDto) {
        log.info("Withdrawing of currency");
        Optional<Wallet> sendersWallet = Optional.ofNullable(walletRepository
                .findByAccountHolderId(withdrawalOrTransferDto.getAccountHolderId())
                .orElseThrow(() -> new UserWithEmailNotFound("Wallet was not found")));

//        Wallet receiverWallet = walletRepository.findByAccountHolderEmailOrWalletAccountNumber(withdrawalOrTransferDto.getReceiversEmailOrAccountNumber(), withdrawalOrTransferDto.getReceiversEmailOrAccountNumber());
//                .orElseThrow(() -> new UserWithEmailNotFound("Receivers Wallet was not found")));

        Optional<AccountUser> user = Optional.ofNullable(userRepository.findById(sendersWallet.get().getAccountHolderId())
                .orElseThrow(() -> new UserWithEmailNotFound("Wallet was not found")));

        Transaction transactionToBeDone = new Transaction();
        Transaction transactionSummary = new Transaction();
        Double amountToWithdraw = withdrawalOrTransferDto.getAmount();
        Double amountHaving = sendersWallet.get().getWalletBalance();
        if(Objects.equals(withdrawalOrTransferDto.getTransactionPin(), "0000"))
        {
            throw new AmountTooSmallOrBiggerException("For your security, you need to change your transaction pin before any transaction.");
        }
        if(sendersWallet.get().getTransactionPin().equals(withdrawalOrTransferDto.getTransactionPin()))
        {
        if(amountHaving < 999 || amountToWithdraw > amountHaving)
        {
            throw new InsufficientFundsException("Amount initiated is below your wallet balance");
        }
            TransactionLevel userLevel = user.get().getTransactionLevel();
            if (userLevel.equals(TransactionLevel.LEVEL_ONE_ALL))
            {
                if(withdrawalOrTransferDto.getAmount()>=1000 && withdrawalOrTransferDto.getAmount()<=20000)
                {
                    transactionToBeDone = getWithdrawalOrTransferSummary(sendersWallet, transactionSummary, withdrawalOrTransferDto);
                } else
                {
                    throw new AmountTooSmallOrBiggerException("Amount to Withdraw cannot be less than N1000 and Not more than 20000");
                }
            } else if (userLevel.equals(TransactionLevel.LEVEL_TWO_SILVER))
            {
                if(withdrawalOrTransferDto.getAmount()>=1000 && withdrawalOrTransferDto.getAmount()<=700000)
                {
                    transactionToBeDone = getWithdrawalOrTransferSummary(sendersWallet, transactionSummary, withdrawalOrTransferDto);
                } else
                {
                    throw new AmountTooSmallOrBiggerException("Amount to Withdraw cannot be less than N1000 and Not more than 700000 ");
                }
            } else if (userLevel.equals(TransactionLevel.LEVEL_THREE_GOLD))
            {
                if(withdrawalOrTransferDto.getAmount()>=50 && withdrawalOrTransferDto.getAmount()<=20000000)
                {
                    transactionToBeDone = getWithdrawalOrTransferSummary(sendersWallet, transactionSummary, withdrawalOrTransferDto);
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

    private Transaction getWithdrawalOrTransferSummary (Optional<Wallet> sendersWallet, Transaction transaction, WithdrawalOrTransferDto transactionDone) {
        deductionMethod(sendersWallet, transactionDone.getAmount());
        String date = setDateAndTimeForTransaction();

        Transaction userWithdrawalTransaction = Transaction.builder()
                .transactionAmount(transactionDone.getAmount())
                .transactionStatus(TransactionStatus.SUCCESSFUL)
                .transactionType(TransactionType.WITHDRAWALS)
                .userId(transactionDone.getAccountHolderId())
                .walletId(sendersWallet.get().getId())
                .dateAndTimeForTransaction(date)
                .Summary(transactionDone.getTransactionSummary())
                .build();
        transactionRepository.save(userWithdrawalTransaction);

        transactionDone = mapper.map(userWithdrawalTransaction, WithdrawalOrTransferDto.class);
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
        Wallet walletToChangeTransactionPin = (walletRepository.findByAccountHolderId(changeTransactionPinDto.getAccountHolderId())
                .orElseThrow(() -> new UserWithEmailNotFound("Wallet was not found")));
        AccountUser user = userRepository.findById(changeTransactionPinDto.getAccountHolderId())
                .orElseThrow(() ->new UserWithEmailNotFound("Use was not found"));

        walletToChangeTransactionPin.setTransactionPin(changeTransactionPinDto.getNewPin());
        walletRepository.save(walletToChangeTransactionPin);

        return "Pin changed Successfully";
    }




    private Transaction generateTransactionSummary(TopUpDto topUpDto, Wallet walletToTopUp, String date) {
        Transaction userTransaction = Transaction.builder()
                .transactionAmount(topUpDto.getAmount())
                .transactionStatus(TransactionStatus.SUCCESSFUL)
                .transactionType(TransactionType.DEPOSIT)
                .userId(topUpDto.getAccountHolderId())
                .walletId(walletToTopUp.getId())
                .dateAndTimeForTransaction(date)
                .Summary(topUpDto.getTransactionSummary())
                .build();
        transactionRepository.save(userTransaction);
        return userTransaction;
    }

    private String setDateAndTimeForTransaction() {
        String pattern = "dd MMMMM yyyy HH:mm:ss.zzz";
        SimpleDateFormat simpleDateFormat =new SimpleDateFormat(pattern, new Locale("fr", "FR"));
        String date = simpleDateFormat.format(new Date());
        return date;
    }

    private String generateRandomAccountNumber () {
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
