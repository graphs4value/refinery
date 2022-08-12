import MenuIcon from '@mui/icons-material/Menu';
import AppBar from '@mui/material/AppBar';
import IconButton from '@mui/material/IconButton';
import Toolbar from '@mui/material/Toolbar';
import Typography from '@mui/material/Typography';
import React from 'react';

import ToggleDarkModeButton from './ToggleDarkModeButton';

export default function TopBar(): JSX.Element {
  return (
    <AppBar position="static" color="primary">
      <Toolbar>
        <IconButton
          edge="start"
          sx={{ mr: 2 }}
          color="inherit"
          aria-label="menu"
        >
          <MenuIcon />
        </IconButton>
        <Typography variant="h6" component="h1" flexGrow={1}>
          Refinery
        </Typography>
        <ToggleDarkModeButton />
      </Toolbar>
    </AppBar>
  );
}
