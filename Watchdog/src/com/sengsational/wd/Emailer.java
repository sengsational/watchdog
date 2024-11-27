/*
 * Created on Jan 22, 2010
 *
 */
package com.sengsational.wd;


import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.sun.mail.smtp.SMTPTransport;

public class Emailer {

    static String smtpServerName;
    static String smtpServerPort = "25";
    static String logonUser;
    static String logonPassword;
    static String sendUsers;
    
    static StringBuffer problems;
    
    static Emailer emailer;
    
    private Emailer(){
        super();
    }
    
    public static Emailer getInstance(){
        if (emailer == null){
            emailer = new Emailer();
        }
        return emailer;
    }

    public void initialize(String smtpServerName, String smtpServerPort, String logonUser, String logonPassword, String sendUsers) {
        if (smtpServerPort != null) Emailer.smtpServerPort = smtpServerPort;
        Emailer.smtpServerName = smtpServerName;
        Emailer.logonUser = logonUser;
        Emailer.logonPassword = logonPassword;
        Emailer.sendUsers = sendUsers;
    }
    
    public void sendEmailMessage(String subject, String content) {
        System.out.println(new Date() + " Sending email to " + Emailer.sendUsers);
        javax.activation.DataSource dataSource = null;
        try {
            Session session = getSession();
            MimeMessage msg = new MimeMessage(session);
            String sendFromDomain = getLocalMachineName() + ".com";
            int atLoc = logonUser.indexOf("@");
            if (atLoc > -1 && !logonUser.endsWith("@")){
                sendFromDomain = logonUser.substring(atLoc + 1);
            } else {
                System.out.println(new Date() + " ERROR: You have specified a logonUser without an '@'.  Using your machine name as send from domain. " + sendFromDomain );
            }
            //msg.setFrom(new InternetAddress("cwhelpertest@" + sendFromDomain));
            msg.setFrom(new InternetAddress(logonUser));
            msg.setRecipients(Message.RecipientType.TO, getToAddresses());
            msg.setSubject(subject);
            msg.setContent(content,"text/plain");
            SMTPTransport transport =(SMTPTransport)session.getTransport("smtp");//ok
            transport.connect();
            transport.sendMessage(msg, msg.getAllRecipients());
            transport.close();
            System.out.println(new Date() + " Message Sent.");
        } catch (Exception e) {
            System.out.println(new Date() + " ERROR sending TEST email.  " + e.getMessage());
        }
    }

    public void send() {
        try {Thread.sleep(500);} catch (Exception e){}
        System.out.println(new Date() + " Sending email to " + Emailer.sendUsers);
        String sendFromDomain = null;
        try {
            Session session = getSession();
            MimeMessage msg = new MimeMessage(session);
            sendFromDomain = getLocalMachineName() + ".com";
            int atLoc = logonUser.indexOf("@");
            if (atLoc > -1 && !logonUser.endsWith("@")){
                sendFromDomain = logonUser.substring(atLoc + 1);
            }
            //msg.setFrom(new InternetAddress("cwhelper@" + sendFromDomain));
            msg.setFrom(new InternetAddress(logonUser));
            msg.setRecipients(Message.RecipientType.TO, getToAddresses());
            msg.setSubject("Daily CW_EPG News");
            StringBuffer content = new StringBuffer();
            if (content.length() > 0){
                msg.setContent(new String(content),"text/html");
                SMTPTransport transport =(SMTPTransport)session.getTransport("smtp");//ok
                transport.connect();
                transport.sendMessage(msg, msg.getAllRecipients());
                transport.close();
            }
        } catch (Exception e) {
            System.out.println(new Date() + " ERROR sending status email.  " + e.getMessage() + " " + sendFromDomain);
        }
    }

    private Session getSession() {
        Properties props = new Properties();
        props.put("mail.smtp.host", Emailer.smtpServerName);
        props.put("mail.smtp.port", Emailer.smtpServerPort);
        props.put("mail.smtp.auth","true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");
        Session session = Session.getDefaultInstance(props, new SmtpAuthenticator());
        return session;
    }
    
    private InternetAddress[] getToAddresses(){
        InternetAddress[] toAddresses = new InternetAddress[0];
        String lastToken = "";
        try {
            StringTokenizer tok = new StringTokenizer(Emailer.sendUsers, ";");
            ArrayList<InternetAddress> internetAddressList = new ArrayList<InternetAddress>();
            while(tok.hasMoreTokens()){
                lastToken = tok.nextToken();
                internetAddressList.add(new InternetAddress(lastToken));
            }
            toAddresses = internetAddressList.toArray(new InternetAddress[0]);
        } catch (AddressException e) {
            System.out.println(new Date() + " ERROR: Could not for email address from " + lastToken);
        }
        return toAddresses;
    }
    
    private String getLocalMachineName(){
        String localMachineName = "myTvMachine";
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            localMachineName = localHost.getHostName();
        } catch (Exception e){}
        return localMachineName;
    }
    
    class SmtpAuthenticator extends Authenticator {
        protected PasswordAuthentication getPasswordAuthentication(){
            return new PasswordAuthentication(Emailer.logonUser, Emailer.logonPassword);
        }
    }
    
    public static void main(String[] args) {
        Emailer emailer = Emailer.getInstance();

        String smtpServerName = "smtp.googlemail.com";
        String smtpServerPort = "587";
        String logonUser = "";
        String logonPassword = "";
        String sendUsers = "";

        emailer.initialize(smtpServerName,smtpServerPort, logonUser,logonPassword,sendUsers);
        emailer.sendEmailMessage("Test Email", "This is a test.");
    }

}
