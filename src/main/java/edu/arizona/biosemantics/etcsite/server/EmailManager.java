package edu.arizona.biosemantics.etcsite.server;

import java.util.Properties;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;


public class EmailManager {
	
	public static final String PASSWORD_RESET_SUBJECT = "Password Reset Code";
	public static final String PASSWORD_RESET_BODY = "A password reset authentication code has been generated for your account (<openidproviderid>). You can use this code to reset your password. \n\nCode: <code>\n\nThis code will expire in <expire>.\n\n\n(You are receiving this email because you recently requested an authentication code to reset your account password. If you did not request an authentication code, ignore this email.)";
	public static final String FINISHED_LEARNING_TERMS_SUBJECT = "(<taskname>) Learn Step Complete";
	public static final String FINISHED_LEARNING_TERMS_BODY = "The Text Capture tool has finished processing input files for your task \"<taskname>\". You may review the generated output and resume the task using the Task Manager.";
	public static final String FINISHED_PARSING_SUBJECT = "(<taskname>) Task Complete";
	public static final String FINISHED_PARSING_BODY = "Text Capture has completed for your task \"<taskname>\". You may resume this task using the Task Manager, or view the generated output files using the File Manager.";
	public static final String FINISHED_GENERATING_MATRIX_SUBJECT = "(<taskname>) Generate Step Complete";
	public static final String FINISHED_GENERATING_MATRIX_BODY = "A taxon-character matrix has been generated for your task \"<taskname>\". You may review the generated matrix and resume the task using the Task Manager.";
	
	private static EmailManager instance;
	
	private Session session;
	
	public EmailManager(){
		Properties props = new Properties();
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.smtp.host", Configuration.emailSMTPServer);
		props.put("mail.smtp.port", "587");
 
		session = Session.getInstance(props, new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(Configuration.emailAddress, Configuration.emailPassword);
			}
		});
	}
	
	public static EmailManager getInstance(){
		if (instance == null)
			instance = new EmailManager();
		return instance;
	}
	
	public void sendEmail(final String to, final String subjectLine, final String bodyText) throws MessagingException {
		final Message message = new MimeMessage(session);
		message.setFrom(new InternetAddress(Configuration.emailAddress));
		message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
		message.setSubject(subjectLine);
		message.setText(bodyText);

		Thread sendThread = new Thread(){
			public void run(){
				try {
					Transport.send(message);
					//System.out.println("Sent message to " + to + ". Subject line: " + subjectLine + ", Body: " + bodyText);
				} catch (MessagingException e) {
					e.printStackTrace();
				}
			}
		};
		sendThread.start();
	}
}
