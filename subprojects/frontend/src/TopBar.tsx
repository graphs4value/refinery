import AppBar from '@mui/material/AppBar';
import Toolbar from '@mui/material/Toolbar';
import Typography from '@mui/material/Typography';
import { useTheme } from '@mui/material/styles';
import useMediaQuery from '@mui/material/useMediaQuery';
import { throttle } from 'lodash-es';
import { observer } from 'mobx-react-lite';
import React, { useEffect, useMemo, useState } from 'react';

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
      };
    }
    // Nothing to clean up if `windowControlsOverlay` is unsupported.
    return () => {};
  }, [updateWindowControlsOverlayVisible]);
  return windowControlsOverlayVisible;
}

export default observer(function TopBar(): JSX.Element {
  const { editorStore } = useRootStore();
  const overlayVisible = useWindowControlsOverlayVisible();
  const { breakpoints } = useTheme();
  const showGenerateButton = useMediaQuery(breakpoints.down('sm'));

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
        <Typography variant="h6" component="h1" flexGrow={1}>
          Refinery
        </Typography>
        {showGenerateButton && (
          <GenerateButton editorStore={editorStore} hideWarnings />
        )}
        <ToggleDarkModeButton />
      </Toolbar>
    </AppBar>
  );
});
