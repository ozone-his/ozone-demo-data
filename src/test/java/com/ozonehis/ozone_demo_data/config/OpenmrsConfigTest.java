/*
 * Copyright © 2025, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.ozone_demo_data.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class OpenmrsConfigTest {

    @Autowired
    private OpenmrsConfig openmrsConfig;

    @Test
    void shouldLoadOpenMRSCredentialsCorrectly() {
        openmrsConfig.setUsername("admin");
        openmrsConfig.setPassword("Admin123");

        assertEquals("admin", openmrsConfig.getUsername());
        assertEquals("Admin123", openmrsConfig.getPassword());
    }

    @Test
    void shouldLoadOauthConfigurationCorrectly() {
        openmrsConfig.setEnabled(true);
        openmrsConfig.setClientId("testClient");
        openmrsConfig.setClientSecret("testSecret");

        assertTrue(openmrsConfig.isEnabled());
        assertEquals("testClient", openmrsConfig.getClientId());
        assertEquals("testSecret", openmrsConfig.getClientSecret());
    }

    @Test
    void shouldLoadOpenMRSHealthCheckConfigurationCorrectly() {
        openmrsConfig.setMaxRetries(3);
        openmrsConfig.setRetryDelayMillis(1000L);

        assertEquals(3, openmrsConfig.getMaxRetries());
        assertEquals(1000L, openmrsConfig.getRetryDelayMillis());
    }
}
