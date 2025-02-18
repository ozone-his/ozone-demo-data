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
    private RestTemplate restTemplate;

    @InjectMocks
    private SystemAvailabilityChecker systemAvailabilityChecker;

    @BeforeEach
    void setUp() {
        lenient().when(openmrsConfig.getUrl()).thenReturn("http://test-openmrs");
        lenient().when(openmrsConfig.getMaxRetries()).thenReturn(5);
        lenient().when(openmrsConfig.getRetryDelayMillis()).thenReturn(1000L);
    }

    @Test
    void shouldReturnTrueWhenSystemIsAvailable() {
        when(restTemplate.getForEntity("http://test-openmrs/health", String.class))
                .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        boolean result = systemAvailabilityChecker.isOpenMRSAvailable();

        assertTrue(result);
    }

    @Test
    void shouldReturnFalseWhenSystemIsNotAvailable() {
        when(restTemplate.getForEntity("http://test-openmrs/health", String.class))
                .thenThrow(new RuntimeException("Connection failed"));

        boolean result = systemAvailabilityChecker.isOpenMRSAvailable();

        assertFalse(result);
    }

    @Test
    void shouldWaitForOpenMRSAvailability() {
        when(restTemplate.getForEntity("http://test-openmrs/health", String.class))
                .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        boolean result = systemAvailabilityChecker.waitForOpenMRSAvailability();

        assertTrue(result);
    }

    @Test
    void shouldReturnFalseWhenMaxRetriesExceeded() {
        when(restTemplate.getForEntity("http://test-openmrs/health", String.class))
                .thenThrow(new RuntimeException("Connection failed"));

        boolean result = systemAvailabilityChecker.waitForOpenMRSAvailability();

        assertFalse(result);
    }
}
