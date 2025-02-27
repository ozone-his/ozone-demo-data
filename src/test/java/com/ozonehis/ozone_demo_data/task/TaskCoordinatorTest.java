/*
 * Copyright © 2025, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.ozone_demo_data.task;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;

@ExtendWith(MockitoExtension.class)
class TaskCoordinatorTest {

    @Mock
    private TaskExecutor mockTask1;

    @Mock
    private TaskExecutor mockTask2;

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private ApplicationReadyEvent applicationReadyEvent;

    @Test
    void shouldExecuteAllTasksAndShutdown() throws InterruptedException {
        // Setup task behavior only for this test
        doAnswer(invocation -> {
                    Thread.sleep(100);
                    ((CountDownLatch) invocation.getArgument(0)).countDown();
                    return null;
                })
                .when(mockTask1)
                .executeAsync(any());

        doAnswer(invocation -> {
                    Thread.sleep(100);
                    ((CountDownLatch) invocation.getArgument(0)).countDown();
                    return null;
                })
                .when(mockTask2)
                .executeAsync(any());

        TaskCoordinator taskCoordinator = new TaskCoordinator(Arrays.asList(mockTask1, mockTask2), applicationContext);
        taskCoordinator.onApplicationEvent(applicationReadyEvent);

        Thread.sleep(300);

        verify(mockTask1, times(1)).executeAsync(any());
        verify(mockTask2, times(1)).executeAsync(any());
    }

    @Test
    void shouldHandleEmptyTaskList() throws InterruptedException {
        TaskCoordinator taskCoordinator = new TaskCoordinator(Collections.emptyList(), applicationContext);
        taskCoordinator.onApplicationEvent(applicationReadyEvent);

        Thread.sleep(100);

        verifyNoInteractions(mockTask1, mockTask2);
    }

    @Test
    void shouldHandleTaskInterruption() throws InterruptedException {
        doAnswer(invocation -> {
                    throw new InterruptedException("Task interrupted");
                })
                .when(mockTask1)
                .executeAsync(any());

        TaskCoordinator taskCoordinator = new TaskCoordinator(Arrays.asList(mockTask1), applicationContext);
        taskCoordinator.onApplicationEvent(applicationReadyEvent);

        Thread.sleep(200);

        verify(mockTask1, times(1)).executeAsync(any());
    }
}
