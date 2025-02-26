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
import com.ozonehis.ozone_demo_data.util.SystemAvailabilityChecker;
import jakarta.ws.rs.core.Response;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
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

    @Mock
    private SystemAvailabilityChecker systemAvailabilityChecker;

    @InjectMocks
    private KeycloakUserService keycloakUserService;

    @Mock
    private RealmResource realmResource;

    private static AutoCloseable mockCloser;

    private static final String USERS_JSON_PATH = "keycloak/users.json";

    @BeforeEach
    void setUp() {
        mockCloser = openMocks(this);
        keycloakUserService =
                new KeycloakUserService(keycloak, keycloakConfig, new ObjectMapper(), systemAvailabilityChecker);
        keycloakUserService.setUsersJsonPath(USERS_JSON_PATH);

        when(keycloakConfig.getRealm()).thenReturn("test-realm");
        when(keycloakUserService.realmResource()).thenReturn(realmResource);
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
    void shouldCreateUserSuccessfully() {
        UserRepresentation user = new UserRepresentation();
        user.setId("testUserId");
        user.setUsername("testUser");

        UsersResource usersResource = mock(UsersResource.class);
        Response createResponse = mock(Response.class);

        when(realmResource.users()).thenReturn(usersResource);
        // Return empty list first time to indicate user doesn't exist
        when(usersResource.search(user.getUsername()))
                .thenReturn(Collections.emptyList())
                .thenReturn(List.of(user));
        when(usersResource.create(any(UserRepresentation.class))).thenReturn(createResponse);
        when(createResponse.getStatus()).thenReturn(201);

        Optional<String> result = keycloakUserService.createKeycloakUser(user);

        assertTrue(result.isPresent());
        verify(usersResource).create(user);
    }

    @Test
    void shouldHandleFailureWhenCreatingUser() {
        UserRepresentation user = new UserRepresentation();
        user.setUsername("testUser");

        RealmResource realmResource = mock(RealmResource.class);
        when(keycloak.realm(anyString())).thenReturn(realmResource);
        when(realmResource.users()).thenThrow(new RuntimeException("Failed to create user"));

        assertThrows(
                RuntimeException.class, () -> keycloakUserService.createKeycloakUser(user), "Failed to create user");
    }

    @Test
    void shouldAssignRealmRolesSuccessfully() {
        String userId = "testUserId";
        List<String> roles = List.of("role1", "role2");

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
    void shouldAssignClientRolesSuccessfully() {
        String userId = "testUserId";
        String clientId = "client1";
        Map<String, List<String>> clientRoles = Map.of(clientId, List.of("role1", "role2"));

        RealmResource realmResource = mock(RealmResource.class);
        UsersResource usersResource = mock(UsersResource.class);
        UserResource userResource = mock(UserResource.class);
        ClientsResource clientsResource = mock(ClientsResource.class);
        ClientResource clientResource = mock(ClientResource.class);
        RolesResource rolesResource = mock(RolesResource.class);
        RoleResource roleResource = mock(RoleResource.class);
        RoleMappingResource roleMappingResource = mock(RoleMappingResource.class);
        RoleScopeResource roleScopeResource = mock(RoleScopeResource.class);

        ClientRepresentation clientRep = new ClientRepresentation();
        clientRep.setId("client-id-123");
        clientRep.setClientId(clientId);

        when(keycloak.realm(anyString())).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(realmResource.clients()).thenReturn(clientsResource);
        when(usersResource.get(userId)).thenReturn(userResource);
        when(userResource.roles()).thenReturn(roleMappingResource);
        when(clientsResource.findByClientId(clientId)).thenReturn(List.of(clientRep));
        when(clientsResource.get(clientRep.getId())).thenReturn(clientResource);
        when(clientResource.roles()).thenReturn(rolesResource);
        when(rolesResource.get(anyString())).thenReturn(roleResource);
        when(roleResource.toRepresentation()).thenReturn(new RoleRepresentation());
        when(roleMappingResource.clientLevel(anyString())).thenReturn(roleScopeResource);

        keycloakUserService.assignClientRoles(userId, clientRoles);

        verify(roleScopeResource).add(any());
    }

    @Test
    void shouldSkipAssignmentWhenClientRolesEmpty() {
        String userId = "testUserId";
        Map<String, List<String>> clientRoles = Collections.emptyMap();

        keycloakUserService.assignClientRoles(userId, clientRoles);

        verify(keycloak, never()).realm(anyString());
    }
}
