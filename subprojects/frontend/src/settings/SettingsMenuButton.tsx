/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import ContrastIcon from '@mui/icons-material/Contrast';
import DarkModeIcon from '@mui/icons-material/DarkMode';
import LightModeIcon from '@mui/icons-material/LightMode';
import MoreVertIcon from '@mui/icons-material/MoreVert';
import IconButton from '@mui/material/IconButton';
import Menu from '@mui/material/Menu';
import MenuItem from '@mui/material/MenuItem';
import ToggleButton from '@mui/material/ToggleButton';
import ToggleButtonGroup from '@mui/material/ToggleButtonGroup';
import Tooltip from '@mui/material/Tooltip';
import { observer } from 'mobx-react-lite';
import { useId, useState } from 'react';

import { useRootStore } from '../RootStoreProvider';
import { runThemeChange } from '../ToggleDarkModeButton';
import type { ThemePreference } from '../theme/ThemeStore';

import CLISymlinkMenuItem from './CLISymlinkMenuItem';
import ServerSettingsDialog, {
  RestartServerMenuItem,
  type ServerSettingsTab,
} from './ServerSettingsDialog';

export default observer(function SettingsMenuButton(): React.ReactElement {
  const { themeStore } = useRootStore();
  const id = useId();
  const [anchorEl, setAnchorEl] = useState<HTMLElement | undefined>();
  const [serverSettingsOpen, setServerSettingsOpen] = useState(false);
  const [serverSettingsTab, setServerSettingsTab] =
    useState<ServerSettingsTab>('solver');
  const open = anchorEl !== undefined;
  const handleClose = () => setAnchorEl(undefined);
  const title = 'Settings';

  return (
    <>
      <Tooltip title={title}>
        <IconButton
          color="inherit"
          aria-controls={open ? id : undefined}
          aria-haspopup="true"
          aria-expanded={open}
          onClick={(event) =>
            setAnchorEl(open ? undefined : event.currentTarget)
          }
        >
          <MoreVertIcon />
        </IconButton>
      </Tooltip>
      <Menu
        id={id}
        anchorEl={anchorEl}
        open={open}
        aria-label={title}
        onClose={handleClose}
        slotProps={{
          backdrop: {
            sx: {
              appRegion: 'no-drag',
            },
          },
        }}
      >
        <ToggleButtonGroup
          aria-label="Window theme"
          value={themeStore.preference}
          exclusive
          onChange={(event, value: ThemePreference) =>
            runThemeChange(
              () => themeStore.setPreference(value),
              event.currentTarget,
            )
          }
          className="rounded"
          sx={{
            display: 'block',
            mx: 2,
            my: 1,
            '& svg': {
              margin: '0 6px 0 0 !important',
            },
          }}
        >
          <ToggleButton value="light">
            <LightModeIcon fontSize="small" /> light
          </ToggleButton>
          <ToggleButton value="dark">
            <DarkModeIcon fontSize="small" /> dark
          </ToggleButton>
          <ToggleButton value="system">
            <ContrastIcon fontSize="small" /> auto
          </ToggleButton>
        </ToggleButtonGroup>
        <MenuItem
          onClick={() => {
            handleClose();
            setServerSettingsTab('solver');
            setServerSettingsOpen(true);
          }}
        >
          Solver options
        </MenuItem>
        <MenuItem
          onClick={() => {
            handleClose();
            setServerSettingsTab('libraries');
            setServerSettingsOpen(true);
          }}
        >
          Libraries
        </MenuItem>
        <CLISymlinkMenuItem onClose={handleClose} />
        <RestartServerMenuItem onClose={handleClose} />
      </Menu>
      <ServerSettingsDialog
        open={serverSettingsOpen}
        onClose={() => setServerSettingsOpen(false)}
        onTabChange={setServerSettingsTab}
        tab={serverSettingsTab}
      />
    </>
  );
});
