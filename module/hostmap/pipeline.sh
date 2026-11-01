#!/bin/bash
# regenerate member map + runtime table from /tmp/cmap2.json and run JVM hook verifier
set -e
cd /workspace/work/Deekseep/host/map
python3 members.py /tmp/cmap2.json 2>&1 | grep -v DEBUG
python3 genmap.py
cp /tmp/hostmap287.txt.gz /tmp/vh/res/
cp /tmp/hostmap287.txt.gz /workspace/work/Deekseep/module/hostmap/
cd /tmp/vh; source /workspace/work/tools/env.sh
CP="out:/tmp/jc/classes:res:/workspace/work/tools/sdk/platforms/android-35/android.jar"
J="java -XX:+UnlockDiagnosticVMOptions -XX:-BytecodeVerificationRemote -XX:-BytecodeVerificationLocal -cp $CP com.dsmod.probe.VerifyHooks"
timeout 60 $J /tmp/ds287.jar 287 v > v287.txt 2>&1 || true
grep -v LOG v287.txt > r287.txt
python3 cmp.py | awk '{print $1}' | sort | uniq -c
