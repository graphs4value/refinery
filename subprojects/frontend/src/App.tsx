import MenuIcon from '@mui/icons-material/Menu';
import AppBar from '@mui/material/AppBar';
import Box from '@mui/material/Box';
import IconButton from '@mui/material/IconButton';
import Toolbar from '@mui/material/Toolbar';
import Typography from '@mui/material/Typography';
import React from 'react';

import EditorArea from './editor/EditorArea';
import EditorButtons from './editor/EditorButtons';
import GenerateButton from './editor/GenerateButton';

export default function App(): JSX.Element {
  return (
    <Box display="flex" flexDirection="column" sx={{ height: '100vh' }}>
      <AppBar position="static" color="inherit">
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
        </Toolbar>
      </AppBar>
      <Box
        display="flex"
        justifyContent="space-between"
        alignItems="center"
        p={1}
      >
        <EditorButtons />
        <GenerateButton />
      </Box>
      <Box flexGrow={1} flexShrink={1} sx={{ overflow: 'auto' }}>
        <EditorArea />
      </Box>
    </Box>
  );
}
