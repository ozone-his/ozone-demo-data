/*
 * Copyright © 2025, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.ozone_demo_data.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ozonehis.ozone_demo_data.config.KeycloakConfig;
import com.ozonehis.ozone_demo_data.util.SystemAvailabilityChecker;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakUserService {

    @Setter
    @Value("${keycloak.users.json.path}")
    private String usersJsonPath;

    private final Keycloak keycloak;

    private final KeycloakConfig keycloakConfig;

    private final ObjectMapper objectMapper;

    private final SystemAvailabilityChecker systemAvailabilityChecker;

    @Data
    static class KeycloakUsers {

        private List<UserRepresentation> users;
    }

    RealmResource realmResource() {
        return keycloak.realm(keycloakConfig.getRealm());
    }

    public void createUsers() throws IOException {
        log.info("Starting user creation process from JSON file: {}", usersJsonPath);
        if (!systemAvailabilityChecker.waitForKeycloakAvailability()) {
            log.error("Keycloak is not available. Aborting user creation.");
            return;
        }

        KeycloakUsers users = loadUsersFromJson();
        log.info("Found {} users to create", users.getUsers().size());
        users.getUsers().forEach(this::createAndConfigureUser);
        log.info("Completed user creation process");
    }

    KeycloakUsers loadUsersFromJson() throws IOException {
        log.debug("Loading users from JSON file: {}", usersJsonPath);

        // Try loading from external file system first
        File externalFile = new File(usersJsonPath);
        if (externalFile.exists()) {
            log.debug("Loading users from external file system");
            return objectMapper.readValue(externalFile, KeycloakUsers.class);
        }

        // Fallback to classpath resource
        log.debug("External file not found, loading from classpath");
        ClassPathResource resource = new ClassPathResource(usersJsonPath);
        return objectMapper.readValue(resource.getInputStream(), KeycloakUsers.class);
    }

    void createAndConfigureUser(UserRepresentation user) {
        log.info("Processing user creation for username: {}", user.getUsername());
        Optional<String> userId = createKeycloakUser(user);

        if (userId.isPresent()) {
            log.debug("User {} created successfully with ID: {}", user.getUsername(), userId.get());

            if (user.getRealmRoles() != null && !user.getRealmRoles().isEmpty()) {
                log.debug(
                        "Assigning {} realm roles to user {}",
                        user.getRealmRoles().size(),
                        user.getUsername());
                assignRealmRoles(userId.get(), user.getRealmRoles());
            }

            if (user.getClientRoles() != null && !user.getClientRoles().isEmpty()) {
                log.debug(
                        "Assigning client roles from {} clients to user {}",
                        user.getClientRoles().size(),
                        user.getUsername());
                assignClientRoles(userId.get(), user.getClientRoles());
            }

            log.info("Successfully completed configuration for user: {}", user.getUsername());
        } else {
            log.warn("Failed to create user: {}. Skipping role assignments", user.getUsername());
        }
    }

    Optional<String> createKeycloakUser(UserRepresentation userRep) {
        UsersResource usersResource = realmResource().users();
        if (usersResource.search(userRep.getUsername()).isEmpty()) {
            try (var response = usersResource.create(userRep)) {
                if (response.getStatus() != 201) {
                    log.error("Failed to create user: {}", userRep.getUsername());
                    return Optional.empty();
                }
            }
        }

        return Optional.of(
                realmResource().users().search(userRep.getUsername()).get(0).getId());
    }

    void assignRealmRoles(String userId, List<String> realmRoles) {
        if (realmRoles == null || realmRoles.isEmpty()) return;

        log.debug("Starting realm role assignment for user ID: {}", userId);
        List<RoleRepresentation> roles = realmRoles.stream()
                .map(roleName -> realmResource().roles().get(roleName).toRepresentation())
                .toList();

        realmResource().users().get(userId).roles().realmLevel().add(roles);
        log.debug("Successfully assigned {} realm roles to user ID: {}", roles.size(), userId);
    }

    void assignClientRoles(String userId, Map<String, List<String>> clientRoles) {
        if (clientRoles == null || clientRoles.isEmpty()) return;

        log.debug("Starting client role assignment for user ID: {}", userId);
        clientRoles.forEach((clientId, roles) -> {
            String client =
                    realmResource().clients().findByClientId(clientId).get(0).getId();

            List<RoleRepresentation> clientRolesList = roles.stream()
                    .map(roleName -> realmResource()
                            .clients()
                            .get(client)
                            .roles()
                            .get(roleName)
                            .toRepresentation())
                    .toList();

            realmResource().users().get(userId).roles().clientLevel(client).add(clientRolesList);
            log.debug("Successfully assigned {} roles for client {} to user ID: {}", roles.size(), clientId, userId);
        });
    }
}
