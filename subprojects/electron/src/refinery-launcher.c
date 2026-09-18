/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

/*
 * Tiny console launcher for the Refinery CLI on Windows.
 *
 * It mirrors the POSIX `refinery` shell script generated in
 * `electron-builder.config.mjs` (`afterPack`): it sets `ELECTRON_RUN_AS_NODE=1`
 * and runs the bundled Electron binary as Node, passing the packaged `cli.cjs`
 * and forwarding every argument verbatim.
 *
 * Design notes:
 *   - Pure Win32 + CRT, no third-party headers, so it cross-compiles with
 *     `zig cc -target x86_64-windows-gnu` (bundled MinGW-w64) with no Visual
 *     Studio or Windows SDK on the build machine.
 *   - Console subsystem with inherited handles: stdin/stdout/stderr (including
 *     redirected pipes) pass straight through, so the CLI behaves like a normal
 *     terminal program.
 *   - The child command line is built from the *raw* tail of GetCommandLineW()
 *     (everything after argv[0]); we never re-quote the user's arguments, so
 *     paths with spaces, quotes, or backslashes survive byte-for-byte.
 *   - We stop handling Ctrl+C in the launcher *after* the child is spawned, so
 *     the signal reaches the CLI (which owns the console) while the launcher
 *     just waits and then propagates the child's exit code. No cmd.exe is in
 *     the chain, so there is no "Terminate batch job (Y/N)?" prompt.
 */

#include <windows.h>

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <wchar.h>

/*
 * Location of the Electron binary and the packaged CLI entry point, relative to
 * the directory that contains this launcher. The launcher is installed in
 * `<root>\bin\refinery.exe`, so both live one directory up. Override at compile
 * time with -DREFINERY_REL_EXE / -DREFINERY_REL_CLI if the layout changes.
 */
#ifndef REFINERY_REL_EXE
#define REFINERY_REL_EXE L"..\\Refinery.exe"
#endif
#ifndef REFINERY_REL_CLI
#define REFINERY_REL_CLI L"..\\resources\\app.asar\\cli\\index.cjs"
#endif

static void fail(const wchar_t *what, DWORD err) {
    fwprintf(stderr, L"refinery: %ls (error %lu)\n", what, err);
}

/* Absolute path of this launcher's own executable. Caller frees. */
static wchar_t *self_path(void) {
    DWORD cap = MAX_PATH;
    for (;;) {
        wchar_t *buf = malloc(cap * sizeof(wchar_t));
        if (!buf) {
            return NULL;
        }
        DWORD len = GetModuleFileNameW(NULL, buf, cap);
        if (len == 0) {
            free(buf);
            return NULL;
        }
        if (len < cap) {
            return buf; /* fits: len excludes the terminator */
        }
        free(buf); /* truncated: grow and retry */
        cap *= 2;
    }
}

/* Resolve `<dir of self>\<rel>` into a clean absolute path. Caller frees. */
static wchar_t *resolve_relative(const wchar_t *self, const wchar_t *rel) {
    /* Copy self and strip the file name to get its directory. */
    size_t self_len = wcslen(self);
    wchar_t *dir = malloc((self_len + 1) * sizeof(wchar_t));
    if (!dir) {
        return NULL;
    }
    wcscpy(dir, self);
    wchar_t *slash = wcsrchr(dir, L'\\');
    if (slash) {
        slash[1] = L'\0'; /* keep the trailing backslash */
    } else {
        dir[0] = L'\0';
    }

    /* Join dir + rel. */
    size_t joined_len = wcslen(dir) + wcslen(rel) + 1;
    wchar_t *joined = malloc(joined_len * sizeof(wchar_t));
    if (!joined) {
        free(dir);
        return NULL;
    }
    _snwprintf(joined, joined_len, L"%ls%ls", dir, rel);
    free(dir);

    /* Canonicalize, collapsing the ..\ segments. */
    DWORD need = GetFullPathNameW(joined, 0, NULL, NULL);
    if (need == 0) {
        free(joined);
        return NULL;
    }
    wchar_t *full = malloc(need * sizeof(wchar_t));
    if (!full) {
        free(joined);
        return NULL;
    }
    DWORD wrote = GetFullPathNameW(joined, need, full, NULL);
    free(joined);
    if (wrote == 0 || wrote >= need) {
        free(full);
        return NULL;
    }
    return full;
}

/*
 * Return a pointer into `cmd` just past argv[0] and any following whitespace.
 * Uses the same rule the C runtime uses for the program-name field: a quoted
 * argv[0] ends at the closing quote; an unquoted one at the first whitespace.
 */
static const wchar_t *args_tail(const wchar_t *cmd) {
    const wchar_t *p = cmd;
    if (*p == L'"') {
        p++;
        while (*p && *p != L'"') {
            p++;
        }
        if (*p == L'"') {
            p++;
        }
    } else {
        while (*p && *p != L' ' && *p != L'\t') {
            p++;
        }
    }
    while (*p == L' ' || *p == L'\t') {
        p++;
    }
    return p;
}

int main(void) {
    wchar_t *self = self_path();
    if (!self) {
        fail(L"cannot determine launcher path", GetLastError());
        return 1;
    }

    wchar_t *electron = resolve_relative(self, REFINERY_REL_EXE);
    wchar_t *cli = resolve_relative(self, REFINERY_REL_CLI);
    free(self);
    if (!electron || !cli) {
        fail(L"cannot resolve bundled paths", GetLastError());
        return 1;
    }

    if (!SetEnvironmentVariableW(L"ELECTRON_RUN_AS_NODE", L"1")) {
        fail(L"cannot set ELECTRON_RUN_AS_NODE", GetLastError());
        return 1;
    }

    const wchar_t *tail = args_tail(GetCommandLineW());

    /* Build:  "<electron>" "<cli>" <verbatim tail> */
    size_t cap = wcslen(electron) + wcslen(cli) + wcslen(tail) + 8;
    wchar_t *cmdline = malloc(cap * sizeof(wchar_t));
    if (!cmdline) {
        fail(L"out of memory", ERROR_OUTOFMEMORY);
        return 1;
    }
    _snwprintf(cmdline, cap, L"\"%ls\" \"%ls\" %ls", electron, cli, tail);

    STARTUPINFOW si;
    PROCESS_INFORMATION pi;
    ZeroMemory(&si, sizeof si);
    si.cb = sizeof si;
    ZeroMemory(&pi, sizeof pi);

    BOOL ok = CreateProcessW(
        electron, /* lpApplicationName: exact path, no PATH search */
        cmdline,  /* lpCommandLine: mutable, argv[0] included */
        NULL, NULL,
        TRUE, /* bInheritHandles: share stdio, including redirected pipes */
        0,    /* no CREATE_NEW_CONSOLE: stay in the caller's console */
        NULL, /* inherit our (now-modified) environment */
        NULL, /* inherit current directory */
        &si, &pi);

    if (!ok) {
        DWORD err = GetLastError();
        fwprintf(stderr, L"refinery: failed to launch %ls (error %lu)\n",
                 electron, err);
        return 1;
    }

    /*
     * From here on the child owns Ctrl+C: ignore it in the launcher so we do not
     * die first, then wait and forward the child's exit code. The child was
     * created before this call, so it keeps the default handler and still
     * receives the signal.
     */
    SetConsoleCtrlHandler(NULL, TRUE);

    WaitForSingleObject(pi.hProcess, INFINITE);

    DWORD code = 1;
    GetExitCodeProcess(pi.hProcess, &code);
    CloseHandle(pi.hProcess);
    CloseHandle(pi.hThread);
    return (int)code;
}
