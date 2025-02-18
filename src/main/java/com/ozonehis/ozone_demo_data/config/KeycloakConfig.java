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
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * This class is used to configure Keycloak server connection. It contains the configuration details like URL, realm,
 * clientId, clientSecret etc. It also creates a Keycloak object which is used to interact with Keycloak server.
 */
@Setter
@Getter
@Configuration
@ConfigurationProperties(prefix = "keycloak.demo.data")
public class KeycloakConfig {

    @NotBlank
    private String serverUrl;

    @NotBlank
    private String realm;

    @NotBlank
    private String clientId;

    @NotBlank
    private String clientSecret;

    @Bean
    public Keycloak keycloak() {
        return KeycloakBuilder.builder()
                .serverUrl(getServerUrl())
                .realm(getRealm())
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .clientId(getClientId())
                .clientSecret(getClientSecret())
                .build();
    }
}
