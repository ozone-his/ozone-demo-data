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
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakUserService {
	
	private static final String USERS_JSON_PATH = "keycloak/users.json";
	
	private final Keycloak keycloak;
	
	private final KeycloakConfig keycloakConfig;
	
	private final ObjectMapper objectMapper;
	
	@PostConstruct
	public void initializeUsers() {
		try {
			createUsers();
		}
		catch (IOException e) {
			log.error("Failed to initialize Keycloak users", e);
		}
	}
	
	@Data
	private static class KeycloakUsers {
		
		private List<UserRepresentation> users;
	}
	
	private void createUsers() throws IOException {
		KeycloakUsers users = loadUsersFromJson();
		users.getUsers().forEach(this::createAndConfigureUser);
	}
	
	private KeycloakUsers loadUsersFromJson() throws IOException {
		ClassPathResource resource = new ClassPathResource(USERS_JSON_PATH);
		return objectMapper.readValue(resource.getInputStream(), KeycloakUsers.class);
	}
	
	private void createAndConfigureUser(UserRepresentation user) {
		UserRepresentation userRep = convertToUserRepresentation(user);
		String userId = createKeycloakUser(userRep);
		
		if (userId != null) {
			assignRealmRoles(userId, user.getRealmRoles());
			assignClientRoles(userId, user.getClientRoles());
		}
	}
	
	private String createKeycloakUser(UserRepresentation userRep) {
		try {
			keycloak.realm(keycloakConfig.getRealm()).users().create(userRep);
			
			return keycloak.realm(keycloakConfig.getRealm()).users().search(userRep.getUsername()).get(0).getId();
		}
		catch (Exception e) {
			log.error("Failed to create user: {}", userRep.getUsername(), e);
			return null;
		}
	}
	
	private void assignRealmRoles(String userId, List<String> realmRoles) {
		if (realmRoles == null || realmRoles.isEmpty())
			return;
		
		List<RoleRepresentation> roles = realmRoles.stream()
				.map(roleName -> keycloak.realm(keycloakConfig.getRealm()).roles().get(roleName).toRepresentation())
				.toList();
		
		keycloak.realm(keycloakConfig.getRealm()).users().get(userId).roles().realmLevel().add(roles);
	}
	
	private void assignClientRoles(String userId, Map<String, List<String>> clientRoles) {
		if (clientRoles == null || clientRoles.isEmpty())
			return;
		
		clientRoles.forEach((clientId, roles) -> {
			String client = keycloak.realm(keycloakConfig.getRealm()).clients().findByClientId(clientId).get(0).getId();
			
			List<RoleRepresentation> clientRolesList = roles.stream()
					.map(roleName -> keycloak.realm(keycloakConfig.getRealm()).clients().get(client).roles().get(roleName)
							.toRepresentation()).toList();
			
			keycloak.realm(keycloakConfig.getRealm()).users().get(userId).roles().clientLevel(client).add(clientRolesList);
		});
	}
	
	private UserRepresentation convertToUserRepresentation(UserRepresentation user) {
		return objectMapper.convertValue(user, UserRepresentation.class);
	}
}
