/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { observer } from 'mobx-react-lite';

import DirectionalSplitPane from './DirectionalSplitPane';
import ModelWorkArea from './ModelWorkArea';
import { useRootStore } from './RootStoreProvider';
import EditorPane from './editor/EditorPane';

export default observer(function WorkArea(): JSX.Element {
  const { themeStore, editorStore } = useRootStore();
  const lintPanelOpen = editorStore?.lintPanel.state ?? false;

  return (
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
  );
});
