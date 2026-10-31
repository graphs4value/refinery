/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { JsonOutput } from '@tools.refinery/client';

import GraphStore from '../graph/GraphStore';
import ExportSettingsStore from '../graph/export/ExportSettingsStore';

import { ExportRequest } from './ExportRequest';
import type HeadlessStore from './HeadlessStore';

function createGraph(id: string, json: JsonOutput): GraphStore {
  const graph = new GraphStore(undefined, id);
  graph.setSemantics(json);
  return graph;
}

function createExportSettings(request: ExportRequest): ExportSettingsStore {
  const settings = new ExportSettingsStore();
  settings.setFormat(request.outputFormat);
  settings.setTheme(request.theme === 'auto' ? 'dynamic' : request.theme);
  settings.setTransparent(request.transparent);
  if (request.outputFormat === 'png') {
    settings.setScale(request.scale * 100);
  } else {
    settings.setEmbedFonts(request.embedFonts);
  }
  return settings;
}

export default function serveIPCRequests(store: HeadlessStore): void {
  if (!('refineryHeadless' in window)) {
    return;
  }
  window.refineryHeadless.onRequest(async (id, request) => {
    const [a1, a2, a3, a4] = request;
    if (
      a1 === undefined ||
      a2 === undefined ||
      a3 === undefined ||
      a4 === undefined
    ) {
      throw new Error('Message too short');
    }
    const headerLength = (a1 << 24) | (a2 << 16) | (a3 << 8) | a4;
    const decoder = new TextDecoder();
    const header = ExportRequest.parse(
      JSON.parse(decoder.decode(request.slice(4, 4 + headerLength))),
    );
    const body = JsonOutput.parse(
      JSON.parse(decoder.decode(request.slice(4 + headerLength))),
    );
    const graph = createGraph(id, body);
    const exportSettings = createExportSettings(header);
    const responseBody = await store.exportGraph(id, graph, exportSettings);
    const encoder = new TextEncoder();
    const responseHeader = encoder.encode(
      JSON.stringify({ result: 'success' }),
    );
    const responseHeaderLength = responseHeader.length;
    const response = new Uint8Array(
      4 + responseHeaderLength + responseBody.length,
    );
    response[0] = (responseHeaderLength >> 24) & 0xff;
    response[1] = (responseHeaderLength >> 16) & 0xff;
    response[2] = (responseHeaderLength >> 8) & 0xff;
    response[3] = responseHeaderLength & 0xff;
    response.set(responseHeader, 4);
    response.set(responseBody, 4 + responseHeaderLength);
    return response;
  });
}
