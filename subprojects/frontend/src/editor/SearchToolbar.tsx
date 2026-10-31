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
import { styled } from '@mui/material/styles';
import { observer } from 'mobx-react-lite';
import { useCallback, useState } from 'react';

import Tooltip from '../Tooltip';

import type SearchPanelStore from './SearchPanelStore';

const DimLabel = styled(FormControlLabel)(({ theme }) => ({
  '.MuiFormControlLabel-label': {
    ...theme.typography.body2,
    color: theme.palette.text.secondary,
    userSelect: 'none',
  },
}));

const ToolbarRow = styled(Stack)(({ theme }) => ({
  flexWrap: 'wrap',
  alignItems: 'center',
  rowGap: theme.spacing(0.5),
  '& > *': {
    minHeight: theme.spacing(5),
  },
  '& > .MuiTextField-root': {
    justifyContent: 'center',
  },
  '& > .RefineryTooltip-Container': {
    display: 'flex',
    alignItems: 'center',
  },
}));

export default observer(function SearchToolbar({
  searchPanelStore,
  width,
}: {
  searchPanelStore: SearchPanelStore;
  width: number | undefined;
}): React.ReactElement {
  const {
    id: panelId,
    query: { search, valid, caseSensitive, literal, regexp, replace },
    invalidRegexp,
  } = searchPanelStore;
  const split = width !== undefined && width <= 1200;
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
    <Stack
      direction="row"
      sx={{
        alignItems: 'center',
        pl: 1.5,
        pr: 1,
        pt: 0.5,
        pb: 0.5,
        overflowY: 'scroll',
        scrollbarWidth: 'none',
        '::-webkit-scrollbar': {
          background: 'transparent',
          width: 0,
          height: 0,
        },
      }}
    >
      <Stack
        direction={split ? 'column' : 'row'}
        sx={{
          alignItems: 'stretch',
          flexGrow: 1,
          ...(split
            ? {
                gap: 0.5,
              }
            : {}),
        }}
      >
        <ToolbarRow direction="row">
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
            size="medium"
            sx={{ mr: 1 }}
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
                display: 'flex',
                alignItems: 'center',
              })}
            >
              Invalid regexp
            </FormHelperText>
          )}
          <ToolbarRow
            direction="row"
            sx={(theme) => ({ mr: theme.spacing(1) })}
          >
            <Tooltip title="Previous match">
              <IconButton
                disabled={!valid}
                onClick={() => searchPanelStore.findPrevious()}
                color="inherit"
              >
                <KeyboardArrowUpIcon fontSize="small" />
              </IconButton>
            </Tooltip>
            <Tooltip title="Next match">
              <IconButton
                disabled={!valid}
                onClick={() => searchPanelStore.findNext()}
                color="inherit"
              >
                <KeyboardArrowDownIcon fontSize="small" />
              </IconButton>
            </Tooltip>
          </ToolbarRow>
          <ToolbarRow direction="row">
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
              <Tooltip title="Replace">
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
                  aria-controls={replaceId}
                  size="small"
                  className="iconOnly"
                >
                  <FindReplaceIcon fontSize="small" />
                </ToggleButton>
              </Tooltip>
            )}
          </ToolbarRow>
        </ToolbarRow>
        <ToolbarRow
          id={replaceId}
          direction="row"
          sx={{
            display: showReplace ? 'flex' : 'none',
          }}
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
            size="medium"
            sx={{ mr: 1 }}
          />
          <ToolbarRow direction="row">
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
          </ToolbarRow>
        </ToolbarRow>
      </Stack>
      <Stack
        sx={{
          ...(split ? { display: 'none' } : {}),
          alignSelf: 'stretch',
          alignItems: 'center',
        }}
      >
        <IconButton
          aria-label="Close find/replace"
          onClick={() => searchPanelStore.close()}
          color="inherit"
        >
          <CloseIcon />
        </IconButton>
      </Stack>
    </Stack>
  );
});
