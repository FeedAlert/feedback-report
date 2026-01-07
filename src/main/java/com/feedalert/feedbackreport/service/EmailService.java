package com.feedalert.feedbackreport.service;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Attachments;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.Personalization;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;

@Slf4j
@Service
public class EmailService {

    @Value("${sendgrid.api-key}")
    private String sendGridApiKey;

    @Value("${sendgrid.sender-email}")
    private String senderEmail;

    public void sendEmail(List<String> recipients, byte[] pdf) throws IOException {
        SendGrid sg = new SendGrid(sendGridApiKey);
        Mail mail = new Mail();
        mail.setFrom(new Email(senderEmail, "FeedAlert"));
        mail.setSubject("FeedAlert - Relatório Semanal de Satisfação");
        mail.addContent(new Content("text/plain", "Olá! O relatório semanal dos feedbacks dos alunos está pronto e em anexo."));

        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);

        Attachments attach = new Attachments();
        attach.setContent(Base64.getEncoder().encodeToString(pdf));
        attach.setType("application/pdf");
        attach.setFilename("relatorio-semanal-" + today + ".pdf");
        attach.setDisposition("attachment");
        mail.addAttachments(attach);

        Personalization p = new Personalization();
        recipients.forEach(email -> p.addTo(new Email(email)));
        mail.addPersonalization(p);

        Request req = new Request();
        req.setMethod(Method.POST);
        req.setEndpoint("mail/send");
        req.setBody(mail.build());
        sg.api(req);
    }

}
