!define PACKAGE_NAME "Portfolio Performance"
!define EXECUTABLE_NAME "PortfolioPerformance"
!define COMPANY_NAME "Andreas Buchen"
!define COMPANY_URL "http://www.portfolio-performance.info"

#
# PARAMETERS WILL BE PASSED BY makensis:
# makensis -DARCHITECTURE=x64 -DSOFTWARE_VERSION=0.30.1 setup.nsi
#
!if ${ARCHITECTURE} == x64
  !define PROCESSOR_TYPE "x86_64"
!else
  !define PROCESSOR_TYPE "x86"
!endif

#
# COMPRESSION
# USE lzma, otherwise Windows will throw an error when the script was compiled under linux.
#
SetCompressor lzma

#
# GENERAL SYMBOL DEFINITIONS
#
Name "${PACKAGE_NAME}"
!define REGKEY "SOFTWARE\$(^Name)"
!define VERSION ${SOFTWARE_VERSION}
!define COMPANY "${COMPANY_NAME}"
!define URL "${COMPANY_URL}"

#
# SOURCE CODE, PROCESSOR DEFINITIONS
#
!define SOURCE_CODE "..\..\portfolio-product\target\products\name.abuchen.portfolio.distro.product\win32\win32\${PROCESSOR_TYPE}\portfolio"

#
# MULTIUSER SYMBOL DEFINITIONS
#
!define MULTIUSER_MUI
!define MULTIUSER_EXECUTIONLEVEL Highest
!define MULTIUSER_INSTALLMODE_COMMANDLINE
!define MULTIUSER_INSTALLMODE_INSTDIR "${PACKAGE_NAME}"
!define MULTIUSER_INSTALLMODE_INSTDIR_REGISTRY_KEY "${REGKEY}"
!define MULTIUSER_INSTALLMODE_INSTDIR_REGISTRY_VALUE "Path"

#
# MUI SYMBOL DEFINITIONS
#
!define MUI_ICON ".\NSIS\install.ico"
!define MUI_UNICON ".\NSIS\uninstall.ico"

#
# MUI SETTINGS / HEADER
#
!define MUI_HEADERIMAGE
!define MUI_HEADERIMAGE_RIGHT
!define MUI_HEADERIMAGE_BITMAP ".\NSIS\header.bmp"
!define MUI_HEADERIMAGE_UNBITMAP ".\NSIS\header.bmp"
!define MUI_WELCOMEPAGE_TITLE_3LINES

#
# MUI SETTINGS / WIZARD
#
!define MUI_WELCOMEFINISHPAGE_BITMAP ".\NSIS\wizard.bmp"
!define MUI_UNWELCOMEFINISHPAGE_BITMAP ".\NSIS\wizard.bmp"
!define MUI_STARTMENUPAGE_REGISTRY_KEY ${REGKEY}
!define MUI_STARTMENUPAGE_REGISTRY_VALUENAME StartMenuGroup
!define MUI_STARTMENUPAGE_DEFAULTFOLDER "${PACKAGE_NAME}"
!define MUI_FINISHPAGE_RUN $INSTDIR\${EXECUTABLE_NAME}.exe

#
#--------------------------------------------------------------INCLUDES
#

#
# MODERN INTERFACE
#
!include MultiUser.nsh
!include Sections.nsh
!include MUI2.nsh

#
# PROFILES
#
#!include "NSIS\Libs\NTProfiles.nsh"

#
# RESERVED FILES
#
ReserveFile "${NSISDIR}\Plugins\x86-unicode\AdvSplash.dll"

#
#--------------------------------------------------------------VARIABLES
#

Var StartMenuGroup

#
#--------------------------------------------------------------PAGES
#

!insertmacro MUI_PAGE_WELCOME
!insertmacro MUI_PAGE_DIRECTORY
!insertmacro MUI_PAGE_STARTMENU Application $StartMenuGroup
!insertmacro MUI_PAGE_INSTFILES
!insertmacro MUI_PAGE_FINISH
!insertmacro MUI_UNPAGE_CONFIRM
!insertmacro MUI_UNPAGE_INSTFILES

#
# INSTALLER LANGUAGES
#
!insertmacro MUI_LANGUAGE English
!insertmacro MUI_LANGUAGE German
!insertmacro MUI_LANGUAGE Spanish

#
#--------------------------------------------------------------INCLUDES
#

#
# INSTALLER VALUES
#
OutFile ..\..\portfolio-product\target\products\${EXECUTABLE_NAME}-${VERSION}-win-${PROCESSOR_TYPE}-setup.exe
InstallDir $INSTDIR
CRCCheck on
XPStyle on
ShowInstDetails show
VIProductVersion 0.2.0.0
VIAddVersionKey /LANG=${LANG_ENGLISH} ProductName "${PACKAGE_NAME}"
VIAddVersionKey /LANG=${LANG_ENGLISH} ProductVersion "${VERSION}"
VIAddVersionKey /LANG=${LANG_ENGLISH} CompanyName "${COMPANY}"
VIAddVersionKey /LANG=${LANG_ENGLISH} CompanyWebsite "${URL}"
VIAddVersionKey /LANG=${LANG_ENGLISH} FileVersion "${VERSION}"
VIAddVersionKey /LANG=${LANG_ENGLISH} FileDescription ""
VIAddVersionKey /LANG=${LANG_ENGLISH} LegalCopyright "${COMPANY_NAME}"
InstallDirRegKey HKLM "${REGKEY}" Path
ShowUninstDetails show

#
#--------------------------------------------------------------INSTALL SECTIONS
#

#
# SEC0000
#
Section -XCompilation SEC0000
    #
    # SET THE INSTALL PATH
    #
    SetOutPath $INSTDIR
    SetOverwrite on
      
    #
    # INSTALL APPLICATION FILES
    #
    SetOutPath $INSTDIR\configuration
    File /r "${SOURCE_CODE}\configuration\*"

    SetOutPath $INSTDIR\features
    File /r "${SOURCE_CODE}\features\*"

    SetOutPath $INSTDIR\p2
    File /r "${SOURCE_CODE}\p2\*"
    
    SetOutPath $INSTDIR\plugins
    File /r "${SOURCE_CODE}\plugins\*"

    SetOutPath $INSTDIR
    File "${SOURCE_CODE}\artifacts.xml"
    File "${SOURCE_CODE}\${EXECUTABLE_NAME}.exe"
    File "${SOURCE_CODE}\${EXECUTABLE_NAME}.ini"

    WriteRegStr HKLM "${REGKEY}\Components" "${PACKAGE_NAME}" 1
SectionEnd

#
# SEC0001
#
Section -post SEC0001
    #
    # GET THE INSTALL PATH
    #
    WriteRegStr HKLM "${REGKEY}" Path $INSTDIR
    SetOutPath $INSTDIR
    WriteUninstaller $INSTDIR\uninstall.exe
    !insertmacro MUI_STARTMENU_WRITE_BEGIN Application
    #
    # MENU/PROGRAM ICONS
    #
    SetOutPath $SMPROGRAMS\$StartMenuGroup
    SetOutPath $INSTDIR
    CreateShortcut "$SMPROGRAMS\$StartMenuGroup\$(^UninstallLink).lnk" "$INSTDIR\uninstall.exe"
    CreateShortCut "$SMPROGRAMS\$StartMenuGroup\${PACKAGE_NAME}.lnk" "$INSTDIR\${EXECUTABLE_NAME}.exe"
    CreateShortCut "$DESKTOP\${PACKAGE_NAME}.lnk" "$INSTDIR\${EXECUTABLE_NAME}.exe"
    CreateShortCut "$QUICKLAUNCH\${PACKAGE_NAME}.lnk" "$INSTDIR\${EXECUTABLE_NAME}.exe"

    #
    # REGISTRY 
    #
    !insertmacro MUI_STARTMENU_WRITE_END
    WriteRegStr HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)" DisplayName "$(^Name)"
    WriteRegStr HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)" DisplayVersion "${VERSION}"
    WriteRegStr HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)" Publisher "${COMPANY}"
    WriteRegStr HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)" URLInfoAbout "${URL}"
    WriteRegStr HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)" DisplayIcon $INSTDIR\uninstall.exe
    WriteRegStr HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)" UninstallString $INSTDIR\uninstall.exe
    WriteRegDWORD HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)" NoModify 1
    WriteRegDWORD HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)" NoRepair 1
SectionEnd

#
#--------------------------------------------------------------UNINSTALL SECTIONS
#

# Macro for selecting uninstaller sections
!macro SELECT_UNSECTION SECTION_NAME UNSECTION_ID
    Push $R0
    ReadRegStr $R0 HKLM "${REGKEY}\Components" "${SECTION_NAME}"
    StrCmp $R0 1 0 next${UNSECTION_ID}
    !insertmacro SelectSection "${UNSECTION_ID}"
    GoTo done${UNSECTION_ID}

    next${UNSECTION_ID}:
    !insertmacro UnselectSection "${UNSECTION_ID}"

    done${UNSECTION_ID}:
    Pop $R0
!macroend

#
# UNSEC0000
#
Section /o -un.XCompilation UNSEC0000
    DeleteRegValue HKLM "${REGKEY}\Components" "${PACKAGE_NAME}"
SectionEnd

#
# UNSEC0001
#
Section -un.post UNSEC0001
    #
    # DELETE REGISTRY ENTRIES
    #
    DeleteRegKey HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)"
    DeleteRegValue HKLM "${REGKEY}" StartMenuGroup
    DeleteRegValue HKLM "${REGKEY}" Path
    DeleteRegKey /IfEmpty HKLM "${REGKEY}\Components"
    DeleteRegKey /IfEmpty HKLM "${REGKEY}"
     
    #
    # DELETE FILES
    #
    Delete /REBOOTOK "$SMPROGRAMS\$StartMenuGroup\$(^UninstallLink).lnk"
    Delete /REBOOTOK "$DESKTOP\${PACKAGE_NAME}.lnk"
    Delete /REBOOTOK "$QUICKLAUNCH\${PACKAGE_NAME}.lnk"

    Delete /REBOOTOK "$INSTDIR\uninstall.exe"
    Delete /REBOOTOK "$INSTDIR\${EXECUTABLE_NAME}.exe"
    Delete /REBOOTOK "$INSTDIR\${EXECUTABLE_NAME}.ini"
    Delete /REBOOTOK "$INSTDIR\artifacts.xml"

    #
    # DELETE DIRS
    #
    RMDir /R /REBOOTOK "$SMPROGRAMS\$StartMenuGroup"

    #
    # RMDir /REBOOTOK $INSTDIR (DELETE IF NOT EMPTY => WITHOUT /R)
    #
    RMDir /R /REBOOTOK "$INSTDIR\configuration"
    RMDir /R /REBOOTOK "$INSTDIR\features"
    RMDir /R /REBOOTOK "$INSTDIR\p2"
    RMDir /R /REBOOTOK "$INSTDIR\plugins"

    RMDir /REBOOTOK "$INSTDIR"

    Push $R0
    StrCpy $R0 $StartMenuGroup 1
    StrCmp $R0 ">" no_smgroup
    
    no_smgroup:
    Pop $R0
SectionEnd

#
#--------------------------------------------------------------FUNCTIONS
#

#
# INSTALLER
#
Function .onInit
    #
    # SET THE CORRECT REGISTRY ACCESS
    #
    !if ${ARCHITECTURE} == x64
      SetRegView 64
    !else
      SetRegView 32
    !endif
    #
    # INSTALL
    #
    InitPluginsDir
    !insertmacro MULTIUSER_INIT
    !if ${ARCHITECTURE} == x64
      StrCpy $INSTDIR "$PROGRAMFILES64\${PACKAGE_NAME}"
    !else
      StrCpy $INSTDIR "$PROGRAMFILES\${PACKAGE_NAME}"
    !endif
FunctionEnd

#
# UNINSTALLER
#
Function un.onInit
    #
    # SET THE CORRECT REGISTRY ACCESS
    #    
    !if ${ARCHITECTURE} == x64
      SetRegView 64
    !else
      SetRegView 32
    !endif
    #
    # UNINSTALL
    #
    SetAutoClose true
    !insertmacro MUI_STARTMENU_GETFOLDER Application $StartMenuGroup
    !insertmacro MULTIUSER_UNINIT
    !insertmacro SELECT_UNSECTION "${PACKAGE_NAME}" ${UNSEC0000}
FunctionEnd

#
# LANGUAGE STRINGS
#
LangString ^UninstallLink ${LANG_ENGLISH} "Uninstall $(^Name)"
LangString ^UninstallLink ${LANG_GERMAN} "Uninstall $(^Name)"
LangString ^UninstallLink ${LANG_SPANISH} "Uninstall $(^Name)"