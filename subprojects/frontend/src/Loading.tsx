import CircularProgress from '@mui/material/CircularProgress';
import { styled } from '@mui/material/styles';
import React from 'react';

const LoadingRoot = styled('div')({
  width: '100vw',
  height: '100vh',
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
