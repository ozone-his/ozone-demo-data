/*
 * Copyright © 2025, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.ozone_demo_data.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.ozonehis.ozone_demo_data.service.KeycloakUserService;
import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserCreationTaskTest {

    @Mock
    private KeycloakUserService keycloakUserService;

    @Mock
    private CountDownLatch latch;

    @InjectMocks
    private UserCreationTask userCreationTask;

    @Test
    void shouldCreateUsersSuccessfully() throws Exception {
        userCreationTask.executeAsync(latch);

        verify(keycloakUserService).createUsers();
        verify(latch).countDown();
    }

    @Test
    void shouldReturnEnabledValueFromConfiguration() {
        // Test when enabled is false
        ReflectionTestUtils.setField(userCreationTask, "enabled", false);
        assertFalse(userCreationTask.isEnabled());

        // Test when enabled is true
        ReflectionTestUtils.setField(userCreationTask, "enabled", true);
        assertTrue(userCreationTask.isEnabled());
    }

    @Test
    void shouldCountDownLatchWhenUserCreationFails() throws Exception {
        doThrow(new RuntimeException("Failed")).when(keycloakUserService).createUsers();

        assertThrows(RuntimeException.class, () -> userCreationTask.executeAsync(latch));
        verify(latch).countDown();
    }

    @Test
    void shouldWrapOriginalExceptionInRuntimeException() throws Exception {
        Exception originalException = new RuntimeException("Failed to initialize Keycloak users");
        doThrow(originalException).when(keycloakUserService).createUsers();

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> userCreationTask.executeAsync(latch));

        assertEquals("Failed to initialize Keycloak users", thrown.getMessage());
        assertEquals(originalException, thrown.getCause());
    }
}
