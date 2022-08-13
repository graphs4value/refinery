import { alpha, styled } from '@mui/material/styles';

export default styled('div', {
  name: 'EditorParent',
  shouldForwardProp: (propName) => propName !== 'showLineNumbers',
})<{ showLineNumbers: boolean }>(({ theme, showLineNumbers }) => {
  const codeMirrorLintStyle: Record<string, unknown> = {};
  (['error', 'warning', 'info'] as const).forEach((severity) => {
    const color = theme.palette[severity].main;
    codeMirrorLintStyle[`.cm-diagnostic-${severity}`] = {
      borderLeftColor: color,
    };
    codeMirrorLintStyle[`.cm-lintRange-${severity}`] = {
      backgroundImage: 'none',
      textDecoration: `underline wavy ${color}`,
      textDecorationSkipInk: 'none',
    };
  });

  return {
    background: theme.palette.background.default,
    '&, .cm-editor': {
      height: '100%',
    },
    '.cm-content': {
      padding: 0,
    },
    '.cm-scroller, .cm-tooltip-autocomplete, .cm-completionLabel, .cm-completionDetail':
      {
        ...theme.typography.body1,
        fontFamily: '"JetBrains MonoVariable", "JetBrains Mono", monospace',
        fontFeatureSettings: '"liga", "calt"',
        letterSpacing: 0,
        textRendering: 'optimizeLegibility',
      },
    '.cm-scroller': {
      color: theme.palette.text.secondary,
    },
    '.cm-gutters': {
      background: 'transparent',
      color: theme.palette.text.disabled,
      border: 'none',
    },
    '.cm-specialChar': {
      color: theme.palette.secondary.main,
    },
    '.cm-activeLine': {
      background: theme.palette.highlight.activeLine,
    },
    '.cm-foldGutter': {
      color: alpha(theme.palette.text.primary, 0),
      transition: theme.transitions.create('color', {
        duration: theme.transitions.duration.short,
      }),
      '@media (hover: none)': {
        color: theme.palette.text.primary,
      },
    },
    '.cm-gutters:hover .cm-foldGutter': {
      color: theme.palette.text.primary,
    },
    '.cm-activeLineGutter': {
      background: 'transparent',
    },
    '.cm-lineNumbers': {
      ...(!showLineNumbers && {
        display: 'none !important',
      }),
      '.cm-activeLineGutter': {
        color: theme.palette.text.primary,
      },
    },
    '.cm-cursor, .cm-cursor-primary': {
      borderLeft: `2px solid ${theme.palette.primary.main}`,
    },
    '.cm-selectionBackground': {
      background: theme.palette.selection.main,
    },
    '.cm-focused': {
      outline: 'none',
      '.cm-selectionBackground': {
        background: theme.palette.selection.main,
      },
    },
    '.cm-panels-top': {
      color: theme.palette.text.secondary,
    },
    '.cm-panel': {
      '&, & button, & input': {
        fontFamily: theme.typography.fontFamily,
      },
      background: theme.palette.background.default,
      borderTop: `1px solid ${theme.palette.divider2}`,
      'button[name="close"]': {
        background: 'transparent',
        color: theme.palette.text.secondary,
        cursor: 'pointer',
      },
    },
    '.cm-panel.cm-panel-lint': {
      boderBottom: 'none',
      'button[name="close"]': {
        // Close button interferes with scrollbar, so we better hide it.
        // The panel can still be closed from the toolbar.
        display: 'none',
      },
      ul: {
        li: {
          cursor: 'pointer',
          color: theme.palette.text.primary,
        },
        '[aria-selected], &:focus [aria-selected]': {
          background: theme.palette.selection.main,
          color: theme.palette.selection.contrastText,
        },
      },
    },
    '.cm-foldPlaceholder': {
      color: theme.palette.text.secondary,
      backgroundColor: alpha(theme.palette.text.secondary, 0),
      border: `1px solid ${alpha(theme.palette.text.secondary, 0.5)}`,
      borderRadius: theme.shape.borderRadius,
      transition: theme.transitions.create(
        ['background-color', 'border-color', 'color'],
        {
          duration: theme.transitions.duration.short,
        },
      ),
      '&:hover': {
        backgroundColor: alpha(
          theme.palette.text.secondary,
          theme.palette.action.hoverOpacity,
        ),
        borderColor: theme.palette.text.secondary,
        '@media (hover: none)': {
          backgroundColor: 'transparent',
        },
      },
    },
    '.tok-comment': {
      fontStyle: 'italic',
      color: theme.palette.highlight.comment,
    },
    '.tok-number': {
      color: theme.palette.highlight.number,
    },
    '.tok-string': {
      color: theme.palette.secondary.dark,
    },
    '.tok-keyword': {
      color: theme.palette.primary.main,
    },
    '.tok-typeName, .tok-macroName, .tok-atom': {
      color: theme.palette.text.primary,
    },
    '.tok-variableName': {
      color: theme.palette.highlight.parameter,
    },
    '.tok-problem-node': {
      '&, & .tok-variableName': {
        color: theme.palette.text.secondary,
      },
    },
    '.tok-problem-individual': {
      '&, & .tok-variableName': {
        color: theme.palette.text.primary,
      },
    },
    '.tok-problem-abstract, .tok-problem-new': {
      fontStyle: 'italic',
    },
    '.tok-problem-containment': {
      fontWeight: 700,
    },
    '.tok-problem-error': {
      '&, & .tok-typeName': {
        color: theme.palette.error.main,
      },
    },
    '.tok-problem-builtin': {
      '&, & .tok-typeName, & .tok-atom, & .tok-variableName': {
        color: theme.palette.primary.main,
        fontWeight: 400,
        fontStyle: 'normal',
      },
    },
    '.cm-tooltip-autocomplete': {
      background: theme.palette.background.paper,
      ...(theme.palette.mode === 'dark' && {
        overflow: 'hidden',
        borderRadius: theme.shape.borderRadius,
        // https://github.com/mui/material-ui/blob/10c72729c7d03bab8cdce6eb422642684c56dca2/packages/mui-material/src/Paper/Paper.js#L18
        backgroundImage:
          'linear-gradient(rgba(255, 255, 255, 0.09), rgba(255, 255, 255, 0.09))',
      }),
      boxShadow: theme.shadows[4],
      '.cm-completionIcon': {
        color: theme.palette.text.secondary,
      },
      '.cm-completionLabel': {
        color: theme.palette.text.primary,
      },
      '.cm-completionDetail': {
        color: theme.palette.text.secondary,
        fontStyle: 'normal',
      },
      '[aria-selected]': {
        background: `${theme.palette.primary.main} !important`,
        '.cm-completionIcon, .cm-completionLabel, .cm-completionDetail': {
          color: theme.palette.primary.contrastText,
        },
      },
    },
    '.cm-completionIcon': {
      width: 16,
      padding: 0,
      marginRight: '0.5em',
      textAlign: 'center',
    },
    ...codeMirrorLintStyle,
    '.cm-problem-read': {
      background: theme.palette.highlight.occurences.read,
    },
    '.cm-problem-write': {
      background: theme.palette.highlight.occurences.write,
    },
  };
});
