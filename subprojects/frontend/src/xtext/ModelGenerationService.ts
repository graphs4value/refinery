/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import type EditorStore from '../editor/EditorStore';

import type UpdateService from './UpdateService';
import { ModelGenerationResult } from './xtextServiceResults';

export default class ModelGenerationService {
  constructor(
    private readonly store: EditorStore,
    private readonly updateService: UpdateService,
  ) {}

  onPush(push: unknown): void {
    const result = ModelGenerationResult.parse(push);
    if ('status' in result) {
      this.store.setGeneratedModelMessage(result.uuid, result.status);
    } else if ('error' in result) {
      this.store.setGeneratedModelError(result.uuid, result.error);
    } else {
      this.store.setGeneratedModelSemantics(result.uuid, result);
    }
  }

  onDisconnect(): void {
    this.store.modelGenerationCancelled();
  }

  async start(): Promise<void> {
    const result = await this.updateService.startModelGeneration();
    if (!result.cancelled) {
      this.store.addGeneratedModel(result.data.uuid);
    }
  }

  async cancel(): Promise<void> {
    const result = await this.updateService.cancelModelGeneration();
    if (!result.cancelled) {
      this.store.modelGenerationCancelled();
    }
  }
}
