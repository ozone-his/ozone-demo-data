/*
 * Copyright © 2025, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.ozone_demo_data.util;

import static java.lang.Thread.sleep;

import com.ozonehis.ozone_demo_data.config.KeycloakConfig;
import com.ozonehis.ozone_demo_data.config.OpenmrsConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class SystemAvailabilityChecker {

    @Autowired
    private OpenmrsConfig openmrsConfig;

    @Autowired
    private KeycloakConfig keycloakConfig;

    @Autowired
    private RestTemplate restTemplate;

    /**
     * This method checks if OpenMRS server is available or not. It sends a GET request to OpenMRS health endpoint.
     *
     * @return true if OpenMRS server is available, false otherwise.
     */
    public boolean isOpenMRSAvailable() {
        try {
            HttpStatusCode status = restTemplate
                    .getForEntity(openmrsConfig.getUrl() + "/health", String.class)
                    .getStatusCode();
            if (status.is2xxSuccessful()) {
                log.info("OpenMRS server is available");
                return true;
            } else {
                log.warn("OpenMRS server is not available. Status code: {}", status);
                return false;
            }
        } catch (Exception e) {
            log.warn("OpenMRS Server not ready: {}", e.getMessage());
            return false;
        }
    }

    /**
     * This method waits for OpenMRS server to be available. It retries 5 times with a delay of 5 seconds between each
     * retry.
     *
     * @return true if OpenMRS server is available, false otherwise.
     */
    public boolean waitForOpenMRSAvailability() {
        int attempts = 0;
        while (!isOpenMRSAvailable() && attempts < openmrsConfig.getMaxRetries()) {
            try {
                sleep(openmrsConfig.getRetryDelayMillis());
            } catch (InterruptedException e) {
                log.error("Error while waiting for OpenMRS server to be available", e);
                Thread.currentThread().interrupt();
                return false;
            }
            log.info("Waiting for OpenMRS server to be available...");
            attempts++;
        }
        return isOpenMRSAvailable();
    }

    /**
     * This method checks if Keycloak server is available or not. It sends a GET request to Keycloak health endpoint.
     *
     * @return true if Keycloak server is available, false otherwise.
     */
    public boolean isKeycloakAvailable() {
        try {
            HttpStatusCode status = restTemplate
                    .getForEntity(keycloakConfig.getServerUrl() + "/health/ready", String.class)
                    .getStatusCode();
            if (status.is2xxSuccessful()) {
                log.info("Keycloak server is available");
                return true;
            } else {
                log.warn("Keycloak server is not available. Status code: {}", status);
                return false;
            }
        } catch (Exception e) {
            log.warn("Keycloak Server not ready: {}", e.getMessage());
            return false;
        }
    }

    /**
     * This method waits for Keycloak server to be available. It retries 5 times with a delay of 5 seconds between each
     * retry.
     *
     * @return true if Keycloak server is available, false otherwise.
     */
    public boolean waitForKeycloakAvailability() {
        int attempts = 0;
        while (!isKeycloakAvailable() && attempts < keycloakConfig.getMaxRetries()) {
            try {
                sleep(keycloakConfig.getRetryDelayMillis());
            } catch (InterruptedException e) {
                log.error("Error while waiting for Keycloak server to be available", e);
                Thread.currentThread().interrupt();
                return false;
            }
            log.info("Waiting for Keycloak server to be available...");
            attempts++;
        }
        return isKeycloakAvailable();
    }
}
