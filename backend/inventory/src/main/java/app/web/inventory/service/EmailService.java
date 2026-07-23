package app.web.inventory.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Async
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendOtp(String to, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Your InventoryPro OTP Code");
        message.setText("Your OTP code is: " + otp + "\n\nThis code will expire in 10 minutes.");
        mailSender.send(message);
    }

    public void sendSpaceDeletionNoticeToMember(String memberEmail, String spaceName, String ownerName) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(memberEmail);
        message.setSubject("Space \"" + spaceName + "\" Has Been Deleted");
        message.setText(
                "Hi,\n\n" +
                        "The space \"" + spaceName + "\", owned by " + ownerName + ", has been deleted.\n\n" +
                        "You have been automatically removed from this space and all associated products are no longer accessible.\n\n"
                        +
                        "If you have any questions, please contact the space owner.");
        mailSender.send(message);
    }
}
