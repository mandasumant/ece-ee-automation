#This script will read the testexecution.log and return exit code 1 to run command if the testcase fails. 

try {
    write-output "cleaning up the residue process"
    Stop-Process -Name "chromedriver" -Force
    Stop-Process -Name "chrome" -Force
    Stop-Process -Name "saplogon" -Force

} catch {
    write-output "no process found to clean"
}

write-output "Checking if testcase passed or failed"

if( Get-Content testexecution.log | Where-Object { $_.Contains("BUILD FAILURE") } ) {
    write-output "Test case failed"
    throw "Test case failed"
}
