/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import ChevronRightIcon from '@mui/icons-material/ChevronRight';
import CodeIcon from '@mui/icons-material/Code';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import ContrastIcon from '@mui/icons-material/Contrast';
import DarkModeIcon from '@mui/icons-material/DarkMode';
import EditIcon from '@mui/icons-material/Edit';
import ImageIcon from '@mui/icons-material/Image';
import InsertDriveFileOutlinedIcon from '@mui/icons-material/InsertDriveFileOutlined';
import LightModeIcon from '@mui/icons-material/LightMode';
import SaveAltIcon from '@mui/icons-material/SaveAlt';
import ShapeLineIcon from '@mui/icons-material/ShapeLine';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import FormControlLabel from '@mui/material/FormControlLabel';
import MenuItem from '@mui/material/MenuItem';
import Select, { type SelectProps } from '@mui/material/Select';
import Slider from '@mui/material/Slider';
import Stack from '@mui/material/Stack';
import Switch from '@mui/material/Switch';
import Typography from '@mui/material/Typography';
import { styled } from '@mui/material/styles';
import { observer } from 'mobx-react-lite';
import { useCallback, useState } from 'react';

import { useRootStore } from '../../RootStoreProvider';
import getLogger from '../../utils/getLogger';
import type GraphStore from '../GraphStore';
import SlideInPanel from '../SlideInPanel';

import exportDiagram from './exportDiagram';

const log = getLogger('graph.ExportPanel');

const AutoThemeMessage = styled(Typography, {
  name: 'ExportPanel-AutoThemeMessage',
})(({ theme }) => ({
  width: '260px',
  marginInline: theme.spacing(2),
}));

function getLabel(value: number): string {
  return `${value}%`;
}

const marks = [100, 200, 300, 400].map((value) => ({
  value,
  label: (
    <Stack direction="column" alignItems="center">
      <ImageIcon sx={{ width: `${11 + (value / 100) * 3}px` }} />
      <Typography variant="caption">{getLabel(value)}</Typography>
    </Stack>
  ),
}));

function RoundedSelect<Value = unknown>(
  props: Omit<SelectProps<Value>, 'size' | 'sx' | 'variant' | 'MenuProps'>,
): React.ReactElement {
  return (
    <Select
      {...props}
      variant="outlined"
      size="small"
      sx={(theme) => ({
        flexBasis: 0,
        flexGrow: 1,
        borderRadius: '50rem',
        '.MuiOutlinedInput-input': {
          ...theme.typography.button,
          // Add `!important` to avoid jumping around when the `Select` is inserted into the DOM.
          display: 'flex !important',
          alignItems: 'center !important',
          justifyContent: 'center !important',
          '& svg': {
            margin: '0 6px 0 0 !important',
          },
        },
      })}
      MenuProps={{
        sx: (theme) => ({
          display: 'block',
          '.MuiMenuItem-root': {
            ...theme.typography.button,
            display: 'flex',
            alignItems: 'center',
            '& svg': {
              margin: '0 6px 0 0',
            },
          },
        }),
      }}
    />
  );
}

function ExportPanel({
  graph,
  svgContainer,
  dialog,
}: {
  graph: GraphStore;
  svgContainer: HTMLElement | undefined;
  dialog: boolean;
}): React.ReactElement {
  const { exportSettingsStore } = useRootStore();
  const [shiftDown, setShiftDown] = useState(false);

  const icon = useCallback(
    (show: boolean) =>
      show && !dialog ? <ChevronRightIcon /> : <SaveAltIcon />,
    [dialog],
  );

  const { canCopy, format, plainText } = exportSettingsStore;
  const emptyGraph = graph.semantics.nodes.length === 0;
  const disabled = emptyGraph || (plainText && !graph.hasSource);
  const shouldEdit = plainText && shiftDown && !disabled;
  const buttons = useCallback(
    (close: () => void) => (
      <>
        <Button
          color="inherit"
          startIcon={<SaveAltIcon />}
          disabled={disabled}
          onClick={() => {
            exportDiagram(svgContainer, graph, exportSettingsStore, 'download')
              .then(close)
              .catch((error) => {
                log.error('Failed to download diagram', error);
              });
          }}
        >
          Download
        </Button>
        {('write' in navigator.clipboard || plainText) && canCopy && (
          <Button
            color="inherit"
            startIcon={shouldEdit ? <EditIcon /> : <ContentCopyIcon />}
            disabled={disabled}
            onClick={() => {
              exportDiagram(
                svgContainer,
                graph,
                exportSettingsStore,
                shouldEdit ? 'edit' : 'copy',
              )
                .then(close)
                .catch((error) => {
                  log.error('Failed to copy diagram', error);
                });
            }}
          >
            {shouldEdit ? 'Edit' : 'Copy'}
          </Button>
        )}
      </>
    ),
    [
      svgContainer,
      graph,
      exportSettingsStore,
      plainText,
      canCopy,
      disabled,
      shouldEdit,
    ],
  );

  return (
    <SlideInPanel
      anchor="right"
      dialog={dialog}
      title="Export diagram"
      icon={icon}
      iconLabel="Export"
      buttons={buttons}
      onKeyDown={({ key }) => {
        if (key === 'Shift') {
          setShiftDown(true);
        }
      }}
      onKeyUp={({ key }) => {
        if (key === 'Shift') {
          setShiftDown(false);
        }
      }}
      onMouseMove={({ shiftKey }) => {
        if (shiftKey !== shiftDown) {
          setShiftDown(shiftKey);
        }
      }}
    >
      <Stack
        direction="row"
        sx={(theme) => ({
          marginTop: theme.spacing(2),
          marginInline: theme.spacing(2),
          minWidth: '260px',
          gap: theme.spacing(2),
        })}
      >
        <RoundedSelect
          aria-label="Format"
          value={format}
          onChange={(event) =>
            exportSettingsStore.setFormat(event.target.value)
          }
        >
          <MenuItem value="svg">
            <ShapeLineIcon fontSize="small" /> SVG
          </MenuItem>
          <MenuItem value="pdf">
            <InsertDriveFileOutlinedIcon fontSize="small" /> PDF
          </MenuItem>
          <MenuItem value="png">
            <ImageIcon fontSize="small" /> PNG
          </MenuItem>
          <MenuItem value="refinery">
            <CodeIcon fontSize="small" /> Refinery
          </MenuItem>
        </RoundedSelect>
        {exportSettingsStore.canSetTheme && (
          <RoundedSelect
            aria-label="Theme"
            value={exportSettingsStore.theme}
            onChange={(event) =>
              exportSettingsStore.setTheme(event.target.value)
            }
          >
            <MenuItem value="light">
              <LightModeIcon fontSize="small" /> Light
            </MenuItem>
            <MenuItem value="dark">
              <DarkModeIcon fontSize="small" /> Dark
            </MenuItem>
            {exportSettingsStore.canSetDynamicTheme && (
              <MenuItem value="dynamic">
                <ContrastIcon fontSize="small" /> Auto
              </MenuItem>
            )}
          </RoundedSelect>
        )}
      </Stack>
      {exportSettingsStore.canChangeTransparency && (
        <FormControlLabel
          control={
            <Switch
              checked={exportSettingsStore.transparent}
              onClick={() => exportSettingsStore.toggleTransparent()}
            />
          }
          label="Transparent background"
        />
      )}
      {exportSettingsStore.canEmbedFonts && (
        <FormControlLabel
          control={
            <Switch
              checked={exportSettingsStore.embedFonts}
              onClick={() => exportSettingsStore.toggleEmbedFonts()}
            />
          }
          label={
            <Stack direction="column">
              <Typography>Embed fonts</Typography>
              <Typography variant="caption">
                {exportSettingsStore.format === 'pdf' ? (
                  <>+20&thinsp;kB fully embedded</>
                ) : (
                  <>+75&thinsp;kB, only supported in browsers</>
                )}
              </Typography>
            </Stack>
          }
        />
      )}
      {exportSettingsStore.theme === 'dynamic' && (
        <>
          <AutoThemeMessage mt={2}>
            For embedding into HTML directly
          </AutoThemeMessage>
          <AutoThemeMessage variant="caption" mt={1}>
            Set <code>data-theme=&quot;dark&quot;</code> on a containing element
            to use a dark theme
          </AutoThemeMessage>
        </>
      )}
      {exportSettingsStore.canScale && (
        <Box mx={4} mt={1} mb={2}>
          <Slider
            aria-label="Image scale"
            value={exportSettingsStore.scale}
            min={100}
            max={400}
            valueLabelFormat={getLabel}
            getAriaValueText={getLabel}
            step={50}
            valueLabelDisplay="auto"
            marks={marks}
            onChange={(_, value) => {
              if (typeof value === 'number') {
                exportSettingsStore.setScale(value);
              }
            }}
          />
        </Box>
      )}
    </SlideInPanel>
  );
}

export default observer(ExportPanel);
