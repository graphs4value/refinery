/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import CloseIcon from '@mui/icons-material/Close';
import SendIcon from '@mui/icons-material/Send';
import CircularProgress from '@mui/material/CircularProgress';
import IconButton from '@mui/material/IconButton';
import Stack from '@mui/material/Stack';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import { observer, useLocalObservable } from 'mobx-react-lite';
import { useEffect, useRef } from 'react';

import Loading from '../Loading';
import Tooltip from '../Tooltip';
import type EditorStore from '../editor/EditorStore';

import ChatStore, { type Message } from './ChatStore';

const ChatMessages = observer(function ChatMessages({
  messages,
  running,
}: {
  messages: Message[];
  running: boolean;
}): React.ReactElement {
  const bottomRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  });

  return (
    <Stack
      direction="column"
      sx={(theme) => ({
        flexGrow: 1,
        p: 2,
        pb: 0,
        gap: 2,
        overflow: 'auto',
        width: '100%',
        '.message-text--user': {
          alignSelf: 'flex-end',
          textAlign: 'right',
          p: 2,
          ml: 4,
          borderRadius: theme.shape.borderRadius,
          background: theme.palette.primary.main,
          color: theme.palette.primary.contrastText,
        },
        '.message-text--assistant': {
          alignSelf: 'flex-start',
          p: 2,
          mr: 4,
          borderRadius: theme.shape.borderRadius,
          background: theme.palette.outer.elevated,
        },
        '.message-text--refinery': {
          px: 2,
          color: theme.palette.text.secondary,
          fontWeight: theme.typography.fontWeightMedium,
        },
        '.message-text--error': {
          px: 2,
          color: theme.palette.error.main,
          fontWeight: theme.typography.fontWeightMedium,
        },
      })}
    >
      {messages.map(({ id, role, content }) => (
        <Typography key={id} className={`message-text message-text--${role}`}>
          {content}
        </Typography>
      ))}
      {running && (
        <CircularProgress
          size={60}
          color="inherit"
          sx={{ alignSelf: 'center' }}
        />
      )}
      <Stack ref={bottomRef} />
    </Stack>
  );
});

function ChatArea({
  editorStore,
}: {
  editorStore: EditorStore | undefined;
}): React.ReactElement {
  const chatStore = useLocalObservable(() => new ChatStore());

  useEffect(() => {
    chatStore.setEditorStore(editorStore);
  }, [chatStore, editorStore]);

  if (editorStore === undefined) {
    return <Loading />;
  }

  return (
    <Stack
      direction="column"
      sx={{ height: '100%', width: '100%', overflow: 'auto' }}
    >
      <ChatMessages messages={chatStore.log} running={chatStore.running} />
      <Stack
        direction="row"
        sx={{ p: 1, gap: 1, width: '100%', alignItems: 'center' }}
      >
        <TextField
          value={chatStore.running ? '' : chatStore.message}
          disabled={chatStore.running}
          onChange={(event) => chatStore.setMessage(event.target.value)}
          multiline
          minRows={3}
          maxRows={16}
          placeholder={
            chatStore.running ? 'Generating model' : 'Type a message'
          }
          variant="outlined"
          size="small"
          sx={{ flexGrow: 1 }}
        />
        {chatStore.running ? (
          <Tooltip title="Cancel">
            <IconButton onClick={() => chatStore.cancel()}>
              <CloseIcon />
            </IconButton>
          </Tooltip>
        ) : (
          <Tooltip title="Send message">
            <IconButton
              onClick={() => chatStore.generate()}
              disabled={!chatStore.canGenerate}
            >
              <SendIcon />
            </IconButton>
          </Tooltip>
        )}
      </Stack>
    </Stack>
  );
}

export default observer(ChatArea);
