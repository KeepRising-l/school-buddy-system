param(
    [switch]$NoLaunch
)

$ErrorActionPreference = 'Stop'
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
[Console]::OutputEncoding = $utf8NoBom
$OutputEncoding = $utf8NoBom

$RootDir = Split-Path -Parent $PSScriptRoot
Set-Location $RootDir

Write-Host "================================================================" -ForegroundColor Cyan
Write-Host "       校园兴趣搭子系统 (AI 智能版) — 一键环境检测与启动" -ForegroundColor Cyan
Write-Host "================================================================" -ForegroundColor Cyan
Write-Host ""

$missingEnvs = New-Object System.Collections.Generic.List[string]

# ------------------------------------------------------------------
# 1. 检测 JDK 17 环境
# ------------------------------------------------------------------
Write-Host "[1/6] 正在检测 Java (JDK 17) 环境..." -ForegroundColor Yellow
$jdk17Home = $null
if (Test-Path "D:\develop\JDK17\bin\java.exe") {
    $jdk17Home = "D:\develop\JDK17"
} elseif ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME "bin\java.exe"))) {
    $jdk17Home = $env:JAVA_HOME
}

$javaExe = $null
if ($jdk17Home) {
    $javaExe = Join-Path $jdk17Home "bin\java.exe"
} else {
    $javaCmd = Get-Command java -ErrorAction SilentlyContinue
    if ($javaCmd) {
        $javaExe = $javaCmd.Source
    }
}

if (-not $javaExe -or -not (Test-Path $javaExe)) {
    $missingEnvs.Add("JDK 17 (未检测到 java.exe，请安装 JDK 17 并配置 JAVA_HOME)")
    Write-Host "  [×] 未检测到 JDK 17 环境！" -ForegroundColor Red
} else {
    $prevEap = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    $javaVerOutput = & $javaExe -version 2>&1 | Out-String
    $ErrorActionPreference = $prevEap
    if ($jdk17Home) {
        $env:JAVA_HOME = $jdk17Home
        $env:Path = "$(Join-Path $jdk17Home 'bin');$env:Path"
    }
    $firstLine = ($javaVerOutput -split "`r?`n")[0]
    Write-Host "  [√] Java 环境正常: $firstLine" -ForegroundColor Green
}

# ------------------------------------------------------------------
# 2. 检测 Maven 环境
# ------------------------------------------------------------------
Write-Host "[2/6] 正在检测 Maven (mvn) 环境..." -ForegroundColor Yellow
$mvnCmd = Get-Command mvn -ErrorAction SilentlyContinue
if (-not $mvnCmd) {
    $commonMvnPaths = @(
        "D:\develop\apache-maven*\bin\mvn.cmd",
        "C:\Program Files\apache-maven*\bin\mvn.cmd",
        "D:\apache-maven*\bin\mvn.cmd"
    )
    foreach ($pattern in $commonMvnPaths) {
        $found = Get-Item $pattern -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($found) {
            $env:Path = "$($found.DirectoryName);$env:Path"
            $mvnCmd = Get-Command mvn -ErrorAction SilentlyContinue
            break
        }
    }
}

if (-not $mvnCmd) {
    $missingEnvs.Add("Apache Maven (未检测到 mvn 命令，请安装 Maven 并配置到系统 PATH)")
    Write-Host "  [×] 未检测到 Maven (mvn) 环境！" -ForegroundColor Red
} else {
    Write-Host "  [√] Maven 环境正常: $($mvnCmd.Source)" -ForegroundColor Green
}

# ------------------------------------------------------------------
# 3. 检测 Node.js 与 npm 环境
# ------------------------------------------------------------------
Write-Host "[3/6] 正在检测 Node.js 与 npm 环境..." -ForegroundColor Yellow
$nodeCmd = Get-Command node -ErrorAction SilentlyContinue
$npmCmd  = Get-Command npm -ErrorAction SilentlyContinue
if (-not $nodeCmd -or -not $npmCmd) {
    $missingEnvs.Add("Node.js / npm (未检测到 node 或 npm 命令，请安装 Node.js 18+)")
    Write-Host "  [×] 未检测到 Node.js 或 npm 环境！" -ForegroundColor Red
} else {
    $nodeVer = (& node -v 2>&1) | Out-String
    $npmVer  = (& npm -v 2>&1) | Out-String
    Write-Host "  [√] Node.js 环境正常: Node $($nodeVer.Trim()) / npm $($npmVer.Trim())" -ForegroundColor Green
}

# ------------------------------------------------------------------
# 4. 检测 MySQL 客户端与 MySQL 服务连接
# ------------------------------------------------------------------
Write-Host "[4/6] 正在检测 MySQL 客户端与数据库服务..." -ForegroundColor Yellow
$mysqlCmd = Get-Command mysql -ErrorAction SilentlyContinue
$mysqlExe = $null
if ($mysqlCmd) {
    $mysqlExe = $mysqlCmd.Source
} else {
    $commonMysqlPaths = @(
        "C:\Program Files\MySQL\MySQL Server*\bin\mysql.exe",
        "D:\develop\MySQL*\bin\mysql.exe",
        "D:\MySQL*\bin\mysql.exe",
        "C:\mysql*\bin\mysql.exe"
    )
    foreach ($pattern in $commonMysqlPaths) {
        $found = Get-Item $pattern -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($found) {
            $mysqlExe = $found.FullName
            $env:Path = "$($found.DirectoryName);$env:Path"
            break
        }
    }
}

# 读取数据库配置 (优先从 backend-v2/src/main/resources/application-local.yml 读取)
$localYmlPath = Join-Path $RootDir "backend-v2\src\main\resources\application-local.yml"
$exampleYmlPath = Join-Path $RootDir "backend-v2\src\main\resources\application-local.yml.example"
if (-not (Test-Path $localYmlPath) -and (Test-Path $exampleYmlPath)) {
    Copy-Item $exampleYmlPath $localYmlPath -Force
}

$dbHost = "localhost"
$dbPort = 3306
$dbUser = "root"
$dbPass = "1234"

if (Test-Path $localYmlPath) {
    $ymlContent = Get-Content $localYmlPath -Raw -Encoding UTF8
    if ($ymlContent -match "jdbc:mysql://([^:/]+):(\d+)/") {
        $dbHost = $Matches[1]
        $dbPort = [int]$Matches[2]
    }
    if ($ymlContent -match "(?m)^\s*username:\s*(.+)$") {
        $dbUser = $Matches[1].Trim().Trim('"').Trim("'")
    }
    if ($ymlContent -match "(?m)^\s*password:\s*(.+)$") {
        $dbPass = $Matches[1].Trim().Trim('"').Trim("'")
    }
}

if (-not $mysqlExe -or -not (Test-Path $mysqlExe)) {
    $missingEnvs.Add("MySQL Client (未检测到 mysql.exe 客户端命令行工具，请安装 MySQL 8.0+ 或将其 bin 目录加入 PATH)")
    Write-Host "  [×] 未检测到 MySQL 客户端 (mysql.exe)！" -ForegroundColor Red
} else {
    # 检测端口连通性
    $tcpClient = New-Object System.Net.Sockets.TcpClient
    $connected = $false
    try {
        $asyncRes = $tcpClient.BeginConnect($dbHost, $dbPort, $null, $null)
        $waitRes = $asyncRes.AsyncWaitHandle.WaitOne(2000, $false)
        if ($waitRes -and $tcpClient.Connected) {
            $connected = $true
        }
    } catch {
        $connected = $false
    } finally {
        $tcpClient.Close()
    }

    if (-not $connected) {
        $missingEnvs.Add("MySQL Server 服务未启动 (无法连接 ${dbHost}:${dbPort}，请先启动 MySQL 服务)")
        Write-Host "  [×] MySQL 服务未启动或端口 ${dbHost}:${dbPort} 无法连接！" -ForegroundColor Red
    } else {
        $prevEap = $ErrorActionPreference
        $ErrorActionPreference = 'Continue'
        $env:MYSQL_PWD = $dbPass
        $testQuery = & $mysqlExe -h $dbHost -P $dbPort -u $dbUser --default-character-set=utf8mb4 -e "SELECT 1;" 2>&1 | Out-String
        $mysqlExit = $LASTEXITCODE
        $ErrorActionPreference = $prevEap
        if ($mysqlExit -ne 0) {
            $missingEnvs.Add("MySQL 数据库认证失败 (无法使用账号 '$dbUser' 连接 ${dbHost}:${dbPort}，请检查 backend-v2/src/main/resources/application-local.yml 中的密码配置)")
            Write-Host "  [×] MySQL 数据库连接认证失败！错误信息: $($testQuery.Trim())" -ForegroundColor Red
        } else {
            Write-Host "  [√] MySQL 客户端与数据库服务连接正常 (${dbHost}:${dbPort})" -ForegroundColor Green
        }
    }
}

# ------------------------------------------------------------------
# 若存在缺失环境，汇总报错并终止启动
# ------------------------------------------------------------------
if ($missingEnvs.Count -gt 0) {
    Write-Host ""
    Write-Host "================================================================" -ForegroundColor Red
    Write-Host " [启动失败] 检测到以下运行环境缺失或异常，请修复后重试：" -ForegroundColor Red
    Write-Host "================================================================" -ForegroundColor Red
    $idx = 1
    foreach ($err in $missingEnvs) {
        Write-Host "  $idx. $err" -ForegroundColor Red
        $idx++
    }
    Write-Host "================================================================" -ForegroundColor Red
    exit 1
}

# ------------------------------------------------------------------
# 5. 自动导入各行业名人内置测试数据
# ------------------------------------------------------------------
Write-Host ""
Write-Host "[5/6] 正在导入各行业名人内置测试数据 (UTF-8)..." -ForegroundColor Yellow

$sqlFiles = @(
    @{ Path = (Join-Path $RootDir "database\init-all.sql");   Desc = "建库建表 + 48位名人核心账号 + 30条名人组局帖 + 公告与举报数据" },
    @{ Path = (Join-Path $RootDir "database\more-users.sql"); Desc = "50位跨界名人账号及150条兴趣段位标签" },
    @{ Path = (Join-Path $RootDir "database\demo-posts.sql"); Desc = "8条未来相对时间名人热招组局帖" }
)

$env:MYSQL_PWD = $dbPass
foreach ($item in $sqlFiles) {
    if (-not (Test-Path $item.Path)) {
        Write-Host "  [×] 找不到 SQL 脚本文件: $($item.Path)" -ForegroundColor Red
        exit 1
    }
    Write-Host "  -> 正在导入: $($item.Desc)..." -ForegroundColor Gray
    # 确保 SQL 文件自身无 UTF-8 BOM 头，并由 mysql 直接通过 source 读取文件，避免 PowerShell 管道注入 BOM 或截断多字节字符
    $rawBytes = [System.IO.File]::ReadAllBytes($item.Path)
    if ($rawBytes.Length -ge 3 -and $rawBytes[0] -eq 0xEF -and $rawBytes[1] -eq 0xBB -and $rawBytes[2] -eq 0xBF) {
        $stripped = New-Object byte[] ($rawBytes.Length - 3)
        [System.Array]::Copy($rawBytes, 3, $stripped, 0, $stripped.Length)
        [System.IO.File]::WriteAllBytes($item.Path, $stripped)
    }
    $prevEap = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    $cmdLine = "`"`"$mysqlExe`" -h $dbHost -P $dbPort -u $dbUser --default-character-set=utf8mb4 < `"$($item.Path)`"`""
    $importOut = & cmd.exe /d /s /c $cmdLine 2>&1 | Out-String
    $importExit = $LASTEXITCODE
    $ErrorActionPreference = $prevEap
    if ($importExit -ne 0) {
        Write-Host "  [×] 导入 $($item.Path) 失败: $importOut" -ForegroundColor Red
        exit 1
    }
}
$env:MYSQL_PWD = $null
Write-Host "  [√] 名人内置测试数据全部导入成功！（共 98 个名人账号、38 条组局帖）" -ForegroundColor Green

# ------------------------------------------------------------------
# 6. 在桌面生成用户账号密码清单文档
# ------------------------------------------------------------------
Write-Host ""
Write-Host "[6/6] 正在桌面生成测试用户账号密码清单文档..." -ForegroundColor Yellow
$desktopDirs = New-Object System.Collections.Generic.List[string]
$sysDesktop = [Environment]::GetFolderPath('Desktop')
if ($sysDesktop -and (Test-Path $sysDesktop)) {
    $desktopDirs.Add($sysDesktop)
}
$userDesktop = Join-Path $env:USERPROFILE "Desktop"
if ((Test-Path $userDesktop) -and (-not $desktopDirs.Contains($userDesktop))) {
    $desktopDirs.Add($userDesktop)
}

$sourceDoc = Join-Path $RootDir "docs\测试用户清单.md"
$primaryTxtPath = ""

if ((Test-Path $sourceDoc) -and ($desktopDirs.Count -gt 0)) {
    $docContent = Get-Content $sourceDoc -Raw -Encoding UTF8
    Write-Host "  [√] 已在桌面创建账号密码文档：" -ForegroundColor Green
    foreach ($dDir in $desktopDirs) {
        $mdPath  = Join-Path $dDir "校园搭子系统-测试账号密码清单.md"
        $txtPath = Join-Path $dDir "校园搭子系统-测试账号密码清单.txt"
        [System.IO.File]::WriteAllText($mdPath,  $docContent, (New-Object System.Text.UTF8Encoding($true)))
        [System.IO.File]::WriteAllText($txtPath, $docContent, (New-Object System.Text.UTF8Encoding($true)))
        if (-not $primaryTxtPath) { $primaryTxtPath = $txtPath }
        Write-Host "      - $mdPath" -ForegroundColor Cyan
        Write-Host "      - $txtPath" -ForegroundColor Cyan
    }
} else {
    Write-Host "  [!] 未找到 docs\测试用户清单.md 或桌面目录，跳过桌面文档生成" -ForegroundColor Yellow
}

if ($NoLaunch) {
    Write-Host ""
    Write-Host "[检测与数据导入完成] (-NoLaunch 模式，未启动后台窗口)" -ForegroundColor Green
    exit 0
}

# ------------------------------------------------------------------
# 7. 检查前端依赖并启动后端、H5前端、管理端后台
# ------------------------------------------------------------------
Write-Host ""
Write-Host "正在检查前端依赖并启动前后端服务..." -ForegroundColor Yellow

$h5Dir = Join-Path $RootDir "frontend-h5"
$adminDir = Join-Path $RootDir "frontend-admin"
$backendDir = Join-Path $RootDir "backend-v2"

if (-not (Test-Path (Join-Path $h5Dir "node_modules"))) {
    Write-Host "  -> 首次运行，正在为 frontend-h5 安装依赖 (npm install)..." -ForegroundColor Gray
    Push-Location $h5Dir
    & npm install
    Pop-Location
}

if (-not (Test-Path (Join-Path $adminDir "node_modules"))) {
    Write-Host "  -> 首次运行，正在为 frontend-admin 安装依赖 (npm install)..." -ForegroundColor Gray
    Push-Location $adminDir
    & npm install
    Pop-Location
}

# 清理可能干扰 Spring Boot 端口的外部环境变量
$env:SERVER__PORT = $null
$env:SERVER__HOST = $null

$jdkSetupCmd = ""
if ($jdk17Home) {
    $jdkSetupCmd = "set `"JAVA_HOME=$jdk17Home`" && set `"PATH=$jdk17Home\bin;%PATH%`" && "
}

Write-Host "  -> 正在启动 [后端服务 backend-v2] (端口 8080)..." -ForegroundColor Green
Start-Process cmd.exe -WorkingDirectory $backendDir -ArgumentList "/k title 校园搭子系统-后端服务(8080) && chcp 65001 >nul && set `"SERVER__PORT=`" && set `"SERVER__HOST=`" && ${jdkSetupCmd}mvn spring-boot:run"

Write-Host "  -> 正在启动 [H5 用户端 frontend-h5] (端口 3000)..." -ForegroundColor Green
Start-Process cmd.exe -WorkingDirectory $h5Dir -ArgumentList "/k title 校园搭子系统-H5用户端(3000) && chcp 65001 >nul && npm run dev"

Write-Host "  -> 正在启动 [管理端后台 frontend-admin] (端口 3001)..." -ForegroundColor Green
Start-Process cmd.exe -WorkingDirectory $adminDir -ArgumentList "/k title 校园搭子系统-管理端后台(3001) && chcp 65001 >nul && npm run dev"

Write-Host ""
Write-Host "================================================================" -ForegroundColor Green
Write-Host "  🎉 校园兴趣搭子系统已全部启动！" -ForegroundColor Green
Write-Host "----------------------------------------------------------------" -ForegroundColor Green
Write-Host "  📱 H5 用户端地址   : http://localhost:3000" -ForegroundColor Cyan
Write-Host "     快捷名人学生号 : 20240001 (姚明) / 密码: 123456" -ForegroundColor White
Write-Host "                      20240002 (谷爱凌) / 密码: 123456" -ForegroundColor White
Write-Host "                      2342170101 (雷军) / 密码: 2342170101123" -ForegroundColor White
Write-Host ""
Write-Host "  💻 管理端后台地址 : http://localhost:3001" -ForegroundColor Cyan
Write-Host "     管理员账号     : admin001 (包拯) / 密码: 123456" -ForegroundColor White
Write-Host ""
Write-Host "  📄 完整98位名人账号密码清单已生成至您的桌面：" -ForegroundColor Yellow
Write-Host "     $primaryTxtPath" -ForegroundColor White
Write-Host "================================================================" -ForegroundColor Green
