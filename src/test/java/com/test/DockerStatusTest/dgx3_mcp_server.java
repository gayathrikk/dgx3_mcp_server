package com.test.DockerStatusTest;

import com.jcraft.jsch.*;
import javax.mail.*;
import javax.mail.internet.*;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Properties;

public class dgx3_mcp_server {

    @Test(priority = 1)
    public void mcp_server() {

        String vmIpAddress = "172.20.23.156";
        String username = "appUser";
        String password = "Brain@123";
        String containerName = "mcp";  

        System.out.println("dgx3_mcp_server Docker Name = " + containerName);

        if (containerName.isEmpty()) {
            System.out.println("Container name is required.");
            return;
        }

        try {
            JSch jsch = new JSch();
            com.jcraft.jsch.Session session = jsch.getSession(username, vmIpAddress, 22);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();

            // ✅ Using container name instead of ID
            ChannelExec channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand("docker inspect --format='{{.State.Status}}' " + containerName);
            channel.setInputStream(null);
            channel.setErrStream(System.err);
            BufferedReader reader = new BufferedReader(new InputStreamReader(channel.getInputStream()));
            channel.connect();

            String line;
            boolean isRunning = false;

            while ((line = reader.readLine()) != null) {
                System.out.println("Docker Status: " + line.trim());
                if (line.trim().equals("running")) {
                    isRunning = true;
                }
            }

            channel.disconnect();
            session.disconnect();

            // If container is not running, send alert
            if (!isRunning) {
                sendEmailAlert("Hi,\n\n🚨 This is dgx3_mcp_server Docker (mcp). I am currently down. Kindly restart the container at your earliest convenience.");
                assert false : "Container is not in the expected state.";
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendEmailAlert(String messageBody) {
        String from = "automationsoftware25@gmail.com";

        // TO recipients
        String[] to = {
            "sriramv@htic.iitm.ac.in"
        };

        // CC recipients
        String[] cc = {
            "divya.d@htic.iitm.ac.in",
            "venip@htic.iitm.ac.in"
        };

        String subject = "Docker Container Alert - dgx3_mcp_server";
        final String username = "automationsoftware25@gmail.com";
        final String password = "wjzcgaramsqvagxu"; // App-specific password

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        javax.mail.Session mailSession = javax.mail.Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        try {
            Message message = new MimeMessage(mailSession);
            message.setFrom(new InternetAddress(from, "Docker Monitor"));

            message.setRecipients(
                Message.RecipientType.TO,
                InternetAddress.parse(String.join(",", to))
            );
            message.setRecipients(
                Message.RecipientType.CC,
                InternetAddress.parse(String.join(",", cc))
            );

            message.setSubject(subject);
            message.setText(messageBody);

            Transport.send(message);
            System.out.println("✅ Alert email sent successfully.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

