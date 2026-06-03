param(
    [Parameter(Mandatory = $true)]
    [int]$VersionCode,

    [Parameter(Mandatory = $true)]
    [string]$VersionName,

    [string]$Tag,

    [string]$CommitMessage
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$buildFile = Join-Path $repoRoot "app\build.gradle.kts"

if (-not (Test-Path $buildFile)) {
    throw "تعذر العثور على app/build.gradle.kts"
}

if (-not $Tag -or [string]::IsNullOrWhiteSpace($Tag)) {
    $Tag = "v$VersionName"
}

if (-not $CommitMessage -or [string]::IsNullOrWhiteSpace($CommitMessage)) {
    $CommitMessage = "Release $Tag"
}

$buildText = Get-Content -Raw -Path $buildFile
$updatedText = $buildText

$updatedText = [regex]::Replace(
    $updatedText,
    '(?m)^\s*versionCode\s*=\s*\d+\s*$',
    "    versionCode = $VersionCode"
)

$updatedText = [regex]::Replace(
    $updatedText,
    '(?m)^\s*versionName\s*=\s*".*?"\s*$',
    "    versionName = `"$VersionName`""
)

if ($updatedText -eq $buildText) {
    throw "لم يتم العثور على versionCode أو versionName لتحديثهما."
}

Set-Content -Path $buildFile -Value $updatedText -Encoding UTF8

Push-Location $repoRoot
try {
    git add app/build.gradle.kts
    git commit -m $CommitMessage
    git push origin HEAD:main
    git tag $Tag
    git push origin $Tag
} finally {
    Pop-Location
}

Write-Host "تم نشر $Tag بنجاح."
