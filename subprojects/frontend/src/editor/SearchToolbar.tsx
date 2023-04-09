/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

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
import { styled } from '@mui/material/styles';
import useMediaQuery from '@mui/material/useMediaQuery';
import { observer } from 'mobx-react-lite';
import { useCallback, useState } from 'react';

import type SearchPanelStore from './SearchPanelStore';

const SPLIT_MEDIA_QUERY = '@media (max-width: 1200px)';

const DimLabel = styled(FormControlLabel)(({ theme }) => ({
  '.MuiFormControlLabel-label': {
    ...theme.typography.body2,
    color: theme.palette.text.secondary,
  },
}));

export default observer(function SearchToolbar({
  searchPanelStore,
}: {
  searchPanelStore: SearchPanelStore;
}): JSX.Element {
  const {
    id: panelId,
    query: { search, valid, caseSensitive, literal, regexp, replace },
    invalidRegexp,
  } = searchPanelStore;
  const split = useMediaQuery(SPLIT_MEDIA_QUERY);
  const [showRepalceState, setShowReplaceState] = useState(false);

  const showReplace = !split || showRepalceState || replace !== '';

  const searchHelperId = `${panelId}-search-helper`;
  const replaceId = `${panelId}-replace`;

  const searchFieldRef = useCallback(
    (element: HTMLInputElement | null) =>
      searchPanelStore.setSearchField(element ?? undefined),
    [searchPanelStore],
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
              searchPanelStore.updateQuery({ search: event.target.value })
            }
            onKeyDown={(event) => {
              if (event.key === 'Enter') {
                event.preventDefault();
                if (event.shiftKey) {
                  searchPanelStore.findPrevious();
                } else {
                  searchPanelStore.findNext();
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
              onClick={() => searchPanelStore.findPrevious()}
              color="inherit"
            >
              <KeyboardArrowUpIcon fontSize="small" />
            </IconButton>
            <IconButton
              aria-label="Next"
              disabled={!valid}
              onClick={() => searchPanelStore.findNext()}
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
            <DimLabel
              control={
                <Checkbox
                  checked={caseSensitive}
                  onChange={(event) =>
                    searchPanelStore.updateQuery({
                      caseSensitive: event.target.checked,
                    })
                  }
                  size="small"
                />
              }
              label="Match case"
            />
            <DimLabel
              control={
                <Checkbox
                  checked={literal}
                  onChange={(event) =>
                    searchPanelStore.updateQuery({
                      literal: event.target.checked,
                    })
                  }
                  size="small"
                />
              }
              label="Literal"
            />
            <DimLabel
              control={
                <Checkbox
                  checked={regexp}
                  onChange={(event) =>
                    searchPanelStore.updateQuery({
                      regexp: event.target.checked,
                    })
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
                    searchPanelStore.updateQuery({ replace: '' });
                    setShowReplaceState(false);
                  } else {
                    setShowReplaceState(true);
                  }
                }}
                aria-label="Show replace options"
                aria-controls={replaceId}
                size="small"
                className="iconOnly"
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
              searchPanelStore.updateQuery({ replace: event.target.value })
            }
            onKeyDown={(event) => {
              if (event.key === 'Enter') {
                event.preventDefault();
                searchPanelStore.replaceNext();
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
              onClick={() => searchPanelStore.replaceNext()}
              color="inherit"
              startIcon={<FindReplaceIcon fontSize="inherit" />}
            >
              Replace
            </Button>
            <Button
              disabled={!valid}
              onClick={() => searchPanelStore.replaceAll()}
              color="inherit"
              startIcon={<FindReplaceIcon fontSize="inherit" />}
            >
              Replace all
            </Button>
          </Stack>
        </Stack>
      </Stack>
      <Stack
        direction="row"
        alignSelf="stretch"
        alignItems="start"
        mt="1px"
        sx={{
          [SPLIT_MEDIA_QUERY]: { display: 'none' },
        }}
      >
        <IconButton
          aria-label="Close find/replace"
          onClick={() => searchPanelStore.close()}
          color="inherit"
        >
          <CloseIcon fontSize="small" />
        </IconButton>
      </Stack>
    </Toolbar>
  );
});
