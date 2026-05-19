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

    private static final String FROM    = System.getenv("EMAIL_USER"); 
    private static final String APP_PWD = System.getenv("EMAIL_APP_PASSWORD"); 

    @Override
    public String execute(JsonObject parameters) {

        if (FROM == null || APP_PWD == null) {
            return "Failed to send email: Server configuration missing credentials.";
        }

        try {
            String to      = parameters.get("To").getAsString();
            String subject = parameters.get("Subject").getAsString();
            String body    = parameters.get("Body").getAsString();

            sendEmail(to, subject, body);
            return "Email sent successfully to " + to;
        } catch (NullPointerException e) {
            return "Failed to send email: Missing required parameters ('to', 'subject', or 'body').";
        } catch (Exception e) {
            return "Failed to send email: " + e.getMessage();
        }
    }

    private void sendEmail(String to, String subject, String body) throws MessagingException {
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
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