#!/bin/bash

set -e

echo "🚀 Setting up Android development environment..."

# Define Android SDK paths
export ANDROID_HOME="/usr/local/lib/android/sdk"
export ANDROID_SDK_ROOT="${ANDROID_HOME}"
export PATH="${PATH}:${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools"

# Android SDK versions - update ANDROID_PLATFORM_VERSION to match compileSdk in compose-highlight/build.gradle.kts
ANDROID_PLATFORM_VERSION="37"
ANDROID_BUILD_TOOLS_VERSION="36.0.0"
# Command Line Tools version 11076708 (released 2024-01) - check https://developer.android.com/studio#command-tools
CMDLINE_TOOLS_VERSION="11076708"
CMDLINE_TOOLS_URL="https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_TOOLS_VERSION}_latest.zip"

# Install Android SDK Command Line Tools if not present
if [ ! -d "${ANDROID_HOME}/cmdline-tools" ]; then
    echo "📥 Downloading Android SDK Command Line Tools (version ${CMDLINE_TOOLS_VERSION})..."
    sudo mkdir -p "${ANDROID_HOME}/cmdline-tools"
    cd /tmp
    wget -q "${CMDLINE_TOOLS_URL}" -O commandlinetools.zip
    unzip -q commandlinetools.zip
    sudo mv cmdline-tools "${ANDROID_HOME}/cmdline-tools/latest"
    rm commandlinetools.zip
    cd -

    # Set proper ownership and permissions
    sudo chown -R vscode:vscode "${ANDROID_HOME}"
    sudo chmod -R 755 "${ANDROID_HOME}"
fi

# Accept Android SDK licenses
echo "📝 Accepting Android SDK licenses..."
yes | sdkmanager --licenses || true

# Install required Android SDK components
echo "📦 Installing Android SDK components (API ${ANDROID_PLATFORM_VERSION}, Build Tools ${ANDROID_BUILD_TOOLS_VERSION})..."
sdkmanager "platform-tools" "platforms;android-${ANDROID_PLATFORM_VERSION}" "build-tools;${ANDROID_BUILD_TOOLS_VERSION}" || true

# Update SDK components
echo "🔄 Updating SDK components..."
sdkmanager --update || true

# Set proper permissions for Gradle wrapper
echo "🔧 Setting Gradle wrapper permissions..."
chmod +x ./gradlew

# Install Gradle dependencies (helps with IDE indexing)
echo "📚 Downloading Gradle dependencies..."
./gradlew --version

echo "✅ Android development environment setup complete!"
echo "📦 Build the library with: ./gradlew :compose-highlight:assembleDebug"
echo "📱 Build the sample app with: ./gradlew :sample:assembleDebug"
echo "🧪 Run tests with: ./gradlew :compose-highlight:test"
echo "🎨 Format code with: ./gradlew formatKotlin"
echo "🔍 Lint code with: ./gradlew lintKotlin"
