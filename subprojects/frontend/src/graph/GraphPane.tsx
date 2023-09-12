/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import Stack from '@mui/material/Stack';
import { Suspense, lazy } from 'react';

import Loading from '../Loading';

import type GraphStore from './GraphStore';

const GraphArea = lazy(() => import('./GraphArea'));

export default function GraphPane({
  graph,
}: {
  graph: GraphStore;
}): JSX.Element {
  return (
    <Stack
      direction="column"
      height="100%"
      overflow="auto"
      alignItems="center"
      justifyContent="center"
    >
      <Suspense fallback={<Loading />}>
        <GraphArea graph={graph} />
      </Suspense>
    </Stack>
  );
}
