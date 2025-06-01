# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository Overview

This is the Android client for ActivityWatch, an open-source time tracking application. The app consists of:
- A native Android app written in Kotlin
- An embedded Rust server (aw-server-rust) compiled as native libraries
- A web UI (aw-webui) served by the embedded server
- Usage tracking watchers that collect data about app usage

## Build Commands

### Prerequisites
```bash
# Install Rust with Android targets
./scripts/setup-rust-with-ndk.sh

# Initialize and update git submodules
git submodule update --init --recursive
```

### Common Development Commands

**Build the entire project:**
```bash
make build
```

**Build specific components:**
```bash
# Build aw-server-rust for all architectures
make aw-server-rust

# Build only for specific architecture
make aw-server-rust-arm64-v8a
make aw-server-rust-armeabi-v7a
make aw-server-rust-x86
make aw-server-rust-x86_64

# Build web UI
make aw-webui

# Build APK
make apk

# Build AAB (for Play Store)
make aab-release
```

**Run tests:**
```bash
# Unit tests
make test

# E2E tests (requires connected device/emulator)
make test-e2e
```

**Clean build:**
```bash
make clean
```

**Install on device:**
```bash
# Install debug APK
make install

# Install specific architecture APK
make install ARCH=arm64-v8a
```

## Architecture Overview

### Native Integration
The app integrates aw-server-rust through JNI:
- Rust server is compiled to .so libraries for each architecture
- `RustInterface.kt` provides the JNI bridge
- Libraries are loaded at runtime based on device architecture

### Data Collection
The app uses several watchers to collect usage data:
- **UsageStatsWatcher**: Uses Android's UsageStats API to track app usage
- **ChromeWatcher**: Monitors Chrome browser activity
- **AlarmReceiver**: Manages periodic data collection

### UI Components
- **MainActivity**: Main navigation and server lifecycle management
- **OnboardingActivity**: First-run setup and permissions
- **WebUIFragment**: Displays the aw-webui in a WebView
- **TestFragment**: Development tools and server status

### Data Storage
- Server stores data in `/data/data/net.activitywatch.android/files/`
- Preferences managed via `AWPreferences.kt`
- Bucket data models in `models/` directory

## Important Version Management

When releasing:
1. Update `versionName` and `versionCode` in `mobile/build.gradle`
2. `versionCode` must be incremented for each release (including test releases)
3. Use semantic versioning for `versionName` (e.g., 0.12.1)

## Testing on Different Devices

### Standard Android Devices
```bash
./gradlew installDebug
```

### Oculus Quest / Meta Quest
The app supports VR headsets through SideQuest:
- Build for ARM64: `make apk ARCH=arm64-v8a`
- Deploy via SideQuest or `adb install`

## Key Configuration Files

- **Network Security**: `mobile/src/main/res/xml/network_security_config.xml` - allows cleartext traffic to localhost
- **Accessibility Service**: `mobile/src/main/res/xml/accessibility_service_config.xml` - for enhanced activity tracking
- **Encrypted Files**: `.age` files contain encrypted secrets (keystore, API keys)

## Release Process

Releases are automated via GitHub Actions:
1. Push a tag matching `v*` pattern
2. CI builds and signs APKs/AABs
3. Creates draft GitHub release
4. Can deploy to Play Store via Fastlane

For manual Play Store deployment:
```bash
make play-upload-production
```

## Development Tips

- The embedded server runs on port 5600
- Access web UI at http://localhost:5600 when server is running
- Check server logs in TestFragment for debugging
- Use `adb logcat | grep ActivityWatch` to filter logs
- Test on minimum SDK 24 (Android 7.0) up to latest