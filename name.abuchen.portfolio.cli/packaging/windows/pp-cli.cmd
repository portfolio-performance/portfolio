@echo off
rem Launcher for the bundled pp-cli. It is a .cmd so that PowerShell's execution
rem policy never blocks it. No parenthesised blocks: the install path may contain ')'.
setlocal
set "PP_CLI_HOME=%~dp0"
set "PP_CLI_JSA=%PP_CLI_HOME%pp-cli.jsa"
set "PP_CLI_CDS="
rem the option is quoted as one token because the install path may contain spaces
if exist "%PP_CLI_JSA%" set "PP_CLI_CDS="-XX:SharedArchiveFile=%PP_CLI_JSA%""
rem install.ps1 sets PP_CLI_BUILD_ARCHIVE to record the AppCDS archive from one training run
if defined PP_CLI_BUILD_ARCHIVE set "PP_CLI_CDS="-XX:ArchiveClassesAtExit=%PP_CLI_JSA%""
rem -Xlog:disable keeps JVM warnings (for example a stale archive) off stdout, which carries the JSON
"%PP_CLI_HOME%runtime\bin\java.exe" %PP_CLI_CDS% -Xlog:disable -XX:TieredStopAtLevel=1 -cp "%PP_CLI_HOME%lib\*" name.abuchen.portfolio.cli.PpCli %*
exit /b %ERRORLEVEL%
