# GoThere — boot emulator + install debug APK (prepped 2026-05-31 for next-day testing)
# Usage:  powershell -ExecutionPolicy Bypass -File run-emulator.ps1
# Or just open the project in Android Studio and hit Run on the BenefitBot_Test device.

$ErrorActionPreference = "Stop"
$SDK = "$env:LOCALAPPDATA\Android\Sdk"
$ADB = "$SDK\platform-tools\adb.exe"
$EMU = "$SDK\emulator\emulator.exe"
$AVD = "BenefitBot_Test"   # API 36 / Google Play / x86_64 — same image GoThere targets
$APK = "$PSScriptRoot\android\app\build\outputs\apk\debug\app-debug.apk"

# 1. Rebuild debug APK (fast if unchanged)
Write-Host "Building debug APK..." -ForegroundColor Cyan
& "$PSScriptRoot\android\gradlew.bat" -p "$PSScriptRoot\android" :app:assembleDebug --console=plain

# 2. Boot emulator if not already running
$running = & $ADB devices | Select-String "emulator-"
if (-not $running) {
    Write-Host "Booting emulator $AVD..." -ForegroundColor Cyan
    Start-Process -FilePath $EMU -ArgumentList "-avd", $AVD
    & $ADB wait-for-device
    # wait for full boot
    do { Start-Sleep -Seconds 2; $boot = (& $ADB shell getprop sys.boot_completed 2>$null).Trim() } until ($boot -eq "1")
}

# 3. Install + launch
Write-Host "Installing GoThere debug build..." -ForegroundColor Cyan
& $ADB install -r "$APK"
& $ADB shell monkey -p com.gothere.app -c android.intent.category.LAUNCHER 1
Write-Host "GoThere (1.9.0 debug) running on $AVD." -ForegroundColor Green
