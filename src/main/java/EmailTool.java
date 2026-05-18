import java.util.Properties;
import com.google.gson.JsonObject;
import jakarta.mail.Authenticator;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

public class EmailTool implements ToolHandler {

    private static final String FROM    = "mehdicherkane538@gmail.com";
    private static final String APP_PWD = ""; 

    @Override
    public String execute(JsonObject parameters) {
        String to      = parameters.get("to").getAsString();
        String subject = parameters.get("subject").getAsString();
        String body    = parameters.get("body").getAsString();

        try {
            sendEmail(to, subject, body);
            return "Email sent successfully to " + to;
        } catch (Exception e) {
            return "Failed to send email: " + e.getMessage();
        }
    }

    private void sendEmail(String to, String subject, String body) throws MessagingException {
        Properties props = new Properties();
        props.put("mail.smtp.host",           "smtp.gmail.com");
        props.put("mail.smtp.port",           "587");
        props.put("mail.smtp.auth",           "true");
        props.put("mail.smtp.starttls.enable","true");

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(FROM, APP_PWD);
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(FROM));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        message.setSubject(subject);
        message.setText(body);

        Transport.send(message);
    }
    
}
