/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import Stack from '@mui/material/Stack';
import { Suspense, lazy } from 'react';

import Loading from '../Loading';

const GraphArea = lazy(() => import('./GraphArea'));

export default function GraphPane(): JSX.Element {
  return (
    <Stack
      direction="column"
      height="100%"
      overflow="auto"
      alignItems="center"
      justifyContent="center"
    >
      <Suspense fallback={<Loading />}>
        <GraphArea />
      </Suspense>
    </Stack>
  );
}
