package com.example.cerbo.service.chatGptService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class ChatGptService {

    private static final Logger logger = LoggerFactory.getLogger(ChatGptService.class);
    private static final String GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String DEFAULT_MODEL = "llama3-70b-8192";
    private static final double DEFAULT_TEMPERATURE = 0.7;

    @Value("${groq.api.key}")
    private String apiKey;

    /**
     * Génère une remarque synthétique en utilisant l'IA ou retourne l'input original
     * @param inputRemarks Les remarques à traiter
     * @param useAI Si true, utilise l'IA pour générer la remarque
     * @return La remarque traitée ou l'input original
     */
    public String generateSyntheticRemark(String inputRemarks, boolean useAI) {
        if (inputRemarks == null || inputRemarks.trim().isEmpty()) {
            return inputRemarks;
        }

        // Si useAI est false, retourner directement l'input original
        if (!useAI) {
            logger.debug("Utilisation de l'IA désactivée - retour de l'input original");
            return inputRemarks;
        }

        try {
            return callGroqApi(inputRemarks);
        } catch (RestClientException e) {
            logger.error("Erreur lors de l'appel à l'API Groq", e);
            return inputRemarks; // Fallback à l'input original en cas d'erreur
        }
    }
    private String callGroqApi(String inputRemarks) throws RestClientException {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = createHeaders();
        Map<String, Object> requestBody = createRequestBody(inputRemarks);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(GROQ_API_URL, request, Map.class);

        if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
            throw new RestClientException("Réponse invalide de l'API Groq: " + response.getStatusCode());
        }

        return extractContentFromResponse(response.getBody());
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        return headers;
    }

    private Map<String, Object> createRequestBody(String inputRemarks) {
        String prompt = buildPrompt(inputRemarks);

        Map<String, Object> message = Map.of(
                "role", "user",
                "content", prompt
        );

        return Map.of(
                "model", DEFAULT_MODEL,
                "messages", List.of(message),
                "temperature", DEFAULT_TEMPERATURE
        );
    }

    private String buildPrompt(String inputRemarks) {
        if (!inputRemarks.contains("\n")) {
            return "Corrige uniquement les fautes d'orthographe et de grammaire de la phrase suivante. "
                    + "Ne retourne que la version corrigée, sans explication, sans introduction, "
                    + "sans ajouter de texte inutile :\n" + inputRemarks;
        } else {
            return "Combine et corrige les remarques suivantes en une seule remarque claire, "
                    + "professionnelle et sans fautes. Ne retourne que la nouvelle remarque, "
                    + "sans explication ni introduction :\n" + inputRemarks;
        }
    }

    private String extractContentFromResponse(Map<String, Object> responseBody) {
        try {
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
            if (choices == null || choices.isEmpty()) {
                throw new RestClientException("Aucun choix dans la réponse de l'API");
            }

            Map<String, Object> firstChoice = choices.get(0);
            Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");

            if (message == null || !message.containsKey("content")) {
                throw new RestClientException("Format de réponse inattendu");
            }

            return message.get("content").toString().trim();
        } catch (ClassCastException e) {
            throw new RestClientException("Erreur de parsing de la réponse", e);
        }
    }
}