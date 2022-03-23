package com.decadave.ewalletapp.shared.configuration.mail_sender;

import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@AllArgsConstructor
public class MailSenderImpl {
    private final JavaMailSenderImpl javaMailSender;

    public void sendMail (String from, String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setSentDate(new Date());
        message.setText(text);
        javaMailSender.send(message);
    }
}
