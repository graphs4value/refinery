/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import MoreHorizIcon from '@mui/icons-material/MoreHoriz';
import MoreVertIcon from '@mui/icons-material/MoreVert';
import Box from '@mui/material/Box';
import Stack from '@mui/material/Stack';
import { alpha, useTheme } from '@mui/material/styles';
import { useCallback, useRef, useState } from 'react';
import { useResizeDetector } from 'react-resize-detector';

export default function DirectionalSplitPane({
  primary: left,
  secondary: right,
  primaryOnly: showLeftOnly,
  secondaryOnly: showRightOnly,
}: {
  primary: React.ReactNode | ((horizontal: boolean) => React.ReactNode);
  secondary: React.ReactNode | ((horizontal: boolean) => React.ReactNode);
  primaryOnly?: boolean;
  secondaryOnly?: boolean;
}): React.ReactElement {
  const theme = useTheme();
  const stackRef = useRef<HTMLDivElement | null>(null);
  const { ref: resizeRef, width, height } = useResizeDetector();
  const sliderRef = useRef<HTMLDivElement>(null);
  const [resizing, setResizing] = useState<number | undefined>();
  const [fraction, setFraction] = useState(0.5);

  const horizontalSplit =
    width !== undefined && height !== undefined && height > width;
  const direction = horizontalSplit ? 'column' : 'row';
  const axis = horizontalSplit ? 'height' : 'width';
  const primarySize = showLeftOnly
    ? '100%'
    : `calc(${fraction * 100}% - 0.5px)`;
  const secondarySize = showRightOnly
    ? '100%'
    : `calc(${(1 - fraction) * 100}% - 0.5px)`;
  let snapPosition: number | undefined;
  if (fraction >= 0.45 && fraction <= 0.55) {
    snapPosition = 0.5;
  } else if (fraction >= 0.2833 && fraction <= 0.3833) {
    snapPosition = 0.3333;
  } else if (fraction >= 0.6166 && fraction <= 0.7166) {
    snapPosition = 0.6666;
  }
  const ref = useCallback(
    (element: HTMLDivElement | null) => {
      resizeRef(element);
      stackRef.current = element;
    },
    [resizeRef],
  );

  return (
    <Stack
      direction={direction}
      sx={{
        height: '100%',
        width: '100%',
        overflow: 'hidden',
        position: 'relative',
      }}
      ref={ref}
    >
      {!showRightOnly && (
        <Box {...{ [axis]: primarySize }}>
          {typeof left === 'function' ? left(horizontalSplit) : left}
        </Box>
      )}
      <Box
        sx={{
          overflow: 'visible',
          position: 'relative',
          [axis]: '0px',
          display: showLeftOnly || showRightOnly ? 'none' : 'flex',
          flexDirection: direction,
          [horizontalSplit ? 'borderBottom' : 'borderRight']:
            `1px solid ${theme.palette.outer.border}`,
        }}
      >
        <Box
          ref={sliderRef}
          sx={{
            display: 'flex',
            position: 'absolute',
            [axis]: theme.spacing(2),
            ...(horizontalSplit
              ? {
                  top: theme.spacing(-1),
                  left: 0,
                  right: 0,
                  transform: 'translateY(0.5px)',
                }
              : {
                  left: theme.spacing(-1),
                  top: 0,
                  bottom: 0,
                  transform: 'translateX(0.5px)',
                }),
            zIndex: 999,
            alignItems: 'center',
            justifyContent: 'center',
            color: theme.palette.text.secondary,
            cursor: horizontalSplit ? 'ns-resize' : 'ew-resize',
            touchAction: 'none',
            userSelect: 'none',
            '@media (pointer: coarse)': {
              [axis]: '24px',
              [horizontalSplit ? 'top' : 'left']: '-12px',
            },
            '.MuiSvgIcon-root': {
              opacity: resizing !== undefined ? 1 : 0,
            },
            ...(resizing !== undefined
              ? {
                  background: alpha(
                    theme.palette.text.primary,
                    theme.palette.action.activatedOpacity,
                  ),
                }
              : {
                  '&:hover': {
                    background: alpha(
                      theme.palette.text.primary,
                      theme.palette.action.hoverOpacity,
                    ),
                    // Appear above the editor scrollbar when hovered.
                    zIndex: 1100,
                    '.MuiSvgIcon-root': {
                      opacity: 1,
                    },
                    '@media (hover: none)': {
                      background: 'transparent',
                      zIndex: 999,
                      '.MuiSvgIcon-root': {
                        opacity: 0,
                      },
                    },
                  },
                }),
          }}
          onPointerDown={(event) => {
            if (event.button !== 0) {
              return;
            }
            sliderRef.current?.setPointerCapture(event.pointerId);
            setResizing(event.pointerId);
          }}
          onPointerUp={(event) => {
            if (resizing !== event.pointerId || event.button !== 0) {
              return;
            }
            sliderRef.current?.releasePointerCapture(event.pointerId);
            if (
              snapPosition !== undefined &&
              !event.shiftKey &&
              !event.altKey
            ) {
              // Since there is no `doubleclick` even on touchscreen,
              // also allow resetting the split by snapping to the middle.
              setFraction(snapPosition);
            }
            setResizing(undefined);
          }}
          onPointerMove={(event) => {
            if (resizing !== event.pointerId) {
              return;
            }
            const container = stackRef.current;
            if (container === null) {
              return;
            }
            const rect = container.getBoundingClientRect();
            const newFraction = horizontalSplit
              ? (event.clientY - rect.top) / rect.height
              : (event.clientX - rect.left) / rect.width;
            setFraction(Math.min(0.9, Math.max(0.1, newFraction)));
          }}
          onMouseEnter={(event) => {
            // Update `z-index` explicitly instead of relying on the `:hover` style
            // to avoid interference with the `doubleclick` handler in the area located
            // above the editor scrollbar in Firefox.
            // eslint-disable-next-line no-param-reassign -- Update event target.
            event.currentTarget.style.zIndex = '1100';
          }}
          onMouseLeave={(event) => {
            // eslint-disable-next-line no-param-reassign -- Update event target.
            event.currentTarget.style.zIndex = '999';
          }}
          onDoubleClick={() => setFraction(0.5)}
        >
          {horizontalSplit ? <MoreHorizIcon /> : <MoreVertIcon />}
        </Box>
      </Box>
      {!showLeftOnly && (
        <Box {...{ [axis]: secondarySize }}>
          {typeof right === 'function' ? right(horizontalSplit) : right}
        </Box>
      )}
      {resizing !== undefined && snapPosition !== undefined && (
        <Box
          sx={{
            position: 'absolute',
            pointerEvents: 'none',
            zIndex: 1200,
            backgroundRepeat: 'repeat',
            ...(horizontalSplit
              ? {
                  top: `${snapPosition * 100}%`,
                  left: 0,
                  width: '100%',
                  height: '1px',
                  // 5pt dash, 2pt space, just as in the graph view for unknown edges.
                  backgroundImage: `linear-gradient(to right, ${theme.palette.primary.main} 6.666px, transparent 6.666px)`,
                  backgroundSize: '9.333px 100%',
                }
              : {
                  left: `${snapPosition * 100}%`,
                  top: 0,
                  height: '100%',
                  width: '1px',
                  backgroundImage: `linear-gradient(to bottom, ${theme.palette.primary.main} 6.666px, transparent 6.666px)`,
                  backgroundSize: '100% 9.333px',
                }),
          }}
        />
      )}
    </Stack>
  );
}
