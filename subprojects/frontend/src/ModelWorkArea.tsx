/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import CloseIcon from '@mui/icons-material/Close';
import SentimentVeryDissatisfiedIcon from '@mui/icons-material/SentimentVeryDissatisfied';
import CircularProgress from '@mui/material/CircularProgress';
import IconButton from '@mui/material/IconButton';
import Stack from '@mui/material/Stack';
import Tab from '@mui/material/Tab';
import Tabs from '@mui/material/Tabs';
import { styled } from '@mui/material/styles';
import { observer } from 'mobx-react-lite';

import DirectionalSplitPane from './DirectionalSplitPane';
import Loading from './Loading';
import { useRootStore } from './RootStoreProvider';
import type GeneratedModelStore from './editor/GeneratedModelStore';
import GraphPane from './graph/GraphPane';
import type GraphStore from './graph/GraphStore';
import TablePane from './table/TablePane';
import type ThemeStore from './theme/ThemeStore';

const SplitGraphPane = observer(function SplitGraphPane({
  graph,
  themeStore,
}: {
  graph: GraphStore;
  themeStore: ThemeStore;
}): JSX.Element {
  return (
    <DirectionalSplitPane
      primary={<GraphPane graph={graph} />}
      secondary={<TablePane graph={graph} />}
      primaryOnly={!themeStore.showTable}
      secondaryOnly={!themeStore.showGraph}
    />
  );
});

const GenerationStatus = styled('div', {
  name: 'ModelWorkArea-GenerationStatus',
  shouldForwardProp: (prop) => prop !== 'error',
})<{ error: boolean }>(({ error, theme }) => ({
  color: error ? theme.palette.error.main : theme.palette.text.primary,
}));

const GeneratedModelPane = observer(function GeneratedModelPane({
  generatedModel,
  themeStore,
}: {
  generatedModel: GeneratedModelStore;
  themeStore: ThemeStore;
}): JSX.Element {
  const { message, error, graph } = generatedModel;

  if (graph !== undefined) {
    return <SplitGraphPane graph={graph} themeStore={themeStore} />;
  }

  return (
    <Stack
      direction="column"
      alignItems="center"
      justifyContent="center"
      height="100%"
      width="100%"
      overflow="hidden"
      my={2}
    >
      <Stack
        direction="column"
        alignItems="center"
        flexGrow={1}
        flexShrink={1}
        flexBasis={0}
        sx={(theme) => ({
          maxHeight: '6rem',
          height: 'calc(100% - 8rem)',
          marginBottom: theme.spacing(1),
          padding: error ? 0 : theme.spacing(1),
          color: theme.palette.text.secondary,
          '.MuiCircularProgress-root, .MuiCircularProgress-svg, .MuiSvgIcon-root':
            {
              height: '100% !important',
              width: '100% !important',
            },
        })}
      >
        {error ? (
          <SentimentVeryDissatisfiedIcon
            className="VisibilityDialog-emptyIcon"
            fontSize="inherit"
          />
        ) : (
          <CircularProgress color="inherit" />
        )}
      </Stack>
      <GenerationStatus error={error}>{message}</GenerationStatus>
    </Stack>
  );
});

function ModelWorkArea(): JSX.Element {
  const { editorStore, themeStore } = useRootStore();

  if (editorStore === undefined) {
    return <Loading />;
  }

  const { graph, generatedModels, selectedGeneratedModel } = editorStore;

  const generatedModelNames: string[] = [];
  const generatedModelTabs: JSX.Element[] = [];
  generatedModels.forEach((value, key) => {
    generatedModelNames.push(key);
    /* eslint-disable react/no-array-index-key -- Key is a string here, not the array index. */
    generatedModelTabs.push(
      <Tab
        label={value.title}
        key={key}
        onAuxClick={(event) => {
          if (event.button === 1) {
            editorStore.deleteGeneratedModel(key);
            event.preventDefault();
            event.stopPropagation();
          }
        }}
      />,
    );
    /* eslint-enable react/no-array-index-key */
  });
  const generatedModel =
    selectedGeneratedModel === undefined
      ? undefined
      : generatedModels.get(selectedGeneratedModel);
  const selectedIndex =
    selectedGeneratedModel === undefined
      ? 0
      : generatedModelNames.indexOf(selectedGeneratedModel) + 1;

  return (
    <Stack direction="column" height="100%" width="100%" overflow="hidden">
      <Stack
        direction="row"
        sx={(theme) => ({
          display: generatedModelNames.length === 0 ? 'none' : 'flex',
          alignItems: 'center',
          borderBottom: `1px solid ${theme.palette.outer.border}`,
        })}
      >
        <Tabs
          value={selectedIndex}
          onChange={(_event, value) => {
            if (value === 0) {
              editorStore.selectGeneratedModel(undefined);
            } else if (typeof value === 'number') {
              editorStore.selectGeneratedModel(generatedModelNames[value - 1]);
            }
          }}
          variant="scrollable"
          scrollButtons="auto"
          sx={{ flexGrow: 1 }}
        >
          <Tab label="Initial model" />
          {generatedModelTabs}
        </Tabs>
        <IconButton
          aria-label="Close generated model"
          onClick={() =>
            editorStore.deleteGeneratedModel(selectedGeneratedModel)
          }
          disabled={selectedIndex === 0}
          sx={{ mx: 1 }}
        >
          <CloseIcon fontSize="small" />
        </IconButton>
      </Stack>
      {generatedModel === undefined ? (
        <SplitGraphPane graph={graph} themeStore={themeStore} />
      ) : (
        <GeneratedModelPane
          generatedModel={generatedModel}
          themeStore={themeStore}
        />
      )}
    </Stack>
  );
}

export default observer(ModelWorkArea);
