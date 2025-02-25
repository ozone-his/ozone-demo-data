/*
 * Copyright © 2025, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.ozone_demo_data.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.ozonehis.ozone_demo_data.config.OpenmrsConfig;
import com.ozonehis.ozone_demo_data.util.SystemAvailabilityChecker;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
class DemoDataGenerationServiceTest {

    @Mock
    private SystemAvailabilityChecker systemAvailabilityChecker;

    @Mock
    private OpenmrsConfig openmrsConfig;

    @InjectMocks
    private DemoDataGenerationService service;

    @BeforeEach
    void setUp() {
        // Configure common mock behaviors
        when(systemAvailabilityChecker.waitForOpenMRSAvailability()).thenReturn(true);
        when(openmrsConfig.getUrl()).thenReturn("http://test-openmrs.com");
    }

    @Test
    void shouldCreateRequestBodyWithDefaultConfiguration() {
        service.numberOfDemoPatients = 50;

        Map<String, Object> requestBody = service.createRequestBody();

        assertNotNull(requestBody);
        assertEquals(50, requestBody.get("numberOfDemoPatients"));
        assertTrue((Boolean) requestBody.get("createIfNotExists"));
    }

    @Test
    void shouldBuildGenerateDemoDataUrl() {
        when(openmrsConfig.getUrl()).thenReturn("http://openmrs.example.com");

        String url = service.buildGenerateDemoDataUrl();

        assertEquals("http://openmrs.example.com/ws/rest/v1/referencedemodata/generate", url);
    }

    @Test
    void shouldReturnHeadersWithBasicAuthWhenOAuthIsDisabled() {
        // Configure basic auth scenario
        service.oauthEnabled = false;
        when(openmrsConfig.getUsername()).thenReturn("testuser");
        when(openmrsConfig.getPassword()).thenReturn("testpass");

        HttpHeaders headers = service.createAuthenticatedHeaders();

        assertEquals(MediaType.APPLICATION_JSON, headers.getContentType());
        assertTrue(headers.containsKey(HttpHeaders.AUTHORIZATION));
    }
}
