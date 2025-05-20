#!/bin/bash

# Make script executable
chmod +x gradlew

# Accept Android licenses
echo y | $ANDROID_HOME/tools/bin/sdkmanager "build-tools;33.0.0" > /dev/null

# Display Java version
echo "Java version:"
java -version
