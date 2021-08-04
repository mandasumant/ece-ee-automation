Set-ExecutionPolicy Unrestricted
Invoke-Expression "C:\'Program Files (x86)'\SAP\FrontEnd\SAPgui\saplogon.exe"
Start-Sleep -s 5
#Invoke-Expression "C:\TestingHub\SAPConfig\saplogonconfig.vbs"

$svcName = Get-Service -DisplayName *java.exe* | select -Exp Name
$svcKey = Get-Item HKLM:\SYSTEM\CurrentControlSet\Services\$svcName

# Set 9th bit, from http://www.codeproject.com/KB/install/cswindowsservicedesktop.aspx
$newType = $svcKey.GetValue('Type') -bor 0x100
Set-ItemProperty $svcKey.PSPath -Name Type -Value $newType

$svcName = Get-Service -DisplayName *saplogon.exe* | select -Exp Name
$svcKey = Get-Item HKLM:\SYSTEM\CurrentControlSet\Services\$svcName

# Set 9th bit, from http://www.codeproject.com/KB/install/cswindowsservicedesktop.aspx
$newType = $svcKey.GetValue('Type') -bor 0x100
Set-ItemProperty $svcKey.PSPath -Name Type -Value $newType