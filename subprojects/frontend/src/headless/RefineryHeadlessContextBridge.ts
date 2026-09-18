/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

export type RequestCallback = (
  id: string,
  body: Uint8Array,
) => Promise<Uint8Array>;

export default interface RefineryHeadlessContextBridge {
  logLevel: string;
  log(obj: object): void;
  onRequest(callback: RequestCallback): void;
}
