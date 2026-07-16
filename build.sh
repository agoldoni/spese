#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$PROJECT_DIR"

# --- Configurazione ---
ANDROID_SDK="${ANDROID_HOME:-$HOME/Android/Sdk}"
export ANDROID_HOME="$ANDROID_SDK"
BUILD_TYPE="${1:-debug}"   # debug | release

# Verifica Android SDK
if [ ! -d "$ANDROID_SDK" ]; then
    echo "[ERRORE] Android SDK non trovato in: $ANDROID_SDK"
    echo "         Imposta la variabile ANDROID_HOME oppure installa Android Studio."
    exit 1
fi

# --- Selezione JDK tramite sdkman ---
# Il progetto richiede Java 17: JDK piu' recenti (es. 25) rompono il lint di AGP
# 8.2.2 (IllegalArgumentException sul parsing della versione) e GraalVM rompe
# JdkImageTransform via jlink. La versione esatta e' fissata in .sdkmanrc.
SDKMAN_DIR="${SDKMAN_DIR:-$HOME/.sdkman}"
SDKMAN_INIT="$SDKMAN_DIR/bin/sdkman-init.sh"

if [ -f "$SDKMAN_INIT" ]; then
    # sdkman non e' compatibile con 'set -u': va disattivato attorno alle sue chiamate.
    set +u
    # shellcheck source=/dev/null
    source "$SDKMAN_INIT"
    # 'env install' scarica il JDK di .sdkmanrc se manca, 'env' lo attiva in questa shell.
    if ! { sdk env install && sdk env; }; then
        set -u
        echo "[ERRORE] sdkman non e' riuscito a selezionare il JDK indicato in .sdkmanrc."
        exit 1
    fi
    set -u
    export JAVA_HOME
    echo "[INFO] JDK: $JAVA_HOME"
else
    echo "[ATTENZIONE] sdkman non trovato in $SDKMAN_DIR: uso il JDK di sistema."
    echo "             Installalo da https://sdkman.io per la selezione automatica."
fi

# Rete di sicurezza: senza sdkman il JDK di sistema potrebbe non essere adatto.
# Si controlla lo stesso java che usera' gradlew, cioe' quello di JAVA_HOME se impostata.
if [ -n "${JAVA_HOME:-}" ]; then
    JAVA_CMD="$JAVA_HOME/bin/java"
else
    JAVA_CMD="java"
fi
JAVA_VERSION_OUTPUT="$("$JAVA_CMD" -version 2>&1)"
JAVA_MAJOR="$(echo "$JAVA_VERSION_OUTPUT" | head -1 | sed -E 's/.*version "([0-9]+).*/\1/')"
if [ "$JAVA_MAJOR" != "17" ]; then
    echo "[ERRORE] JDK non compatibile: rilevata major '$JAVA_MAJOR'."
    echo "         Il progetto richiede Java 17 (AGP 8.2.2 non supporta JDK piu' recenti)."
    exit 1
fi
if echo "$JAVA_VERSION_OUTPUT" | grep -qi "graalvm"; then
    echo "[ATTENZIONE] JDK GraalVM rilevato: il suo 'jlink' puo' far fallire"
    echo "             JdkImageTransform. In caso di errore usa la Temurin 17 di .sdkmanrc."
fi

# Installa gradlew se non presente
if [ ! -f "./gradlew" ]; then
    echo "[INFO] gradlew non trovato, lo scarico..."
    gradle wrapper --gradle-version 8.2
fi

chmod +x ./gradlew

echo "[INFO] Build type: $BUILD_TYPE"

case "$BUILD_TYPE" in
    debug)
        echo "[INFO] Avvio build debug..."
        ./gradlew assembleDebug
        APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
        ;;
    release)
        # Verifica che le credenziali di firma siano disponibili
        KEYSTORE_FILE="${KEYSTORE_FILE:-$HOME/.android/release-key.jks}"
        if [ ! -f "$KEYSTORE_FILE" ]; then
            echo "[ERRORE] Keystore non trovato: $KEYSTORE_FILE"
            echo "         Imposta KEYSTORE_FILE per un percorso diverso."
            exit 1
        fi
        if [ -z "${KEYSTORE_PASSWORD:-}" ] || [ -z "${KEY_PASSWORD:-}" ]; then
            echo "[ERRORE] Per il build release servono le variabili d'ambiente:"
            echo "         export KEYSTORE_PASSWORD=<password>"
            echo "         export KEY_ALIAS=<alias>        (default: release)"
            echo "         export KEY_PASSWORD=<password>"
            exit 1
        fi
        echo "[INFO] Avvio build release..."
        ./gradlew assembleRelease
        APK_PATH="app/build/outputs/apk/release/app-release.apk"
        ;;
    clean)
        echo "[INFO] Pulizia progetto..."
        ./gradlew clean
        echo "[OK] Clean completato."
        exit 0
        ;;
    *)
        echo "[ERRORE] Build type non valido: '$BUILD_TYPE'"
        echo "         Uso: $0 [debug|release|clean]"
        exit 1
        ;;
esac

if [ -f "$APK_PATH" ]; then
    echo ""
    echo "[OK] Build completata con successo!"
    echo "     APK: $PROJECT_DIR/$APK_PATH"
else
    echo "[ERRORE] APK non trovato. Controlla i log sopra."
    exit 1
fi
