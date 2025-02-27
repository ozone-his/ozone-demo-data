/*
 * Copyright © 2025, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.ozone_demo_data.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(
        properties = {
            "keycloak.serverUrl=http://localhost:8080/keycloak",
            "keycloak.demo.data.realm=master",
            "keycloak.demo.data.clientId=demo",
            "keycloak.demo.data.clientSecret=Admin123",
            "keycloak.healthcheck.maxRetries=3",
            "keycloak.healthcheck.retryDelayMillis=1000"
        })
class KeycloakConfigTest {

    @Autowired
    private KeycloakConfig keycloakConfig;

    @Test
    void shouldLoadKeycloakConfigurationCorrectly() {
        assertEquals("http://localhost:8080/keycloak", keycloakConfig.getServerUrl());
        assertEquals("master", keycloakConfig.getRealm());
        assertEquals("demo", keycloakConfig.getClientId());
        assertEquals("Admin123", keycloakConfig.getClientSecret());

        // Health check configuration
        assertEquals(3, keycloakConfig.getMaxRetries());
        assertEquals(1000L, keycloakConfig.getRetryDelayMillis());
    }
}
