package com.example.sendmail2;

import static com.example.sendmail2.GmailOperations.createEmail;
import static com.example.sendmail2.GmailOperations.createMessageWithEmail;
import static com.example.sendmail2.GmailOperations.sendEmail;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.Message;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

public class MainActivity extends AppCompatActivity {
    private Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        button=findViewById(R.id.button);
        button.setOnClickListener(click-> {Toast.makeText(this,"Clicked",Toast.LENGTH_SHORT).show();
            try {
                sendEmail();
            } catch (MessagingException e) {
                e.printStackTrace();
            } catch (GeneralSecurityException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void sendEmail() throws IOException, GeneralSecurityException, MessagingException {
        // Must run on another thread
        Runnable runnable= () -> {
            Gmail service = null;
            try {
                service = GmailAPI.getGmailService(this);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (GeneralSecurityException e) {
                e.printStackTrace();
            }
            MimeMessage Mimemessage = null;
            try {
                Mimemessage = createEmail("quangtuan202@gmail.com","quangtuan202@gmail.com","This my demo test subject","This is my body text");
            } catch (MessagingException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            Message message = null;
            try {
                message = createMessageWithEmail(Mimemessage);
            } catch (MessagingException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                message = service.users().messages().send("me", message).execute();
            } catch (IOException e) {
                e.printStackTrace();
            }

            System.out.println("Message id: " + message.getId());
            try {
                System.out.println(message.toPrettyString());
            } catch (IOException e) {
                e.printStackTrace();
            }

        };
        new Thread(runnable).start();


    }



}