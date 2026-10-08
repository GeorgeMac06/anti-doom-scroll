# Tests the session rules on the PC, then builds anti-doom-scroll.apk with the Android SDK
# command-line tools (no Gradle). Pass -Install to install it on the USB-connected phone.
param([switch]$Install)
$ErrorActionPreference = 'Stop'
$sdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { "$env:LOCALAPPDATA\Android\Sdk" }
$bt = "$sdk\build-tools\36.0.0"
$jar = "$sdk\platforms\android-37.0\android.jar"
$keystore = "$env:USERPROFILE\.android\debug.keystore"
$src = "$PSScriptRoot\src"
$out = "$PSScriptRoot\build"

function Check($step) { if ($LASTEXITCODE -ne 0) { throw "$step failed ($LASTEXITCODE)" } }

Remove-Item -Recurse -Force $out -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force "$out\test", "$out\classes", "$out\dex" | Out-Null

# Session rules run on the plain JVM: SessionPolicy has no Android imports.
& javac --release 11 -d "$out\test" "$src\dev\antidoomscroll\SessionPolicy.java" "$PSScriptRoot\test\SessionPolicyTest.java"; Check 'javac (tests)'
& java -cp "$out\test" SessionPolicyTest; Check 'SessionPolicyTest'

& "$bt\aapt2.exe" compile --dir "$PSScriptRoot\res" -o "$out\res.zip"; Check 'aapt2 compile'
& "$bt\aapt2.exe" link -I $jar --manifest "$PSScriptRoot\AndroidManifest.xml" -o "$out\unsigned.apk" "$out\res.zip"; Check 'aapt2 link'

$sources = Get-ChildItem -Recurse $src -Filter *.java | ForEach-Object FullName
& javac --release 11 -Xlint:-options -cp $jar -d "$out\classes" $sources; Check 'javac'
$classes = Get-ChildItem -Recurse "$out\classes" -Filter *.class | ForEach-Object FullName
& "$bt\d8.bat" --lib $jar --min-api 29 --output "$out\dex" $classes; Check 'd8'
& jar --update --no-manifest --file "$out\unsigned.apk" -C "$out\dex" classes.dex; Check 'jar'

& "$bt\zipalign.exe" -f -p 4 "$out\unsigned.apk" "$out\aligned.apk"; Check 'zipalign'
& "$bt\apksigner.bat" sign --ks $keystore --ks-pass pass:android --ks-key-alias androiddebugkey --key-pass pass:android --out "$out\anti-doom-scroll.apk" "$out\aligned.apk"; Check 'apksigner sign'
& "$bt\apksigner.bat" verify "$out\anti-doom-scroll.apk"; Check 'apksigner verify'
Write-Host "Built $out\anti-doom-scroll.apk"

if ($Install) {
    & "$sdk\platform-tools\adb.exe" install -r "$out\anti-doom-scroll.apk"; Check 'adb install'
}
