/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import createCache from '@emotion/cache';
import { serializeStyles } from '@emotion/serialize';
import type { StyleSheet } from '@emotion/utils';
import cancelSVG from '@material-icons/svg/svg/cancel/baseline.svg?raw';
import labelSVG from '@material-icons/svg/svg/label/baseline.svg?raw';
import labelOutlinedSVG from '@material-icons/svg/svg/label/outline.svg?raw';
import SaveAltIcon from '@mui/icons-material/SaveAlt';
import IconButton from '@mui/material/IconButton';
import { styled, useTheme, type Theme } from '@mui/material/styles';
import { useCallback } from 'react';

import { createGraphTheme } from './GraphTheme';
import { SVG_NS } from './postProcessSVG';

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

function appendStyles(
  svgDocument: XMLDocument,
  svg: SVGSVGElement,
  theme: Theme,
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
    colorNodes: true,
    noEmbedIcons: true,
  });
  const rules: string[] = [];
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

function downloadSVG(svgDocument: XMLDocument) {
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

export default function ExportButton({
  svgContainer,
}: {
  svgContainer: HTMLElement | undefined;
}): JSX.Element {
  const theme = useTheme();
  const saveCallback = useCallback(() => {
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
    appendStyles(svgDocument, copyOfSVG, theme);
    downloadSVG(svgDocument);
  }, [theme, svgContainer]);

  return (
    <ExportButtonRoot>
      <IconButton aria-label="Save SVG" onClick={saveCallback}>
        <SaveAltIcon />
      </IconButton>
    </ExportButtonRoot>
  );
}
