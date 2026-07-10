#!/usr/bin/env bash
set -euo pipefail

SDK_ROOT=/opt/android-sdk
mkdir -p "$SDK_ROOT/cmdline-tools"

# Install prerequisites depending on distro
if command -v apt-get >/dev/null 2>&1; then
  export DEBIAN_FRONTEND=noninteractive
  apt-get update
  apt-get install -y --no-install-recommends curl unzip wget ca-certificates
elif command -v apk >/dev/null 2>&1; then
  apk add --no-cache curl unzip wget ca-certificates gcompat libc6-compat libstdc++
fi

cd "$SDK_ROOT/cmdline-tools"

echo "Downloading Android command-line tools..."
curl -sL -o cmdline-tools.zip https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
unzip -q cmdline-tools.zip
rm cmdline-tools.zip
# The zip creates a nested 'cmdline-tools' directory; move it to 'latest'
if [ -d cmdline-tools ]; then
  mv cmdline-tools latest || true
fi

# Ensure executables are runnable
if [ -d "$SDK_ROOT/cmdline-tools/latest/bin" ]; then
  chmod -R a+rx "$SDK_ROOT/cmdline-tools/latest/bin" || true
fi

export ANDROID_SDK_ROOT="$SDK_ROOT"
export PATH="$SDK_ROOT/cmdline-tools/latest/bin:$PATH"

echo "Accepting licenses and installing platform-tools, platform 34, build-tools..."
yes | "$SDK_ROOT/cmdline-tools/latest/bin/sdkmanager" --sdk_root="$SDK_ROOT" --licenses >/dev/null 2>&1 || true
"$SDK_ROOT/cmdline-tools/latest/bin/sdkmanager" --sdk_root="$SDK_ROOT" "platform-tools" "platforms;android-34" "build-tools;34.0.0"

# Persist environment for all users
cat > /etc/profile.d/android-sdk.sh <<'EOF'
export ANDROID_SDK_ROOT=/opt/android-sdk
export ANDROID_HOME=/opt/android-sdk
export PATH=$PATH:/opt/android-sdk/cmdline-tools/latest/bin:/opt/android-sdk/platform-tools
EOF

# Write local.properties in workspace so Gradle can find the SDK
WORKSPACE_DIR="${CODESPACE_VSCODE_FOLDER:-/workspaces/MCQ-Scanner-Checker}"
if [ -d "$WORKSPACE_DIR" ]; then
  echo "sdk.dir=$SDK_ROOT" > "$WORKSPACE_DIR/local.properties"
fi

# Ensure vscode user can use the SDK without extra steps
if id -u vscode >/dev/null 2>&1; then
  chown -R vscode:vscode "$SDK_ROOT" || true
fi

echo "Android SDK setup complete."
