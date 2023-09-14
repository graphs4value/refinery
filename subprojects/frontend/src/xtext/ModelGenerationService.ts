/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import type { Transaction } from '@codemirror/state';

import type EditorStore from '../editor/EditorStore';

import type UpdateService from './UpdateService';
import { ModelGenerationResult } from './xtextServiceResults';

const INITIAL_RANDOM_SEED = 1;

export default class ModelGenerationService {
  private nextRandomSeed = INITIAL_RANDOM_SEED;

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

  onTransaction(transaction: Transaction): void {
    if (transaction.docChanged) {
      this.resetRandomSeed();
    }
  }

  onDisconnect(): void {
    this.store.modelGenerationCancelled();
    this.resetRandomSeed();
  }

  async start(randomSeed?: number): Promise<void> {
    const randomSeedOrNext = randomSeed ?? this.nextRandomSeed;
    this.nextRandomSeed = randomSeedOrNext + 1;
    const result =
      await this.updateService.startModelGeneration(randomSeedOrNext);
    if (!result.cancelled) {
      this.store.addGeneratedModel(result.data.uuid, randomSeedOrNext);
    }
  }

  async cancel(): Promise<void> {
    const result = await this.updateService.cancelModelGeneration();
    if (!result.cancelled) {
      this.store.modelGenerationCancelled();
    }
  }

  private resetRandomSeed() {
    this.nextRandomSeed = INITIAL_RANDOM_SEED;
  }
}
