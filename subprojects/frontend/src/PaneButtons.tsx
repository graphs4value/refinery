/*
 * SPDX-FileCopyrightText: 2021-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import CodeIcon from '@mui/icons-material/Code';
import SchemaRoundedIcon from '@mui/icons-material/SchemaRounded';
import TableChartIcon from '@mui/icons-material/TableChart';
import ToggleButton from '@mui/material/ToggleButton';
import ToggleButtonGroup from '@mui/material/ToggleButtonGroup';
import { alpha, styled } from '@mui/material/styles';
import { observer } from 'mobx-react-lite';

import type ThemeStore from './theme/ThemeStore';

const PaneButtonGroup = styled(ToggleButtonGroup, {
  name: 'PaneButtons-Group',
  shouldForwardProp: (prop) => prop !== 'hideLabel',
})<{ hideLabel: boolean }>(({ theme, hideLabel }) => {
  const color =
    theme.palette.mode === 'dark'
      ? theme.palette.primary.main
      : theme.palette.text.primary;
  return {
    gap: theme.spacing(1),
    '.MuiToggleButton-root': {
      fontSize: '1rem',
      lineHeight: '1.5',
      // Must remove margin along with the border to avoid the button
      // moving around (into the space of the missing border) when selected.
      margin: '0',
      border: 'none',
      ...(hideLabel ? {} : { paddingBlock: 6 }),
      '&::before': {
        content: '" "',
        position: 'absolute',
        bottom: 0,
        left: 0,
        width: '0%',
        height: '2px',
        background: color,
        transition: theme.transitions.create('width', {
          duration: theme.transitions.duration.standard,
        }),
      },
      '&.MuiToggleButtonGroup-grouped': {
        borderTopLeftRadius: theme.shape.borderRadius,
        borderTopRightRadius: theme.shape.borderRadius,
        borderBottomLeftRadius: 0,
        borderBottomRightRadius: 0,
      },
      '&:not(.Mui-selected)': {
        color: theme.palette.text.secondary,
      },
      '&.Mui-selected': {
        color,
        '&::before': {
          width: '100%',
        },
        '&:not(:active)': {
          background: 'transparent',
        },
        '&:hover': {
          background: alpha(
            theme.palette.text.primary,
            theme.palette.action.hoverOpacity,
          ),
          '@media (hover: none)': {
            background: 'transparent',
          },
        },
      },
    },
    ...(hideLabel
      ? {}
      : {
          '& svg': {
            margin: '0 6px 0 -4px',
          },
        }),
  };
});

function PaneButtons({
  themeStore,
  hideLabel,
}: {
  themeStore: ThemeStore;
  hideLabel?: boolean;
}): JSX.Element {
  return (
    <PaneButtonGroup
      size={hideLabel ? 'small' : 'medium'}
      hideLabel={hideLabel ?? PaneButtons.defaultProps.hideLabel}
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
            themeStore.setSelectedPane('graph', event.shiftKey);
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
            themeStore.setSelectedPane('table', event.shiftKey);
          } else {
            themeStore.toggleTable();
          }
        }}
      >
        <TableChartIcon fontSize="small" />
        {!hideLabel && 'Table'}
      </ToggleButton>
    </PaneButtonGroup>
  );
}

PaneButtons.defaultProps = {
  hideLabel: false,
};

export default observer(PaneButtons);
