/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { closeLintPanel, openLintPanel } from '@codemirror/lint';

import type EditorStore from './EditorStore';
import PanelStore from './PanelStore';

export default class LintPanelStore extends PanelStore {
  constructor(store: EditorStore) {
    super('cm-panel-lint', openLintPanel, closeLintPanel, store);
  }
}
