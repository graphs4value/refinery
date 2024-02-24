/*
 * SPDX-FileCopyrightText: 2023-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import Box from '@mui/material/Box';
import { useTheme } from '@mui/material/styles';
import { observer } from 'mobx-react-lite';
import { useState } from 'react';
import { useResizeDetector } from 'react-resize-detector';

import DotGraphVisualizer from './DotGraphVisualizer';
import type GraphStore from './GraphStore';
import VisibilityPanel from './VisibilityPanel';
import ZoomCanvas from './ZoomCanvas';
import ExportPanel from './export/ExportPanel';

function GraphArea({ graph }: { graph: GraphStore }): JSX.Element {
  const { breakpoints } = useTheme();
  const { ref, width, height } = useResizeDetector({
    refreshMode: 'debounce',
  });
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
      <ZoomCanvas>
        {(fitZoom) => (
          <DotGraphVisualizer
            graph={graph}
            fitZoom={fitZoom}
            setSvgContainer={setSvgContainer}
          />
        )}
      </ZoomCanvas>
      <VisibilityPanel graph={graph} dialog={dialog} />
      <ExportPanel graph={graph} svgContainer={svgContainer} dialog={dialog} />
    </Box>
  );
}

export default observer(GraphArea);
