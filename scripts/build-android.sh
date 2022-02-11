#!/bin/bash

publish_plugin() {
    PLUGIN_VERSION=$(grep '"version": ' package.json | awk '{print $2}' | tr -d '",')
    # Export ENV variables used by Gradle for the plugin
    export PLUGIN_VERSION
    export CAPACITOR_VERSION

    ./android/gradlew clean build publishReleasePublicationToMavenLocal --max-workers 1 -b ./android/build.gradle -Pandroid.useAndroidX=true -Pandroid.enableJetifier=true
}

# Get the latest version of Capacitor
CAPACITOR_PACKAGE_JSON="https://raw.githubusercontent.com/ionic-team/capacitor/main/android/package.json"
CAPACITOR_VERSION=$(curl -s $CAPACITOR_PACKAGE_JSON | awk -F\" '/"version":/ {print $4}')

# Don't continue if there was a problem getting the latest version of Capacitor
if [[ $CAPACITOR_VERSION ]]; then
    printf %"s\n\n" "Attempting to publish new plugins with dependency on Capacitor Version $CAPACITOR_VERSION"
else
    printf %"s\n\n" "Error resolving latest Capacitor version from $CAPACITOR_PACKAGE_JSON"
    exit 1
fi

# Get latest com.capacitorjs:core XML version info
CAPACITOR_PUBLISHED_URL="https://repo1.maven.org/maven2/com/capacitorjs/core/maven-metadata.xml"
CAPACITOR_PUBLISHED_DATA=$(curl -s $CAPACITOR_PUBLISHED_URL)
CAPACITOR_PUBLISHED_VERSION="$(perl -ne 'print and last if s/.*<latest>(.*)<\/latest>.*/\1/;' <<< $CAPACITOR_PUBLISHED_DATA)"

# Check if we need to publish a new native version of the Capacitor Android library
if [[ "$CAPACITOR_VERSION" != "$CAPACITOR_PUBLISHED_VERSION" ]]; then
    printf %"s\n" "Publish Capacitor Core first! The latest published Android library version $CAPACITOR_PUBLISHED_VERSION in MavenCentral is outdated. There is an unpublished version $CAPACITOR_VERSION in ionic-team/capacitor."
    exit 1
else
    # Capacitor version in MavenCentral is up to date, continue publishing the native Capacitor Plugins
    printf %"s\n\n" "Latest native Capacitor Android library is version $CAPACITOR_PUBLISHED_VERSION and is up to date, continuing with plugin publishing..."

    publish_plugin
    
fi

