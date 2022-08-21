import CircularProgress from '@mui/material/CircularProgress';
import { styled } from '@mui/material/styles';
import React from 'react';

const LoadingRoot = styled('div')({
  width: '100%',
  height: '100%',
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
});

export default function Loading() {
  return (
    <LoadingRoot>
      <CircularProgress />
    </LoadingRoot>
  );
}
