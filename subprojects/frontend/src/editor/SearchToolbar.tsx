import CloseIcon from '@mui/icons-material/Close';
import FindReplaceIcon from '@mui/icons-material/FindReplace';
import KeyboardArrowDownIcon from '@mui/icons-material/KeyboardArrowDown';
import KeyboardArrowUpIcon from '@mui/icons-material/KeyboardArrowUp';
import Button from '@mui/material/Button';
import Checkbox from '@mui/material/Checkbox';
import FormControlLabel from '@mui/material/FormControlLabel';
import FormHelperText from '@mui/material/FormHelperText';
import IconButton from '@mui/material/IconButton';
import Stack from '@mui/material/Stack';
import TextField from '@mui/material/TextField';
import ToggleButton from '@mui/material/ToggleButton';
import Toolbar from '@mui/material/Toolbar';
import useMediaQuery from '@mui/material/useMediaQuery';
import { observer } from 'mobx-react-lite';
import React, { useCallback, useState } from 'react';

import type SearchPanelStore from './SearchPanelStore';

const SPLIT_MEDIA_QUERY = '@media (max-width: 1200px)';
const ABBREVIATE_MEDIA_QUERY = '@media (max-width: 720px)';

function SearchToolbar({ store }: { store: SearchPanelStore }): JSX.Element {
  const {
    id: panelId,
    query: { search, valid, caseSensitive, literal, regexp, replace },
    invalidRegexp,
  } = store;
  const split = useMediaQuery(SPLIT_MEDIA_QUERY);
  const abbreviate = useMediaQuery(ABBREVIATE_MEDIA_QUERY);
  const [showRepalceState, setShowReplaceState] = useState(false);

  const showReplace = !split || showRepalceState || replace !== '';

  const searchHelperId = `${panelId}-search-helper`;
  const replaceId = `${panelId}-replace`;

  const searchFieldRef = useCallback(
    (element: HTMLInputElement | null) =>
      store.setSearchField(element ?? undefined),
    [store],
  );

  return (
    <Toolbar
      variant="dense"
      sx={{ py: 0.5, alignItems: 'center', minHeight: 'auto' }}
    >
      <Stack
        direction={split ? 'column' : 'row'}
        sx={{
          alignItems: 'center',
          flexGrow: 1,
          [SPLIT_MEDIA_QUERY]: {
            alignItems: 'start',
            gap: 0.5,
          },
        }}
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
            sx={{ mt: '4px', mr: 1 }}
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
              color="inherit"
            >
              <KeyboardArrowUpIcon fontSize="small" />
            </IconButton>
            <IconButton
              aria-label="Next"
              disabled={!valid}
              onClick={() => store.findNext()}
              color="inherit"
            >
              <KeyboardArrowDownIcon fontSize="small" />
            </IconButton>
          </Stack>
          <Stack
            direction="row"
            flexWrap="wrap"
            alignItems="center"
            rowGap={0.5}
          >
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
              aria-label="Match case"
              label={abbreviate ? 'Case' : 'Match case'}
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
              aria-label="Literal"
              label={abbreviate ? 'Lit' : 'Literal'}
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
            {split && (
              <ToggleButton
                value="show-replace"
                selected={showReplace}
                onClick={() => {
                  if (showReplace) {
                    store.updateQuery({ replace: '' });
                    setShowReplaceState(false);
                  } else {
                    setShowReplaceState(true);
                  }
                }}
                aria-label="Show replace options"
                aria-controls={replaceId}
                size="small"
                sx={{ borderRadius: '100%' }}
              >
                <FindReplaceIcon fontSize="small" />
              </ToggleButton>
            )}
          </Stack>
        </Stack>
        <Stack
          id={replaceId}
          direction="row"
          flexWrap="wrap"
          alignItems="center"
          rowGap={0.5}
          display={showReplace ? 'flex' : 'none'}
        >
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
            sx={{ mt: '4px', mr: 1 }}
          />
          <Stack
            direction="row"
            flexWrap="wrap"
            alignItems="center"
            rowGap={0.5}
          >
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
      <Stack direction="row" alignSelf="stretch" alignItems="start" mt="1px">
        <IconButton
          aria-label="Close find/replace"
          onClick={() => store.close()}
          color="inherit"
        >
          <CloseIcon fontSize="small" />
        </IconButton>
      </Stack>
    </Toolbar>
  );
}

export default observer(SearchToolbar);
