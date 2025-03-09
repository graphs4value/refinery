/*
 * SPDX-FileCopyrightText: 2023-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import UpdatedDisabledIcon from '@mui/icons-material/UpdateDisabled';
import Box from '@mui/material/Box';
import Grow from '@mui/material/Grow';
import Typography from '@mui/material/Typography';
import { useTheme } from '@mui/material/styles';
import { observer } from 'mobx-react-lite';
import { useState } from 'react';
import { useResizeDetector } from 'react-resize-detector';

import DotGraphVisualizer from './DotGraphVisualizer';
import type GraphStore from './GraphStore';
import SVGIcons from './SVGIcons';
import VisibilityPanel from './VisibilityPanel';
import ZoomCanvas from './ZoomCanvas';
import ExportPanel from './export/ExportPanel';

const SyncWarning = observer(function SyncWarning({
  graph: { dimView },
}: {
  graph: GraphStore;
}): React.ReactElement {
  const theme = useTheme();
  return (
    <Box
      sx={{
        position: 'absolute',
        bottom: 0,
        left: 0,
      }}
    >
      <Grow
        in={dimView}
        mountOnEnter
        unmountOnExit
        timeout={theme.transitions.duration.short}
      >
        <Box sx={{ margin: theme.spacing(2) }}>
          <Typography
            variant="body2"
            sx={{
              color:
                theme.palette.mode === 'dark'
                  ? theme.palette.text.primary
                  : theme.palette.text.secondary,
            }}
          >
            <UpdatedDisabledIcon
              fontSize="small"
              sx={{ verticalAlign: 'text-top', marginRight: '0.5ch' }}
            />
            View not in sync with code editor
          </Typography>
        </Box>
      </Grow>
    </Box>
  );
});

const Overlay = observer(function Overlay({
  graph: { dimView },
}: {
  graph: GraphStore;
}): React.ReactElement {
  return (
    <Box
      sx={(theme) => ({
        position: 'absolute',
        top: 0,
        left: 0,
        width: '100%',
        height: '100%',
        pointerEvents: 'none',
        backgroundColor: dimView ? theme.palette.outer.disabled : 'transparent',
        mixBlendMode: theme.palette.mode === 'dark' ? 'lighten' : 'darken',
        transition: theme.transitions.create('background-color', {
          duration: theme.transitions.duration.short,
        }),
        '@media (prefers-reduced-motion: reduce)': {
          backgroundColor: 'transparent',
        },
      })}
    />
  );
});

export default function GraphArea({
  graph,
}: {
  graph: GraphStore;
}): React.ReactElement {
  const { breakpoints } = useTheme();
  const { ref, width, height } = useResizeDetector();
  const [svgContainer, setSvgContainer] = useState<HTMLElement | undefined>();

  const breakpoint = breakpoints.values.sm;
  const dialog =
    width === undefined ||
    height === undefined ||
    width < breakpoint ||
    height < breakpoint;

  return (
    <Box
      width="100%"
      height="100%"
      overflow="hidden"
      position="relative"
      ref={ref}
    >
      <SVGIcons />
      <ZoomCanvas>
        {(fitZoom, zoom) => (
          <DotGraphVisualizer
            graph={graph}
            fitZoom={fitZoom}
            setSvgContainer={setSvgContainer}
            simplify={zoom <= 0.25}
          />
        )}
      </ZoomCanvas>
      <Overlay graph={graph} />
      <SyncWarning graph={graph} />
      <VisibilityPanel graph={graph} dialog={dialog} />
      <ExportPanel graph={graph} svgContainer={svgContainer} dialog={dialog} />
    </Box>
  );
}
