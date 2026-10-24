# Windows installer definition for an Eclipse based application

# required (if not uncomment below) input parameters:
!define APPNAME "Portfolio Performance"
!define EXECUTABLENAME "PortfolioPerformance"
!define APPEXE "PortfolioPerformance.exe"
!define APPINI "PortfolioPerformance.ini"
!define FOLDER_NAME "PortfolioPerformance"
!define INPUT_DIR "..\target\products\name.abuchen.portfolio.distro.product\win32\win32\x86_64\portfolio"
!define INSTALLSIZE 177818 # size (in kB)
!define PUBLISHER "Andreas Buchen"

!include nsDialogs.nsh
!include LogicLib.nsh
!include FileFunc.nsh

RequestExecutionLevel user

Var InstallType
Var InstallTypeFromCmdLine
Var Dialog
Var RadioUser
Var RadioAllUsers

OutFile "..\target\products\${EXECUTABLENAME}-${SOFTWARE_VERSION}-setup.exe"
Name "${APPNAME}"

BrandingText " "

ChangeUI all "${NSISDIR}\Contrib\UIs\sdbarker_tiny.exe"

Icon ".\install.ico"
ShowInstDetails nevershow
InstProgressFlags smooth
ManifestDPIAware true

Page custom InstallTypePage InstallTypePageLeave
Page directory
Page instfiles


Function .onInit
  Var /GLOBAL cmdLineParams
  Var /GLOBAL allUsersFlag
  Var /GLOBAL currentUserFlag
  
  StrCpy $InstallType "user"
  StrCpy $InstallTypeFromCmdLine "0"
  StrCpy $INSTDIR "$LOCALAPPDATA\Programs\${FOLDER_NAME}"
  
  # Check for command line parameters
  ${GetParameters} $cmdLineParams
  
  # Check for /AllUsers flag
  ClearErrors
  ${GetOptions} $cmdLineParams "/AllUsers" $allUsersFlag
  ${IfNot} ${Errors}
    StrCpy $InstallType "allusers"
    StrCpy $INSTDIR "$PROGRAMFILES\${FOLDER_NAME}"
    StrCpy $InstallTypeFromCmdLine "1"
  ${EndIf}
  
  # Check for /CurrentUser flag
  ClearErrors
  ${GetOptions} $cmdLineParams "/CurrentUser" $currentUserFlag
  ${IfNot} ${Errors}
    StrCpy $InstallType "user"
    StrCpy $INSTDIR "$LOCALAPPDATA\Programs\${FOLDER_NAME}"
    StrCpy $InstallTypeFromCmdLine "1"
  ${EndIf}
FunctionEnd

Function InstallTypePage
  # Skip this page if installation type was specified via command line
  ${If} $InstallTypeFromCmdLine == "1"
    Abort
  ${EndIf}
  
  nsDialogs::Create 1018
  Pop $Dialog

  ${If} $Dialog == error
    Abort
  ${EndIf}

  ${NSD_CreateLabel} 0 0 100% 24u "Choose installation type:"
  Pop $0

  ${NSD_CreateRadioButton} 10 30u 100% 12u "Install for current user only (recommended)"
  Pop $RadioUser
  ${NSD_OnClick} $RadioUser OnRadioUser

  ${NSD_CreateRadioButton} 10 50u 100% 12u "Install for all users (requires administrator privileges)"
  Pop $RadioAllUsers
  ${NSD_OnClick} $RadioAllUsers OnRadioAllUsers

  ${If} $InstallType == "user"
    ${NSD_Check} $RadioUser
  ${Else}
    ${NSD_Check} $RadioAllUsers
  ${EndIf}

  nsDialogs::Show
FunctionEnd

Function OnRadioUser
  StrCpy $InstallType "user"
  StrCpy $INSTDIR "$LOCALAPPDATA\Programs\${FOLDER_NAME}"
FunctionEnd

Function OnRadioAllUsers
  StrCpy $InstallType "allusers"
  StrCpy $INSTDIR "$PROGRAMFILES\${FOLDER_NAME}"
FunctionEnd

Function InstallTypePageLeave
FunctionEnd

Section

  Var /GLOBAL APPNAMEFULL
  StrCpy $APPNAMEFULL "${APPNAME}"

  # output
  SetOutPath $INSTDIR

  # uninstall previous version
  IfFileExists "$INSTDIR\uninstall.exe" 0 noprevious
  ExecWait '"$INSTDIR\uninstall.exe" /S _?=$INSTDIR'
  noprevious:

  # all files
  #SetOverWrite try
  File /r /x "p2" "${INPUT_DIR}\*"

  # uninstall.exe
  WriteUninstaller "$INSTDIR\uninstall.exe"
  # AccessControl plugin not available in standard NSIS installation
  # AccessControl::GrantOnFile "$INSTDIR" "(BU)" "GenericRead + GenericWrite + Delete"
  # Pop $0
  # ${If} $0 == error
  #    Pop $0
  #  DetailPrint `AccessControl error: $0`
  # ${EndIf}

  # shortcuts
  CreateDirectory  "$SMPROGRAMS\$APPNAMEFULL"
  CreateShortCut   "$SMPROGRAMS\$APPNAMEFULL\$APPNAMEFULL.lnk" "$INSTDIR\${APPEXE}"
  WriteINIStr      "$SMPROGRAMS\$APPNAMEFULL\$APPNAMEFULL Website.URL" "InternetShortcut" "URL" "https://www.portfolio-performance.info"
  WriteINIStr      "$SMPROGRAMS\$APPNAMEFULL\$APPNAMEFULL Forum.URL" "InternetShortcut" "URL" "https://forum.portfolio-performance.info"
  CreateShortCut   "$SMPROGRAMS\$APPNAMEFULL\Uninstall $APPNAMEFULL.lnk" "$INSTDIR\uninstall.exe"

  # register uninstaller based on installation type
  ${If} $InstallType == "allusers"
    SetShellVarContext all
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\$APPNAMEFULL" "DisplayName" "$APPNAMEFULL"
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\$APPNAMEFULL" "UninstallString" "$\"$INSTDIR\uninstall.exe$\""
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\$APPNAMEFULL" "QuietUninstallString" "$\"$INSTDIR\uninstall.exe$\" /S"
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\$APPNAMEFULL" "InstallLocation" "$\"$INSTDIR$\""
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\$APPNAMEFULL" "DisplayIcon" "$\"$INSTDIR\${APPEXE}$\""
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\$APPNAMEFULL" "DisplayVersion" "${SOFTWARE_VERSION}"
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\$APPNAMEFULL" "Publisher" "${PUBLISHER}"
    WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\$APPNAMEFULL" "NoModify" 1
    WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\$APPNAMEFULL" "NoRepair" 1
    WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\$APPNAMEFULL" "EstimatedSize" ${INSTALLSIZE}
  ${Else}
    SetShellVarContext current
  WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\$APPNAMEFULL" "DisplayName" "$APPNAMEFULL"
  WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\$APPNAMEFULL" "UninstallString" "$\"$INSTDIR\uninstall.exe$\""
  WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\$APPNAMEFULL" "QuietUninstallString" "$\"$INSTDIR\uninstall.exe$\" /S"
  WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\$APPNAMEFULL" "InstallLocation" "$\"$INSTDIR$\""
  WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\$APPNAMEFULL" "DisplayIcon" "$\"$INSTDIR\${APPEXE}$\""
  WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\$APPNAMEFULL" "DisplayVersion" "${SOFTWARE_VERSION}"
  WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\$APPNAMEFULL" "Publisher" "${PUBLISHER}"
  WriteRegDWORD HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\$APPNAMEFULL" "NoModify" 1
  WriteRegDWORD HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\$APPNAMEFULL" "NoRepair" 1
  WriteRegDWORD HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\$APPNAMEFULL" "EstimatedSize" ${INSTALLSIZE} 
  ${EndIf}

SectionEnd

Section "Uninstall"

  # Detect installation type from registry
  ReadRegStr $0 HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "DisplayName"
  ${If} $0 != ""
    SetShellVarContext all
  ${Else}
    SetShellVarContext current
  ${EndIf}
  # all files but not workspace
  RMDir /r $INSTDIR\configuration
  RMDir /r $INSTDIR\features
  RMDir /r $INSTDIR\p2
  RMDir /r $INSTDIR\plugins
  Delete $INSTDIR\artifacts.xml
  Delete $INSTDIR\${APPEXE}
  Delete $INSTDIR\${APPINI}

  # remove uninstaller from the registry
ReadRegStr $0 HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}" "DisplayName"
  ${If} $0 != ""
    DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}"
  ${Else}
    DeleteRegKey HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}"
  ${EndIf}
  # remove shortcuts
  RMDir /r "$SMPROGRAMS\${APPNAME}"
#  Delete "$SMPROGRAMS\${APPNAME}"
  Delete "$DESKTOP\${APPNAME}.lnk"

  # remove uninstaller
  Delete $INSTDIR\uninstall.exe

  # remove install directory if empty
  # see http://nsis.sourceforge.net/Delete_dir_only_if_empty
  StrCpy $0 "$INSTDIR"
  Call un.DeleteDirIfEmpty

SectionEnd


Function un.DeleteDirIfEmpty
  FindFirst $R0 $R1 "$0\*.*"
  strcmp $R1 "." 0 NoDelete
   FindNext $R0 $R1
   strcmp $R1 ".." 0 NoDelete
    ClearErrors
    FindNext $R0 $R1
    IfErrors 0 NoDelete
     FindClose $R0
     Sleep 1000
     RMDir "$0"
  NoDelete:
   FindClose $R0
FunctionEnd
