import { styled } from '@mui/material/styles';

export const EditorParent = styled('div')(({ theme }) => ({
  background: theme.palette.background.default,
  '&, .cm-editor': {
    height: '100%',
  },
  '.cm-scroller': {
    fontSize: 16,
    fontFamily: '"JetBrains MonoVariable", "JetBrains Mono", monospace',
    fontFeatureSettings: '"liga", "calt"',
    fontWeight: 400,
    letterSpacing: 0,
    textRendering: 'optimizeLegibility',
    color: theme.palette.text.secondary,
  },
  '.cm-gutters': {
    background: theme.palette.background.default,
    color: theme.palette.text.disabled,
    border: 'none',
  },
  '.cm-specialChar': {
    color: theme.palette.secondary.main,
  },
  '.cm-activeLine': {
    background: 'rgba(0, 0, 0, 0.3)',
  },
  '.cm-activeLineGutter': {
    background: 'rgba(0, 0, 0, 0.3)',
    color: theme.palette.text.primary,
  },
  '.cm-cursor, .cm-cursor-primary': {
    borderColor: theme.palette.primary.main,
    background: theme.palette.common.black,
  },
  '.cm-selectionBackground': {
    background: '#3e4453',
  },
  '.cm-focused': {
    outline: 'none',
    '.cm-selectionBackground': {
      background: '#3e4453',
    },
  },
  '.cm-panels-top': {
    color: theme.palette.text.secondary,
  },
  '.cm-panel': {
    background: theme.palette.background.paper,
    borderTop: `1px solid ${theme.palette.divider}`,
    'button[name="close"]': {
      color: theme.palette.text.secondary,
      cursor: 'pointer',
    },
  },
  '.cm-foldPlaceholder': {
    background: theme.palette.background.paper,
    borderColor: theme.palette.text.disabled,
    color: theme.palette.text.secondary,
  },
  '.cmt-comment': {
    fontVariant: 'italic',
    color: theme.palette.text.disabled,
  },
  '.cmt-number': {
    color: '#6188a6',
  },
  '.cmt-string': {
    color: theme.palette.secondary.dark,
  },
  '.cmt-keyword': {
    color: theme.palette.primary.main,
  },
  '.cmt-typeName, .cmt-atom': {
    color: theme.palette.text.primary,
  },
  '.cmt-variableName': {
    color: '#c8ae9d',
  },
}));
