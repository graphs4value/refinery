/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import CodeIcon from '@mui/icons-material/Code';
import SchemaRoundedIcon from '@mui/icons-material/SchemaRounded';
import TableChartIcon from '@mui/icons-material/TableChart';
import ToggleButton from '@mui/material/ToggleButton';
import ToggleButtonGroup from '@mui/material/ToggleButtonGroup';
import { observer } from 'mobx-react-lite';

import type ThemeStore from './theme/ThemeStore';

function PaneButtons({
  themeStore,
  hideLabel,
}: {
  themeStore: ThemeStore;
  hideLabel?: boolean;
}): JSX.Element {
  return (
    <ToggleButtonGroup
      size={hideLabel ? 'small' : 'medium'}
      className="rounded"
      sx={{
        '.MuiToggleButton-root': {
          ...(hideLabel
            ? {}
            : {
                paddingBlock: '6px',
              }),
          fontSize: '1rem',
          lineHeight: '1.5',
        },
        ...(hideLabel
          ? {}
          : {
              '& svg': {
                margin: '0 6px 0 -4px',
              },
            }),
      }}
    >
      <ToggleButton
        value="code"
        selected={themeStore.showCode}
        onClick={(event) => {
          if (event.shiftKey || event.ctrlKey) {
            themeStore.setSelectedPane('code');
          } else {
            themeStore.toggleCode();
          }
        }}
      >
        <CodeIcon fontSize="small" />
        {!hideLabel && 'Code'}
      </ToggleButton>
      <ToggleButton
        value="graph"
        selected={themeStore.showGraph}
        onClick={(event) => {
          if (event.shiftKey || event.ctrlKey) {
            themeStore.setSelectedPane('graph');
          } else {
            themeStore.toggleGraph();
          }
        }}
      >
        <SchemaRoundedIcon fontSize="small" />
        {!hideLabel && 'Graph'}
      </ToggleButton>
      <ToggleButton
        value="table"
        selected={themeStore.showTable}
        onClick={(event) => {
          if (event.shiftKey || event.ctrlKey) {
            themeStore.setSelectedPane('table');
          } else {
            themeStore.toggleTable();
          }
        }}
      >
        <TableChartIcon fontSize="small" />
        {!hideLabel && 'Table'}
      </ToggleButton>
    </ToggleButtonGroup>
  );
}

PaneButtons.defaultProps = {
  hideLabel: false,
};

export default observer(PaneButtons);
