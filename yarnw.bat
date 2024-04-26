@rem SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
@rem
@rem SPDX-License-Identifier: EPL-2.0

@echo off
setlocal
set script_dir=%~dp0
set node_bin=%script_dir%.node
set Path=%node_bin%;%Path%
%node_bin%\yarn.CMD %*
set exit_code=%ERRORLEVEL%
endlocal & if %exit_code% neq 0 exit /b %exit_code%
