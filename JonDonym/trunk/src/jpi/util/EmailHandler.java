/*
 Copyright (c) 2000 - 2006, The JAP-Team
 All rights reserved.
 Redistribution and use in source and binary forms, with or without modification,
 are permitted provided that the following conditions are met:

  - Redistributions of source code must retain the above copyright notice,
 this list of conditions and the following disclaimer.

  - Redistributions in binary form must reproduce the above copyright notice,
 this list of conditions and the following disclaimer in the documentation and/or
 other materials provided with the distribution.

  - Neither the name of the University of Technology Dresden, Germany nor the names of its contributors
 may be used to endorse or promote products derived from this software without specific
 prior written permission.


 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS ``AS IS'' AND ANY EXPRESS
 OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS
 BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE
 */

package jpi.util;

import javax.mail.Transport;
import javax.mail.Session;
import java.util.Properties;
import javax.mail.internet.MimeMessage;
import javax.mail.PasswordAuthentication;
import logging.LogType;
import javax.mail.internet.InternetAddress;
import logging.LogLevel;
import logging.LogHolder;
import javax.mail.Message;
import javax.mail.Authenticator;
import anon.pay.xml.XMLEmail;

/**
 * Utitlity class that handles sending emails
 * @author Elmar Schraml
 */
public class EmailHandler
{

	//Configuration: which smtp server to use
	private static final String EMAIL_SERVER = "smtp.gmail.com";
	private static final String EMAIL_SERVER_PORT = "465";  //587;
	static final String USERNAME = "anontestmailer";
	static final String PASSWORD = "hsitrwc2";
	static final String USER = "anontestmailer@googlemail.com";
	//where to send an email to (other addresses can be passed as parameters)
	public static final String SUPPORT_ADDRESS = "jap@inf.tu-dresden.de";
	public static final String DEBUG_ADDRESS = "elmar.schraml@gmail.com";
	//predefined data for standard emails
	public static final String SUBJECT_SUPPORTREQUEST = "Support request from a mixoperator";
	public static final String SUBJECT_MIXEDCASCADE = "AN.ON: Your free Mix is part of a for-profit cascade";
	public static final String BODY_MIXEDCASCADE = "Your free Mix is part of a for-profit cascade."
                           +"Which perfectly all right with us, we just thought you would want to know"
						   +"since somebody else is making money here, but not you";
	public static final String JPI_ADDRESS = "jap@anon.inf.tu-dresden.de";
	/**
	 * sendEmail
	 *
	 * this method gets called by all other methods to send email (for support, with XMLEMail as param etc),
	 * if you use a different smtp server, make your adjustments in this method
	 *
	 * @param recipientAddress String: use one of the constants like SUPPORT_ADDRESS, or specify your own
	 * @param returnAddress String: is used as given, EmailHandler does not check its validity
	 * @param subject String: use one of the constants like SUBJECT_SUPPORTREQUEST, or specify your own
	 * @param body String
	 */
	public static void sendEmail(String returnAddress, String recipient, String subject, String body)
	{
		try
		{
			Properties props = new Properties();
			props.put("mail.smtp.host", EMAIL_SERVER);
			props.put("mail.smtp.starttls.enable","true");
			props.put("mail.smtp.auth", "true");
			props.put("mail.smtp.port", EMAIL_SERVER_PORT);
			props.put("mail.smtp.user", USER);
			props.put("mail.smtp.socketFactory.port", EMAIL_SERVER_PORT);
			props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
			props.put("mail.smtp.socketFactory.fallback", "false");

			Authenticator auth = new SupportAuthenticator();
			Session session = Session.getDefaultInstance(props, auth);
			Message msg = new MimeMessage(session);
			InternetAddress addressFrom = new InternetAddress(returnAddress);
			msg.setFrom(addressFrom);
			InternetAddress addressTo = new InternetAddress(recipient);
			msg.setRecipient(Message.RecipientType.TO, addressTo);
			msg.setSubject(subject);
			msg.setContent(body, "text/plain");
			msg.saveChanges(); // implicit with send()
			//Transport transport = session.getTransport("smtp");
			//transport.connect(EMAIL_SERVER, USERNAME, PASSWORD);
			//transport.sendMessage(msg, msg.getAllRecipients());
			//transport.close();
			Transport.send(msg);
			LogHolder.log(LogLevel.INFO, LogType.MISC, "email message was sent");
		} catch (Exception e)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.PAY, "could not send email",e);
		}
	}

	/**
	 * sendSupportEmail: calls sendEmail with this class's String constants used for recipient and subject
	 *
	 * @param returnAddress String
	 * @param body String
	 */
	public static void sendSupportEmail(String returnAddress, String body)
	{
		String subject = SUBJECT_SUPPORTREQUEST;
		String recipient = DEBUG_ADDRESS;
		//String recipient = SUPPORT_ADDRESS;
		sendEmail(returnAddress,recipient,subject,body);
	}

	public static void sendEmail(XMLEmail theMail)
	{
		//get additional information about this user from database



	}
}

//Session needs an Authenticator, which is abstract, so we build our own here
class SupportAuthenticator extends Authenticator
{
	public PasswordAuthentication getPasswordAuthentication()
	{
		String username = EmailHandler.USER; //.USERNAME
		String password = EmailHandler.PASSWORD;
		return new PasswordAuthentication(username, password);
	}
}
