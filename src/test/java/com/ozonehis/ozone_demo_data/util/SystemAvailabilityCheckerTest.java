/*
 * Copyright © 2025, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.ozone_demo_data.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.ozonehis.ozone_demo_data.config.KeycloakConfig;
import com.ozonehis.ozone_demo_data.config.OpenmrsConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class SystemAvailabilityCheckerTest {

    @Mock
    private OpenmrsConfig openmrsConfig;

    @Mock
    private KeycloakConfig keycloakConfig;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private SystemAvailabilityChecker systemAvailabilityChecker;

    @BeforeEach
    void setUp() {
        lenient().when(openmrsConfig.getUrl()).thenReturn("http://openmrs");
        lenient().when(openmrsConfig.getMaxRetries()).thenReturn(5);
        lenient().when(openmrsConfig.getRetryDelayMillis()).thenReturn(1000L);

        lenient().when(keycloakConfig.getServerUrl()).thenReturn("http://keycloak");
        lenient().when(keycloakConfig.getMaxRetries()).thenReturn(5);
        lenient().when(keycloakConfig.getRetryDelayMillis()).thenReturn(1000L);
    }

    @Test
    void shouldReturnTrueWhenOpenMRSSystemIsAvailable() {
        when(restTemplate.getForEntity("http://openmrs/health/started", String.class))
                .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        boolean result = systemAvailabilityChecker.isOpenMRSAvailable();

        assertTrue(result);
    }

    @Test
    void shouldReturnFalseWhenOpenMRSIsNotAvailable() {
        when(restTemplate.getForEntity("http://openmrs/health/started", String.class))
                .thenReturn(new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE));

        boolean result = systemAvailabilityChecker.isOpenMRSAvailable();

        assertFalse(result);
    }

    @Test
    void shouldWaitForOpenMRSAvailability() {
        when(restTemplate.getForEntity("http://openmrs/health/started", String.class))
                .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        boolean result = systemAvailabilityChecker.waitForOpenMRSAvailability();

        assertTrue(result);
    }

    @Test
    void shouldReturnFalseWhenOpenMRSMaxRetriesExceeded() {
        when(restTemplate.getForEntity("http://openmrs/health", String.class))
                .thenThrow(new RuntimeException("Connection failed"));

        boolean result = systemAvailabilityChecker.waitForOpenMRSAvailability();

        assertFalse(result);
    }

    @Test
    void shouldReturnTrueWhenKeycloakSystemIsAvailable() {
        when(restTemplate.getForEntity("http://keycloak/health/ready", String.class))
                .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        boolean result = systemAvailabilityChecker.isKeycloakAvailable();

        assertTrue(result);
    }

    @Test
    void shouldReturnFalseWhenKeycloakIsNotAvailable() {
        when(restTemplate.getForEntity("http://keycloak/health/ready", String.class))
                .thenReturn(new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE));

        boolean result = systemAvailabilityChecker.isKeycloakAvailable();

        assertFalse(result);
    }

    @Test
    void shouldWaitForKeycloakAvailability() {
        when(restTemplate.getForEntity("http://keycloak/health/ready", String.class))
                .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        boolean result = systemAvailabilityChecker.waitForKeycloakAvailability();

        assertTrue(result);
    }

    @Test
    void shouldReturnFalseWhenKeycloakMaxRetriesExceeded() {
        when(restTemplate.getForEntity("http://keycloak/health/ready", String.class))
                .thenThrow(new RuntimeException("Connection failed"));

        boolean result = systemAvailabilityChecker.waitForKeycloakAvailability();

        assertFalse(result);
    }
}
