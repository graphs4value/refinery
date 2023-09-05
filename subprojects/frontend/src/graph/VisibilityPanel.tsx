/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import ChevronLeftIcon from '@mui/icons-material/ChevronLeft';
import TuneIcon from '@mui/icons-material/Tune';
import Badge from '@mui/material/Badge';
import Dialog from '@mui/material/Dialog';
import IconButton from '@mui/material/IconButton';
import Paper from '@mui/material/Paper';
import Slide from '@mui/material/Slide';
import { styled } from '@mui/material/styles';
import { observer } from 'mobx-react-lite';
import { useCallback, useId, useState } from 'react';

import type GraphStore from './GraphStore';
import VisibilityDialog from './VisibilityDialog';

const VisibilityPanelRoot = styled('div', {
  name: 'VisibilityPanel-Root',
})(({ theme }) => ({
  position: 'absolute',
  padding: theme.spacing(1),
  top: 0,
  left: 0,
  maxHeight: '100%',
  maxWidth: '100%',
  overflow: 'hidden',
  display: 'flex',
  flexDirection: 'column',
  alignItems: 'start',
  '.VisibilityPanel-drawer': {
    overflow: 'hidden',
    display: 'flex',
    maxWidth: '100%',
    margin: theme.spacing(1),
  },
}));

function VisibilityPanel({
  graph,
  dialog,
}: {
  graph: GraphStore;
  dialog: boolean;
}): JSX.Element {
  const id = useId();
  const [showFilter, setShowFilter] = useState(false);
  const close = useCallback(() => setShowFilter(false), []);

  return (
    <VisibilityPanelRoot>
      <IconButton
        role="switch"
        aria-checked={showFilter}
        aria-controls={dialog ? undefined : id}
        aria-label="Show filter panel"
        onClick={() => setShowFilter(!showFilter)}
      >
        <Badge
          color="primary"
          variant="dot"
          invisible={graph.visibility.size === 0}
        >
          {showFilter && !dialog ? <ChevronLeftIcon /> : <TuneIcon />}
        </Badge>
      </IconButton>
      {dialog ? (
        <Dialog open={showFilter} onClose={close} maxWidth="xl">
          <VisibilityDialog graph={graph} close={close} dialog />
        </Dialog>
      ) : (
        <Slide
          direction="right"
          in={showFilter}
          id={id}
          mountOnEnter
          unmountOnExit
        >
          <Paper className="VisibilityPanel-drawer" elevation={4}>
            <VisibilityDialog graph={graph} close={close} />
          </Paper>
        </Slide>
      )}
    </VisibilityPanelRoot>
  );
}

export default observer(VisibilityPanel);
