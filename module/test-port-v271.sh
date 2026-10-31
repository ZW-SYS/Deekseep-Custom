#!/usr/bin/env bash
# Regression checks for the 1.8.0 port helpers. Requires a prior module-universal build.
set -e
cd "$(dirname "$0")"
source ../scripts/android-tools.sh
OUT="build/port-v271-test"
UNIVERSAL_CLASSES="../module-universal/build/classes"
if [[ ! -f "$UNIVERSAL_CLASSES/com/dsmod/probe/HostV271.class" ]]; then
    echo "Universal classes are missing; build module-universal before running tests" >&2
    exit 1
fi
rm -rf "$OUT"; mkdir -p "$OUT/stubs" "$OUT/classes"
javac -source 8 -target 8 -nowarn -d "$OUT/stubs" tests/v271stubs/*.java 2>/dev/null
# The stand-ins live in the default package, so they are loaded reflectively by simple name.
javac -source 8 -target 8 -nowarn -cp "$UNIVERSAL_CLASSES:$OUT/stubs:$ANDROID_JAR" \
    -d "$OUT/classes" tests/com/dsmod/probe/PortV271RegressionTest.java
java -cp "$OUT/classes:$UNIVERSAL_CLASSES:$OUT/stubs:$ANDROID_JAR" \
    com.dsmod.probe.PortV271RegressionTest
