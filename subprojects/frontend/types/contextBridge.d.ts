/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

interface Window {
  refinery?: import('../src/RefineryContextBridge').default;

  refineryHeadless?: import('../src/headless/RefineryHeadlessContextBridge').default;
}
