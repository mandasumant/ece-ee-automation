param([String]$transactionid, [String]$loggroup)

Write-Host "Transactionid: $transactionid"
Write-Host "Stackname: $loggroup"

$cwPath = 'C:\Program Files\Amazon\AmazonCloudWatchAgent\'
$configPath = $cwPath + "config.json"
Copy-Item ".\cloudwatchAgentconfig.json" -Destination $configPath

$cwd =$PSScriptRoot
Write-Host "Current path $cwd"

(Get-Content -path $configPath) | ForEach-Object {
    $_.replace('replace_stackloggroup', $loggroup).replace('replacewithtransactionid', $transactionid)
 } | Set-Content  -Path $configPath

cd $cwPath
./amazon-cloudwatch-agent-ctl.ps1 -a fetch-config -m ec2 -c file:".\config.json" -s

cd $cwd
