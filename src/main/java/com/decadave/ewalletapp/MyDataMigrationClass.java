package com.decadave.ewalletapp;


import com.decadave.ewalletapp.KYC.KYC;
import com.decadave.ewalletapp.KYC.KYCEntity;
import com.decadave.ewalletapp.KYC.KYCEntityRepository;
import com.decadave.ewalletapp.KYC.KycDto;
import com.decadave.ewalletapp.accountUser.AccountUser;
import com.decadave.ewalletapp.accountUser.AccountUserRepository;
import com.decadave.ewalletapp.role.Role;
import com.decadave.ewalletapp.role.RoleRepository;
import com.decadave.ewalletapp.shared.enums.Gender;
import com.decadave.ewalletapp.shared.enums.TransactionLevel;
import com.decadave.ewalletapp.shared.exceptions.UserWithEmailNotFound;
import com.decadave.ewalletapp.shared.exceptions.kycUpdatedAlreadyException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.transaction.Transactional;
import java.util.*;

@Component
@RequiredArgsConstructor
public class MyDataMigrationClass
{
    private final AccountUserRepository userRepository;
    private final RoleRepository roleRepository;
    private final KYCEntityRepository kycEntityRepository;
    private final PasswordEncoder passwordEncoder;
        @PostConstruct
        private  void  userCreator()
        {

            Role role1 = Role.builder()
                    .name("USER")
                    .build();
            Role role2 = Role.builder()
                    .name("ADMIN")
                    .build();
            List<Role> roles = new ArrayList<>();
            roles.add(role1);
            roles.add(role2);
            roleRepository.saveAll(roles);


                    Set<Role> adminROle = new HashSet<>();
                    Set<Role> userRole = new HashSet<>();
                    adminROle.add(role2);
                    userRole.add(role1);
            AccountUser user = AccountUser.builder()
                    .email("davidbaba@gmail.com")
                    .firstName("David")
                    .gender(Gender.MALE)
                    .lastName("Baba")
                    .password(passwordEncoder.encode("12345"))
                    .transactionLevel(TransactionLevel.LEVEL_TWO_SILVER)
                    .isAccountVerified(true)
                    .roles(adminROle)
                    .build();

            AccountUser user2 = AccountUser.builder()
                    .email("david@gmail.com")
                    .firstName("David2")
                    .gender(Gender.FEMALE)
                    .lastName("Baba2")
                    .password(passwordEncoder.encode("12345dddsa"))
                    .transactionLevel(TransactionLevel.LEVEL_THREE_GOLD)
                    .isAccountVerified(true)
                    .roles(userRole)
                    .build();
            List<AccountUser> users = new ArrayList<>();
            users.add(user);
            users.add(user2);
            userRepository.saveAll(users);



            KYCEntity userKyc1 = KYCEntity.builder()
                    .accountHolderEmail("davidbaba2013@gmail.com")
                    .bvnNumber("00001")
                    .driverLicenceNumber("00010")
                    .passportUrl("david.png")
                    .build();

            KYCEntity userKyc2 = KYCEntity.builder()
                    .accountHolderEmail("han2020@gmail.com")
                    .bvnNumber("00002")
                    .driverLicenceNumber("00020")
                    .passportUrl("han.png")
                    .build();

            KYCEntity userKyc3 = KYCEntity.builder()
                    .accountHolderEmail("dan@gmail.com")
                    .bvnNumber("00003")
                    .driverLicenceNumber("00030")
                    .passportUrl("dan.png")
                    .build();

            KYCEntity userKyc4 = KYCEntity.builder()
                    .accountHolderEmail("Abigirl@gmail.com")
                    .bvnNumber("00004")
                    .driverLicenceNumber("00040")
                    .passportUrl("abigirl.png")
                    .build();

            List<KYCEntity> usersKyc = new ArrayList<>();
            usersKyc.add(userKyc1);
            usersKyc.add(userKyc2);
            usersKyc.add(userKyc3);
            usersKyc.add(userKyc4);

            kycEntityRepository.saveAll(usersKyc);
        }
}


//    @Override
//    @Transactional
//    public String doKycDocumentation(KycDto kycDto) {
//        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication()
//                .getPrincipal();
//        String email = userDetails.getUsername();
//        AccountUser user = userRepository.findByEmail(email).orElseThrow(() ->
//                new UserWithEmailNotFound("User with email not found"));
//        {
//            KYC userKyc = kycRepository.findByAccountHolderId(user.getId());
//
//            if (Objects.equals(userKyc.getBVN(), "") || Objects.equals(userKyc.getBVN(),"string"))
//            {
//                userKyc.setBVN(kycDto.getBVN());
//            }
//            else
//            {
//                throw new kycUpdatedAlreadyException("Your BVN has been updated already, visit the admin for any changes required");
//            }
//            if(Objects.equals(userKyc.getDriverLicence(), "") || Objects.equals(userKyc.getDriverLicence(), "string"))
//            {
//                userKyc.setDriverLicence(kycDto.getDriverLicence());
//            }
//            else
//            {
//                throw new kycUpdatedAlreadyException("Your Driver Licence has been updated already, visit the admin for any changes required");
//            }
//            if(Objects.equals(userKyc.getPassportUrl(), "") || Objects.equals(userKyc.getPassportUrl(), "string"))
//            {
//                userKyc.setPassportUrl(kycDto.getPassportUrl());
//            }
//            else
//            {
//                throw new kycUpdatedAlreadyException("Your Passport has been updated already, visit the admin for any changes required");
//            }
//            kycRepository.save(userKyc);
//        }
//        return "KYC completed, wait for verification, validation and approval";
//    }