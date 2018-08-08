#!/usr/bin/env bash
# Go to the root directory
cd ../
# Build the project
./gradlew build
# Build the docker image
docker build -t ardent-test -f Dockerfile.ardent.test .

# Go to the compose/test directory, which is where our test compose file is located
cd compose/test

# Compose up the application!
docker-compose up

# Go back to this directory
cd ../