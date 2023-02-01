# Windows installer definition for an Eclipse based application

# required (if not uncomment below) input parameters:
!define APPNAME "Portfolio Performance"
!define EXECUTABLENAME "PortfolioPerformance"
!define APPEXE "PortfolioPerformance.exe"
!define APPINI "PortfolioPerformance.ini"
!define FOLDER_NAME "PortfolioPerformance"
!define INPUT_DIR "..\target\products\name.abuchen.portfolio.distro.product\win32\win32\x86_64\portfolio"
!define INSTDIR "$LOCALAPPDATA\Programs\${FOLDER_NAME}"
!define INSTALLSIZE 177818 # size (in kB)
!define PUBLISHER "Andreas Buchen"

!include nsDialogs.nsh

OutFile "..\target\products\${EXECUTABLENAME}-${SOFTWARE_VERSION}-setup.exe"
Name "${APPNAME}"

BrandingText " "

ChangeUI all "${NSISDIR}\Contrib\UIs\sdbarker_tiny.exe"

Icon ".\install.ico"
ShowInstDetails nevershow
InstProgressFlags smooth
ManifestDPIAware true

InstallDir "${INSTDIR}"

Page directory
Page instfiles

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
  File /r "${INPUT_DIR}\*"

  # uninstall.exe
  WriteUninstaller "$INSTDIR\uninstall.exe"

  AccessControl::GrantOnFile "$INSTDIR" "(BU)" "GenericRead + GenericWrite + Delete"
  Pop $0
  ${If} $0 == error
    Pop $0
    DetailPrint `AccessControl error: $0`
  ${EndIf}

  # shortcuts
  CreateDirectory  "$SMPROGRAMS\$APPNAMEFULL"
  CreateShortCut   "$SMPROGRAMS\$APPNAMEFULL\$APPNAMEFULL.lnk" "$INSTDIR\${APPEXE}"
  WriteINIStr      "$SMPROGRAMS\$APPNAMEFULL\$APPNAMEFULL Website.URL" "InternetShortcut" "URL" "https://www.portfolio-performance.info"
  WriteINIStr      "$SMPROGRAMS\$APPNAMEFULL\$APPNAMEFULL Forum.URL" "InternetShortcut" "URL" "https://forum.portfolio-performance.info"
  CreateShortCut   "$SMPROGRAMS\$APPNAMEFULL\Uninstall $APPNAMEFULL.lnk" "$INSTDIR\uninstall.exe"

  # register uninstaller
  # see http://nsis.sourceforge.net/A_simple_installer_with_start_menu_shortcut_and_uninstaller
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\$APPNAMEFULL" "DisplayName" "$APPNAMEFULL"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\$APPNAMEFULL" "UninstallString" "$\"$INSTDIR\uninstall.exe$\""
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\$APPNAMEFULL" "QuietUninstallString" "$\"$INSTDIR\uninstall.exe$\" /S"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\$APPNAMEFULL" "InstallLocation" "$\"$INSTDIR$\""
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\$APPNAMEFULL" "DisplayIcon" "$\"$INSTDIR\${APPEXE}$\""
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\$APPNAMEFULL" "DisplayVersion" "${SOFTWARE_VERSION}"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\$APPNAMEFULL" "Publisher" "${PUBLISHER}"
  # there is no option for modifying or repairing the install
  WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\$APPNAMEFULL" "NoModify" 1
  WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\$APPNAMEFULL" "NoRepair" 1
  # set the INSTALLSIZE constant (!defined at the top of this script) so Add/Remove Programs can accurately report the size
  WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\$APPNAMEFULL" "EstimatedSize" ${INSTALLSIZE}

SectionEnd

Section "Uninstall"

  # all files but not workspace
  RMDir /r $INSTDIR\configuration
  RMDir /r $INSTDIR\features
  RMDir /r $INSTDIR\p2
  RMDir /r $INSTDIR\plugins
  Delete $INSTDIR\artifacts.xml
  Delete $INSTDIR\${APPEXE}
  Delete $INSTDIR\${APPINI}

  # remove uninstaller from the registry
  DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME}"

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
