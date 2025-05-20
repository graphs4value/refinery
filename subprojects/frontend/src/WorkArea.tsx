/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import Box from '@mui/material/Box';
import Stack from '@mui/material/Stack';
import { observer } from 'mobx-react-lite';

import DirectionalSplitPane from './DirectionalSplitPane';
import ModelWorkArea from './ModelWorkArea';
import { useRootStore } from './RootStoreProvider';
import ChatPane from './chat/ChatPane';
import EditorPane from './editor/EditorPane';

export default observer(function WorkArea(): React.ReactElement {
  const { themeStore, editorStore } = useRootStore();
  const lintPanelOpen = editorStore?.lintPanel.state ?? false;

  return (
    <Stack
      direction="row"
      sx={{ width: '100%', height: '100%', overflow: 'hidden' }}
    >
      <DirectionalSplitPane
        primary={<EditorPane />}
        secondary={(horizontal) => (
          <ModelWorkArea
            touchesTop={!themeStore.showCode || !horizontal || lintPanelOpen}
          />
        )}
        primaryOnly={!themeStore.showGraph && !themeStore.showTable}
        secondaryOnly={!themeStore.showCode}
      />
      {themeStore.showChat && (
        <Box
          sx={(theme) => ({
            height: '100%',
            width: '90%',
            maxWidth: '500px',
            overflow: 'hidden',
            borderLeft: `1px solid ${theme.palette.outer.border}`,
          })}
        >
          <ChatPane editorStore={editorStore} />
        </Box>
      )}
    </Stack>
  );
});
