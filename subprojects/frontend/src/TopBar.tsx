/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import BookIcon from '@mui/icons-material/Book';
import GitHubIcon from '@mui/icons-material/GitHub';
import HomeIcon from '@mui/icons-material/Home';
import AppBar from '@mui/material/AppBar';
import IconButton from '@mui/material/IconButton';
import Stack from '@mui/material/Stack';
import Tooltip from '@mui/material/Tooltip';
import { styled, useTheme } from '@mui/material/styles';
import useMediaQuery from '@mui/material/useMediaQuery';
import { throttle } from 'lodash-es';
import { observer } from 'mobx-react-lite';
import { useEffect, useId, useMemo, useState } from 'react';
import { useResizeDetector } from 'react-resize-detector';

import PaneButtons from './PaneButtons';
import { useRootStore } from './RootStoreProvider';
import ToggleDarkModeButton from './ToggleDarkModeButton';
import ConcretizeButton from './editor/ConcretizeButton';
import GenerateButton from './editor/GenerateButton';
import SettingsMenuButton from './settings/SettingsMenuButton';
import isElectron from './utils/isElectron';

const TITLEBAR_LEFT_PADDING = 16;
const TITLEBAR_RIGHT_PADDING = 8;
const CENTER_PANE_HALF_WIDTH = 200;
const CENTER_PANE_SPACE = 500;
const CONTROLS_COMPACT_SPACE = 720;

function useWindowControlsOverlay(): {
  visible: boolean;
  titlebarAreaRect: DOMRect | undefined;
} {
  const [windowControlsOverlay, setWindowControlsOverlay] = useState(() => {
    const overlay = navigator.windowControlsOverlay;
    return {
      visible: overlay?.visible ?? false,
      titlebarAreaRect: overlay?.visible
        ? overlay.getTitlebarAreaRect()
        : undefined,
    };
  });
  const updateWindowControlsOverlay = useMemo(
    () =>
      throttle(
        ({
          titlebarAreaRect,
          visible,
        }: WindowControlsOverlayGeometryChangeEvent) =>
          setWindowControlsOverlay({
            visible,
            titlebarAreaRect: visible ? titlebarAreaRect : undefined,
          }),
        250,
      ),
    [],
  );
  useEffect(() => {
    const { windowControlsOverlay } = navigator;
    if (windowControlsOverlay !== undefined) {
      windowControlsOverlay.addEventListener(
        'geometrychange',
        updateWindowControlsOverlay,
      );
      return () => {
        windowControlsOverlay.removeEventListener(
          'geometrychange',
          updateWindowControlsOverlay,
        );
        updateWindowControlsOverlay.cancel();
      };
    }
    // Nothing to clean up if `windowControlsOverlay` is unsupported.
    return undefined;
  }, [updateWindowControlsOverlay]);
  return windowControlsOverlay;
}

function useViewportWidth(): number | undefined {
  const [width, setWidth] = useState(() =>
    typeof window === 'undefined' ? undefined : window.innerWidth,
  );
  useEffect(() => {
    const updateWidth = () => setWidth(window.innerWidth);
    window.addEventListener('resize', updateWidth);
    return () => window.removeEventListener('resize', updateWidth);
  }, []);
  return width;
}

function RefineryIcon({ size }: { size: number }): React.ReactElement {
  const theme = useTheme();
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      width={size}
      height={size}
      viewBox="0 0 512 515"
    >
      <path
        d="M447.98 179.335c-139.95-9.583-301.272-50.91-384-147.336v46.117C98.45 129.623 209.442 178.137 294.243 199.1c-84.796 20.963-195.791 69.476-230.265 120.985v46.117c82.73-96.422 244.053-137.752 384.002-147.334z"
        fill={theme.palette.text.primary}
      />
      <path
        d="M447.98 296.729c-113.755 4.192-287.485 40.727-384 136.557v46.716c95.14-103.612 279.898-137.754 384-143.745z"
        fill={theme.palette.primary.main}
      />
    </svg>
  );
}

const DevModeBadge = styled('div')(({ theme }) => ({
  ...theme.typography.button,
  display: 'inline-block',
  padding: `0 ${theme.shape.borderRadius}px`,
  background: theme.palette.text.primary,
  color: theme.palette.outer.background,
  borderRadius: theme.shape.borderRadius,
}));

const FileName = styled('span', {
  shouldForwardProp: (prop) => prop !== 'unsavedChanges',
})<{ unsavedChanges: boolean }>(({ theme, unsavedChanges }) => ({
  marginLeft: theme.spacing(1),
  minWidth: 0,
  overflow: 'hidden',
  textOverflow: 'ellipsis',
  whiteSpace: 'nowrap',
  flex: '1 1 auto',
  fontWeight: theme.typography.fontWeightLight,
  fontSize: '1.25rem',
  lineHeight: '1.6rem',
  color: unsavedChanges
    ? theme.palette.text.primary
    : theme.palette.text.secondary,
}));

const ButtonStack = styled(Stack)({
  gap: '4px',
  '.rounded': {
    borderRadius: '0px !important',
    clipPath: 'inset(0 2px 0 2px)',
  },
  '.rounded:first-of-type': {
    borderTopLeftRadius: '50em !important',
    borderBottomLeftRadius: '50em !important',
    clipPath: 'inset(0 2px 0 0)',
  },
  '.rounded:last-of-type': {
    borderTopRightRadius: '50em !important',
    borderBottomRightRadius: '50em !important',
    clipPath: 'inset(0 0 0 2px)',
  },
  '.rounded:first-of-type:last-of-type': {
    clipPath: 'none',
  },
  // Concretize is wrapped by the shared Tooltip component when it is
  // icon-only. Keep that wrapper from turning the split button into two
  // independently rounded pills.
  '& > .RefineryTooltip-Container:first-of-type .rounded': {
    borderTopLeftRadius: '50em !important',
    borderBottomLeftRadius: '50em !important',
    borderTopRightRadius: '0 !important',
    borderBottomRightRadius: '0 !important',
    clipPath: 'inset(0 2px 0 0)',
  },
  ['& > .RefineryTooltip-Container + .rounded:last-of-type, ' +
  '& > .rounded + .rounded:last-of-type']: {
    borderTopLeftRadius: '0 !important',
    borderBottomLeftRadius: '0 !important',
    borderTopRightRadius: '50em !important',
    borderBottomRightRadius: '50em !important',
    clipPath: 'inset(0 0 0 2px)',
  },
});

const AppName = styled('h1')(({ theme }) => ({
  ...theme.typography.h6,
  display: 'block',
  margin: 0,
  // Keep the short application name intact. The file name below it is the
  // flexible element and is the one that receives an ellipsis when needed.
  flex: '0 0 auto',
  whiteSpace: 'nowrap',
}));

export default observer(function TopBar(): React.ReactElement {
  const { editorStore, themeStore, hasChat } = useRootStore();
  const theme = useTheme();
  const { visible: overlayVisible, titlebarAreaRect } =
    useWindowControlsOverlay();
  const mobileLayout = useMediaQuery(theme.breakpoints.down('sm'));
  const viewportWidth = useViewportWidth();
  const mobilePaneButtonsId = useId();
  const { ref: titlebarRef, width: titlebarElementWidth } =
    useResizeDetector<HTMLDivElement>({ handleHeight: false });

  // The titlebar can be narrower than the document, especially when the
  // browser's window-controls overlay reserves space for native buttons. Use
  // its measured rectangle when available, and account for the left and right
  // titlebar margins independently so either side can become tight.
  const hasFileName = editorStore?.simpleName !== undefined;
  const titlebarStart = titlebarAreaRect?.x ?? 0;
  // ResizeObserver reports the content box, while the titlebar width is the
  // border box. Add the fixed horizontal padding back for the fallback.
  const measuredTitlebarWidth =
    titlebarElementWidth === undefined
      ? undefined
      : titlebarElementWidth + TITLEBAR_LEFT_PADDING + TITLEBAR_RIGHT_PADDING;
  const titlebarWidth =
    titlebarAreaRect?.width ??
    measuredTitlebarWidth ??
    viewportWidth ??
    Number.POSITIVE_INFINITY;
  const titlebarEnd = titlebarStart + titlebarWidth;
  const windowWidth = viewportWidth ?? titlebarEnd;
  const centerX = windowWidth / 2;
  // These are the spaces where the measured control groups can actually
  // render; the titlebar's horizontal padding is outside both groups.
  const leftSpace = Math.max(
    0,
    centerX - titlebarStart - TITLEBAR_LEFT_PADDING,
  );
  const rightSpace = Math.max(
    0,
    titlebarEnd - centerX - TITLEBAR_RIGHT_PADDING,
  );
  const space = Math.min(leftSpace, rightSpace);
  const [mobilePaneButtonsVisible, setMobilePaneButtonsVisible] =
    useState(false);
  const centerPaneButtons = !mobileLayout && space >= CENTER_PANE_SPACE;
  const compactControls = mobileLayout || rightSpace < CONTROLS_COMPACT_SPACE;
  const showApplicationName = !hasFileName || centerPaneButtons;
  const showMobilePaneButtons = mobileLayout && mobilePaneButtonsVisible;

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
      <Stack
        direction="row"
        ref={titlebarRef}
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
          position: 'relative',
          alignItems: 'center',
          pl: 2,
          pr: 1,
          py: mobileLayout ? 0.5 : 1.5,
        }}
      >
        <Stack
          direction="row"
          sx={{
            minWidth: 0,
            flex: '0 1 auto',
            // Reserve the center pane's footprint when it is overlaid so a
            // long file name cannot intrude into the centered controls.
            maxWidth: centerPaneButtons
              ? `${Math.max(0, leftSpace - CENTER_PANE_HALF_WIDTH - 8)}px`
              : '50%',
            alignItems: 'center',
            '& > svg': { flexShrink: 0 },
          }}
        >
          {mobileLayout ? (
            <IconButton
              size="small"
              color="inherit"
              aria-label={
                mobilePaneButtonsVisible
                  ? 'Hide editor panes'
                  : 'Show editor panes'
              }
              aria-controls={mobilePaneButtonsId}
              aria-expanded={showMobilePaneButtons}
              onClick={() => setMobilePaneButtonsVisible((visible) => !visible)}
              sx={(theme) => ({
                width: theme.spacing(4),
                height: theme.spacing(4),
                minWidth: 0,
                p: 0,
                flexShrink: 0,
                position: 'relative',
                zIndex: 2,
                // Keep the visual button and its flex dimensions unchanged,
                // while providing a comfortable touch target on mobile.
                '&::before': {
                  content: '""',
                  position: 'absolute',
                  inset: theme.spacing(-0.5),
                  borderRadius: '50%',
                  backgroundColor: 'transparent',
                  transition: theme.transitions.create('background-color'),
                },
                '&:hover': {
                  backgroundColor: 'transparent',
                },
                '&:hover::before': {
                  backgroundColor: theme.palette.action.hover,
                },
              })}
            >
              <RefineryIcon size={32} />
            </IconButton>
          ) : (
            <RefineryIcon size={32} />
          )}
          {showApplicationName && (
            <AppName sx={{ pl: 1 }}>
              Refinery {import.meta.env.DEV && <DevModeBadge>Dev</DevModeBadge>}
            </AppName>
          )}
          {hasFileName && (
            <FileName unsavedChanges={editorStore.unsavedChanges}>
              {editorStore.simpleName}
            </FileName>
          )}
        </Stack>
        {!mobileLayout && !centerPaneButtons && (
          <Stack direction="row" sx={{ ml: 1, alignItems: 'center' }}>
            <PaneButtons themeStore={themeStore} hasChat={hasChat} hideLabel />
          </Stack>
        )}
        {!mobileLayout && centerPaneButtons && (
          <Stack
            direction="row"
            sx={{
              position: 'absolute',
              top: 0,
              bottom: 0,
              left: overlayVisible
                ? 'calc(50vw - env(titlebar-area-x, 0px))'
                : '50%',
              transform: 'translateX(-50%)',
              alignItems: 'center',
            }}
          >
            <PaneButtons themeStore={themeStore} hasChat={hasChat} />
          </Stack>
        )}
        {mobileLayout && (
          <Stack
            direction="row"
            id={mobilePaneButtonsId}
            role="toolbar"
            aria-label="Editor panes"
            aria-hidden={!showMobilePaneButtons}
            onClick={(event) => {
              if (event.target === event.currentTarget) {
                setMobilePaneButtonsVisible(false);
              }
            }}
            onKeyDown={(event) => {
              if (event.key === 'Escape') {
                setMobilePaneButtonsVisible(false);
              }
            }}
            sx={(theme) => ({
              position: 'absolute',
              top: 0,
              bottom: 0,
              left: 48,
              right: 0,
              opacity: showMobilePaneButtons ? 1 : 0,
              pointerEvents: showMobilePaneButtons ? 'auto' : 'none',
              transition: theme.transitions.create('opacity', {
                duration: theme.transitions.duration.short,
              }),
              alignItems: 'center',
              px: 1,
              borderRadius: theme.shape.borderRadius,
              background: theme.palette.outer.background,
              zIndex: 1,
            })}
          >
            <PaneButtons themeStore={themeStore} hasChat={hasChat} hideLabel />
          </Stack>
        )}
        <Stack
          direction="row"
          sx={(theme) => ({
            ml: 'auto',
            pl: 1,
            gap: theme.spacing(1),
            alignItems: 'center',
            flexShrink: 0,
          })}
        >
          {!compactControls && (
            <Stack direction="row" sx={{ alignItems: 'center' }}>
              <Tooltip title="Refinery home page">
                <IconButton
                  href="https://refinery.tools/"
                  target="_blank"
                  color="inherit"
                >
                  <HomeIcon />
                </IconButton>
              </Tooltip>
              <Tooltip title="Refinery documentation">
                <IconButton
                  href="https://refinery.tools/learn/"
                  target="_blank"
                  color="inherit"
                >
                  <BookIcon />
                </IconButton>
              </Tooltip>
              <Tooltip title="Check us out at GitHub">
                <IconButton
                  href="https://github.com/graphs4value/refinery"
                  target="_blank"
                  color="inherit"
                >
                  <GitHubIcon />
                </IconButton>
              </Tooltip>
            </Stack>
          )}
          <ButtonStack direction="row">
            <ConcretizeButton
              editorStore={editorStore}
              hideLabel={!centerPaneButtons}
            />
            <GenerateButton
              editorStore={editorStore}
              hideWarnings={compactControls}
            />
          </ButtonStack>
          {isElectron ? <SettingsMenuButton /> : <ToggleDarkModeButton />}
        </Stack>
      </Stack>
    </AppBar>
  );
});
