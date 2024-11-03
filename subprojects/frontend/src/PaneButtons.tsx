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

import Tooltip from './Tooltip';
import type ThemeStore from './theme/ThemeStore';
import type { SelectedPane } from './theme/ThemeStore';

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

const PaneButton = observer(function PaneButton({
  themeStore,
  value,
  label,
  icon,
  hideLabel,
}: {
  themeStore: ThemeStore;
  value: SelectedPane;
  label: string;
  icon: React.ReactNode;
  hideLabel: boolean;
}): JSX.Element {
  const button = (
    <ToggleButton
      value={value}
      selected={themeStore.isShowing(value)}
      onClick={(event) => {
        if (event.shiftKey || event.ctrlKey) {
          themeStore.setSelectedPane(value, event.shiftKey);
        } else {
          themeStore.togglePane(value);
        }
      }}
    >
      {icon}
      {!hideLabel && label}
    </ToggleButton>
  );
  return hideLabel ? <Tooltip title={label}>{button}</Tooltip> : button;
});

function PaneButtons({
  themeStore,
  hideLabel,
}: {
  themeStore: ThemeStore;
  hideLabel?: boolean;
}): JSX.Element {
  const hideLabelOrDefault = hideLabel ?? false;
  return (
    <PaneButtonGroup
      size={hideLabel ? 'small' : 'medium'}
      hideLabel={hideLabelOrDefault}
    >
      <PaneButton
        themeStore={themeStore}
        value="code"
        label="Code"
        icon={<CodeIcon fontSize="small" />}
        hideLabel={hideLabelOrDefault}
      />
      <PaneButton
        themeStore={themeStore}
        value="graph"
        label="Graph"
        icon={<SchemaRoundedIcon fontSize="small" />}
        hideLabel={hideLabelOrDefault}
      />
      <PaneButton
        themeStore={themeStore}
        value="table"
        label="Table"
        icon={<TableChartIcon fontSize="small" />}
        hideLabel={hideLabelOrDefault}
      />
    </PaneButtonGroup>
  );
}

export default observer(PaneButtons);
