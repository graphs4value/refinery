import { alpha, styled } from '@mui/material/styles';

/**
 * Returns a squiggly underline background image encoded as a CSS `url()` data URI with Base64.
 *
 * Based on
 * https://github.com/codemirror/lint/blob/f524b4a53b0183bb343ac1e32b228d28030d17af/src/lint.ts#L501
 *
 * @param color the color of the underline
 * @returns the CSS `url()`
 */
function underline(color: string) {
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="6" height="3">
    <path d="m0 3 l2 -2 l1 0 l2 2 l1 0" stroke="${color}" fill="none" stroke-width=".7"/>
  </svg>`;
  const svgBase64 = window.btoa(svg);
  return `url('data:image/svg+xml;base64,${svgBase64}')`;
}

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
      backgroundImage: underline(color),
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
        fontSize: 16,
        fontFamily: '"JetBrains MonoVariable", "JetBrains Mono", monospace',
        fontFeatureSettings: '"liga", "calt"',
        fontWeight: 400,
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
      background: alpha(theme.palette.text.secondary, 0.06),
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
      '&, & button, & input': {
        fontFamily: '"Roboto","Helvetica","Arial",sans-serif',
      },
      background: theme.palette.background.paper,
      borderTop: `1px solid ${theme.palette.divider}`,
      'button[name="close"]': {
        background: 'transparent',
        color: theme.palette.text.secondary,
        cursor: 'pointer',
      },
    },
    '.cm-panel.cm-panel-lint': {
      'button[name="close"]': {
        // Close button interferes with scrollbar, so we better hide it.
        // The panel can still be closed from the toolbar.
        display: 'none',
      },
      ul: {
        li: {
          borderBottom: `1px solid ${theme.palette.divider}`,
          cursor: 'pointer',
        },
        '[aria-selected]': {
          background: '#3e4453',
          color: theme.palette.text.primary,
        },
        '&:focus [aria-selected]': {
          background: theme.palette.primary.main,
          color: theme.palette.primary.contrastText,
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
      color: theme.palette.text.disabled,
    },
    '.tok-number': {
      color: '#6188a6',
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
      color: '#c8ae9d',
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
    '.cm-problem-write': {
      background: 'rgba(255, 255, 128, 0.3)',
    },
    '.cm-problem-read': {
      background: 'rgba(255, 255, 255, 0.15)',
    },
  };
});
