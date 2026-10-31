#!/usr/bin/env bash
# Sandbox build: Open debug APK, signature stripped + zipalign. Recreates /tmp tooling if wiped.
set -e
source /workspace/work/tools/env.sh
ROOT=/workspace/work/Deekseep
G=/tmp/gradle-9.8.2
if [ ! -x $G/bin/gradle ]; then
  (cd /tmp && curl -sL -o gradle.zip https://services.gradle.org/distributions/gradle-9.8.2-bin.zip && unzip -q -o gradle.zip -d /tmp)
fi
mkdir -p /tmp/gh
python3 - <<'PY'
import os,urllib.parse as u
p=u.urlparse(os.environ.get('HTTPS_PROXY') or os.environ.get('https_proxy'))
user=u.unquote(p.username or ''); pw=u.unquote(p.password or '')
L=[]
for s in ('http','https'):
    L+=[f"systemProp.{s}.proxyHost={p.hostname}",f"systemProp.{s}.proxyPort={p.port}",f"systemProp.{s}.proxyUser={user}",f"systemProp.{s}.proxyPassword={pw}"]
L+=["systemProp.jdk.http.auth.tunneling.disabledSchemes=","systemProp.jdk.http.auth.proxying.disabledSchemes="]
open('/tmp/gh/gradle.properties','w').write("\n".join(L)+"\n")
PY
chmod 600 /tmp/gh/gradle.properties
export GRADLE_USER_HOME=/tmp/gh
BT=$ANDROID_SDK_ROOT/build-tools/35.0.0
cd $ROOT/compose-launcher
$G/bin/gradle --no-daemon -PprotectedBuild=false -PlocalApiIncluded=false \
  -Pandroid.aapt2FromMavenOverride=$BT/aapt2 :app:assembleDebug
A=app/build/outputs/apk/debug/app-debug.apk
rm -rf /tmp/ustrip; mkdir /tmp/ustrip; cp $A /tmp/ustrip/in.apk
zip -q -d /tmp/ustrip/in.apk 'META-INF/*.SF' 'META-INF/*.RSA' 'META-INF/*.EC' 'META-INF/MANIFEST.MF' 2>/dev/null || true
$BT/zipalign -f -p 4 /tmp/ustrip/in.apk /tmp/ustrip/out.apk
$BT/zipalign -c -p 4 /tmp/ustrip/out.apk
cp /tmp/ustrip/out.apk $ROOT/out/YunSeek-1.8.1-unsigned-aligned.apk
(cd $ROOT/out && sha256sum YunSeek-1.8.1-unsigned-aligned.apk > SHA256.txt && cat SHA256.txt)
echo BUILD_DONE
