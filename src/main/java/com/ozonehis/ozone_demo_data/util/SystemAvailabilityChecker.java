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

    public boolean isOpenMRSAvailable() {
        return isSystemAvailable(openmrsConfig.getUrl() + "/health/started", "OpenMRS");
    }

    /**
     * Wait for OpenMRS server to be available
     *
     * @return true if OpenMRS server is available, false otherwise
     */
    public boolean waitForOpenMRSAvailability() {
        return waitForSystemAvailability(
                openmrsConfig.getUrl() + "/health/started",
                openmrsConfig.getMaxRetries(),
                openmrsConfig.getRetryDelayMillis(),
                "OpenMRS");
    }

    public boolean isKeycloakAvailable() {
        return isSystemAvailable(keycloakConfig.getServerUrl() + "/health/ready", "Keycloak");
    }

    /**
     * Wait for Keycloak server to be available
     *
     * @return true if Keycloak server is available, false otherwise
     */
    public boolean waitForKeycloakAvailability() {
        return waitForSystemAvailability(
                keycloakConfig.getServerUrl() + "/health/ready",
                keycloakConfig.getMaxRetries(),
                keycloakConfig.getRetryDelayMillis(),
                "Keycloak");
    }

    /**
     * Check if the system is available
     *
     * @param url        the URL to check
     * @param systemName the name of the system
     * @return true if the system is available, false otherwise
     */
    private boolean isSystemAvailable(String url, String systemName) {
        try {
            HttpStatusCode status = restTemplate.getForEntity(url, String.class).getStatusCode();
            if (status.is2xxSuccessful()) {
                log.info("{} server is available", systemName);
                return true;
            } else {
                log.warn("{} server is not available. Status code: {}", systemName, status);
                return false;
            }
        } catch (Exception e) {
            log.warn("{} Server not ready: {}", systemName, e.getMessage());
            return false;
        }
    }

    /**
     * Wait for the system to be available
     *
     * @param url              the URL to check
     * @param maxRetries       the maximum number of retries
     * @param retryDelayMillis the delay between retries in milliseconds
     * @param systemName       the name of the system
     * @return true if the system is available, false otherwise
     */
    private boolean waitForSystemAvailability(String url, int maxRetries, long retryDelayMillis, String systemName) {
        int attempts = 0;
        boolean isAvailable;

        while (!(isAvailable = isSystemAvailable(url, systemName)) && attempts < maxRetries) {
            try {
                sleep(retryDelayMillis);
            } catch (InterruptedException e) {
                log.error("Error while waiting for {} server to be available: {}", systemName, e.getMessage());
                Thread.currentThread().interrupt();
                return false;
            }
            log.info("Waiting for {} server to be available...", systemName);
            attempts++;
        }
        return isAvailable;
    }
}
