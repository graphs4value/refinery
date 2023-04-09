/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import CircularProgress from '@mui/material/CircularProgress';
import { styled } from '@mui/material/styles';

const LoadingRoot = styled('div')(({ theme }) => ({
  width: '100%',
  height: '100%',
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  background: theme.palette.outer.background,
}));

export default function Loading() {
  return (
    <LoadingRoot>
      <CircularProgress size={60} color="inherit" />
    </LoadingRoot>
  );
}
