#!/usr/bin/env bash
set -e

SDK_ROOT=/opt/android-sdk
sudo mkdir -p "$SDK_ROOT/cmdline-tools"
sudo chown -R $(whoami) "$SDK_ROOT"
cd "$SDK_ROOT/cmdline-tools"

curl -sL -o cmdline-tools.zip https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
unzip -q cmdline-tools.zip
rm cmdline-tools.zip
mkdir -p latest
mv bin lib NOTICE.txt source.properties latest/ 2>/dev/null || true

export ANDROID_SDK_ROOT="$SDK_ROOT"
export ANDROID_HOME="$SDK_ROOT"
export PATH="$SDK_ROOT/cmdline-tools/latest/bin:$PATH"

yes | sdkmanager --licenses > /dev/null 2>&1 || true
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"

echo "export ANDROID_SDK_ROOT=$SDK_ROOT" >> ~/.bashrc
echo "export ANDROID_HOME=$SDK_ROOT" >> ~/.bashrc
echo "export PATH=\$PATH:$SDK_ROOT/cmdline-tools/latest/bin:$SDK_ROOT/platform-tools" >> ~/.bashrc

echo "sdk.dir=$SDK_ROOT" > /workspaces/MCQ-Scanner-Checker/local.properties

echo "Android SDK setup complete."
