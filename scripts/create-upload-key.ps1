# Creates the Google Play upload key (upload-keystore.jks) and keystore.properties.
# Run from anywhere:  powershell -ExecutionPolicy Bypass -File scripts\create-upload-key.ps1
# You type the password here; it is never shown on screen.

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$jks = Join-Path $root 'upload-keystore.jks'
$props = Join-Path $root 'keystore.properties'

if (Test-Path $jks) { Write-Host "upload-keystore.jks already exists - not overwriting it." -ForegroundColor Yellow; exit 1 }

$keytool = 'C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe'
if (-not (Test-Path $keytool)) { $keytool = (Get-Command keytool -ErrorAction Stop).Source }

function Read-Plain($prompt) {
    $s = Read-Host -Prompt $prompt -AsSecureString
    $b = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($s)
    try { [Runtime.InteropServices.Marshal]::PtrToStringBSTR($b) } finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($b) }
}

do {
    $pw = Read-Plain 'Choose a password for the upload key (6+ characters)'
    $pw2 = Read-Plain 'Type it again'
    if ($pw.Length -lt 6) { Write-Host 'Too short, try again.' -ForegroundColor Yellow; continue }
    if ($pw -ne $pw2) { Write-Host 'They did not match, try again.' -ForegroundColor Yellow; $pw = '' }
} while ($pw.Length -lt 6)

$name = Read-Host -Prompt 'Name for the certificate (your name or studio; press Enter for "Flux Calc")'
if ([string]::IsNullOrWhiteSpace($name)) { $name = 'Flux Calc' }
$name = $name -replace '[,=+<>#;"\\]', ' '

$env:FLUX_UPLOAD_PW = $pw
try {
    & $keytool -genkeypair -keystore $jks -storetype PKCS12 -alias upload `
        -keyalg RSA -keysize 2048 -validity 10000 `
        -dname "CN=$name" -storepass:env FLUX_UPLOAD_PW -keypass:env FLUX_UPLOAD_PW
    if ($LASTEXITCODE -ne 0) { throw "keytool failed ($LASTEXITCODE)" }
} finally { Remove-Item Env:FLUX_UPLOAD_PW -ErrorAction SilentlyContinue }

# Java .properties escaping: backslashes, and anything outside ASCII as \uXXXX.
$esc = -join ($pw.ToCharArray() | ForEach-Object {
    if ($_ -eq '\') { '\\' } elseif ([int]$_ -gt 126 -or [int]$_ -lt 32) { '\u{0:x4}' -f [int]$_ } else { $_ }
})
$content = "storeFile=upload-keystore.jks`nstorePassword=$esc`nkeyAlias=upload`nkeyPassword=$esc`n"
[IO.File]::WriteAllText($props, $content, [Text.Encoding]::ASCII)

Write-Host ''
Write-Host 'Done. Created:' -ForegroundColor Green
Write-Host "  $jks"
Write-Host "  $props"
Write-Host 'Back up BOTH files (and the password) somewhere safe, e.g. a password manager.' -ForegroundColor Cyan
