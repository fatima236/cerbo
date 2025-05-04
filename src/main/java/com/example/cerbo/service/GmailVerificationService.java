package com.example.cerbo.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.Profile;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

@Service
public class GmailVerificationService {

    private static final String CREDENTIALS_JSON = """
        {
          "web": {
            "client_id": "612013417547-5kmr6nhvbenu2fcepqesosq4rrkbjdac.apps.googleusercontent.com",
            "project_id": "directed-sonar-458718-h2",
            "auth_uri": "https://accounts.google.com/o/oauth2/auth",
            "token_uri": "https://oauth2.googleapis.com/token",
            "auth_provider_x509_cert_url": "https://www.googleapis.com/oauth2/v1/certs",
            "client_secret": "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",
            "redirect_uris": ["http://localhost:8081/oauth2callback"],
            "javascript_origins": ["http://localhost:3000"]
          }
        }
        """;

    public boolean isGmailAccountValid(String email) {
        // Vérification basique du format d'abord
        if (!email.matches("^[a-zA-Z0-9._%+-]+@(gmail\\.com|gmail\\.ma)$")) {
            return false;
        }

        try {
            // 1. Configurer les credentials
            GoogleCredentials credentials = GoogleCredentials.fromStream(
                            new ByteArrayInputStream(CREDENTIALS_JSON.getBytes()))
                    .createScoped(Collections.singleton(GmailScopes.GMAIL_READONLY));

            // 2. Créer le service Gmail
            NetHttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
            Gmail service = new Gmail.Builder(
                    transport,
                    GsonFactory.getDefaultInstance(),
                    new HttpCredentialsAdapter(credentials))
                    .setApplicationName("CERBO")
                    .build();

            // 3. Vérifier l'existence du compte
            // Méthode 1: Vérifier le profil utilisateur
            try {
                Profile profile = service.users().getProfile(email).execute();
                return profile != null && profile.getEmailAddress() != null;
            } catch (IOException e) {
                if (e.getMessage().contains("404")) {
                    return false; // Compte non trouvé
                }
                throw e; // Relancer les autres erreurs
            }

        } catch (Exception e) {
            System.err.println("Erreur de vérification Gmail: " + e.getMessage());
            return false;
        }
    }
}