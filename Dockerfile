#
# Copyright © 2025, Ozone HIS <info@ozone-his.com>
#
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#

FROM eclipse-temurin:17-jre-jammy
LABEL maintainer="ozone-his.com"
# Set the working directory to /app
WORKDIR /app
# Copy the JAR file into the container at /app
COPY target/ozone-demo-data.jar /app/ozone-demo-data.jar

# Expose port 8080 for the application
EXPOSE 8080
# Set the command to run the application
CMD ["java", "-jar", "/app/ozone-demo-data.jar"]
