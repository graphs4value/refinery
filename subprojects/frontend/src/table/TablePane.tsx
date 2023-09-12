/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import Stack from '@mui/material/Stack';
import { Suspense, lazy } from 'react';

import Loading from '../Loading';
import type GraphStore from '../graph/GraphStore';

const TableArea = lazy(() => import('./TableArea'));

export default function TablePane({
  graph,
}: {
  graph: GraphStore;
}): JSX.Element {
  return (
    <Stack direction="column" height="100%" overflow="auto" alignItems="center">
      <Suspense fallback={<Loading />}>
        <TableArea graph={graph} />
      </Suspense>
    </Stack>
  );
}
