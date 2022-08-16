import errorSVG from '@material-icons/svg/svg/error/baseline.svg?raw';
import expandMoreSVG from '@material-icons/svg/svg/expand_more/baseline.svg?raw';
import infoSVG from '@material-icons/svg/svg/info/baseline.svg?raw';
import warningSVG from '@material-icons/svg/svg/warning/baseline.svg?raw';
import { alpha, styled } from '@mui/material/styles';

import editorClassNames from './editorClassNames';

function svgURL(svg: string): string {
  return `url('data:image/svg+xml;utf8,${svg}')`;
}

export default styled('div', {
  name: 'EditorTheme',
  shouldForwardProp: (propName) => propName !== 'showLineNumbers',
})<{ showLineNumbers: boolean }>(({ theme, showLineNumbers }) => {
  let codeMirrorLintStyle: Record<string, unknown> = {};
  (
    [
      {
        severity: 'error',
        icon: errorSVG,
      },
      {
        severity: 'warning',
        icon: warningSVG,
      },
      {
        severity: 'info',
        icon: infoSVG,
      },
    ] as const
  ).forEach(({ severity, icon }) => {
    const palette = theme.palette[severity];
    const color = palette.main;
    const iconStyle = {
      background: color,
      maskImage: svgURL(icon),
      maskSize: '16px 16px',
      height: 16,
      width: 16,
    };
    const tooltipColor =
      theme.palette.mode === 'dark' ? palette.main : palette.light;
    codeMirrorLintStyle = {
      ...codeMirrorLintStyle,
      [`.cm-lintRange-${severity}`]: {
        backgroundImage: 'none',
        textDecoration: `underline wavy ${color}`,
        textDecorationSkipInk: 'none',
      },
      [`.cm-diagnostic-${severity}`]: {
        marginLeft: 0,
        padding: '4px 8px 4px 32px',
        borderLeft: 'none',
        position: 'relative',
        '::before': {
          ...iconStyle,
          content: '" "',
          position: 'absolute',
          top: 6,
          left: 8,
        },
      },
      [`.cm-tooltip .cm-diagnostic-${severity}::before`]: {
        background: tooltipColor,
      },
      [`.cm-lint-marker-${severity}`]: {
        ...iconStyle,
        display: 'block',
        margin: '4px 0',
        // Remove original CodeMirror icon.
        content: '""',
        '::before': {
          // Remove original CodeMirror icon.
          content: '""',
          display: 'none',
        },
      },
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
    '.cm-scroller': {
      color: theme.palette.text.secondary,
    },
    '.cm-scroller, .cm-tooltip-autocomplete, .cm-completionLabel, .cm-completionDetail':
      {
        ...theme.typography.body1,
        fontFamily: '"JetBrains MonoVariable", "JetBrains Mono", monospace',
        fontFeatureSettings: '"liga", "calt"',
        letterSpacing: 0,
        textRendering: 'optimizeLegibility',
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
    '.cm-gutter-lint': {
      width: 16,
      '.cm-gutterElement': {
        padding: 0,
      },
    },
    '.cm-foldGutter': {
      opacity: 0,
      width: 16,
      transition: theme.transitions.create('opacity', {
        duration: theme.transitions.duration.short,
      }),
      '@media (hover: none)': {
        opacity: 1,
      },
    },
    '.cm-gutters:hover .cm-foldGutter': {
      opacity: 1,
    },
    [`.${editorClassNames.foldMarker}`]: {
      display: 'block',
      margin: '4px 0',
      padding: 0,
      maskImage: svgURL(expandMoreSVG),
      maskSize: '16px 16px',
      height: 16,
      width: 16,
      background: theme.palette.text.primary,
      border: 'none',
      cursor: 'pointer',
    },
    [`.${editorClassNames.foldMarkerClosed}`]: {
      transform: 'rotate(-90deg)',
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
      background: theme.palette.highlight.selection,
    },
    '.cm-focused': {
      outline: 'none',
      '.cm-selectionBackground': {
        background: theme.palette.highlight.selection,
      },
    },
    '.cm-panels-top': {
      color: theme.palette.text.secondary,
      borderBottom: `1px solid ${theme.palette.outer.border}`,
      marginBottom: theme.spacing(1),
    },
    '.cm-panel': {
      position: 'relative',
      overflow: 'hidden',
      background: theme.palette.outer.background,
      borderTop: `1px solid ${theme.palette.outer.border}`,
      '&, & button, & input': {
        fontFamily: theme.typography.fontFamily,
      },
      'button[name="close"]': {
        background: 'transparent',
        color: theme.palette.text.secondary,
        cursor: 'pointer',
      },
    },
    '.cm-panel.cm-panel-lint': {
      borderTop: `1px solid ${theme.palette.outer.border}`,
      borderBottom: 'none',
      'button[name="close"]': {
        // Close button interferes with scrollbar, so we better hide it.
        // The panel can still be closed from the toolbar.
        display: 'none',
      },
      ul: {
        maxHeight: 'max(112px, 20vh)',
        li: {
          cursor: 'pointer',
          color: theme.palette.text.primary,
        },
        '.cm-diagnostic': {
          ...theme.typography.body2,
          '&[aria-selected="true"]': {
            color: theme.palette.text.primary,
            background: 'transparent',
            fontWeight: 700,
          },
          ':hover': {
            background: alpha(
              theme.palette.text.primary,
              theme.palette.action.hoverOpacity,
            ),
          },
        },
      },
    },
    [`.${editorClassNames.foldPlaceholder}`]: {
      ...theme.typography.body1,
      padding: 0,
      fontFamily: 'inherit',
      fontFeatureSettings: '"liga", "calt"',
      color: theme.palette.text.secondary,
      backgroundColor: alpha(
        theme.palette.text.secondary,
        theme.palette.action.focusOpacity,
      ),
      border: 'none',
      cursor: 'pointer',
      transition: theme.transitions.create(['background-color', 'color'], {
        duration: theme.transitions.duration.short,
      }),
      '&:hover': {
        color: theme.palette.text.primary,
        backgroundColor: alpha(
          theme.palette.text.secondary,
          theme.palette.action.focusOpacity + theme.palette.action.hoverOpacity,
        ),
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
      color: theme.palette.secondary,
    },
    '.tok-keyword': {
      color: theme.palette.primary.main,
    },
    '.tok-typeName, .tok-atom': {
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
    '.cm-tooltip.cm-tooltip-autocomplete': {
      background: theme.palette.background.paper,
      borderRadius: theme.shape.borderRadius,
      overflow: 'hidden',
      ...(theme.palette.mode === 'dark' && {
        // https://github.com/mui/material-ui/blob/10c72729c7d03bab8cdce6eb422642684c56dca2/packages/mui-material/src/Paper/Paper.js#L18
        backgroundImage:
          'linear-gradient(rgba(255, 255, 255, 0.07), rgba(255, 255, 255, 0.07))',
      }),
      boxShadow: theme.shadows[2],
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
      'li[aria-selected="true"]': {
        background: alpha(
          theme.palette.text.primary,
          theme.palette.action.focusOpacity,
        ),
        '.cm-completionIcon, .cm-completionLabel, .cm-completionDetail': {
          color: theme.palette.text.primary,
        },
      },
    },
    '.cm-tooltip.cm-tooltip-hover, .cm-tooltip.cm-tooltip-lint': {
      ...theme.typography.body2,
      // https://github.com/mui/material-ui/blob/dee9529f7a298c54ae760761112c3ae9ba082137/packages/mui-material/src/Tooltip/Tooltip.js#L121-L125
      background: alpha(theme.palette.grey[700], 0.92),
      borderRadius: theme.shape.borderRadius,
      color: theme.palette.common.white,
      overflow: 'hidden',
      maxWidth: 400,
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
