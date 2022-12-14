import GitHubIcon from '@mui/icons-material/GitHub';
import AppBar from '@mui/material/AppBar';
import Button from '@mui/material/Button';
import IconButton from '@mui/material/IconButton';
import Stack from '@mui/material/Stack';
import Toolbar from '@mui/material/Toolbar';
import Typography from '@mui/material/Typography';
import { styled, useTheme } from '@mui/material/styles';
import useMediaQuery from '@mui/material/useMediaQuery';
import { throttle } from 'lodash-es';
import { observer } from 'mobx-react-lite';
import { useEffect, useMemo, useState } from 'react';

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
  const { editorStore } = useRootStore();
  const overlayVisible = useWindowControlsOverlayVisible();
  const { breakpoints } = useTheme();
  const small = useMediaQuery(breakpoints.down('sm'));
  const large = useMediaQuery(breakpoints.up('md'));

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
          Refinery {import.meta.env.DEV && <DevModeBadge>Dev</DevModeBadge>}
        </Typography>
        <Stack direction="row" marginRight={1}>
          <GenerateButton editorStore={editorStore} hideWarnings={small} />
          {large && (
            <>
              <Button
                arial-label="Budapest University of Technology and Economics, Critical Systems Research Group"
                className="rounded"
                color="inherit"
                href="https://ftsrg.mit.bme.hu"
                target="_blank"
                sx={{ marginLeft: 1 }}
              >
                BME FTSRG
              </Button>
              <Button
                aria-label="McGill University, Department of Electrical and Computer Engineering"
                className="rounded"
                color="inherit"
                href="https://www.mcgill.ca/ece/daniel-varro"
                target="_blank"
              >
                McGill ECE
              </Button>
              <Button
                aria-label="2022 Amazon Research Awards recipent"
                className="rounded"
                color="inherit"
                href="https://www.amazon.science/research-awards/recipients/daniel-varro-fall-2021"
                target="_blank"
              >
                Amazon Science
              </Button>
              <IconButton
                aria-label="GitHub"
                href="https://github.com/graphs4value/refinery"
                target="_blank"
                color="inherit"
              >
                <GitHubIcon />
              </IconButton>
            </>
          )}
        </Stack>
        <ToggleDarkModeButton />
      </Toolbar>
    </AppBar>
  );
});
