param(
    [string]$JdkDir = ".\.jdk\jdk-21"
)

if (Test-Path "$JdkDir\bin\java.exe") {
    Write-Host "JDK 21 is already present at $JdkDir"
    exit 0
}

Write-Host "Downloading Eclipse Temurin JDK 21..."
$url = "https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.3%2B9/OpenJDK21U-jdk_x64_windows_hotspot_21.0.3_9.zip"
$zipPath = ".\.jdk\jdk-21.zip"

if (-not (Test-Path ".\.jdk")) {
    New-Item -ItemType Directory -Path ".\.jdk" | Out-Null
}

Invoke-WebRequest -Uri $url -OutFile $zipPath

Write-Host "Extracting JDK..."
Expand-Archive -Path $zipPath -DestinationPath ".\.jdk\extracted" -Force

# Find the extracted folder name (usually jdk-21.0.3+9)
$extractedFolder = Get-ChildItem -Path ".\.jdk\extracted" | Select-Object -First 1

# Rename/Move to jdk-21
Move-Item -Path $extractedFolder.FullName -Destination $JdkDir -Force

# Clean up
Remove-Item -Path $zipPath -Force
Remove-Item -Path ".\.jdk\extracted" -Recurse -Force

Write-Host "JDK 21 setup complete!"
