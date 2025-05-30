/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import Stack from '@mui/material/Stack';
import { Suspense, lazy } from 'react';

import Loading from '../Loading';
import type EditorStore from '../editor/EditorStore';

const ChatArea = lazy(() => import('./ChatArea'));

export default function ChatPane({
  editorStore,
}: {
  editorStore: EditorStore | undefined;
}): React.ReactElement {
  return (
    <Stack
      direction="column"
      sx={{ height: '100%', overflow: 'auto', alignItems: 'center' }}
    >
      <Suspense fallback={<Loading />}>
        <ChatArea editorStore={editorStore} />
      </Suspense>
    </Stack>
  );
}
