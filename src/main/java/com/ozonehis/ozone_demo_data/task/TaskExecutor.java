/*
 * Copyright © 2025, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.ozone_demo_data.task;

import java.util.concurrent.CountDownLatch;

/**
 * This interface defines the contract for executing tasks.
 */
public interface TaskExecutor {

    /**
     * Executes the task.
     *
     * @param latch the countdown latch to signal when the task is complete
     */
    void executeAsync(CountDownLatch latch);

    /**
     * Checks if the task is enabled.
     *
     * @return true if the task is enabled, false otherwise
     */
    boolean isEnabled();
}
