/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import GitHubIcon from '@mui/icons-material/GitHub';
import AppBar from '@mui/material/AppBar';
import IconButton from '@mui/material/IconButton';
import Stack from '@mui/material/Stack';
import Toolbar from '@mui/material/Toolbar';
import Typography from '@mui/material/Typography';
import { styled, useTheme } from '@mui/material/styles';
import useMediaQuery from '@mui/material/useMediaQuery';
import { throttle } from 'lodash-es';
import { observer } from 'mobx-react-lite';
import { useEffect, useMemo, useState } from 'react';

import PaneButtons from './PaneButtons';
import { useRootStore } from './RootStoreProvider';
import ToggleDarkModeButton from './ToggleDarkModeButton';
import GenerateButton from './editor/GenerateButton';

function useWindowControlsOverlayVisible(): boolean {
  const [windowControlsOverlayVisible, setWindowControlsOverlayVisible] =
    useState(false);
  const updateWindowControlsOverlayVisible = useMemo(
    () =>
      throttle(
        ({ visible }: WindowControlsOverlayGeometryChangeEvent) =>
          setWindowControlsOverlayVisible(visible),
        250,
      ),
    [],
  );
  useEffect(() => {
    if ('windowControlsOverlay' in navigator) {
      const { windowControlsOverlay } = navigator;
      setWindowControlsOverlayVisible(windowControlsOverlay.visible);
      windowControlsOverlay.addEventListener(
        'geometrychange',
        updateWindowControlsOverlayVisible,
      );
      return () => {
        windowControlsOverlay.removeEventListener(
          'geometrychange',
          updateWindowControlsOverlayVisible,
        );
        updateWindowControlsOverlayVisible.cancel();
      };
    }
    // Nothing to clean up if `windowControlsOverlay` is unsupported.
    return () => {};
  }, [updateWindowControlsOverlayVisible]);
  return windowControlsOverlayVisible;
}

const DevModeBadge = styled('div')(({ theme }) => ({
  ...theme.typography.button,
  display: 'inline-block',
  padding: `0 ${theme.shape.borderRadius}px`,
  background: theme.palette.text.primary,
  color: theme.palette.outer.background,
  borderRadius: theme.shape.borderRadius,
}));

export default observer(function TopBar(): JSX.Element {
  const { editorStore, themeStore } = useRootStore();
  const overlayVisible = useWindowControlsOverlayVisible();
  const { breakpoints } = useTheme();
  const medium = useMediaQuery(breakpoints.up('sm'));
  const large = useMediaQuery(breakpoints.up('md'));
  const veryLarge = useMediaQuery(breakpoints.up('lg'));

  return (
    <AppBar
      position="static"
      elevation={0}
      color="transparent"
      sx={(theme) => ({
        background: theme.palette.outer.background,
        borderBottom: `1px solid ${theme.palette.outer.border}`,
        appRegion: 'drag',
        '.MuiButtonBase-root': {
          appRegion: 'no-drag',
        },
      })}
    >
      <Toolbar
        sx={{
          ...(overlayVisible
            ? {
                marginLeft: 'env(titlebar-area-x, 0)',
                marginTop: 'env(titlebar-area-y, 0)',
                width: 'env(titlebar-area-width, 100%)',
                minHeight: 'env(titlebar-area-height, auto)',
              }
            : {
                minHeight: 'auto',
              }),
          py: 0.5,
        }}
      >
        <Typography variant="h6" component="h1">
          Refinery {import.meta.env.DEV && <DevModeBadge>Dev</DevModeBadge>}
        </Typography>
        <Stack direction="row" alignItems="center" flexGrow={1} marginLeft={1}>
          {medium && !large && (
            <PaneButtons themeStore={themeStore} hideLabel />
          )}
        </Stack>
        {large && (
          <Stack
            direction="row"
            alignItems="center"
            sx={{
              position: 'absolute',
              top: 0,
              bottom: 0,
              left: '50%',
              transform: 'translateX(-50%)',
            }}
          >
            <PaneButtons themeStore={themeStore} />
          </Stack>
        )}
        <Stack
          direction="row"
          marginLeft={1}
          marginRight={1}
          gap={1}
          alignItems="center"
        >
          <GenerateButton editorStore={editorStore} hideWarnings={!veryLarge} />
          {large && (
            <IconButton
              aria-label="GitHub"
              href="https://github.com/graphs4value/refinery"
              target="_blank"
              color="inherit"
            >
              <GitHubIcon />
            </IconButton>
          )}
        </Stack>
        <ToggleDarkModeButton />
      </Toolbar>
    </AppBar>
  );
});
