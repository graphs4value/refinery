/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import ArrowDownwardIcon from '@mui/icons-material/ArrowDownward';
import ArrowUpwardIcon from '@mui/icons-material/ArrowUpward';
import CreateNewFolderIcon from '@mui/icons-material/CreateNewFolderOutlined';
import DeleteIcon from '@mui/icons-material/Delete';
import DragIndicatorIcon from '@mui/icons-material/DragIndicator';
import NoteAddIcon from '@mui/icons-material/NoteAddOutlined';
import Alert from '@mui/material/Alert';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import IconButton from '@mui/material/IconButton';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import { useTheme } from '@mui/material/styles';
import { type DragEvent, useRef, useState } from 'react';

import {
  PathSelectionResult as PathSelectionResultSchema,
  type PathSelectionResult,
} from '../RefineryContextBridge';
import getLogger from '../utils/getLogger';

const log = getLogger('PathListEditor');

export type PathListEditorKind = 'directory' | 'jar';

export default function PathListEditor({
  kind,
  paths,
  onPathsChange,
  pathDelimiter,
  pending,
  selectPath,
  getPathForFile,
}: {
  kind: PathListEditorKind;
  paths: readonly string[];
  onPathsChange: (paths: readonly string[]) => void;
  pathDelimiter: string | undefined;
  pending: boolean;
  selectPath: () => Promise<PathSelectionResult>;
  getPathForFile: (file: File) => string;
}): React.ReactElement {
  const theme = useTheme();
  const [draggedIndex, setDraggedIndex] = useState<number>();
  const [dragTargetIndex, setDragTargetIndex] = useState<number>();
  const [externalDragOver, setExternalDragOver] = useState(false);
  const [error, setError] = useState<string>();
  const moveButtonRefs = useRef(new Map<string, HTMLButtonElement>());
  const isDirectory = kind === 'directory';
  const itemName = isDirectory ? 'directory' : 'JAR file';
  const itemNamePlural = isDirectory ? 'directories' : 'JAR files';

  const resetDragState = () => {
    setDraggedIndex(undefined);
    setDragTargetIndex(undefined);
    setExternalDragOver(false);
  };

  const addPaths = (newPaths: readonly string[]) => {
    setError(undefined);
    if (newPaths.length === 0) {
      return;
    }
    const acceptedPaths: string[] = [];
    let rejectedPath: string | undefined;
    let duplicatePath: string | undefined;
    let invalidPath: string | undefined;
    for (const newPath of newPaths) {
      if (!isDirectory && !newPath.toLowerCase().endsWith('.jar')) {
        invalidPath ??= newPath;
      } else if (
        pathDelimiter !== undefined &&
        newPath.includes(pathDelimiter)
      ) {
        rejectedPath ??= newPath;
      } else if (paths.includes(newPath) || acceptedPaths.includes(newPath)) {
        duplicatePath ??= newPath;
      } else {
        acceptedPaths.push(newPath);
      }
    }
    if (invalidPath !== undefined) {
      setError('Only JAR files can be added to the list.');
    } else if (rejectedPath !== undefined && pathDelimiter !== undefined) {
      setError(
        `The ${itemName} "${rejectedPath}" cannot be added because its path contains "${pathDelimiter}", the path separator.`,
      );
    } else if (duplicatePath !== undefined) {
      setError(`The ${itemName} "${duplicatePath}" is already in the list.`);
    }
    if (acceptedPaths.length === 0) {
      return;
    }
    onPathsChange([...paths, ...acceptedPaths]);
  };

  const select = () => {
    (async () => {
      const rawPath = await selectPath();
      const selectedPath = PathSelectionResultSchema.parse(rawPath);
      if (selectedPath === undefined) {
        return;
      }
      if (typeof selectedPath === 'string') {
        addPaths([selectedPath]);
      } else {
        setError(`Failed to select ${itemName}.`);
      }
    })().catch((error: unknown) => {
      log.error({ err: error }, `Failed to select ${itemName}`);
      setError(`Failed to select ${itemName}.`);
    });
  };

  const removePath = (path: string) => {
    onPathsChange(paths.filter((currentPath) => currentPath !== path));
  };

  const movePath = (from: number, to: number) => {
    if (
      from === to ||
      from < 0 ||
      to < 0 ||
      from >= paths.length ||
      to >= paths.length
    ) {
      return;
    }
    const newPaths = [...paths];
    const path = newPaths.splice(from, 1)[0];
    if (path === undefined) {
      return;
    }
    newPaths.splice(to, 0, path);
    onPathsChange(newPaths);
  };

  const movePathWithFocus = (
    from: number,
    to: number,
    direction: 'up' | 'down',
  ) => {
    const path = paths[from];
    if (path === undefined) {
      return;
    }
    movePath(from, to);
    const focusDirection =
      (direction === 'up' && to === 0) ||
      (direction === 'down' && to === paths.length - 1)
        ? direction === 'up'
          ? 'down'
          : 'up'
        : direction;
    requestAnimationFrame(() => {
      moveButtonRefs.current.get(`${focusDirection}:${path}`)?.focus();
    });
  };

  const isAcceptedDrop = (item: DataTransferItem, file: File | null) => {
    const entry = item.webkitGetAsEntry();
    if (isDirectory) {
      return entry?.isDirectory === true;
    }
    return (
      entry?.isFile === true &&
      file?.name.toLowerCase().endsWith('.jar') === true
    );
  };

  const handleDrop = (event: DragEvent<HTMLElement>, index?: number) => {
    if (pending) {
      event.preventDefault();
      event.stopPropagation();
      resetDragState();
      return;
    }
    event.preventDefault();
    event.stopPropagation();
    if (draggedIndex !== undefined && index !== undefined) {
      movePath(draggedIndex, index);
    } else {
      const fileItems = Array.from(event.dataTransfer.items).filter(
        (item) => item.kind === 'file',
      );
      const acceptedItems = fileItems
        .map((item) => ({ item, file: item.getAsFile() }))
        .filter(({ item, file }) => isAcceptedDrop(item, file));
      const newPaths = acceptedItems
        .map(({ file }) => file)
        .filter((file): file is File => file !== null)
        .map((file) => {
          try {
            return getPathForFile(file);
          } catch (error) {
            log.error({ err: error }, 'Failed to get dropped path');
            return undefined;
          }
        })
        .filter((path): path is string => path !== undefined && path !== '');
      addPaths(newPaths);
      if (fileItems.length > 0 && acceptedItems.length === 0) {
        setError(`Only ${itemNamePlural} can be added to the list.`);
      }
    }
    resetDragState();
  };

  return (
    <Stack spacing={1}>
      {error !== undefined && <Alert severity="error">{error}</Alert>}
      <Button
        variant="outlined"
        startIcon={isDirectory ? <CreateNewFolderIcon /> : <NoteAddIcon />}
        onClick={select}
        disabled={pending}
      >
        Add {itemName}
      </Button>
      <Box
        onDragOver={(event) => {
          event.preventDefault();
          if (
            draggedIndex === undefined &&
            Array.from(event.dataTransfer.items).some(
              (item) => item.kind === 'file',
            )
          ) {
            setExternalDragOver(true);
          }
        }}
        onDragLeave={(event) => {
          if (!event.currentTarget.contains(event.relatedTarget as Node)) {
            setExternalDragOver(false);
          }
        }}
        onDrop={handleDrop}
        sx={{
          border: '1px dashed',
          borderColor: externalDragOver ? 'primary.main' : 'divider',
          borderRadius: 1,
          bgcolor: externalDragOver ? 'action.hover' : undefined,
          minHeight: '96px',
          p: 1,
          display: 'flex',
          flexDirection: 'column',
          justifyContent: paths.length === 0 ? 'center' : 'flex-start',
        }}
      >
        {paths.length === 0 ? (
          <Typography
            variant="body2"
            color="textSecondary"
            sx={{ p: 2, textAlign: 'center' }}
          >
            Drag {itemNamePlural} here to add them.
          </Typography>
        ) : (
          <Stack
            spacing={0.5}
            role="list"
            sx={{ maxHeight: '200px', overflowY: 'auto', pr: 0.5 }}
          >
            {paths.map((path, index) => (
              <Box
                key={path}
                draggable={!pending}
                onDragStart={(event) => {
                  event.dataTransfer.effectAllowed = 'move';
                  event.dataTransfer.setData('text/plain', path);
                  setDraggedIndex(index);
                  setDragTargetIndex(undefined);
                  setExternalDragOver(false);
                }}
                onDragEnd={resetDragState}
                onDragOver={(event) => {
                  event.preventDefault();
                  if (draggedIndex !== undefined && draggedIndex !== index) {
                    setDragTargetIndex((currentIndex) =>
                      currentIndex === index ? currentIndex : index,
                    );
                  }
                }}
                onDrop={(event) => handleDrop(event, index)}
                role="listitem"
                sx={{
                  alignItems: 'center',
                  borderRadius: 1,
                  display: 'flex',
                  gap: 0.5,
                  minWidth: 0,
                  px: 0.5,
                  '&:hover': { bgcolor: 'action.hover' },
                  ...(dragTargetIndex === index
                    ? {
                        bgcolor: 'action.selected',
                        outline: `2px solid ${theme.palette.primary.main}`,
                        outlineOffset: '-2px',
                      }
                    : {}),
                }}
              >
                <DragIndicatorIcon
                  fontSize="small"
                  color="disabled"
                  aria-hidden="true"
                />
                <Typography noWrap title={path} sx={{ flex: 1, minWidth: 0 }}>
                  {path}
                </Typography>
                <IconButton
                  aria-label={`Move ${path} up`}
                  onClick={() => movePathWithFocus(index, index - 1, 'up')}
                  disabled={pending || index === 0}
                  ref={(element) => {
                    const key = `up:${path}`;
                    if (element === null) {
                      moveButtonRefs.current.delete(key);
                    } else {
                      moveButtonRefs.current.set(key, element);
                    }
                  }}
                  size="small"
                >
                  <ArrowUpwardIcon fontSize="small" />
                </IconButton>
                <IconButton
                  aria-label={`Move ${path} down`}
                  onClick={() => movePathWithFocus(index, index + 1, 'down')}
                  disabled={pending || index === paths.length - 1}
                  ref={(element) => {
                    const key = `down:${path}`;
                    if (element === null) {
                      moveButtonRefs.current.delete(key);
                    } else {
                      moveButtonRefs.current.set(key, element);
                    }
                  }}
                  size="small"
                >
                  <ArrowDownwardIcon fontSize="small" />
                </IconButton>
                <IconButton
                  aria-label={`Remove ${path}`}
                  onClick={() => removePath(path)}
                  disabled={pending}
                  size="small"
                >
                  <DeleteIcon fontSize="small" />
                </IconButton>
              </Box>
            ))}
          </Stack>
        )}
      </Box>
    </Stack>
  );
}
