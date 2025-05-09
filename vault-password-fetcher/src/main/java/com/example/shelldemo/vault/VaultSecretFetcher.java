package com.example.shelldemo.vault;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;

public class VaultSecretFetcher {
    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    public String fetchOraclePassword(String vaultBaseUrl, String roleId, String secretId, String dbName, String ait) throws Exception {
        // 1. Authenticate to Vault
        String loginUrl = vaultBaseUrl + "/v1/auth/approle/login";
        String loginBody = String.format("{\"role_id\":\"%s\",\"secret_id\":\"%s\"}", roleId, secretId);
        HttpRequest loginRequest = HttpRequest.newBuilder()
                .uri(URI.create(loginUrl))
                .header("content-type", "application/json")
                .POST(BodyPublishers.ofString(loginBody, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> loginResponse = client.send(loginRequest, HttpResponse.BodyHandlers.ofString());
        if (loginResponse.statusCode() != 200) {
            throw new VaultSecretFetcherException("Vault login failed: " + loginResponse.body());
        }
        String clientToken = mapper.readTree(loginResponse.body()).at("/auth/client_token").asText();
        if (clientToken == null || clientToken.isEmpty()) {
            throw new VaultSecretFetcherException("No client token received from Vault");
        }

        // 2. Fetch Oracle password secret
        String secretPath = String.format("%s/v1/secrets/database/oracle/static-creds/%s-%s", vaultBaseUrl, ait, dbName);
        HttpRequest secretRequest = HttpRequest.newBuilder()
                .uri(URI.create(secretPath))
                .header("x-vault-token", clientToken)
                .GET()
                .build();
        HttpResponse<String> secretResponse = client.send(secretRequest, HttpResponse.BodyHandlers.ofString());
        if (secretResponse.statusCode() != 200) {
            throw new VaultSecretFetcherException("Vault secret fetch failed: " + secretResponse.body());
        }
        // Parse the password from the response (adjust the path as needed)
        String password = mapper.readTree(secretResponse.body()).at("/data/password").asText();
        if (password == null || password.isEmpty()) {
            throw new VaultSecretFetcherException("No password found in Vault secret");
        }
        return password;
    }
} 