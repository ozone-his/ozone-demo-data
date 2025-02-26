/*
 * Copyright © 2025, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.ozone_demo_data.service;

import com.ozonehis.ozone_demo_data.config.KeycloakConfig;
import com.ozonehis.ozone_demo_data.config.OpenmrsConfig;
import com.ozonehis.ozone_demo_data.util.SystemAvailabilityChecker;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class DemoDataService {

    private static final String GENERATE_DEMO_DATA_ENDPOINT = "/ws/rest/v1/referencedemodata/generate";

    private static final String KEYCLOAK_REALM = "ozone";

    private static final int DEFAULT_DEMO_PATIENTS = 50;

    private final SystemAvailabilityChecker systemAvailabilityChecker;

    private final RestTemplate restTemplate;

    private final OpenmrsConfig openmrsConfig;

    private final KeycloakConfig keycloakConfig;

    @Value("${openmrs.oauth.enabled:false}")
    boolean oauthEnabled;

    @Value("${number.of.demo.patients:" + DEFAULT_DEMO_PATIENTS + "}")
    int numberOfDemoPatients;

    public void triggerDemoData() {
        try {
            if (!systemAvailabilityChecker.waitForOpenMRSAvailability()) {
                log.error("OpenMRS is not available. Aborting demo data generation.");
                return;
            }
            triggerDemoDataGeneration();
        } catch (Exception e) {
            throw new DemoDataGenerationException("Failed to generate demo data", e);
        }
    }

    private void triggerDemoDataGeneration() {
        HttpHeaders headers = createAuthenticatedHeaders();
        Map<String, Object> requestBody = createRequestBody();

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        String generateDemoDataUrl = buildGenerateDemoDataUrl();

        ResponseEntity<String> response = restTemplate.postForEntity(generateDemoDataUrl, request, String.class);

        validateResponse(response);
        log.info("Demo data generation completed successfully");
    }

    HttpHeaders createAuthenticatedHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String authToken = oauthEnabled ? obtainOAuthToken() : obtainBasicAuthToken();

        if (oauthEnabled) {
            headers.setBearerAuth(authToken);
        } else {
            headers.setBasicAuth(authToken);
        }

        return headers;
    }

    private String obtainOAuthToken() {
        log.info("OAuth2 authentication enabled. Obtaining OAuth token...");
        try (Keycloak keycloak = createKeycloakClient()) {
            return keycloak.tokenManager().getAccessToken().getToken();
        } catch (Exception e) {
            throw new AuthenticationException("Failed to obtain OAuth token", e);
        }
    }

    private Keycloak createKeycloakClient() {
        return KeycloakBuilder.builder()
                .serverUrl(keycloakConfig.getServerUrl())
                .realm(KEYCLOAK_REALM)
                .clientId(openmrsConfig.getClientId())
                .clientSecret(openmrsConfig.getClientSecret())
                .grantType("client_credentials")
                .build();
    }

    private String obtainBasicAuthToken() {
        log.info("Basic authentication enabled. Obtaining basic auth token...");
        return Base64.getEncoder()
                .encodeToString((openmrsConfig.getUsername() + ":" + openmrsConfig.getPassword())
                        .getBytes(StandardCharsets.UTF_8));
    }

    Map<String, Object> createRequestBody() {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("numberOfDemoPatients", numberOfDemoPatients);
        requestBody.put("createIfNotExists", true);
        return requestBody;
    }

    String buildGenerateDemoDataUrl() {
        return openmrsConfig.getUrl() + GENERATE_DEMO_DATA_ENDPOINT;
    }

    private void validateResponse(ResponseEntity<String> response) {
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new DemoDataGenerationException(
                    "Demo data generation failed with status: " + response.getStatusCode());
        }
    }

    private static class AuthenticationException extends RuntimeException {

        public AuthenticationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private static class DemoDataGenerationException extends RuntimeException {

        public DemoDataGenerationException(String message) {
            super(message);
        }

        public DemoDataGenerationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
