<#
.SYNOPSIS
  Loads the RecurIQ tables and demo data into a hosted MySQL database (for example Aiven).

.EXAMPLE
  .\scripts\load-demo-db.ps1 -DbHost recuriq-db-you.aivencloud.com -Port 12345

  Asks for the database password, then creates the subscription_billing database,
  loads schema.sql and data.sql, and prints how many subscriptions were loaded (42).

.NOTES
  This replaces any existing subscription_billing database on that server.
  Use -CheckOnly to just test the connection and count rows without changing anything.
#>
param(
    [Parameter(Mandatory)] [string] $DbHost,
    [Parameter(Mandatory)] [int] $Port,
    [string] $User = 'avnadmin',
    [switch] $CheckOnly
)

$ErrorActionPreference = 'Stop'
$resources = Join-Path $PSScriptRoot '..\backend\src\main\resources'

# Find the mysql client: on PATH, or in the default Windows install folder
$mysql = (Get-Command mysql -ErrorAction SilentlyContinue).Source
if (-not $mysql) {
    $mysql = Get-ChildItem 'C:\Program Files\MySQL\MySQL Server *\bin\mysql.exe' -ErrorAction SilentlyContinue |
        Select-Object -Last 1 -ExpandProperty FullName
}
if (-not $mysql) { throw 'mysql.exe was not found. Install MySQL or add its bin folder to PATH.' }

# The password is passed through MYSQL_PWD for this run only, so it is never written to disk or shown
$hadPassword = [bool] $env:MYSQL_PWD
if (-not $hadPassword) {
    $secure = Read-Host 'Database password' -AsSecureString
    $env:MYSQL_PWD = [Runtime.InteropServices.Marshal]::PtrToStringAuto(
        [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure))
}

function Invoke-Sql([string] $sql) {
    & $mysql -h $DbHost -P $Port -u $User --ssl-mode=REQUIRED -N -e $sql
    if ($LASTEXITCODE -ne 0) { throw "mysql failed (exit code $LASTEXITCODE). Check the host, port and password." }
}

try {
    if (-not $CheckOnly) {
        foreach ($file in 'schema.sql', 'data.sql') {
            $path = (Resolve-Path (Join-Path $resources $file)).Path -replace '\\', '/'
            Write-Host "Loading $file ..."
            Invoke-Sql "source $path"
        }
    }
    $count = Invoke-Sql 'SELECT COUNT(*) FROM subscription_billing.subscription'
    Write-Host "Connected. Subscriptions in the database: $count"
    if (-not $CheckOnly -and "$count".Trim() -ne '42') {
        Write-Warning 'Expected 42 subscriptions from the demo data.'
    }
}
finally {
    if (-not $hadPassword) { Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue }
}
