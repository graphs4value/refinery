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
import type { Theme } from '@mui/material/styles';
import { nanoid } from 'nanoid';

import { darkTheme, lightTheme } from '../../theme/ThemeProvider';
import { copyBlob, saveBlob } from '../../utils/fileIO';
import type GraphStore from '../GraphStore';
import { createGraphTheme } from '../GraphTheme';
import icons from '../icons';
import { SVG_NS } from '../postProcessSVG';

import type ExportSettingsStore from './ExportSettingsStore';

const PROLOG = '<?xml version="1.0" encoding="UTF-8" standalone="no"?>';
const PNG_CONTENT_TYPE = 'image/png';
const SVG_CONTENT_TYPE = 'image/svg+xml';
const EXPORT_ID = 'export-image';

function fixIDs(id: string, svgDocument: XMLDocument) {
  const idMap = new Map<string, string>();
  let i = 0;
  svgDocument.querySelectorAll('[id]').forEach((node) => {
    const oldId = node.getAttribute('id');
    if (oldId === null) {
      return;
    }
    if (oldId.endsWith(',clip')) {
      const newId = `refinery-${id}-clip-${i}`;
      i += 1;
      idMap.set(`url(#${oldId})`, `url(#${newId})`);
      node.setAttribute('id', newId);
    } else {
      node.removeAttribute('id');
    }
  });
  svgDocument.querySelectorAll('[clip-path]').forEach((node) => {
    const oldPath = node.getAttribute('clip-path');
    if (oldPath === null) {
      return;
    }
    const newPath = idMap.get(oldPath);
    if (newPath === undefined) {
      return;
    }
    node.setAttribute('clip-path', newPath);
  });
}

function addBackground(
  svgDocument: XMLDocument,
  svg: SVGSVGElement,
  theme: Theme,
): void {
  const viewBox = svg.getAttribute('viewBox')?.split(' ');
  const rect = svgDocument.createElementNS(SVG_NS, 'rect');
  rect.setAttribute('x', viewBox?.[0] ?? '0');
  rect.setAttribute('y', viewBox?.[1] ?? '0');
  rect.setAttribute('width', viewBox?.[2] ?? '0');
  rect.setAttribute('height', viewBox?.[3] ?? '0');
  rect.setAttribute('fill', theme.palette.background.default);
  svg.prepend(rect);
}

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
  font-style: italic;
  font-display: swap;
  font-weight: 300 800;
  src: url(${variableItalicDataURL}) format('woff2-variations');
}`;
  return variableFontCSS;
}

interface ThemeVariant {
  selector: string;
  theme: Theme;
}

function appendStyles(
  id: string,
  svgDocument: XMLDocument,
  svg: SVGSVGElement,
  themes: ThemeVariant[],
  colorNodes: boolean,
  hexTypeHashes: string[],
  fontsCSS: string,
): void {
  const className = `refinery-${id}`;
  svg.classList.add(className);
  const rules: string[] = [fontsCSS];
  themes.forEach(({ selector, theme }) => {
    const cache = createCache({
      key: 'refinery',
      container: svg,
      prepend: true,
    });
    // @ts-expect-error `CSSObject` types don't match up between `@mui/material` and
    // `@emotion/serialize`, but they are compatible in practice.
    const styles = serializeStyles([createGraphTheme], cache.registered, {
      theme,
      colorNodes,
      hexTypeHashes,
      useOpacity: true,
    });
    const sheet = {
      insert(rule) {
        rules.push(rule);
      },
    } as StyleSheet;
    cache.insert(`${selector} .${className}`, styles, sheet, false);
  });
  const styleElement = svgDocument.createElementNS(SVG_NS, 'style');
  svg.prepend(styleElement);
  styleElement.innerHTML = rules.join('');
}

function fixIcons(
  id: string,
  svgDocument: XMLDocument,
  svg: SVGSVGElement,
): void {
  const prefix = `refinery-${id}-`;
  const hrefPrefix = `#${prefix}`;
  svg.querySelectorAll('use').forEach((use) => {
    const href = use.getAttribute('href');
    if (href === null) {
      return;
    }
    use.setAttribute('href', href.replace(/^#refinery-/, hrefPrefix));
  });
  const defs = svgDocument.createElementNS(SVG_NS, 'defs');
  svg.prepend(defs);
  icons.forEach((value) => {
    const importedValue = svgDocument.importNode(value, true);
    importedValue.id = `${prefix}${importedValue.id}`;
    defs.appendChild(importedValue);
  });
}

function serializeSVG(svgDocument: XMLDocument): Blob {
  const serializer = new XMLSerializer();
  const svgText = `${PROLOG}\n${serializer.serializeToString(svgDocument)}`;
  return new Blob([svgText], {
    type: SVG_CONTENT_TYPE,
  });
}

async function serializePNG(
  serializedSVG: Blob,
  svg: SVGSVGElement,
  settings: ExportSettingsStore,
  theme: Theme,
): Promise<Blob> {
  const scale = settings.scale / 100;
  const baseWidth = svg.width.baseVal.value;
  const baseHeight = svg.height.baseVal.value;
  const exactWidth = baseWidth * scale;
  const exactHeight = baseHeight * scale;
  const width = Math.round(exactWidth);
  const height = Math.round(exactHeight);

  const canvas = document.createElement('canvas');
  canvas.width = width;
  canvas.height = height;

  const image = document.createElement('img');
  const url = window.URL.createObjectURL(serializedSVG);
  try {
    await new Promise((resolve, reject) => {
      image.addEventListener('load', () => resolve(undefined));
      image.addEventListener('error', ({ error }) =>
        reject(
          error instanceof Error
            ? error
            : new Error(`Failed to load image: ${error}`),
        ),
      );
      image.src = url;
    });
  } finally {
    window.URL.revokeObjectURL(url);
  }

  const context = canvas.getContext('2d');
  if (context === null) {
    throw new Error('Failed to get canvas 2D context');
  }
  if (!settings.transparent) {
    context.fillStyle = theme.palette.background.default;
    context.fillRect(0, 0, width, height);
  }
  context.drawImage(
    image,
    0,
    0,
    baseWidth,
    baseHeight,
    0,
    0,
    exactWidth,
    exactHeight,
  );

  return new Promise<Blob>((resolve, reject) => {
    canvas.toBlob((exportedBlob) => {
      if (exportedBlob === null) {
        reject(new Error('Failed to export PNG blob'));
      } else {
        resolve(exportedBlob);
      }
    }, PNG_CONTENT_TYPE);
  });
}

let serializePDFCached:
  | ((svg: SVGSVGElement, embedFonts: boolean) => Promise<Blob>)
  | undefined;

async function serializePDF(
  svg: SVGSVGElement,
  settings: ExportSettingsStore,
): Promise<Blob> {
  if (serializePDFCached === undefined) {
    serializePDFCached = (await import('./serializePDF')).default;
  }
  return serializePDFCached(svg, settings.embedFonts);
}

export default async function exportDiagram(
  svgContainer: HTMLElement | undefined,
  graph: GraphStore,
  settings: ExportSettingsStore,
  mode: 'download' | 'copy',
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

  const id = nanoid();
  fixIDs(id, svgDocument);

  let theme: Theme;
  let themes: ThemeVariant[];
  if (settings.theme === 'dynamic') {
    theme = lightTheme;
    themes = [
      {
        selector: '',
        theme: lightTheme,
      },
      {
        selector: '[data-theme="dark"]',
        theme: darkTheme,
      },
    ];
  } else {
    theme = settings.theme === 'light' ? lightTheme : darkTheme;
    themes = [
      {
        selector: '',
        theme,
      },
    ];
  }
  if (!settings.transparent) {
    addBackground(svgDocument, copyOfSVG, theme);
  }

  fixIcons(id, svgDocument, copyOfSVG);

  const { colorNodes } = graph;
  let fontsCSS = '';
  if (settings.format === 'png') {
    // If we are creating a PNG, font file size doesn't matter,
    // and we can reuse fonts the browser has already downloaded.
    fontsCSS = await fetchVariableFontCSS();
  } else if (settings.format === 'svg' && settings.embedFonts) {
    fontsCSS = await fetchFontCSS();
  }
  appendStyles(
    id,
    svgDocument,
    copyOfSVG,
    themes,
    colorNodes,
    graph.hexTypeHashes,
    fontsCSS,
  );

  if (settings.format === 'pdf') {
    const pdf = await serializePDF(copyOfSVG, settings);
    await saveBlob(pdf, `${graph.name}.pdf`, {
      id: EXPORT_ID,
      types: [
        {
          description: 'PDF files',
          accept: {
            'application/pdf': ['.pdf', '.PDF'],
          },
        },
      ],
    });
    return;
  }
  const serializedSVG = serializeSVG(svgDocument);
  if (settings.format === 'png') {
    const png = await serializePNG(serializedSVG, svg, settings, theme);
    if (mode === 'copy') {
      await copyBlob(png);
    } else {
      await saveBlob(png, `${graph.name}.png`, {
        id: EXPORT_ID,
        types: [
          {
            description: 'PNG graphics',
            accept: {
              [PNG_CONTENT_TYPE]: ['.png', '.PNG'],
            },
          },
        ],
      });
    }
  } else if (mode === 'copy') {
    await copyBlob(serializedSVG);
  } else {
    await saveBlob(serializedSVG, `${graph.name}.svg`, {
      id: EXPORT_ID,
      types: [
        {
          description: 'SVG graphics',
          accept: {
            [SVG_CONTENT_TYPE]: ['.svg', '.SVG'],
          },
        },
      ],
    });
  }
}
