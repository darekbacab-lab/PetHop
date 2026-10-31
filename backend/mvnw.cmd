<# : batch portion
@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements. See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership. The ASF licenses this file
@REM to you under the Apache License, Version 2.0 (the
@REM "License"); you may not use this file except in compliance
@REM with the License. You may obtain a copy of the License at
@REM
@REM https://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed under the License is distributed on an
@REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM KIND, either express or implied. See the License for the
@REM specific language governing permissions and limitations
@REM under the License.
@REM ----------------------------------------------------------------------------

@REM Apache Maven Wrapper startup batch script, version 3.3.4 (only-script)
@REM MVNW_REPOURL, MVNW_USERNAME, MVNW_PASSWORD and MVNW_VERBOSE are optional.
@IF "%__MVNW_ARG0_NAME__%"=="" (SET __MVNW_ARG0_NAME__=%~nx0)
@SET __MVNW_CMD__=
@SET __MVNW_PSMODULEP_SAVE=%PSModulePath%
@SET PSModulePath=
@FOR /F "usebackq tokens=1* delims==" %%A IN (`powershell -noprofile "& {$scriptDir='%~dp0'; $script='%__MVNW_ARG0_NAME__%'; icm -ScriptBlock ([Scriptblock]::Create((Get-Content -Raw '%~f0'))) -NoNewScope}"`) DO @(
  IF "%%A"=="MVN_CMD" (set __MVNW_CMD__=%%B) ELSE IF "%%B"=="" (echo %%A) ELSE (echo %%A=%%B)
)
@SET PSModulePath=%__MVNW_PSMODULEP_SAVE%
@SET __MVNW_PSMODULEP_SAVE=
@SET __MVNW_ARG0_NAME__=
@SET MVNW_USERNAME=
@SET MVNW_PASSWORD=
@IF NOT "%__MVNW_CMD__%"=="" ("%__MVNW_CMD__%" %*)
@echo Cannot start Maven from the wrapper. >&2 && exit /b 1
@GOTO :EOF
: end batch / begin PowerShell #>

$ErrorActionPreference = 'Stop'
if ($env:MVNW_VERBOSE -eq 'true') { $VerbosePreference = 'Continue' }

$propertiesPath = Join-Path $scriptDir '.mvn/wrapper/maven-wrapper.properties'
if (-not (Test-Path -LiteralPath $propertiesPath -PathType Leaf)) {
  Write-Error "Cannot find Maven Wrapper properties: $propertiesPath"
}
$properties = Get-Content -Raw -LiteralPath $propertiesPath | ConvertFrom-StringData
$distributionUrl = $properties.distributionUrl
if (-not $distributionUrl) { Write-Error "distributionUrl is missing from $propertiesPath" }

$distributionName = [IO.Path]::GetFileName(([Uri]$distributionUrl).AbsolutePath)
$distributionNameMain = $distributionName -replace '\.[^.]*$','' -replace '-bin$',''
if (-not $distributionNameMain -or $distributionName -eq $distributionNameMain) {
  Write-Error "distributionUrl must point to an Apache Maven *-bin.zip archive: $distributionUrl"
}

$mavenCommand = $script -replace '^mvnw','mvn'
$mavenHomeRoot = if ($env:MAVEN_USER_HOME) { Join-Path $env:MAVEN_USER_HOME 'wrapper/dists' } else { Join-Path $HOME '.m2/wrapper/dists' }
$mavenHomeParent = Join-Path $mavenHomeRoot $distributionNameMain
$urlHash = ([Security.Cryptography.SHA256]::Create().ComputeHash([Text.Encoding]::UTF8.GetBytes($distributionUrl)) | ForEach-Object { $_.ToString('x2') }) -join ''
$mavenHome = Join-Path $mavenHomeParent $urlHash
$mavenExe = Join-Path $mavenHome "bin/$mavenCommand"

if (Test-Path -LiteralPath $mavenExe -PathType Leaf) {
  Write-Verbose "Using cached Maven at $mavenHome"
  Write-Output "MVN_CMD=$mavenExe"
  exit 0
}

$tempMarker = New-TemporaryFile
$tempDir = "$($tempMarker.FullName).dir"
Remove-Item -LiteralPath $tempMarker.FullName -Force
New-Item -ItemType Directory -Path $tempDir | Out-Null
try {
  New-Item -ItemType Directory -Path $mavenHomeParent -Force | Out-Null
  $archivePath = Join-Path $tempDir $distributionName
  Write-Verbose "Downloading Maven from $distributionUrl"
  $client = New-Object System.Net.WebClient
  if ($env:MVNW_USERNAME -and $env:MVNW_PASSWORD) {
    $client.Credentials = New-Object System.Net.NetworkCredential($env:MVNW_USERNAME, $env:MVNW_PASSWORD)
  }
  [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
  $client.DownloadFile($distributionUrl, $archivePath)

  $expectedChecksum = $properties.distributionSha256Sum
  if ($expectedChecksum) {
    $actualChecksum = (Get-FileHash -LiteralPath $archivePath -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($actualChecksum -ne $expectedChecksum.ToLowerInvariant()) {
      Write-Error 'Maven distribution SHA-256 does not match maven-wrapper.properties.'
    }
  }

  Expand-Archive -LiteralPath $archivePath -DestinationPath $tempDir
  $extractedMavenHome = Join-Path $tempDir $distributionNameMain
  $extractedMavenExe = Join-Path $extractedMavenHome "bin/$mavenCommand"
  if (-not (Test-Path -LiteralPath $extractedMavenExe -PathType Leaf)) {
    Write-Error "The downloaded archive did not contain $distributionNameMain/bin/$mavenCommand"
  }

  # Move directly to the not-yet-created hash destination. The version parent may
  # already exist, so never move the extracted folder onto that parent itself.
  try {
    Move-Item -LiteralPath $extractedMavenHome -Destination $mavenHome
  } catch {
    # A parallel wrapper process may have installed the same distribution first.
    if (-not (Test-Path -LiteralPath $mavenExe -PathType Leaf)) { throw }
  }
} finally {
  if (Test-Path -LiteralPath $tempDir) {
    Remove-Item -LiteralPath $tempDir -Recurse -Force -ErrorAction SilentlyContinue
  }
}

if (-not (Test-Path -LiteralPath $mavenExe -PathType Leaf)) {
  Write-Error "Maven installation did not produce the expected executable: $mavenExe"
}
Write-Output "MVN_CMD=$mavenExe"
