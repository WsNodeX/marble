#!/usr/bin/env bash
# Build, demo, and (optionally) test Marble with nothing but the JDK.
set -e

JAVAC="${JAVAC:-javac}"
JAVA="${JAVA:-java}"

rm -rf build
mkdir -p build/main build/test

echo "== compiling main =="
"$JAVAC" -d build/main $(find src/main/java -name '*.java')

echo "== running demo =="
"$JAVA" -cp build/main marble.examples.Demo

echo "== compiling tests =="
"$JAVAC" -cp build/main -d build/test $(find src/test/java -name '*.java')

if [ -n "${JUNIT_JAR:-}" ] && [ -f "${JUNIT_JAR}" ]; then
  echo "== running tests =="
  "$JAVA" -cp "build/main:build/test:${JUNIT_JAR}" \
    org.junit.platform.console.ConsoleLauncher execute --scan-class-path build/test
else
  echo "Set JUNIT_JAR to a junit-platform-console-standalone jar to also run the test suite."
fi
