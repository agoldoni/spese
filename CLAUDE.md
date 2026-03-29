# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

"Spese" is a native Android app for personal expense management, built with Java 17, XML layouts, and Material Design 3. Currently in early stage (v1.0.0) with only scaffold code (MainActivity, InfoActivity, toolbar, menu). No business logic, database, or network calls yet.

## Build & Install Commands

```bash
# Build debug APK
./build.sh debug

# Build release APK
./build.sh release

# Clean build artifacts
./build.sh clean

# Direct Gradle commands
./gradlew assembleDebug
./gradlew assembleRelease

# Install debug APK on all connected devices
./install-all.sh

# Build + install in one step
./install-all.sh --build
```

APK output path: `app/build/outputs/apk/debug/app-debug.apk`

## Tech Stack

- **Language:** Java 17 (no Kotlin)
- **Android SDK:** minSdk 26, targetSdk/compileSdk 34
- **UI:** Material Design 3 via `com.google.android.material:material:1.11.0`
- **Build:** Gradle 8.2, AGP 8.2.2
- **No test framework configured** — testing is manual at this stage

## Architecture

Single-module Gradle project (`app/`). All source code lives under `app/src/main/java/com/spese/`. Activities use `AppCompatActivity` with `MaterialToolbar`. Navigation is intent-based (MainActivity -> InfoActivity via options menu). Strings are externalized in `res/values/strings.xml` (no hardcoded strings in code). Version is read from `BuildConfig.VERSION_NAME` (configured in `app/build.gradle`).

## Language

The project uses Italian for all user-facing strings, documentation, comments, and commit messages.

## Data Model

Tutte le entità del database devono usare **UUID** (`TEXT PRIMARY KEY`) come chiave primaria, generato con `java.util.UUID.randomUUID().toString()`. Non usare mai `INTEGER PRIMARY KEY AUTOINCREMENT`. Questo per predisporre l'app a una futura condivisione/sincronizzazione dati tra istanze diverse.

## Implementation Plans

Feature plans are stored in `docs/features/<feature-name>/implementation-plan.md`.
