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
import com.ozonehis.ozone_demo_data.exceptions.AuthenticationException;
import com.ozonehis.ozone_demo_data.exceptions.DemoDataGenerationException;
import com.ozonehis.ozone_demo_data.util.SystemAvailabilityChecker;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class DemoDataService {

    private static final String GENERATE_DEMO_DATA_ENDPOINT = "/ws/rest/v1/referencedemodata/generate";

    private static final int DEFAULT_DEMO_PATIENTS = 50;

    private static final String CREATE_DEMO_PATIENTS_ON_NEXT_STARTUP_PROPERTY =
            "referencedemodata.createDemoPatientsOnNextStartup";

    private static final String SYSTEM_SETTING_ENDPOINT = "/ws/rest/v1/systemsetting";

    private final SystemAvailabilityChecker systemAvailabilityChecker;

    private final RestTemplate restTemplate;

    private final OpenmrsConfig openmrsConfig;

    private final KeycloakConfig keycloakConfig;

    @Value("${openmrs.oauth.enabled:false}")
    boolean oauthEnabled;

    @Value("${openmrs.demo.patients:" + DEFAULT_DEMO_PATIENTS + "}")
    int numberOfDemoPatients;

    private boolean isDemoDataGenerated = false;

    public synchronized void triggerDemoData() {
        if (isDemoDataGenerated) {
            log.info("Demo data already generated. Skipping.");
            return;
        }
        try {
            if (!systemAvailabilityChecker.waitForOpenMRSAvailability()) {
                log.error("OpenMRS is not available. Aborting demo data generation.");
                return;
            }
            triggerDemoDataGeneration();
            isDemoDataGenerated = true;
        } catch (Exception e) {
            throw new DemoDataGenerationException("Failed to generate demo data", e);
        }
    }

    @SuppressWarnings("unchecked")
    void updateCreateDemoPatientsOnNextStartupSetting() {
        try {
            // Get the system setting details
            HttpHeaders headers = createAuthenticationHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Get setting UUID
            String settingUrl = openmrsConfig.getUrl() + SYSTEM_SETTING_ENDPOINT + "/?q="
                    + CREATE_DEMO_PATIENTS_ON_NEXT_STARTUP_PROPERTY;
            HttpEntity<Void> request = new HttpEntity<>(headers);

            var response = restTemplate.exchange(settingUrl, HttpMethod.GET, request, Map.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.error("Failed to get {} system setting", CREATE_DEMO_PATIENTS_ON_NEXT_STARTUP_PROPERTY);
                return;
            }

            List<Map<String, Object>> results =
                    (List<Map<String, Object>>) response.getBody().get("results");
            if (results.isEmpty()) {
                log.error(CREATE_DEMO_PATIENTS_ON_NEXT_STARTUP_PROPERTY + " system setting not found");
                return;
            }

            String uuid = (String) results.get(0).get("uuid");
            HttpEntity<Map<String, String>> updateRequest = new HttpEntity<>(Map.of("value", "0"), headers);
            String updateUrl = openmrsConfig.getUrl() + SYSTEM_SETTING_ENDPOINT + "/" + uuid;

            restTemplate
                    .exchange(updateUrl, HttpMethod.POST, updateRequest, String.class)
                    .getStatusCode()
                    .is2xxSuccessful();

            log.info("Successfully updated system setting to disable demo data generation on the next startup");
        } catch (Exception e) {
            log.error("Failed to update system setting: {}", e.getMessage(), e);
        }
    }

    private void triggerDemoDataGeneration() {
        HttpHeaders headers = createAuthenticationHeaders();
        Map<String, Object> requestBody = createRequestBody();

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        String generateDemoDataUrl = buildGenerateDemoDataUrl();

        ResponseEntity<String> response = restTemplate.postForEntity(generateDemoDataUrl, request, String.class);

        validateResponse(response);
        updateCreateDemoPatientsOnNextStartupSetting();
        log.info("Demo data generation completed successfully");
    }

    HttpHeaders createAuthenticationHeaders() {
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

    private Keycloak openKeycloak() {
        return KeycloakBuilder.builder()
                .serverUrl(keycloakConfig.getServerUrl())
                .realm(keycloakConfig.getRealm())
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .clientId(openmrsConfig.getClientId())
                .clientSecret(openmrsConfig.getClientSecret())
                .build();
    }

    private String obtainOAuthToken() {
        log.info("OAuth2 authentication enabled. Obtaining OAuth token...");
        try (Keycloak keycloak = openKeycloak()) {
            return keycloak.tokenManager().getAccessToken().getToken();
        } catch (Exception e) {
            throw new AuthenticationException("Failed to obtain OAuth token", e);
        } finally {
            openKeycloak().close();
        }
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
}
