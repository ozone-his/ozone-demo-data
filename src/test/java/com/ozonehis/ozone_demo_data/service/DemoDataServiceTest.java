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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestTemplate;

@ExtendWith(SpringExtension.class)
class DemoDataServiceTest {

    @Mock
    private SystemAvailabilityChecker systemAvailabilityChecker;

    @Mock
    private OpenmrsConfig openmrsConfig;

    @InjectMocks
    private DemoDataService service;

    @Mock
    private RestTemplate restTemplate;

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

        HttpHeaders headers = service.createAuthenticationHeaders();

        assertEquals(MediaType.APPLICATION_JSON, headers.getContentType());
        assertTrue(headers.containsKey(HttpHeaders.AUTHORIZATION));
    }

    @Test
    void shouldUpdateCreateDemoPatientsOnNextStartupSettingSettingIfExists() {
        when(openmrsConfig.getUrl()).thenReturn("http://test-openmrs.com");
        Map<String, Object> settingResponse = new HashMap<>();
        List<Map<String, Object>> results = List.of(Map.of("uuid", "test-uuid"));
        settingResponse.put("results", results);

        when(restTemplate.exchange(
                        contains("/systemsetting/?q=referencedemodata.createDemoPatientsOnNextStartup"),
                        eq(HttpMethod.GET),
                        any(),
                        eq(Map.class)))
                .thenReturn(ResponseEntity.ok(settingResponse));

        when(restTemplate.exchange(contains("/systemsetting/test-uuid"), eq(HttpMethod.POST), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok().build());

        service.updateCreateDemoPatientsOnNextStartupSetting();

        verify(restTemplate)
                .exchange(contains("/systemsetting/test-uuid"), eq(HttpMethod.POST), any(), eq(String.class));
    }

    @Test
    void shouldHandleEmptyResultsWhenCreateDemoPatientsOnNextStartupSettingSettingDoesNotExist() {
        when(openmrsConfig.getUrl()).thenReturn("http://test-openmrs.com");
        Map<String, Object> settingResponse = new HashMap<>();
        settingResponse.put("results", List.of());

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(settingResponse));

        service.updateCreateDemoPatientsOnNextStartupSetting();

        verify(restTemplate, never())
                .exchange(contains("/systemsetting/"), eq(HttpMethod.POST), any(), eq(String.class));
    }

    @Test
    void shouldHandleFailedGetCreateDemoPatientsOnNextStartupSettingRequest() {
        when(openmrsConfig.getUrl()).thenReturn("http://test-openmrs.com");
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(Map.class)))
                .thenReturn(ResponseEntity.badRequest().build());

        service.updateCreateDemoPatientsOnNextStartupSetting();

        verify(restTemplate, never())
                .exchange(contains("/systemsetting/"), eq(HttpMethod.POST), any(), eq(String.class));
    }

    @Test
    void shouldHandleNullResponseBody() {
        when(openmrsConfig.getUrl()).thenReturn("http://test-openmrs.com");
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(Map.class)))
                .thenReturn(ResponseEntity.ok().build());

        service.updateCreateDemoPatientsOnNextStartupSetting();

        verify(restTemplate, never())
                .exchange(contains("/systemsetting/"), eq(HttpMethod.POST), any(), eq(String.class));
    }
}
