/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import CloseIcon from '@mui/icons-material/Close';
import Alert from '@mui/material/Alert';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import CircularProgress from '@mui/material/CircularProgress';
import IconButton from '@mui/material/IconButton';
import MenuItem from '@mui/material/MenuItem';
import Stack from '@mui/material/Stack';
import Tab from '@mui/material/Tab';
import Tabs from '@mui/material/Tabs';
import Typography from '@mui/material/Typography';
import { styled, useTheme } from '@mui/material/styles';
import { isEqual } from 'lodash-es';
import { useEffect, useId, useState } from 'react';

import RefineryContextBridge, {
  RestartServerResult,
  ServerSettingsResponse as ServerSettingsResponseSchema,
  type ServerSettings,
} from '../RefineryContextBridge';
import { useRootStore } from '../RootStoreProvider';
import Dialog from '../dialog/Dialog';
import DialogActionBar from '../dialog/DialogActionBar';
import getLogger from '../utils/getLogger';

import LogarithmicSlider from './LogarithmicSlider';
import PathListEditor from './PathListEditor';
import {
  MAX_SEMANTICS_TIMEOUT_MS,
  MIN_MODEL_GENERATION_TIMEOUT_SEC,
  MIN_SEMANTICS_TIMEOUT_MS,
  UNLIMITED_MODEL_GENERATION_TIMEOUT_SEC,
} from './serverLimits';
import {
  GIBIBYTE,
  JVM_COMPRESSED_OOPS_THRESHOLD_BYTES,
  MEBIBYTE,
  MIN_MAX_MEMORY_BYTES,
} from './serverMemory';

const log = getLogger('settings.ServerSettingsDialog');

// A newer restart supersedes older results, so a late menu failure cannot
// cover a settings dialog that is already restarting the server.
let latestRestartGeneration = 0;

function beginRestart(): number {
  latestRestartGeneration += 1;
  return latestRestartGeneration;
}

function isCurrentRestart(generation: number): boolean {
  return generation === latestRestartGeneration;
}

export function restartBackendServer(
  refinery: RefineryContextBridge,
  settings?: ServerSettings,
): Promise<RestartServerResult> {
  return refinery
    .restartServer(settings)
    .then((rawResult) => RestartServerResult.parse(rawResult));
}

export function RestartServerMenuItem({
  onClose,
}: {
  onClose: () => void;
}): React.ReactElement {
  const rootStore = useRootStore();

  return (
    <MenuItem
      onClick={() => {
        onClose();
        const refinery = window.refinery;
        if (refinery === undefined) {
          return;
        }
        const generation = beginRestart();
        const showRestartError = () => {
          rootStore.showError(
            'Failed to restart solver',
            'The solver could not be restarted.',
          );
        };
        restartBackendServer(refinery)
          .then((success) => {
            if (!success && isCurrentRestart(generation)) {
              showRestartError();
            }
          })
          .catch((error: unknown) => {
            log.error({ err: error }, 'Failed to restart server');
            if (isCurrentRestart(generation)) {
              showRestartError();
            }
          });
      }}
      sx={{ color: 'error.main' }}
    >
      Restart solver
    </MenuItem>
  );
}

const SEMANTICS_TIMEOUT_MARKS = [
  { value: MIN_SEMANTICS_TIMEOUT_MS, label: '1 s' },
  { value: 2_000, label: '2 s' },
  { value: 5_000, label: '5 s' },
  { value: 10_000, label: '10 s' },
  { value: 30_000, label: '30 s' },
  { value: MAX_SEMANTICS_TIMEOUT_MS, label: '60 s' },
] as const;

const MEMORY_MARK_MIN_DISTANCE = 180;

function formatMemory(valueBytes: number): string {
  if (valueBytes < GIBIBYTE) {
    return `${Math.round(valueBytes / MEBIBYTE)} MiB`;
  }
  const gibibytes = valueBytes / GIBIBYTE;
  return `${Number(gibibytes.toFixed(1))} GiB`;
}

function getMemoryMarks(
  maximum: number,
  defaultMemory: number,
): readonly {
  value: number;
  label: React.ReactNode;
}[] {
  const candidates = new Map<number, { label: string; priority: number }>();
  const addCandidate = (value: number, label: string, priority: number) => {
    if (value < MIN_MAX_MEMORY_BYTES || value > maximum) {
      return;
    }
    const previous = candidates.get(value);
    if (previous === undefined || priority > previous.priority) {
      candidates.set(value, { label, priority });
    }
  };

  addCandidate(MIN_MAX_MEMORY_BYTES, formatMemory(MIN_MAX_MEMORY_BYTES), 1200);
  for (let value = 256 * MEBIBYTE; value < maximum; value *= 2) {
    addCandidate(value, formatMemory(value), 100);
  }
  addCandidate(defaultMemory, formatMemory(defaultMemory), 950);
  addCandidate(
    JVM_COMPRESSED_OOPS_THRESHOLD_BYTES,
    formatMemory(JVM_COMPRESSED_OOPS_THRESHOLD_BYTES),
    900,
  );
  const seventyFivePercent = Math.round((maximum * 0.75) / MEBIBYTE) * MEBIBYTE;
  addCandidate(seventyFivePercent, formatMemory(seventyFivePercent), 850);
  addCandidate(maximum, formatMemory(maximum), 1100);

  const position = (value: number) =>
    Math.log(value / MIN_MAX_MEMORY_BYTES) /
    Math.log(maximum / MIN_MAX_MEMORY_BYTES);
  const labeledValues = new Set<number>();
  for (const [value] of [...candidates.entries()].sort(
    ([, left], [, right]) => right.priority - left.priority,
  )) {
    if (
      [...labeledValues].every(
        (other) =>
          Math.abs(position(value) - position(other)) * 1_000 >=
          MEMORY_MARK_MIN_DISTANCE,
      )
    ) {
      labeledValues.add(value);
    }
  }
  return [...candidates.entries()]
    .sort(([left], [right]) => left - right)
    .map(([value, { label }]) => ({
      value,
      label: labeledValues.has(value) ? label : undefined,
    }));
}

function getMemoryStatus(
  valueBytes: number,
  systemMemoryBytes: number,
): {
  readonly color: 'warning' | 'error' | undefined;
  readonly description: string;
} {
  if (valueBytes > systemMemoryBytes * 0.75) {
    return {
      color: 'error',
      description: 'May reduce system performance by causing swapping.',
    };
  }
  if (valueBytes > JVM_COMPRESSED_OOPS_THRESHOLD_BYTES) {
    return {
      color: 'warning',
      description:
        'May reduce solver performance because JVM references become wider.',
    };
  }
  return {
    color: undefined,
    description: 'Within the recommended memory range.',
  };
}

function describeMemoryValue(
  valueBytes: number,
  systemMemoryBytes: number,
): string {
  const value = formatMemory(valueBytes);
  const { color, description } = getMemoryStatus(valueBytes, systemMemoryBytes);
  return color === undefined ? value : `${value}, ${description}`;
}

const MODEL_GENERATION_TIMEOUT_MAX_SEC = 14_400;
const MODEL_GENERATION_TIMEOUT_MARKS = [
  { value: MIN_MODEL_GENERATION_TIMEOUT_SEC, label: '1 min' },
  { value: 120, label: '2 min' },
  { value: 300, label: '5 min' },
  { value: 600, label: '10 min' },
  { value: 1_800, label: '30 min' },
  { value: 3_600, label: '1 h' },
  { value: 7_200, label: '2 h' },
  { value: MODEL_GENERATION_TIMEOUT_MAX_SEC, label: '4 h' },
  {
    value: UNLIMITED_MODEL_GENERATION_TIMEOUT_SEC,
    label: (
      <abbr title="Unlimited" style={{ textDecoration: 'none' }}>
        Unlim.
      </abbr>
    ),
  },
] as const;

function formatSemanticsTimeout(valueMs: number): string {
  const seconds = valueMs / 1_000;
  return `${Number.isInteger(seconds) ? seconds : seconds.toFixed(1)} s`;
}

function formatGenerationTimeout(valueSec: number): string {
  if (valueSec >= UNLIMITED_MODEL_GENERATION_TIMEOUT_SEC) {
    return 'Unlimited';
  }
  const minutes = Math.round(valueSec / 60);
  if (minutes >= 60) {
    const hours = Math.floor(minutes / 60);
    const remainingMinutes = minutes % 60;
    return remainingMinutes === 0
      ? `${hours} h`
      : `${hours} h ${remainingMinutes} min`;
  }
  return `${minutes} min`;
}

function haveSettingsChanged(
  draft: ServerSettings | undefined,
  initialSettings: ServerSettings | undefined,
): boolean {
  return (
    draft !== undefined &&
    initialSettings !== undefined &&
    !isEqual(draft, initialSettings)
  );
}

export type ServerSettingsTab = 'solver' | 'libraries';

const ServerSettingsTitleBar = styled('div', {
  name: 'ServerSettingsTitleBar',
})(({ theme }) => ({
  alignItems: 'center',
  appRegion: 'drag',
  borderBottom: `1px solid ${theme.palette.divider}`,
  display: 'flex',
  minHeight: '57px',
  paddingTop: 0,
  paddingBottom: 0,
  paddingLeft: theme.spacing(2),
  paddingRight: theme.spacing(1),
  '.MuiTabs-root': {
    alignSelf: 'stretch',
    flexGrow: 1,
    flexShrink: 0,
  },
  '.MuiTabs-list': {
    minHeight: '100%',
  },
  '.MuiTab-root': {
    minHeight: '100%',
    minWidth: 0,
    paddingLeft: theme.spacing(1),
    paddingRight: theme.spacing(1),
    appRegion: 'no-drag',
  },
  '.MuiIconButton-root': {
    flexGrow: 0,
    flexShrink: 0,
    marginLeft: theme.spacing(2),
  },
}));

export default function ServerSettingsDialog({
  open,
  onClose,
  onTabChange,
  tab,
}: {
  open: boolean;
  onClose: () => void;
  onTabChange: (tab: ServerSettingsTab) => void;
  tab: ServerSettingsTab;
}): React.ReactElement {
  const id = useId();
  const theme = useTheme();
  const semanticsTimeoutLabelId = `${id}-semantics-timeout-label`;
  const generationTimeoutLabelId = `${id}-generation-timeout-label`;
  const maxMemoryLabelId = `${id}-max-memory-label`;
  const [draft, setDraft] = useState<ServerSettings>();
  const [initialSettings, setInitialSettings] = useState<ServerSettings>();
  const [systemMemoryBytes, setSystemMemoryBytes] = useState<number>();
  const [defaultMaxMemoryBytes, setDefaultMaxMemoryBytes] = useState<number>();
  const [pathDelimiter, setPathDelimiter] = useState<string>();
  const [loading, setLoading] = useState(true);
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string>();

  const reset = () => {
    setDraft(undefined);
    setInitialSettings(undefined);
    setSystemMemoryBytes(undefined);
    setDefaultMaxMemoryBytes(undefined);
    setPathDelimiter(undefined);
    setError(undefined);
    setLoading(true);
  };

  useEffect(() => {
    if (!open) {
      return undefined;
    }
    const refinery = window.refinery;
    let disposed = false;
    if (refinery === undefined) {
      return undefined;
    }
    (async () => {
      try {
        const rawSettings = await refinery.getServerSettings();
        const {
          settings: serverSettings,
          systemMemoryBytes: availableSystemMemoryBytes,
          defaultMaxMemoryBytes: availableDefaultMaxMemoryBytes,
          pathDelimiter: availablePathDelimiter,
        } = ServerSettingsResponseSchema.parse(rawSettings);
        if (!disposed) {
          setDraft(serverSettings);
          setInitialSettings(serverSettings);
          setSystemMemoryBytes(availableSystemMemoryBytes);
          setDefaultMaxMemoryBytes(availableDefaultMaxMemoryBytes);
          setPathDelimiter(availablePathDelimiter);
        }
      } finally {
        if (!disposed) {
          setLoading(false);
        }
      }
    })().catch((err: unknown) => {
      log.error({ err }, 'Failed to load settings');
      if (!disposed) {
        setError('Failed to load settings.');
      }
    });
    return () => {
      disposed = true;
    };
  }, [open]);

  const close = () => {
    if (pending) {
      return;
    }
    onClose();
  };

  const restartFailed = () => {
    setPending(false);
    setError('Failed to restart the solver.');
  };

  const apply = () => {
    if (draft === undefined || initialSettings === undefined || pending) {
      return;
    }
    const refinery = window.refinery;
    if (refinery === undefined) {
      return;
    }
    const settingsChanged = haveSettingsChanged(draft, initialSettings);
    beginRestart();
    (async () => {
      setError(undefined);
      setPending(true);
      try {
        const success = await restartBackendServer(
          refinery,
          settingsChanged ? draft : undefined,
        );
        if (success) {
          close();
        } else {
          restartFailed();
        }
      } finally {
        setPending(false);
      }
    })().catch((err: unknown) => {
      log.error({ err }, 'Failed to restart solver');
      restartFailed();
    });
  };

  const updateSemanticsTimeout = (semanticsTimeoutMs: number) => {
    if (draft === undefined) {
      return;
    }
    setDraft({
      ...draft,
      semanticsTimeoutMs,
    });
  };

  const updateGenerationTimeout = (modelGenerationTimeoutSec: number) => {
    if (draft === undefined) {
      return;
    }
    setDraft({
      ...draft,
      modelGenerationTimeoutSec,
    });
  };

  const updateMaxMemory = (maxMemoryBytes: number) => {
    if (draft === undefined) {
      return;
    }
    setDraft({
      ...draft,
      maxMemoryBytes,
    });
  };

  const updateLibraryPaths = (libraryPaths: readonly string[]) => {
    setDraft((currentDraft) =>
      currentDraft === undefined
        ? currentDraft
        : { ...currentDraft, libraryPaths: [...libraryPaths] },
    );
  };

  const updateClasspathJars = (classpathJars: readonly string[]) => {
    setDraft((currentDraft) =>
      currentDraft === undefined
        ? currentDraft
        : { ...currentDraft, classpathJars: [...classpathJars] },
    );
  };

  const selectLibraryDirectory = () =>
    window.refinery?.selectLibraryDirectory() ?? Promise.resolve(undefined);

  const selectClasspathJar = () =>
    window.refinery?.selectClasspathJar() ?? Promise.resolve(undefined);

  const getPathForFile = (file: File) =>
    window.refinery?.getPathForFile(file) ?? '';

  const dismiss = pending ? undefined : close;
  const settingsChanged = haveSettingsChanged(draft, initialSettings);
  const maximumMemory =
    systemMemoryBytes === undefined
      ? undefined
      : Math.max(
          Math.floor(systemMemoryBytes / MEBIBYTE) * MEBIBYTE,
          MIN_MAX_MEMORY_BYTES,
        );
  const memoryStatus =
    draft === undefined || systemMemoryBytes === undefined
      ? undefined
      : getMemoryStatus(draft.maxMemoryBytes, systemMemoryBytes);
  const memoryCaptionColor =
    memoryStatus?.color === undefined
      ? theme.palette.text.secondary
      : theme.palette[memoryStatus.color][
          theme.palette.mode === 'dark' ? 'main' : 'dark'
        ];

  return (
    <Dialog
      open={open}
      onClose={dismiss}
      maxWidth="sm"
      fullWidth
      disableRestoreFocus
      slotProps={{
        transition: {
          onExited: reset,
        },
      }}
    >
      <ServerSettingsTitleBar>
        <Tabs
          value={tab}
          onChange={(_event, value: ServerSettingsTab) => {
            setError(undefined);
            onTabChange(value);
          }}
          aria-label="Settings"
        >
          <Tab value="solver" label="Solver options" disabled={pending} />
          <Tab value="libraries" label="Libraries" disabled={pending} />
        </Tabs>
        <IconButton
          aria-label="Close"
          disabled={pending}
          onClick={close}
          sx={{ appRegion: 'no-drag' }}
        >
          <CloseIcon />
        </IconButton>
      </ServerSettingsTitleBar>
      <Box sx={{ minWidth: 0, p: 2 }}>
        {error !== undefined && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        )}
        {loading ? (
          <Stack direction="row" spacing={2} sx={{ alignItems: 'center' }}>
            <CircularProgress size={24} />
            <span>Loading settings</span>
          </Stack>
        ) : (
          draft !== undefined &&
          (tab === 'libraries' ? (
            <Stack spacing={2}>
              <Stack spacing={1}>
                <Typography>
                  Directories containing modules available through the{' '}
                  <Typography
                    component="code"
                    sx={(theme) => theme.typography.editor}
                  >
                    import
                  </Typography>{' '}
                  mechanism.
                </Typography>
                <PathListEditor
                  kind="directory"
                  paths={draft.libraryPaths}
                  onPathsChange={updateLibraryPaths}
                  pathDelimiter={pathDelimiter}
                  pending={pending}
                  selectPath={selectLibraryDirectory}
                  getPathForFile={getPathForFile}
                />
              </Stack>
              <Stack spacing={1}>
                <Typography>
                  Plugin JAR files added to the solver Java classpath.
                </Typography>
                <PathListEditor
                  kind="jar"
                  paths={draft.classpathJars}
                  onPathsChange={updateClasspathJars}
                  pathDelimiter={pathDelimiter}
                  pending={pending}
                  selectPath={selectClasspathJar}
                  getPathForFile={getPathForFile}
                />
              </Stack>
            </Stack>
          ) : (
            systemMemoryBytes !== undefined &&
            defaultMaxMemoryBytes !== undefined &&
            maximumMemory !== undefined &&
            memoryStatus !== undefined && (
              <Stack spacing={2}>
                <Box>
                  <Typography id={maxMemoryLabelId} gutterBottom>
                    Maximum memory: {formatMemory(draft.maxMemoryBytes)}
                  </Typography>
                  <Box sx={{ mx: 3 }}>
                    <LogarithmicSlider
                      ariaLabelledby={maxMemoryLabelId}
                      minimum={MIN_MAX_MEMORY_BYTES}
                      maximum={maximumMemory}
                      step={MEBIBYTE}
                      marks={getMemoryMarks(
                        maximumMemory,
                        defaultMaxMemoryBytes,
                      )}
                      value={draft.maxMemoryBytes}
                      formatValue={formatMemory}
                      describeValue={(value) =>
                        describeMemoryValue(value, systemMemoryBytes)
                      }
                      onChange={updateMaxMemory}
                      color={memoryStatus.color}
                      disabled={pending}
                    />
                    <Typography
                      variant="caption"
                      sx={{
                        color: memoryCaptionColor,
                        display: 'block',
                        mt: 0.5,
                      }}
                    >
                      {memoryStatus.description}
                    </Typography>
                  </Box>
                </Box>
                <Box>
                  <Typography id={semanticsTimeoutLabelId} gutterBottom>
                    Analysis timeout:{' '}
                    {formatSemanticsTimeout(draft.semanticsTimeoutMs)}
                  </Typography>
                  <Box sx={{ mx: 3 }}>
                    <LogarithmicSlider
                      ariaLabelledby={semanticsTimeoutLabelId}
                      minimum={MIN_SEMANTICS_TIMEOUT_MS}
                      maximum={MAX_SEMANTICS_TIMEOUT_MS}
                      step={1_000}
                      marks={SEMANTICS_TIMEOUT_MARKS}
                      value={draft.semanticsTimeoutMs}
                      formatValue={formatSemanticsTimeout}
                      onChange={updateSemanticsTimeout}
                      disabled={pending}
                    />
                  </Box>
                </Box>
                <Box>
                  <Typography id={generationTimeoutLabelId} gutterBottom>
                    Generation timeout:{' '}
                    {formatGenerationTimeout(draft.modelGenerationTimeoutSec)}
                  </Typography>
                  <Box sx={{ mx: 3 }}>
                    <LogarithmicSlider
                      ariaLabelledby={generationTimeoutLabelId}
                      minimum={MIN_MODEL_GENERATION_TIMEOUT_SEC}
                      maximum={MODEL_GENERATION_TIMEOUT_MAX_SEC}
                      step={60}
                      unlimitedValue={UNLIMITED_MODEL_GENERATION_TIMEOUT_SEC}
                      marks={MODEL_GENERATION_TIMEOUT_MARKS}
                      value={draft.modelGenerationTimeoutSec}
                      formatValue={formatGenerationTimeout}
                      onChange={updateGenerationTimeout}
                      disabled={pending}
                    />
                  </Box>
                </Box>
              </Stack>
            )
          ))
        )}
      </Box>
      <DialogActionBar>
        <Button
          variant="text"
          onClick={apply}
          disabled={draft === undefined || loading || pending}
          color={pending ? 'inherit' : 'error'}
        >
          {pending
            ? 'Restarting'
            : settingsChanged
              ? 'Apply and restart solver'
              : 'Restart solver'}
        </Button>
      </DialogActionBar>
    </Dialog>
  );
}
