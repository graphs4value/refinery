/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import CheckIcon from '@mui/icons-material/Check';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import DesktopWindowsIcon from '@mui/icons-material/DesktopWindowsOutlined';
import FileOpenIcon from '@mui/icons-material/FileOpen';
import Alert from '@mui/material/Alert';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import CircularProgress from '@mui/material/CircularProgress';
import Divider from '@mui/material/Divider';
import Stack from '@mui/material/Stack';
import TextField from '@mui/material/TextField';
import visuallyHidden from '@mui/utils/visuallyHidden';
import { observer } from 'mobx-react-lite';
import {
  type KeyboardEvent,
  type SubmitEvent,
  useCallback,
  useEffect,
  useId,
  useLayoutEffect,
  useRef,
  useState,
} from 'react';

import Dialog from '../dialog/Dialog';
import DialogActionBar from '../dialog/DialogActionBar';
import DialogTitleBar from '../dialog/DialogTitleBar';
import type EditorStore from '../editor/EditorStore';
import getLogger from '../utils/getLogger';
import isElectron from '../utils/isElectron';

import { parseShareURI } from './shareURI';

const log = getLogger('persistence.ShareDialog');

const ELECTRON_SHARE_BASE_URL = 'https://refinery.services';

function createShareURI(fragment: string): string {
  const url = isElectron
    ? new URL(ELECTRON_SHARE_BASE_URL)
    : new URL(window.location.pathname, window.location.origin);
  url.hash = fragment;
  return url.href;
}

function createDesktopURI(fragment: string): string {
  return `refinery://open/${fragment}`;
}

async function readClipboard(): Promise<string | undefined> {
  if (navigator.clipboard === undefined) {
    return undefined;
  }
  return navigator.clipboard.readText();
}

const ShareDialog = observer(function ShareDialog({
  editorStore,
  dialogID,
}: {
  editorStore: EditorStore | undefined;
  dialogID: string;
}): React.ReactElement {
  const copyStatusID = useId();
  const openLinkFormID = useId();
  const requestID = useRef(0);
  const openLinkInputRef = useRef<HTMLInputElement>(null);
  const dialogWasOpen = useRef(false);
  const openerRef = useRef<HTMLElement | null>(null);
  const restoreFocusAfterClose = useRef(true);
  const [fragment, setFragment] = useState<string>();
  const [error, setError] = useState<string>();
  const [copied, setCopied] = useState(false);
  const [uriToOpen, setURIToOpen] = useState('');

  const dialogMode = editorStore?.shareDialogOpen;
  const open = dialogMode !== undefined;
  const copyOnOpen = dialogMode === 'copyLink';
  const pasteOnOpen = dialogMode === 'pasteLink';

  useLayoutEffect(() => {
    if (open && !dialogWasOpen.current) {
      // Capture the opener before MUI's focus trap moves focus into the dialog.
      const activeElement = document.activeElement;
      openerRef.current =
        activeElement instanceof HTMLElement ? activeElement : null;
    }
    dialogWasOpen.current = open;
  }, [open]);

  const copyURI = useCallback((uri: string, expectedRequestID: number) => {
    if (navigator.clipboard === undefined) {
      setError('Failed to copy share URI');
      return;
    }
    navigator.clipboard
      .writeText(uri)
      .then(() => {
        if (requestID.current === expectedRequestID) {
          setCopied(true);
        }
      })
      .catch((err: unknown) => {
        log.error({ err }, 'Failed to copy share URI');
        if (requestID.current === expectedRequestID) {
          setError('Failed to copy share URI');
        }
      });
  }, []);

  const pasteClipboardLink = useCallback((overwrite: boolean) => {
    const currentRequestID = requestID.current;
    const focusInput = () => {
      window.requestAnimationFrame(() => {
        if (requestID.current === currentRequestID) {
          openLinkInputRef.current?.focus();
        }
      });
    };
    readClipboard()
      .then((clipboardText) => {
        const uri = clipboardText?.trim();
        if (uri !== undefined && parseShareURI(uri) !== undefined) {
          setURIToOpen((currentURI) => {
            if (
              (!overwrite && currentURI !== '') ||
              requestID.current !== currentRequestID
            ) {
              return currentURI;
            }
            return uri;
          });
        }
        focusInput();
      })
      .catch((err: unknown) => {
        log.debug({ err }, 'Failed to read shared link from clipboard');
        focusInput();
      });
  }, []);

  useEffect(() => {
    if (!open || editorStore === undefined) {
      return;
    }
    const currentRequestID = requestID.current + 1;
    requestID.current = currentRequestID;
    restoreFocusAfterClose.current = true;
    setFragment(undefined);
    setError(undefined);
    setCopied(false);
    setURIToOpen('');

    if (pasteOnOpen) {
      pasteClipboardLink(false);
    }

    editorStore
      .getShareFragment()
      .then((newFragment) => {
        if (requestID.current !== currentRequestID) {
          return;
        }
        setFragment(newFragment);
        if (copyOnOpen) {
          copyURI(createShareURI(newFragment), currentRequestID);
        }
      })
      .catch((err: unknown) => {
        log.error({ err }, 'Failed to create share URI');
        if (requestID.current === currentRequestID) {
          setError('Failed to create share URI');
        }
      });
  }, [
    copyOnOpen,
    copyURI,
    dialogMode,
    editorStore,
    open,
    pasteClipboardLink,
    pasteOnOpen,
  ]);

  const close = () => {
    requestID.current += 1;
    editorStore?.closeShareDialog();
  };

  const shareURI =
    fragment === undefined ? undefined : createShareURI(fragment);
  const desktopURI =
    isElectron || fragment === undefined
      ? undefined
      : createDesktopURI(fragment);
  const hashToOpen = parseShareURI(uriToOpen);

  const copy = () => {
    if (shareURI === undefined) {
      return;
    }
    copyURI(shareURI, requestID.current);
  };

  const handleKeyDown = (event: KeyboardEvent<HTMLDivElement>) => {
    if (
      event.defaultPrevented ||
      !event.shiftKey ||
      (!event.ctrlKey && !event.metaKey) ||
      event.altKey
    ) {
      return;
    }
    switch (event.key.toLowerCase()) {
      case 'x':
        event.preventDefault();
        copy();
        break;
      case 'v':
        event.preventDefault();
        pasteClipboardLink(true);
        break;
    }
  };

  const openSharedModel = (event: SubmitEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (hashToOpen === undefined || editorStore === undefined) {
      return;
    }
    restoreFocusAfterClose.current = false;
    editorStore.openShare(hashToOpen);
    close();
  };

  return (
    <Dialog
      open={open}
      onClose={close}
      fullWidth
      maxWidth="sm"
      // MUI captures focus restoration when the focus trap opens, so keep it
      // disabled and restore the appropriate target explicitly after closing.
      disableRestoreFocus
      slotProps={{
        paper: { id: dialogID, onKeyDown: handleKeyDown },
        transition: {
          onExited: () => {
            if (restoreFocusAfterClose.current) {
              openerRef.current?.focus();
            }
          },
        },
      }}
    >
      <DialogTitleBar close={close} title="Share model" />
      <Box sx={{ minWidth: 0, p: 2 }}>
        {error !== undefined && <Alert severity="error">{error}</Alert>}
        {shareURI === undefined && error === undefined ? (
          <Stack direction="row" spacing={2} sx={{ alignItems: 'center' }}>
            <CircularProgress size={24} />
            <span>Creating share URI</span>
          </Stack>
        ) : (
          shareURI !== undefined && (
            <Box
              component="button"
              type="button"
              aria-label="Copy share URI"
              aria-describedby={copyStatusID}
              onClick={copy}
              sx={(theme) => ({
                backgroundColor: theme.palette.action.hover,
                ...theme.typography.editor,
                border: 0,
                borderRadius: 1,
                color: theme.palette.text.primary,
                cursor: 'pointer',
                display: 'block',
                maxWidth: '100%',
                overflowX: 'auto',
                px: 1.5,
                py: 1,
                textAlign: 'left',
                whiteSpace: 'nowrap',
                width: '100%',
                userSelect: 'all',
                scrollbarWidth: 'none',
                '::-webkit-scrollbar': {
                  background: 'transparent',
                  width: 0,
                  height: 0,
                },
              })}
            >
              {shareURI}
            </Box>
          )
        )}
        <Divider sx={{ my: 2 }}>Open a shared link</Divider>
        <Box component="form" id={openLinkFormID} onSubmit={openSharedModel}>
          <TextField
            inputRef={openLinkInputRef}
            label="Shared link"
            value={uriToOpen}
            onChange={(event) => setURIToOpen(event.target.value)}
            error={uriToOpen !== '' && hashToOpen === undefined}
            helperText={
              uriToOpen !== '' && hashToOpen === undefined
                ? 'Enter a valid shared link'
                : undefined
            }
            autoComplete="off"
            fullWidth
            size="small"
            spellCheck={false}
            slotProps={{
              htmlInput: {
                sx: (theme) => ({
                  ...theme.typography.editor,
                }),
              },
            }}
          />
        </Box>
      </Box>
      <DialogActionBar>
        <Button
          type="submit"
          form={openLinkFormID}
          disabled={hashToOpen === undefined}
          startIcon={<FileOpenIcon />}
          color="inherit"
        >
          Open link
        </Button>
        <Button
          disabled={shareURI === undefined}
          onClick={copy}
          aria-describedby={copyStatusID}
          startIcon={copied ? <CheckIcon /> : <ContentCopyIcon />}
          color="inherit"
        >
          {copied ? 'Copied' : 'Copy link'}
        </Button>
        {!isElectron && (
          <Button
            component="a"
            disabled={desktopURI === undefined}
            href={desktopURI ?? ''}
            target="_blank"
            rel="noreferrer"
            startIcon={<DesktopWindowsIcon />}
          >
            Open in desktop
          </Button>
        )}
      </DialogActionBar>
      <Box
        component="span"
        id={copyStatusID}
        role="status"
        aria-live="polite"
        aria-atomic="true"
        sx={visuallyHidden}
      >
        {copied ? 'Share link copied to clipboard' : ''}
      </Box>
    </Dialog>
  );
});

export default ShareDialog;
