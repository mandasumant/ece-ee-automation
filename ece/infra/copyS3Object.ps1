 
param([String]$S3bucket, [String]$keyName)

$localPath = "C:\TestingHub"

Write-Host "S3bucket: $S3bucket"
Write-Host "S3keyName: $keyName"

Copy-S3Object -BucketName $S3bucket -Key $keyName -LocalFolder $localPath

$bundlepath = Join-Path -Path $localPath -ChildPath "$keyName" -Resolve
Write-Host "$bundlepath"

$destinationpath = Join-Path -Path $localPath -ChildPath "TestingHub-TestCode"

Expand-Archive -Force -LiteralPath "$bundlepath" -DestinationPath "$destinationpath" 
