/*
 * Copyright © 2025, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.ozone_demo_data.task;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskCoordinator {

    // This is a list of all TaskExecutor beans that are available in the application context.
    private final List<TaskExecutor> taskExecutors;

    private final ApplicationContext applicationContext;

    @PostConstruct
    public void executeTasks() {
        int numberOfTasks = taskExecutors.size();
        CountDownLatch latch = new CountDownLatch(numberOfTasks);

        taskExecutors.forEach(task -> {
            new Thread(() -> task.executeAsync(latch)).start();
        });

        new Thread(() -> {
                    try {
                        latch.await();
                        log.info("All tasks completed. Shutting down the application.");
                        SpringApplication.exit(applicationContext, () -> 0);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.error("Task execution interrupted", e);
                    }
                })
                .start();
    }
}
