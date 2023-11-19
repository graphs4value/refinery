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
  primary: React.ReactNode;
  secondary: React.ReactNode;
  primaryOnly?: boolean;
  secondaryOnly?: boolean;
}): JSX.Element {
  const theme = useTheme();
  const stackRef = useRef<HTMLDivElement | null>(null);
  const { ref: resizeRef, width, height } = useResizeDetector();
  const sliderRef = useRef<HTMLDivElement>(null);
  const [resizing, setResizing] = useState(false);
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
      height="100%"
      width="100%"
      overflow="hidden"
      ref={ref}
    >
      {!showRightOnly && <Box {...{ [axis]: primarySize }}>{left}</Box>}
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
            '.MuiSvgIcon-root': {
              opacity: resizing ? 1 : 0,
            },
            ...(resizing
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
                    '.MuiSvgIcon-root': {
                      opacity: 1,
                    },
                  },
                }),
          }}
          onPointerDown={(event) => {
            if (event.button !== 0) {
              return;
            }
            sliderRef.current?.setPointerCapture(event.pointerId);
            setResizing(true);
          }}
          onPointerUp={(event) => {
            if (event.button !== 0) {
              return;
            }
            sliderRef.current?.releasePointerCapture(event.pointerId);
            setResizing(false);
          }}
          onPointerMove={(event) => {
            if (!resizing) {
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
          onDoubleClick={() => setFraction(0.5)}
        >
          {horizontalSplit ? <MoreHorizIcon /> : <MoreVertIcon />}
        </Box>
      </Box>
      {!showLeftOnly && <Box {...{ [axis]: secondarySize }}>{right}</Box>}
    </Stack>
  );
}

DirectionalSplitPane.defaultProps = {
  primaryOnly: false,
  secondaryOnly: false,
};
