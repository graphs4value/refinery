import AppBar from '@mui/material/AppBar';
import Toolbar from '@mui/material/Toolbar';
import Typography from '@mui/material/Typography';
import React from 'react';

import ToggleDarkModeButton from './ToggleDarkModeButton';

export default function TopBar(): JSX.Element {
  return (
    <AppBar
      position="static"
      elevation={0}
      color="transparent"
      sx={(theme) => ({
        background: theme.palette.outer.background,
        borderBottom: `1px solid ${theme.palette.outer.border}`,
      })}
    >
      <Toolbar>
        <Typography variant="h6" component="h1" flexGrow={1}>
          Refinery
        </Typography>
        <ToggleDarkModeButton />
      </Toolbar>
    </AppBar>
  );
}
