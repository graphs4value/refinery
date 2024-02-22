/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import createCache from '@emotion/cache';
import { serializeStyles } from '@emotion/serialize';
import type { StyleSheet } from '@emotion/utils';
import italicFontURL from '@fontsource/open-sans/files/open-sans-latin-400-italic.woff2?url';
import normalFontURL from '@fontsource/open-sans/files/open-sans-latin-400-normal.woff2?url';
import boldFontURL from '@fontsource/open-sans/files/open-sans-latin-700-normal.woff2?url';
import variableItalicFontURL from '@fontsource-variable/open-sans/files/open-sans-latin-wght-italic.woff2?url';
import variableFontURL from '@fontsource-variable/open-sans/files/open-sans-latin-wght-normal.woff2?url';
import cancelSVG from '@material-icons/svg/svg/cancel/baseline.svg?raw';
import labelSVG from '@material-icons/svg/svg/label/baseline.svg?raw';
import labelOutlinedSVG from '@material-icons/svg/svg/label/outline.svg?raw';
import SaveAltIcon from '@mui/icons-material/SaveAlt';
import IconButton from '@mui/material/IconButton';
import { styled, useTheme, type Theme } from '@mui/material/styles';
import { useCallback } from 'react';

import getLogger from '../utils/getLogger';

import { createGraphTheme } from './GraphTheme';
import { SVG_NS } from './postProcessSVG';

const log = getLogger('graph.ExportButton');

const PROLOG = '<?xml version="1.0" encoding="UTF-8" standalone="no"?>';

const ExportButtonRoot = styled('div', {
  name: 'ExportButton-Root',
})(({ theme }) => ({
  position: 'absolute',
  padding: theme.spacing(1),
  top: 0,
  right: 0,
  overflow: 'hidden',
  display: 'flex',
  flexDirection: 'column',
  alignItems: 'start',
}));

const ICONS: Map<string, Element> = new Map();

function importSVG(svgSource: string, className: string): void {
  const parser = new DOMParser();
  const svgDocument = parser.parseFromString(svgSource, 'image/svg+xml');
  const root = svgDocument.children[0];
  if (root === undefined) {
    return;
  }
  root.id = className;
  root.classList.add(className);
  ICONS.set(className, root);
}

importSVG(labelSVG, 'icon-TRUE');
importSVG(labelOutlinedSVG, 'icon-UNKNOWN');
importSVG(cancelSVG, 'icon-ERROR');

async function fetchAsFontURL(url: string): Promise<string> {
  const fetchResult = await fetch(url);
  const buffer = await fetchResult.arrayBuffer();
  const blob = new Blob([buffer], { type: 'font/woff2' });
  return new Promise((resolve, reject) => {
    const fileReader = new FileReader();
    fileReader.addEventListener('load', () => {
      resolve(fileReader.result as string);
    });
    fileReader.addEventListener('error', () => {
      reject(fileReader.error);
    });
    fileReader.readAsDataURL(blob);
  });
}

let fontCSS: string | undefined;
let variableFontCSS: string | undefined;

async function fetchFontCSS(): Promise<string> {
  if (fontCSS !== undefined) {
    return fontCSS;
  }
  const [normalDataURL, boldDataURL, italicDataURL] = await Promise.all([
    fetchAsFontURL(normalFontURL),
    fetchAsFontURL(boldFontURL),
    fetchAsFontURL(italicFontURL),
  ]);
  fontCSS = `
@font-face {
  font-family: 'Open Sans';
  font-style: normal;
  font-display: swap;
  font-weight: 400;
  src: url(${normalDataURL}) format('woff2');
}
@font-face {
  font-family: 'Open Sans';
  font-style: normal;
  font-display: swap;
  font-weight: 700;
  src: url(${boldDataURL}) format('woff2');
}
@font-face {
  font-family: 'Open Sans';
  font-style: italic;
  font-display: swap;
  font-weight: 400;
  src: url(${italicDataURL}) format('woff2');
}`;
  return fontCSS;
}

async function fetchVariableFontCSS(): Promise<string> {
  if (variableFontCSS !== undefined) {
    return variableFontCSS;
  }
  const [variableDataURL, variableItalicDataURL] = await Promise.all([
    fetchAsFontURL(variableFontURL),
    fetchAsFontURL(variableItalicFontURL),
  ]);
  variableFontCSS = `
@font-face {
  font-family: 'Open Sans Variable';
  font-style: normal;
  font-display: swap;
  font-weight: 300 800;
  src: url(${variableDataURL}) format('woff2-variations');
}
@font-face {
  font-family: 'Open Sans Variable';
  font-style: normal;
  font-display: swap;
  font-weight: 300 800;
  src: url(${variableItalicDataURL}) format('woff2-variations');
}`;
  return variableFontCSS;
}

async function appendStyles(
  svgDocument: XMLDocument,
  svg: SVGSVGElement,
  theme: Theme,
  embedFonts?: 'woff2' | 'woff2-variations',
): Promise<void> {
  const cache = createCache({
    key: 'refinery',
    container: svg,
    prepend: true,
  });
  // @ts-expect-error `CSSObject` types don't match up between `@mui/material` and
  // `@emotion/serialize`, but they are compatible in practice.
  const styles = serializeStyles([createGraphTheme], cache.registered, {
    theme,
    colorNodes: true,
    noEmbedIcons: true,
  });
  const rules: string[] = [];
  if (embedFonts === 'woff2') {
    rules.push(await fetchFontCSS());
  } else if (embedFonts === 'woff2-variations') {
    rules.push(await fetchVariableFontCSS());
  }
  const sheet = {
    insert(rule) {
      rules.push(rule);
    },
  } as StyleSheet;
  cache.insert('', styles, sheet, false);
  const styleElement = svgDocument.createElementNS(SVG_NS, 'style');
  svg.prepend(styleElement);
  styleElement.innerHTML = rules.join('');
}

function fixForeignObjects(svgDocument: XMLDocument, svg: SVGSVGElement): void {
  const foreignObjects: SVGForeignObjectElement[] = [];
  svg
    .querySelectorAll('foreignObject')
    .forEach((object) => foreignObjects.push(object));
  foreignObjects.forEach((object) => {
    const useElement = svgDocument.createElementNS(SVG_NS, 'use');
    let x = Number(object.getAttribute('x') ?? '0');
    let y = Number(object.getAttribute('y') ?? '0');
    const width = Number(object.getAttribute('width') ?? '0');
    const height = Number(object.getAttribute('height') ?? '0');
    const size = Math.min(width, height);
    x += (width - size) / 2;
    y += (height - size) / 2;
    useElement.setAttribute('x', String(x));
    useElement.setAttribute('y', String(y));
    useElement.setAttribute('width', String(size));
    useElement.setAttribute('height', String(size));
    useElement.id = object.id;
    object.children[0]?.classList?.forEach((className) => {
      useElement.classList.add(className);
      if (ICONS.has(className)) {
        useElement.setAttribute('href', `#${className}`);
      }
    });
    object.replaceWith(useElement);
  });
  const defs = svgDocument.createElementNS(SVG_NS, 'defs');
  svg.prepend(defs);
  ICONS.forEach((value) => {
    const importedValue = svgDocument.importNode(value, true);
    defs.appendChild(importedValue);
  });
}

function downloadSVG(svgDocument: XMLDocument): void {
  const serializer = new XMLSerializer();
  const svgText = `${PROLOG}\n${serializer.serializeToString(svgDocument)}`;
  const blob = new Blob([svgText], {
    type: 'image/svg+xml',
  });
  const link = document.createElement('a');
  link.href = window.URL.createObjectURL(blob);
  link.download = 'graph.svg';
  link.style.display = 'none';
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
}

async function exportSVG(
  svgContainer: HTMLElement | undefined,
  theme: Theme,
): Promise<void> {
  const svg = svgContainer?.querySelector('svg');
  if (!svg) {
    return;
  }
  const svgDocument = document.implementation.createDocument(
    SVG_NS,
    'svg',
    null,
  );
  const copyOfSVG = svgDocument.importNode(svg, true);
  const originalRoot = svgDocument.childNodes[0];
  if (originalRoot === undefined) {
    svgDocument.appendChild(copyOfSVG);
  } else {
    svgDocument.replaceChild(copyOfSVG, originalRoot);
  }
  fixForeignObjects(svgDocument, copyOfSVG);
  await appendStyles(svgDocument, copyOfSVG, theme);
  downloadSVG(svgDocument);
}

export default function ExportButton({
  svgContainer,
}: {
  svgContainer: HTMLElement | undefined;
}): JSX.Element {
  const theme = useTheme();
  const saveCallback = useCallback(() => {
    exportSVG(svgContainer, theme).catch((error) => {
      log.error('Failed to export SVG', error);
    });
  }, [svgContainer, theme]);

  return (
    <ExportButtonRoot>
      <IconButton aria-label="Save SVG" onClick={saveCallback}>
        <SaveAltIcon />
      </IconButton>
    </ExportButtonRoot>
  );
}
