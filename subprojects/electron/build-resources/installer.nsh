; SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
;
; SPDX-License-Identifier: EPL-2.0

!addplugindir /x86-unicode "${BUILD_RESOURCES_DIR}\nsis-x86-unicode"

!include "WinMessages.nsh"

; Notify running processes (e.g. Explorer) that the environment changed, so new
; shells they spawn pick up the change without a logoff. Already-open terminals
; snapshot their environment at launch and are not affected. Uses a timeout so a
; non-responsive window cannot hang the installer.
!macro BroadcastEnvChange
  SendMessage ${HWND_BROADCAST} ${WM_WININICHANGE} 0 "STR:Environment" /TIMEOUT=5000
!macroend

!macro customInstall
  ; AddValueEx writes REG_EXPAND_SZ (matching a typical user PATH) and skips
  ; duplicates, so re-running it on every upgrade is harmless.
  EnVar::SetHKCU
  EnVar::AddValueEx "Path" "$INSTDIR\bin"
  Pop $0
  DetailPrint "Adding $INSTDIR\bin to PATH: $0"
  !insertmacro BroadcastEnvChange
!macroend

!macro customUnInstall
  ; On upgrade the old uninstaller runs before the new install re-adds the entry.
  EnVar::SetHKCU
  EnVar::DeleteValue "Path" "$INSTDIR\bin"
  Pop $0
  DetailPrint "Removing $INSTDIR\bin from PATH: $0"
  !insertmacro BroadcastEnvChange
!macroend
