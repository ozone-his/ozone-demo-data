/*
 * Copyright © 2025, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.ozone_demo_data.task;

import com.ozonehis.ozone_demo_data.service.DemoDataService;
import java.util.concurrent.CountDownLatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DemoDataGenerationTask implements TaskExecutor {

    @Autowired
    private DemoDataService demoDataService;

    @Value("${openmrs.demo-data.enabled:false}")
    private boolean enabled;

    @Override
    public void executeAsync(CountDownLatch latch) {
        try {
            demoDataService.triggerDemoData();
        } catch (Exception e) {
            throw new RuntimeException("Failed to trigger generate demo data", e);
        } finally {
            latch.countDown();
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
