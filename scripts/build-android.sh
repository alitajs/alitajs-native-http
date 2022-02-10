#!/bin/bash

PLUGIN_VERSION=$(grep '"version": ' package.json | awk '{print $2}' | tr -d '",')
# Export ENV variables used by Gradle for the plugin
export PLUGIN_VERSION

./android/gradlew clean build publishReleasePublicationToMavenLocal --max-workers 1 -b ./android/build.gradle -Pandroid.useAndroidX=true -Pandroid.enableJetifier=true