import { styled } from '@mui/material/styles';

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

export const EditorParent = styled('div')(({ theme }) => {
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
    '.cm-scroller, .cm-tooltip-autocomplete, .cm-completionLabel, .cm-completionDetail': {
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
      background: 'rgba(255, 255, 255, 0.1)',
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
      background: 'transparent',
    },
    '.cm-lineNumbers .cm-activeLineGutter': {
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
      background: theme.palette.background.paper,
      borderColor: theme.palette.text.disabled,
      color: theme.palette.text.secondary,
    },
    '.cmt-comment': {
      fontStyle: 'italic',
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
    '.cmt-typeName, .cmt-macroName, .cmt-atom': {
      color: theme.palette.text.primary,
    },
    '.cmt-variableName': {
      color: '#c8ae9d',
    },
    '.cmt-problem-node': {
      '&, & .cmt-variableName': {
        color: theme.palette.text.secondary,
      },
    },
    '.cmt-problem-individual': {
      '&, & .cmt-variableName': {
        color: theme.palette.text.primary,
      },
    },
    '.cmt-problem-abstract, .cmt-problem-new': {
      fontStyle: 'italic',
    },
    '.cmt-problem-containment': {
      fontWeight: 700,
    },
    '.cmt-problem-error': {
      '&, & .cmt-typeName': {
        color: theme.palette.error.main,
      },
    },
    '.cmt-problem-builtin': {
      '&, & .cmt-typeName, & .cmt-atom, & .cmt-variableName': {
        color: theme.palette.primary.main,
        fontWeight: 400,
        fontStyle: 'normal',
      },
    },
    '.cm-tooltip-autocomplete': {
      background: theme.palette.background.paper,
      boxShadow: `0px 2px 4px -1px rgb(0 0 0 / 20%),
        0px 4px 5px 0px rgb(0 0 0 / 14%),
        0px 1px 10px 0px rgb(0 0 0 / 12%)`,
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
