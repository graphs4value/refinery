/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import ChevronRightIcon from '@mui/icons-material/ChevronRight';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import DarkModeIcon from '@mui/icons-material/DarkMode';
import ImageIcon from '@mui/icons-material/Image';
import InsertDriveFileOutlinedIcon from '@mui/icons-material/InsertDriveFileOutlined';
import LightModeIcon from '@mui/icons-material/LightMode';
import SaveAltIcon from '@mui/icons-material/SaveAlt';
import ShapeLineIcon from '@mui/icons-material/ShapeLine';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import FormControlLabel from '@mui/material/FormControlLabel';
import Slider from '@mui/material/Slider';
import Stack from '@mui/material/Stack';
import Switch from '@mui/material/Switch';
import ToggleButton from '@mui/material/ToggleButton';
import ToggleButtonGroup from '@mui/material/ToggleButtonGroup';
import Typography from '@mui/material/Typography';
import { styled } from '@mui/material/styles';
import { observer } from 'mobx-react-lite';
import { useCallback } from 'react';

import { useRootStore } from '../../RootStoreProvider';
import getLogger from '../../utils/getLogger';
import type GraphStore from '../GraphStore';
import SlideInPanel from '../SlideInPanel';

import exportDiagram from './exportDiagram';

const log = getLogger('graph.ExportPanel');

const SwitchButtonGroup = styled(ToggleButtonGroup, {
  name: 'ExportPanel-SwitchButtonGroup',
})(({ theme }) => ({
  marginTop: theme.spacing(2),
  marginInline: theme.spacing(2),
  minWidth: '260px',
  '.MuiToggleButton-root': {
    width: '100%',
    fontSize: '1rem',
    lineHeight: '1.5',
  },
  '& svg': {
    margin: '0 6px 0 0',
  },
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

function ExportPanel({
  graph,
  svgContainer,
  dialog,
}: {
  graph: GraphStore;
  svgContainer: HTMLElement | undefined;
  dialog: boolean;
}): JSX.Element {
  const { exportSettingsStore } = useRootStore();

  const icon = useCallback(
    (show: boolean) =>
      show && !dialog ? <ChevronRightIcon /> : <SaveAltIcon />,
    [dialog],
  );

  const { format } = exportSettingsStore;
  const emptyGraph = graph.semantics.nodes.length === 0;
  const buttons = useCallback(
    (close: () => void) => (
      <>
        <Button
          color="inherit"
          startIcon={<SaveAltIcon />}
          disabled={emptyGraph}
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
        {'write' in navigator.clipboard && format === 'png' && (
          <Button
            color="inherit"
            startIcon={<ContentCopyIcon />}
            disabled={emptyGraph}
            onClick={() => {
              exportDiagram(svgContainer, graph, exportSettingsStore, 'copy')
                .then(close)
                .catch((error) => {
                  log.error('Failed to copy diagram', error);
                });
            }}
          >
            Copy
          </Button>
        )}
      </>
    ),
    [svgContainer, graph, exportSettingsStore, format, emptyGraph],
  );

  return (
    <SlideInPanel
      anchor="right"
      dialog={dialog}
      title="Export diagram"
      icon={icon}
      iconLabel="Show export panel"
      buttons={buttons}
    >
      <SwitchButtonGroup size="small" className="rounded">
        <ToggleButton
          value="svg"
          selected={exportSettingsStore.format === 'svg'}
          onClick={() => exportSettingsStore.setFormat('svg')}
        >
          <ShapeLineIcon fontSize="small" /> SVG
        </ToggleButton>
        <ToggleButton
          value="pdf"
          selected={exportSettingsStore.format === 'pdf'}
          onClick={() => exportSettingsStore.setFormat('pdf')}
        >
          <InsertDriveFileOutlinedIcon fontSize="small" /> PDF
        </ToggleButton>
        <ToggleButton
          value="png"
          selected={exportSettingsStore.format === 'png'}
          onClick={() => exportSettingsStore.setFormat('png')}
        >
          <ImageIcon fontSize="small" /> PNG
        </ToggleButton>
      </SwitchButtonGroup>
      <SwitchButtonGroup size="small" className="rounded">
        <ToggleButton
          value="svg"
          selected={exportSettingsStore.theme === 'light'}
          onClick={() => exportSettingsStore.setTheme('light')}
        >
          <LightModeIcon fontSize="small" /> Light
        </ToggleButton>
        <ToggleButton
          value="png"
          selected={exportSettingsStore.theme === 'dark'}
          onClick={() => exportSettingsStore.setTheme('dark')}
        >
          <DarkModeIcon fontSize="small" /> Dark
        </ToggleButton>
      </SwitchButtonGroup>
      <FormControlLabel
        control={
          <Switch
            checked={exportSettingsStore.transparent}
            onClick={() => exportSettingsStore.toggleTransparent()}
          />
        }
        label="Transparent background"
      />
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
