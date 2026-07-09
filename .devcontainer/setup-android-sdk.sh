#!/usr/bin/env bash
set -e

SDK_ROOT=/opt/android-sdk
mkdir -p "$SDK_ROOT/cmdline-tools"
cd "$SDK_ROOT/cmdline-tools"

echo "Downloading Android command-line tools..."
curl -sL -o cmdline-tools.zip https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
unzip -q cmdline-tools.zip
rm cmdline-tools.zip
mv cmdline-tools latest

export ANDROID_SDK_ROOT="$SDK_ROOT"
export PATH="$SDK_ROOT/cmdline-tools/latest/bin:$PATH"

echo "Installing platform-tools, platform 34, build-tools..."
yes | sdkmanager --licenses > /dev/null 2>&1 || true
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"

echo "export ANDROID_SDK_ROOT=$SDK_ROOT" >> ~/.bashrc
echo "export ANDROID_HOME=$SDK_ROOT" >> ~/.bashrc
echo "export PATH=\$PATH:$SDK_ROOT/cmdline-tools/latest/bin:$SDK_ROOT/platform-tools" >> ~/.bashrc

WORKSPACE_DIR="${CODESPACE_VSCODE_FOLDER:-$(pwd)}"
if [ -d "$WORKSPACE_DIR" ]; then
  echo "sdk.dir=$SDK_ROOT" > "$WORKSPACE_DIR/local.properties"
fi

echo "Android SDK setup complete."
