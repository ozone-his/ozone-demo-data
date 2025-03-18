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

import com.ozonehis.ozone_demo_data.service.DemoDataService;
import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DemoDataGenerationTaskTest {

    @Mock
    private DemoDataService demoDataService;

    @Mock
    private CountDownLatch latch;

    @InjectMocks
    private DemoDataGenerationTask task;

    @Test
    void shouldTriggerDemoDataAndCountdownLatch() throws Exception {
        task.executeAsync(latch);

        verify(demoDataService).triggerDemoData();
        verify(latch).countDown();
    }

    @Test
    void shouldReturnEnabledValueFromConfiguration() {
        // Test when enabled is false
        ReflectionTestUtils.setField(task, "enabled", false);
        assertFalse(task.isEnabled());

        // Test when enabled is true
        ReflectionTestUtils.setField(task, "enabled", true);
        assertTrue(task.isEnabled());
    }

    @Test
    void shouldCountdownLatchWhenDemoDataServiceThrowsException() throws Exception {
        doThrow(new RuntimeException("Service error")).when(demoDataService).triggerDemoData();

        assertThrows(RuntimeException.class, () -> task.executeAsync(latch));
        verify(latch).countDown();
    }

    @Test
    void shouldWrapServiceExceptionInRuntimeException() {
        Exception serviceException = new RuntimeException("Failed to trigger generate demo data");
        doThrow(serviceException).when(demoDataService).triggerDemoData();

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> task.executeAsync(latch));
        assertEquals("Failed to trigger generate demo data", thrown.getMessage());
        assertEquals(serviceException, thrown.getCause());
    }
}
