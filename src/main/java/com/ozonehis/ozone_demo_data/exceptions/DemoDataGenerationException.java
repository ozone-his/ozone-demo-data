/*
 * Copyright © 2025, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.ozone_demo_data.exceptions;

public class DemoDataGenerationException extends RuntimeException {

    public DemoDataGenerationException(String message) {
        super(message);
    }

    public DemoDataGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
