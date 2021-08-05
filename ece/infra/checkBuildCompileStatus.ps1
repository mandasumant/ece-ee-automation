write-output "Checking if build compilation passed or failed"

if( Get-Content C:\TestingHub\TestingHub-TestCode\buildexecution.log | Where-Object { $_.Contains("BUILD FAILURE") } ) {
    write-output "Build Compilation failed"
    throw "Build Compilation failed"
}