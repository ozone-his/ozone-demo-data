/*
 * Copyright © 2025, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.ozone_demo_data.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ozonehis.ozone_demo_data.config.KeycloakConfig;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.admin.client.resource.RoleScopeResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.InjectMocks;
import org.mockito.Mock;

class KeycloakUserServiceTest {

    @Mock
    private Keycloak keycloak;

    @Mock
    private KeycloakConfig keycloakConfig;

    @InjectMocks
    private KeycloakUserService keycloakUserService;

    private static AutoCloseable mockCloser;

    private static final String USERS_JSON_PATH = "keycloak/users.json";

    @BeforeEach
    void setUp() {
        mockCloser = openMocks(this);
        keycloakUserService = new KeycloakUserService(keycloak, keycloakConfig, new ObjectMapper());
        keycloakUserService.setUsersJsonPath(USERS_JSON_PATH);
    }

    @AfterAll
    static void tearDown() throws Exception {
        mockCloser.close();
    }

    @Test
    void shouldLoadUsersFromJsonFile() throws IOException {
        KeycloakUserService.KeycloakUsers users = keycloakUserService.loadUsersFromJson();

        assertNotNull(users);
        assertEquals(2, users.getUsers().size());

        assertTrue(users.getUsers().stream().anyMatch(user -> "jdoe".equals(user.getUsername())));
        assertTrue(users.getUsers().stream().anyMatch(user -> "mj".equals(user.getUsername())));
    }

    @Test
    void shouldThrowExceptionWhenJsonFileIsMissing() {
        keycloakUserService.setUsersJsonPath("nonexistent/path.json");
        assertThrows(FileNotFoundException.class, () -> keycloakUserService.loadUsersFromJson());
    }

    @Test
    @Disabled("This test is disabled as it requires a running Keycloak instance")
    void shouldCreateUserSuccessfully() {
        UserRepresentation user = new UserRepresentation();
        user.setUsername("testUser");

        RealmResource realmResource = mock(RealmResource.class);
        when(keycloak.realm(anyString())).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(mock(UsersResource.class));
        when(realmResource.users().search(anyString())).thenReturn(List.of(user));

        Optional<String> result = keycloakUserService.createKeycloakUser(user);

        assertTrue(result.isPresent());
        verify(realmResource.users()).create(user);
    }

    @Test
    void shouldHandleFailureWhenCreatingUser() {
        UserRepresentation user = new UserRepresentation();
        user.setUsername("testUser");

        RealmResource realmResource = mock(RealmResource.class);
        when(keycloak.realm(anyString())).thenReturn(realmResource);
        when(realmResource.users()).thenThrow(new RuntimeException("Failed to create user"));

        Optional<String> result = keycloakUserService.createKeycloakUser(user);

        assertTrue(result.isEmpty());
    }

    @Test
    @Disabled
    void shouldAssignRealmRolesSuccessfully() {
        String userId = "testUserId";
        List<String> roles = List.of("role1", "role2");

        RealmResource realmResource = mock(RealmResource.class);
        UsersResource usersResource = mock(UsersResource.class);
        RolesResource rolesResource = mock(RolesResource.class);
        RoleResource roleResource = mock(RoleResource.class);
        UserResource userResource = mock(UserResource.class);
        RoleMappingResource roleMappingResource = mock(RoleMappingResource.class);
        RoleScopeResource roleScopeResource = mock(RoleScopeResource.class);

        when(keycloak.realm(anyString())).thenReturn(realmResource);
        when(realmResource.roles()).thenReturn(rolesResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(userId)).thenReturn(userResource);
        when(userResource.roles()).thenReturn(roleMappingResource);
        when(roleMappingResource.realmLevel()).thenReturn(roleScopeResource);
        when(rolesResource.get(anyString())).thenReturn(roleResource);
        when(roleResource.toRepresentation()).thenReturn(new RoleRepresentation());

        keycloakUserService.assignRealmRoles(userId, roles);

        verify(roleScopeResource).add(any());
    }

    @Test
    void shouldSkipAssignmentWhenRealmRolesEmpty() {
        String userId = "testUserId";
        List<String> roles = Collections.emptyList();

        keycloakUserService.assignRealmRoles(userId, roles);

        verify(keycloak, never()).realm(anyString());
    }

    @Test
    @Disabled
    void shouldAssignClientRolesSuccessfully() {
        String userId = "testUserId";
        Map<String, List<String>> clientRoles = Map.of("client1", List.of("role1", "role2"));

        RealmResource realmResource = mock(RealmResource.class);
        ClientsResource clientsResource = mock(ClientsResource.class);
        ClientResource clientResource = mock(ClientResource.class);
        RolesResource rolesResource = mock(RolesResource.class);
        RoleResource roleResource = mock(RoleResource.class);

        when(keycloak.realm(anyString())).thenReturn(realmResource);
        when(realmResource.clients()).thenReturn(clientsResource);
        when(clientsResource.findByClientId(anyString())).thenReturn(List.of(new ClientRepresentation()));
        when(realmResource.clients().get(anyString())).thenReturn(clientResource);
        when(clientResource.roles()).thenReturn(rolesResource);
        when(rolesResource.get(anyString())).thenReturn(roleResource);

        keycloakUserService.assignClientRoles(userId, clientRoles);

        verify(realmResource.users().get(userId).roles().clientLevel(anyString()))
                .add(any());
    }

    @Test
    void shouldSkipAssignmentWhenClientRolesEmpty() {
        String userId = "testUserId";
        Map<String, List<String>> clientRoles = Collections.emptyMap();

        keycloakUserService.assignClientRoles(userId, clientRoles);

        verify(keycloak, never()).realm(anyString());
    }
}
