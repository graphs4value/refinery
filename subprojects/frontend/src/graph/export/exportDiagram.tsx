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
import type { Theme } from '@mui/material/styles';

import { darkTheme, lightTheme } from '../../theme/ThemeProvider';
import { copyBlob, saveBlob } from '../../utils/fileIO';
import type GraphStore from '../GraphStore';
import { createGraphTheme } from '../GraphTheme';
import { SVG_NS } from '../postProcessSVG';

import type ExportSettingsStore from './ExportSettingsStore';

const PROLOG = '<?xml version="1.0" encoding="UTF-8" standalone="no"?>';
const PNG_CONTENT_TYPE = 'image/png';
const SVG_CONTENT_TYPE = 'image/svg+xml';
const EXPORT_ID = 'export-image';

const ICONS: Map<string, Element> = new Map();

function importSVG(svgSource: string, className: string): void {
  const parser = new DOMParser();
  const svgDocument = parser.parseFromString(svgSource, SVG_CONTENT_TYPE);
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

function appendStyles(
  svgDocument: XMLDocument,
  svg: SVGSVGElement,
  theme: Theme,
  colorNodes: boolean,
  fontsCSS: string,
): void {
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
    noEmbedIcons: true,
  });
  const rules: string[] = [fontsCSS];
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

  const theme = settings.theme === 'light' ? lightTheme : darkTheme;
  if (!settings.transparent) {
    addBackground(svgDocument, copyOfSVG, theme);
  }

  fixForeignObjects(svgDocument, copyOfSVG);

  const { colorNodes } = graph;
  let fontsCSS = '';
  if (settings.format === 'png') {
    // If we are creating a PNG, font file size doesn't matter,
    // and we can reuse fonts the browser has already downloaded.
    fontsCSS = await fetchVariableFontCSS();
  } else if (settings.format === 'svg' && settings.embedFonts) {
    fontsCSS = await fetchFontCSS();
  }
  appendStyles(svgDocument, copyOfSVG, theme, colorNodes, fontsCSS);

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
