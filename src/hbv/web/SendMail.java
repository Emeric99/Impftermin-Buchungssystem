package hbv.web;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import jakarta.servlet.http.HttpServlet;

import java.util.Properties;

public class SendMail extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private static final String SMTP_HOST     = System.getenv().getOrDefault("SMTP_HOST", "mailhog");
    private static final String SMTP_PORT     = System.getenv().getOrDefault("SMTP_PORT", "1025");
    private static final String SMTP_FROM     = System.getenv().getOrDefault("SMTP_FROM", "impftermin@bremerhaven.de");

    public void sendConfirmation(String appointmentId, String center,
                                 String date, String timeSlot,
                                 String vaccine, String userId) {

        // Session-Konfiguration
        Properties props = new Properties();
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);
        props.put("mail.smtp.auth", "false");

        Session session = Session.getInstance(props);

        Thread thread = new Thread(() -> {
            try {
                // E-Mail aufbauen
                MimeMessage message = new MimeMessage(session);
                message.setFrom(new InternetAddress(SMTP_FROM));

                // Empfänger: in echtem System wäre das die E-Mail des Nutzers
                // Für Demo: an eine Test-Adresse senden
                message.setRecipients(Message.RecipientType.TO,
                        InternetAddress.parse("nutzer-" + userId + "@impftermin.de"));

                message.setSubject("Bestätigung Ihres Impftermins – Nr. " + appointmentId);

                // E-Mail-Text
                String body =
                        "Sehr geehrte/r Nutzer/in,\n\n" +
                        "Ihr Impftermin wurde erfolgreich gebucht.\n\n" +
                        "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                        "  Buchungsnummer : " + appointmentId + "\n" +
                        "  Zentrum        : " + center + "\n" +
                        "  Datum          : " + date + "\n" +
                        "  Uhrzeit        : " + timeSlot + "\n" +
                        "  Impfstoff      : " + getVaccineName(vaccine) + "\n" +
                        "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n" +
                        "Bitte erscheinen Sie pünktlich und bringen Sie Ihren Personalausweis mit.\n\n" +
                        "Mit freundlichen Grüßen\n" +
                        "Impfzentrum Bremerhaven";

                message.setText(body, "UTF-8");

                Transport.send(message);

            } catch (MessagingException e) {
                System.err.println("[SendMail] Fehler beim Senden: " + e.getMessage());
            }
        });

        thread.setDaemon(true);
        thread.start();
    }

    private String getVaccineName(String vaccineId) {
        switch (vaccineId) {
            case "1": return "BioNTech/Pfizer";
            case "2": return "Moderna";
            case "3": return "Johnson & Johnson";
            case "4": return "AstraZeneca";
            default:  return "Unbekannt (ID: " + vaccineId + ")";
        }
    }
}package hbv.web;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

public class SendMail {package hbv.web;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import jakarta.servlet.http.HttpServlet;

import java.util.Properties;

public class SendMail extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private static final String SMTP_HOST     = System.getenv().getOrDefault("SMTP_HOST", "mailhog");
    private static final String SMTP_PORT     = System.getenv().getOrDefault("SMTP_PORT", "1025");
    private static final String SMTP_FROM     = System.getenv().getOrDefault("SMTP_FROM", "impftermin@bremerhaven.de");

    public void sendConfirmation(String appointmentId, String center,
                                 String date, String timeSlot,
                                 String vaccine, String userId) {

        // Session-Konfiguration
        Properties props = new Properties();
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);
        props.put("mail.smtp.auth", "false");

        Session session = Session.getInstance(props);

        Thread thread = new Thread(() -> {
            try {
                // E-Mail aufbauen
                MimeMessage message = new MimeMessage(session);
                message.setFrom(new InternetAddress(SMTP_FROM));

                // Empfänger: in echtem System wäre das die E-Mail des Nutzers
                // Für Demo: an eine Test-Adresse senden
                message.setRecipients(Message.RecipientType.TO,
                        InternetAddress.parse("nutzer-" + userId + "@impftermin.de"));

                message.setSubject("Bestätigung Ihres Impftermins – Nr. " + appointmentId);

                // E-Mail-Text
                String body =
                        "Sehr geehrte/r Nutzer/in,\n\n" +
                        "Ihr Impftermin wurde erfolgreich gebucht.\n\n" +
                        "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                        "  Buchungsnummer : " + appointmentId + "\n" +
                        "  Zentrum        : " + center + "\n" +
                        "  Datum          : " + date + "\n" +
                        "  Uhrzeit        : " + timeSlot + "\n" +
                        "  Impfstoff      : " + getVaccineName(vaccine) + "\n" +
                        "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n" +
                        "Bitte erscheinen Sie pünktlich und bringen Sie Ihren Personalausweis mit.\n\n" +
                        "Mit freundlichen Grüßen\n" +
                        "Impfzentrum Bremerhaven";

                message.setText(body, "UTF-8");

                Transport.send(message);

            } catch (MessagingException e) {
                System.err.println("[SendMail] Fehler beim Senden: " + e.getMessage());
            }
        });

        thread.setDaemon(true);
        thread.start();
    }

    private String getVaccineName(String vaccineId) {
        switch (vaccineId) {
            case "1": return "BioNTech/Pfizer";
            case "2": return "Moderna";
            case "3": return "Johnson & Johnson";
            case "4": return "AstraZeneca";
            default:  return "Unbekannt (ID: " + vaccineId + ")";
        }
    }
}

    public static void sendConfirmation(final String appointmentId, final String center,
                                        final String date, final String timeSlot,
                                        final String vaccine, final String userId) {
        // Faire le message
        final String message = appointmentId + "|" + center + "|" + date + "|" + timeSlot + "|" + vaccine + "|" + userId;

        Thread thread = new Thread(new Runnable() {
            public void run() {
                Socket sock = null;
                PrintWriter pw = null;
                BufferedReader br = null;
                try {
                    sock = new Socket("localhost", 2222);
                    pw = new PrintWriter(new OutputStreamWriter(sock.getOutputStream()), true);
                    br = new BufferedReader(new InputStreamReader(sock.getInputStream()));

                    // Sende die Nachricht an den Server
                    pw.println(message);

                   
                    br.readLine();
                } catch (IOException e) {
                    // Fehler
                } finally {
                    try {
                        if (br != null) {
                            br.close();
                        }
                        if (pw != null) {
                            pw.close();
                        }
                        if (sock != null) {
                            sock.close();
                        }
                    } catch (IOException e) {
                        // Fehler
                    }
                }
            }
        });
        thread.start();
    }

}
