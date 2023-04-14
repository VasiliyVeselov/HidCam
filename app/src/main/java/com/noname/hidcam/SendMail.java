package com.noname.hidcam;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

public class SendMail {

    //private static final String SMTP_SERVER = "smtp.gmail.com";
    private static final String USERNAME = "veselov1503@yandex.ru";
    private static final String PASSWORD = "smarT_1988";
    private static final String EMAIL_FROM = "veselov1503@yandex.ru";
    private static final String EMAIL_TO = "veselov1503@mail.ru";
    private static final String EMAIL_SUBJECT = "!ВНИМАНИЕ! Видео, требующее просмотра от Веселова Василия !ВНИМАНИЕ!";
    private static final String EMAIL_TEXT = "Нуждается в помощи: Веселов Василий Владимирович. 15.03.1981 г.р. Зарегистрирован: г. Тверь, ул. Можайского, д 71, кв 153. тел +7(903)808-69-20";


    public void sendEmailWithAttachment(File videoFile) throws AddressException, MessagingException, IOException {
        Properties props = new Properties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.host", "smtp.yandex.ru");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.port", "465");

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(USERNAME, PASSWORD);
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(EMAIL_FROM));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(EMAIL_TO));
        message.setSubject(EMAIL_SUBJECT);
        message.setText(EMAIL_TEXT);

        MimeBodyPart messageBodyPart = new MimeBodyPart();
        messageBodyPart.setText("Please find attached the video file.");

        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(messageBodyPart);

        messageBodyPart = new MimeBodyPart();
        DataSource source = new FileDataSource(videoFile);
        messageBodyPart.setDataHandler(new DataHandler(source));
        messageBodyPart.setFileName(videoFile.getName());
        multipart.addBodyPart(messageBodyPart);

        message.setContent(multipart);


        Transport.send(message);
        Log.e("myLog", " Transport.send(message) " );

    }
}
