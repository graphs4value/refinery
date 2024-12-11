/*
 * SPDX-FileCopyrightText: 2021-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import cancelSVG from '@material-icons/svg/svg/cancel/baseline.svg?raw';
import expandMoreSVG from '@material-icons/svg/svg/expand_more/baseline.svg?raw';
import infoSVG from '@material-icons/svg/svg/info/baseline.svg?raw';
import keySVG from '@material-icons/svg/svg/vpn_key/baseline.svg?raw';
import warningSVG from '@material-icons/svg/svg/warning/baseline.svg?raw';
import {
  alpha,
  styled,
  type CSSObject,
  type Theme,
} from '@mui/material/styles';
import { range } from 'lodash-es';

import svgURL from '../utils/svgURL';
import typeHashTextColor from '../utils/typeHashTextColor';

function createTypeHashStyles(
  theme: Theme,
  colorIdentifiers: boolean,
  hexTypeHashes: string[],
): CSSObject {
  if (!colorIdentifiers) {
    return {};
  }
  const result: CSSObject = {};
  range(theme.palette.highlight.typeHash.length).forEach((i) => {
    // We highlight both `.tok-typeName` and `.tok-variableName`, because type names
    // may occur also in `.tok-variableName` in annotation arguments.
    result[`.tok-problem-typeHash-${i}`] = {
      '&, .tok-typeName, .tok-variableName': {
        color: theme.palette.highlight.typeHash[i]?.text,
        fontWeight: theme.typography.fontWeightEditorTypeHash,
      },
    };
    result[`.cm-completionIcon-typehash-${i} + .cm-completionLabel`] = {
      color: `${theme.palette.highlight.typeHash[i]?.text} !important`,
      fontWeight: theme.typography.fontWeightEditorTypeHash,
    };
  });
  hexTypeHashes.forEach((typeHash) => {
    const color = typeHashTextColor(`#${typeHash}`, theme);
    result[`.tok-problem-typeHash-_${typeHash}`] = {
      '&, .tok-typeName, .tok-variableName': {
        color,
        fontWeight: theme.typography.fontWeightEditorTypeHash,
      },
    };
    result[`.cm-completionIcon-typehash-_${typeHash} + .cm-completionLabel`] = {
      color: `${color} !important`,
      fontWeight: theme.typography.fontWeightEditorTypeHash,
    };
  });
  return result;
}

export default styled('div', {
  name: 'EditorTheme',
  shouldForwardProp: (propName) =>
    propName !== 'showLineNumbers' &&
    propName !== 'showActiveLine' &&
    propName !== 'colorIdentifiers' &&
    propName !== 'hexTypeHashes',
})<{
  showLineNumbers: boolean;
  showActiveLine: boolean;
  colorIdentifiers: boolean;
  hexTypeHashes: string[];
}>(({
  theme,
  showLineNumbers,
  showActiveLine,
  colorIdentifiers,
  hexTypeHashes,
}) => {
  const editorFontStyle: CSSObject = {
    ...theme.typography.editor,
    fontWeight: theme.typography.fontWeightEditorNormal,
    [theme.breakpoints.down('sm')]: {
      // `rem` for JetBrains MonoVariable make the text too large in Safari.
      fontSize: '14px',
      lineHeight: 1.43,
    },
  };

  const scrollbarOpacity = theme.palette.mode === 'dark' ? 0.16 : 0.28;

  const generalStyle: CSSObject = {
    background: theme.palette.background.default,
    '&, .cm-editor': {
      overflow: 'none',
      height: '100%',
    },
    '.cm-scroller': {
      color: theme.palette.text.secondary,
    },
    '.cm-gutters': {
      background: theme.palette.background.default,
      border: 'none',
      marginRight: 1,
    },
    '.cm-content': {
      ...editorFontStyle,
    },
    '.cm-activeLine': {
      backgroundColor: showActiveLine
        ? theme.palette.highlight.activeLine
        : 'transparent',
    },
    '.cm-activeLineGutter': {
      background: 'transparent',
    },
    '.cm-indent-markers': {
      '--indent-marker-bg-color': theme.palette.text.disabled,
      '--indent-marker-active-bg-color':
        theme.palette.mode === 'dark'
          ? theme.palette.text.secondary
          : theme.palette.text.primary,
    },
    '.cm-indent-markers::before': {
      left: -4,
      zIndex: 0,
    },
    '.cm-cursor, .cm-dropCursor, .cm-cursor-primary': {
      borderLeft: `2px solid ${theme.palette.info.main}`,
      marginLeft: -1,
    },
    '.cm-selectionBackground': {
      background: theme.palette.highlight.selection,
    },
    '.cm-focused': {
      outline: 'none',
      '& > .cm-scroller > .cm-selectionLayer .cm-selectionBackground': {
        background: theme.palette.highlight.selection,
      },
    },
    '.cm-line': {
      padding: '0 12px 0 0',
    },
    '.cm-track': {
      // Appar above the directional splitter.
      zIndex: 1000,
    },
    '.cm-thumb': {
      background: theme.palette.text.secondary,
      opacity: scrollbarOpacity,
      mixBlendMode: theme.palette.mode === 'dark' ? 'screen' : 'multiply',
      '&.active, &.cm-thumb-active': {
        opacity: 0.72,
      },
    },
    '.cm-track:hover .cm-thumb': {
      opacity: 0.5,
      '@media (hover: none)': {
        opacity: scrollbarOpacity,
      },
      '&.active, &.cm-thumb-active': {
        opacity: 0.72,
      },
    },
    '.cm-editor:has(> .cm-panels-top) .cm-top-shadow': {
      display: 'none',
    },
  };

  const highlightingStyle: CSSObject = {
    '.cm-specialChar': {
      color: theme.palette.secondary.main,
    },
    '.tok-comment': {
      fontStyle: 'italic',
      color: theme.palette.highlight.comment,
    },
    '.tok-number': {
      color: theme.palette.highlight.number,
    },
    '.tok-string': {
      color: theme.palette.highlight.string,
    },
    '.tok-keyword': {
      color: theme.palette.primary.main,
    },
    '.tok-typeName, .tok-problem-relation .tok-variableName, .tok-atom': {
      color: theme.palette.text.primary,
    },
    '.tok-variableName': {
      color: theme.palette.highlight.parameter,
    },
    '.tok-meta': {
      color: theme.palette.text.secondary,
    },
    '.tok-problem-node': {
      '&, & .tok-variableName': {
        color: theme.palette.text.secondary,
      },
    },
    '.tok-problem-atom': {
      '&, & .tok-variableName': {
        color: theme.palette.text.primary,
      },
    },
    '.tok-problem-abstract': {
      fontStyle: 'italic',
    },
    '.tok-problem-datatype, .tok-problem-aggregator': {
      '&, & .tok-typeName': {
        color: theme.palette.primary.main,
      },
    },
    '.tok-problem-containment': {
      fontWeight: theme.typography.fontWeightEditorBold,
      textDecorationSkipInk: 'none',
    },
    '.tok-problem-error': {
      '&, & .tok-typeName': {
        color: theme.palette.highlight.comment,
        textDecoration: 'line-through',
      },
    },
    '.tok-invalid': {
      '&, & .tok-typeName': {
        color: theme.palette.error.main,
      },
    },
    '.tok-problem-builtin': {
      '&, & .tok-typeName, & .tok-atom, & .tok-variableName': {
        color: theme.palette.primary.main,
        fontWeight: theme.typography.fontWeightEditorNormal,
        fontStyle: 'normal',
      },
    },
    ...createTypeHashStyles(theme, colorIdentifiers, hexTypeHashes),
  };

  const matchingStyle: CSSObject = {
    '.cm-problem-read': {
      background: theme.palette.highlight.occurences.read,
    },
    '.cm-problem-write': {
      background: theme.palette.highlight.occurences.write,
    },
    '.cm-matchingBracket, .cm-nonmatchingBracket': {
      background: 'transparent',
    },
    '.cm-focused .cm-matchingBracket': {
      background: 'transparent',
      outline: `1px solid ${alpha(theme.palette.text.primary, 0.5)}`,
      outlineOffset: -1,
    },
    '.cm-focused .cm-nonmatchingBracket': {
      background: theme.palette.error.main,
      '&, span': {
        color: theme.palette.error.contrastText,
      },
    },
    '.cm-searchMatch': {
      opacity: 1,
      background: theme.palette.highlight.search.match,
      '&, span': {
        color: theme.palette.highlight.search.contrastText,
      },
    },
    '.cm-searchMatch-selected': {
      background: theme.palette.highlight.search.selected,
    },
    '.cm-track-annotation-selection': {
      left: 0,
      width: '100%',
      boxShadow: `0 2px 0 ${theme.palette.info.main} inset`,
      zIndex: 200,
    },
    '.cm-track-annotation-occurrence': {
      left: 0,
      width: '50%',
      background: theme.palette.highlight.comment,
      zIndex: 150,
    },
  };

  const lineNumberStyle: CSSObject = {
    '.cm-lineNumbers': {
      ...editorFontStyle,
      color: theme.palette.text.disabled,
      ...(!showLineNumbers && {
        display: 'none !important',
      }),
      '.cm-gutterElement': {
        padding: '0 2px 0 6px',
      },
      '.cm-activeLineGutter': {
        color: theme.palette.text.primary,
      },
    },
  };

  const panelStyle: CSSObject = {
    '.cm-panels-top': {
      color: theme.palette.text.primary,
      borderBottom: `1px solid ${theme.palette.outer.border}`,
    },
    '.cm-panels-bottom': {
      borderTop: 'none',
    },
    '.cm-panel': {
      color: theme.palette.text.primary,
      background: theme.palette.outer.background,
      borderTop: `1px solid ${theme.palette.outer.border}`,
      margin: 0,
      padding: 0,
      'button[name="close"]': {
        background: 'transparent',
        color: theme.palette.text.secondary,
        cursor: 'pointer',
      },
    },
  };

  function lintSeverityStyle(
    severity: 'error' | 'warning' | 'info',
    icon: string,
    zIndex: number,
  ): CSSObject {
    const palette = theme.palette[severity];
    const color = palette.main;
    const tooltipColor = theme.palette.mode === 'dark' ? color : palette.light;
    const iconStyle: CSSObject = {
      background: color,
      maskImage: svgURL(icon),
      maskSize: '16px 16px',
      height: 16,
      width: 16,
    };
    return {
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
      [`.cm-panel.cm-panel-lint .cm-diagnostic-${severity}::before`]: {
        top: 8,
        [theme.breakpoints.down('sm')]: {
          top: 6,
        },
      },
      [`.cm-lint-marker-${severity}`]: {
        ...iconStyle,
        display: 'block',
        margin: '4px 0',
        // Remove original CodeMirror icon.
        content: '""',
        [theme.breakpoints.down('sm')]: {
          margin: '2px 0',
        },
        '::before': {
          // Remove original CodeMirror icon.
          content: '""',
          display: 'none',
        },
      },
      [`.cm-track-annotation-diagnostic-${severity}`]: {
        background: color,
        zIndex,
      },
    };
  }

  const lintStyle: CSSObject = {
    '.cm-gutter-lint': {
      width: 16,
      '.cm-gutterElement': {
        padding: 0,
      },
    },
    '.cm-tooltip.cm-tooltip-hover, .cm-tooltip.cm-tooltip-lint': {
      ...((theme.components?.MuiTooltip?.styleOverrides?.tooltip as
        | CSSObject
        | undefined) ?? {}),
      ...theme.typography.body2,
      borderRadius: theme.shape.borderRadius,
      overflow: 'hidden',
      maxWidth: 400,
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
        maxHeight: `max(${32 * 4}px, 20vh)`,
        li: {
          cursor: 'pointer',
          color: theme.palette.text.primary,
        },
        '.cm-diagnostic': {
          ...theme.typography.body1,
          [theme.breakpoints.down('sm')]: {
            ...theme.typography.body2,
          },
          '&[aria-selected="true"]': {
            color: theme.palette.text.primary,
            background: 'transparent',
            fontWeight: theme.typography.fontWeightBold,
          },
          ':hover': {
            background: alpha(
              theme.palette.text.primary,
              theme.palette.action.hoverOpacity,
            ),
            '@media (hover: none)': {
              background: 'transparent',
            },
          },
        },
      },
    },
    '.cm-lintRange-active': {
      background: theme.palette.highlight.activeLintRange,
    },
    '.cm-track-annotation-diagnostic': {
      left: '50%',
      width: '50%',
    },
    ...lintSeverityStyle('error', cancelSVG, 120),
    ...lintSeverityStyle('warning', warningSVG, 110),
    ...lintSeverityStyle('info', infoSVG, 100),
  };

  const foldStyle = {
    '.cm-foldGutter': {
      width: 16,
    },
    '.problem-editor-foldMarker': {
      display: 'block',
      margin: '4px 0 4px 0',
      padding: 0,
      maskImage: svgURL(expandMoreSVG),
      maskSize: '16px 16px',
      height: 16,
      width: 16,
      background: theme.palette.text.primary,
      border: 'none',
      cursor: 'pointer',
      WebkitTapHighlightColor: 'transparent',
      [theme.breakpoints.down('sm')]: {
        margin: '2px 0 2px 0',
      },
    },
    '.problem-editor-foldMarker-open': {
      opacity: 0,
      transition: theme.transitions.create('opacity', {
        duration: theme.transitions.duration.short,
      }),
      '@media (hover: none)': {
        opacity: 1,
      },
    },
    '.cm-gutters:hover .problem-editor-foldMarker-open': {
      opacity: 1,
    },
    '.problem-editor-foldMarker-closed': {
      transform: 'rotate(-90deg)',
    },
    '.problem-editor-foldPlaceholder': {
      ...editorFontStyle,
      padding: 0,
      fontFamily: 'inherit',
      background: 'transparent',
      border: 'none',
      cursor: 'pointer',
      WebkitTapHighlightColor: 'transparent',
      // Use an inner `span` element to match the height of other text highlights.
      span: {
        color: theme.palette.text.secondary,
        backgroundColor: 'transparent',
        backgroundImage: `linear-gradient(${theme.palette.highlight.foldPlaceholder}, ${theme.palette.highlight.foldPlaceholder})`,
        transition: theme.transitions.create('background-color', {
          duration: theme.transitions.duration.short,
        }),
      },
      '&:hover span': {
        backgroundColor: alpha(
          theme.palette.highlight.foldPlaceholder,
          theme.palette.action.hoverOpacity,
        ),
        '@media (hover: none)': {
          backgroundColor: 'transparent',
        },
      },
    },
  };

  const completionStyle: CSSObject = {
    '.cm-tooltip.cm-tooltip-autocomplete': {
      ...editorFontStyle,
      // Appear above the scrollbar (and the splitter handle).
      zIndex: 2000,
      background: theme.palette.background.paper,
      border: 'none',
      borderRadius: theme.shape.borderRadius,
      ...(theme.palette.mode === 'dark' && {
        // https://github.com/mui/material-ui/blob/10c72729c7d03bab8cdce6eb422642684c56dca2/packages/mui-material/src/Paper/Paper.js#L18
        backgroundImage:
          'linear-gradient(rgba(255, 255, 255, 0.07), rgba(255, 255, 255, 0.07))',
      }),
      boxShadow: theme.shadows[2],
      '& > ul': {
        // We can't set `overflow: hidden;` on the container to clip the corners of the scroll bar,
        // because it would also hide the documentation tooltip.
        clipPath: `inset(0px round ${theme.shape.borderRadius}px)`,
      },
      '&::-webkit-scrollbar': {
        borderRadius: theme.shape.borderRadius,
      },
      '.cm-completionIcon': {
        color: theme.palette.text.secondary,
      },
      '.cm-completionLabel': {
        ...editorFontStyle,
        color: theme.palette.text.primary,
      },
      '.cm-completionDetail': {
        ...editorFontStyle,
        margin: 0,
        color: theme.palette.text.secondary,
        fontStyle: 'normal',
      },
      '.cm-completionIcon-keyword + .cm-completionLabel, .cm-completionIcon-builtin + .cm-completionLabel':
        {
          color: `${theme.palette.primary.main} !important`,
        },
      '.cm-completionIcon-abstract + .cm-completionLabel': {
        fontStyle: 'italic',
      },
      '.cm-completionIcon-containment + .cm-completionLabel': {
        fontWeight: theme.typography.fontWeightEditorBold,
      },
      '& > ul > li': {
        padding: `0 ${theme.spacing(0.5)}`,
      },
      '& > ul > li[aria-selected="true"]': {
        background: alpha(
          theme.palette.text.primary,
          theme.palette.action.focusOpacity,
        ),
        '.cm-completionIcon, .cm-completionLabel, .cm-completionDetail': {
          color: theme.palette.text.primary,
        },
      },
    },
    '.cm-completionIcon': {
      width: 16,
      padding: 0,
      margin: `0 ${theme.spacing(0.5)} 0 0`,
      textAlign: 'center',
    },
    '.cm-completionIcon-keyword::after, .cm-completionIcon-operator::after': {
      content: '" "',
      display: 'inline-block',
      background: 'currentColor',
      maskImage: svgURL(keySVG),
      maskSize: '16px 16px',
      height: 16,
      width: 16,
      verticalAlign: 'middle',
    },
    '.cm-tooltip.cm-completionInfo': {
      ...((theme.components?.MuiTooltip?.styleOverrides?.tooltip as
        | CSSObject
        | undefined) ?? {}),
      ...theme.typography.body2,
      // Appear above the scrollbar (and the splitter handle).
      zIndex: 2000,
      padding: `0 ${theme.spacing(1)}`,
      borderRadius: theme.shape.borderRadius,
      overflow: 'hidden',
      whiteSpace: 'normal',
      '.refinery-completion-documentation': {
        margin: 0,
        p: {
          margin: `${theme.spacing(1)} 0`,
        },
        'code, pre': {
          ...theme.typography.editor,
          fontWeight: theme.typography.fontWeightEditorNormal,
          fontSize: 'inherit',
        },
        pre: {
          padding: 0,
          margin: `${theme.spacing(1)} 0`,
        },
        code: {
          background: 'rgb(255, 255, 255, 0.1)',
          border: '1px solid rgba(255, 255, 255, 0.5)',
          borderRadius: theme.shape.borderRadius,
        },
        'pre code': {
          background: 'transparent',
          border: 'none',
          borderRadius: 0,
        },
      },
      '&.cm-completionInfo-right': {
        marginLeft: theme.spacing(1),
      },
      '&.cm-completionInfo-left': {
        marginRight: theme.spacing(1),
      },
      '&.cm-completionInfo-right-narrow': {
        marginLeft: theme.spacing(1),
        marginTop: theme.spacing(1),
      },
      '&.cm-completionInfo-left-narrow': {
        marginRight: theme.spacing(1),
        marginTop: theme.spacing(1),
      },
      h3: {
        ...theme.typography.body2,
        padding: 0,
        margin: `${theme.spacing(1)} 0`,
        color: theme.palette.text.secondary,
        fontStyle: 'italic',
      },
    },
    '.refinery-completion-parameters': {
      padding: 0,
      margin: `${theme.spacing(1)} 0`,
      display: 'grid',
      gridTemplateColumns: 'max-content 1fr',
      gap: theme.spacing(1),
    },
    '.refinery-completion-parameter-name': {
      display: 'block',
      padding: 0,
      margin: `-${theme.spacing(1)} 0`,
      fontWeight: theme.typography.fontWeightBold,
    },
    '.refinery-completion-parameter-invalid': {
      color:
        theme.palette.mode === 'dark'
          ? theme.palette.error.light
          : theme.palette.error.main,
    },
    '.refinery-completion-parameter-description': {
      display: 'block',
      padding: 0,
      margin: `-${theme.spacing(1)} 0`,
    },
  };

  return {
    ...generalStyle,
    ...highlightingStyle,
    ...matchingStyle,
    ...lineNumberStyle,
    ...panelStyle,
    ...lintStyle,
    ...foldStyle,
    ...completionStyle,
  };
});
