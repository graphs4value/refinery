import CloseIcon from '@mui/icons-material/Close';
import FindReplaceIcon from '@mui/icons-material/FindReplace';
import KeyboardArrowDownIcon from '@mui/icons-material/KeyboardArrowDown';
import KeyboardArrowUpIcon from '@mui/icons-material/KeyboardArrowUp';
import SearchIcon from '@mui/icons-material/Search';
import Button from '@mui/material/Button';
import Checkbox from '@mui/material/Checkbox';
import FormControlLabel from '@mui/material/FormControlLabel';
import FormHelperText from '@mui/material/FormHelperText';
import IconButton from '@mui/material/IconButton';
import Stack from '@mui/material/Stack';
import TextField from '@mui/material/TextField';
import Toolbar from '@mui/material/Toolbar';
import { observer } from 'mobx-react-lite';
import React, { useCallback } from 'react';

import type SearchPanelStore from './SearchPanelStore';

function SearchToolbar({ store }: { store: SearchPanelStore }): JSX.Element {
  const {
    id: panelId,
    query: { search, valid, caseSensitive, literal, regexp, replace },
    invalidRegexp,
  } = store;

  const searchHelperId = `${panelId}-search-helper`;

  const searchFieldRef = useCallback(
    (element: HTMLInputElement | null) =>
      store.setSearchField(element ?? undefined),
    [store],
  );

  return (
    <Toolbar variant="dense" sx={{ py: 0.5, alignItems: 'start' }}>
      <Stack
        direction="row"
        flexWrap="wrap"
        alignItems="center"
        rowGap={0.5}
        flexGrow={1}
      >
        <Stack direction="row" flexWrap="wrap" alignItems="center" rowGap={0.5}>
          <TextField
            type="search"
            placeholder="Search"
            aria-label="Search"
            {...(invalidRegexp && {
              'aria-describedby': searchHelperId,
            })}
            value={search}
            error={invalidRegexp}
            onChange={(event) =>
              store.updateQuery({ search: event.target.value })
            }
            onKeyDown={(event) => {
              if (event.key === 'Enter') {
                event.preventDefault();
                if (event.shiftKey) {
                  store.findPrevious();
                } else {
                  store.findNext();
                }
              }
            }}
            variant="standard"
            size="small"
            sx={{ my: 0.25, mr: 1 }}
            inputRef={searchFieldRef}
          />
          {invalidRegexp && (
            <FormHelperText
              id={searchHelperId}
              sx={(theme) => ({
                my: 0,
                mr: 1,
                fontSize: 'inherit',
                color: theme.palette.error.main,
              })}
            >
              Invalid regexp
            </FormHelperText>
          )}
          <Stack
            direction="row"
            flexWrap="wrap"
            alignItems="center"
            mr={1}
            rowGap={0.5}
          >
            <IconButton
              aria-label="Previous"
              disabled={!valid}
              onClick={() => store.findPrevious()}
            >
              <KeyboardArrowUpIcon fontSize="small" />
            </IconButton>
            <IconButton
              aria-label="Next"
              disabled={!valid}
              onClick={() => store.findNext()}
            >
              <KeyboardArrowDownIcon fontSize="small" />
            </IconButton>
            <Button
              disabled={!valid}
              onClick={() => store.selectMatches()}
              color="inherit"
              startIcon={<SearchIcon fontSize="inherit" />}
            >
              Find all
            </Button>
          </Stack>
          <Stack direction="row" flexWrap="wrap" rowGap={0.5}>
            <FormControlLabel
              control={
                <Checkbox
                  checked={caseSensitive}
                  onChange={(event) =>
                    store.updateQuery({ caseSensitive: event.target.checked })
                  }
                  size="small"
                />
              }
              label="Match case"
            />
            <FormControlLabel
              control={
                <Checkbox
                  checked={literal}
                  onChange={(event) =>
                    store.updateQuery({ literal: event.target.checked })
                  }
                  size="small"
                />
              }
              label="Literal"
            />
            <FormControlLabel
              control={
                <Checkbox
                  checked={regexp}
                  onChange={(event) =>
                    store.updateQuery({ regexp: event.target.checked })
                  }
                  size="small"
                />
              }
              label="Regexp"
            />
          </Stack>
        </Stack>
        <Stack direction="row" flexWrap="wrap" alignItems="center" rowGap={0.5}>
          <TextField
            placeholder="Replace with"
            aria-label="Replace with"
            value={replace}
            onChange={(event) =>
              store.updateQuery({ replace: event.target.value })
            }
            onKeyDown={(event) => {
              if (event.key === 'Enter') {
                event.preventDefault();
                store.replaceNext();
              }
            }}
            variant="standard"
            size="small"
            sx={{ mr: 1 }}
          />
          <Stack direction="row" flexWrap="wrap" rowGap={0.5}>
            <Button
              disabled={!valid}
              onClick={() => store.replaceNext()}
              color="inherit"
              startIcon={<FindReplaceIcon fontSize="inherit" />}
            >
              Replace
            </Button>
            <Button
              disabled={!valid}
              onClick={() => store.replaceAll()}
              color="inherit"
              startIcon={<FindReplaceIcon fontSize="inherit" />}
            >
              Replace all
            </Button>
          </Stack>
        </Stack>
      </Stack>
      <IconButton onClick={() => store.close()} sx={{ ml: 1 }}>
        <CloseIcon fontSize="small" />
      </IconButton>
    </Toolbar>
  );
}

export default observer(SearchToolbar);
