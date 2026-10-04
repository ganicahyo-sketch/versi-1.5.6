#!/usr/bin/env sh
set -eu
GRADLE_VERSION="9.4.1"
GRADLE_HOME="${GRADLE_HOME:-}"
if [ -n "$GRADLE_HOME" ] && [ -x "$GRADLE_HOME/bin/gradle" ]; then
  exec "$GRADLE_HOME/bin/gradle" "$@"
fi
if command -v gradle >/dev/null 2>&1; then
  exec gradle "$@"
fi
CACHE_BASE="${GRADLE_USER_HOME:-$HOME/.gradle}/wrapper/dists/manual"
DIST="$CACHE_BASE/gradle-$GRADLE_VERSION"
BIN="$DIST/bin/gradle"
if [ ! -x "$BIN" ]; then
  mkdir -p "$DIST"
  TMP="$DIST/gradle.zip"
  URL="https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"
  echo "Gradle $GRADLE_VERSION belum tersedia; mengunduh..."
  if command -v curl >/dev/null 2>&1; then
    curl -fL --retry 3 --connect-timeout 15 -o "$TMP" "$URL"
  elif command -v wget >/dev/null 2>&1; then
    wget -O "$TMP" "$URL"
  else
    echo "curl/wget tidak tersedia." >&2
    exit 1
  fi
  unzip -q "$TMP" -d "$CACHE_BASE"
  rm -f "$TMP"
fi
exec "$BIN" "$@"
