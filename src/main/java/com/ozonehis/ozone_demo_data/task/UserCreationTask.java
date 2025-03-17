/*
 * Copyright © 2025, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.ozone_demo_data.task;

import com.ozonehis.ozone_demo_data.service.KeycloakUserService;
import java.util.concurrent.CountDownLatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class UserCreationTask implements TaskExecutor {

    @Autowired
    private KeycloakUserService keycloakUserService;

    @Value("${keycloak.user-creation.enabled:false}")
    private boolean enabled;

    @Override
    public void executeAsync(CountDownLatch latch) {
        try {
            keycloakUserService.createUsers();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize Keycloak users", e);
        } finally {
            latch.countDown();
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
