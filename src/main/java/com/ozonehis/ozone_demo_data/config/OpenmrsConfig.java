/*
 * Copyright © 2025, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.ozone_demo_data.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * This class is used to configure OpenMRS server connection. It contains the configuration details like URL, username,
 * password, OAuth details etc.
 */
@Setter
@Getter
@Configuration
public class OpenmrsConfig {

    @NotBlank
    @Value("${openmrs.server.url}")
    private String url;

    @Value("${openmrs.username}")
    private String username;

    @Value("${openmrs.password}")
    private String password;

    @NotBlank
    @Value("${openmrs.oauth.enabled}")
    private boolean enabled;

    @Value("${openmrs.oauth.client.id}")
    private String clientId;

    @Value("${openmrs.oauth.client.secret}")
    private String clientSecret;

    // Health check configuration
    @Value("${openmrs.healthcheck.max.retries}")
    private int maxRetries;

    @Value("${openmrs.healthcheck.retry.delay.millis}")
    private long retryDelayMillis;
}
