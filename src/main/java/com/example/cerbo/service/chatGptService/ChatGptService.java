package com.example.cerbo.service.chatGptService;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class ChatGptService {

    @Value("${groq.api.key}")
    private String apiKey;

    private static final String GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";

    public String generateSyntheticRemark(String inputRemarks) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        String prompt;
        if (!inputRemarks.contains("\n")) {
            // Une seule remarque
            prompt = "Corrige uniquement les fautes d'orthographe et de grammaire de la phrase suivante. Ne retourne que la version corrigée, sans explication, sans introduction, sans ajouter de texte inutile :\n" + inputRemarks;
        } else {
            // Plusieurs remarques
            prompt = "Combine et corrige les remarques suivantes en une seule remarque claire, professionnelle et sans fautes. Ne retourne que la nouvelle remarque, sans explication ni introduction :\n" + inputRemarks;
        }


        Map<String, Object> message = Map.of(
                "role", "user",
                "content", prompt
        );

        Map<String, Object> body = Map.of(
                "model", "llama3-70b-8192", // tu peux aussi tester "llama3-8b-8192"
                "messages", List.of(message),
                "temperature", 0.7
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(GROQ_API_URL, request, Map.class);

        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
        if (choices != null && !choices.isEmpty()) {
            Map<String, Object> messageResult = (Map<String, Object>) choices.get(0).get("message");
            return messageResult.get("content").toString().trim();
        }

        throw new RuntimeException("Erreur lors de la génération de la remarque par Groq");
    }
}

