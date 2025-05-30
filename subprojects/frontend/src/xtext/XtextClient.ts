/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import type {
  CompletionContext,
  CompletionResult,
} from '@codemirror/autocomplete';
import type { Transaction } from '@codemirror/state';
import type { Tooltip } from '@codemirror/view';
import { type IReactionDisposer, reaction } from 'mobx';

import type PWAStore from '../PWAStore';
import type EditorStore from '../editor/EditorStore';
import getLogger from '../utils/getLogger';

import ContentAssistService from './ContentAssistService';
import HighlightingService from './HighlightingService';
import HoverService from './HoverService';
import ModelGenerationService from './ModelGenerationService';
import OccurrencesService from './OccurrencesService';
import SemanticsService from './SemanticsService';
import UpdateService from './UpdateService';
import ValidationService from './ValidationService';
import XtextWebSocketClient from './XtextWebSocketClient';
import type { BackendConfigWithDefaults } from './fetchBackendConfig';
import type { XtextWebPushService } from './xtextMessages';

const log = getLogger('xtext.XtextClient');

export default class XtextClient {
  readonly webSocketClient: XtextWebSocketClient;

  private readonly updateService: UpdateService;

  private readonly contentAssistService: ContentAssistService;

  private readonly highlightingService: HighlightingService;

  private readonly validationService: ValidationService;

  private readonly occurrencesService: OccurrencesService;

  private readonly hoverService: HoverService;

  private readonly semanticsService: SemanticsService;

  private readonly modelGenerationService: ModelGenerationService;

  private readonly keepAliveDisposer: IReactionDisposer;

  constructor(
    private readonly store: EditorStore,
    private readonly pwaStore: PWAStore,
    backendConfig: BackendConfigWithDefaults,
    onUpdate: (text: string) => void,
  ) {
    this.webSocketClient = new XtextWebSocketClient(
      backendConfig,
      () => this.onReconnect(),
      () => this.onDisconnect(),
      this.onPush.bind(this),
    );
    this.updateService = new UpdateService(
      store,
      this.webSocketClient,
      onUpdate,
    );
    this.contentAssistService = new ContentAssistService(this.updateService);
    this.highlightingService = new HighlightingService(
      store,
      this.updateService,
    );
    this.validationService = new ValidationService(store, this.updateService);
    this.occurrencesService = new OccurrencesService(store, this.updateService);
    this.hoverService = new HoverService(store, this.updateService);
    this.semanticsService = new SemanticsService(store, this.validationService);
    this.modelGenerationService = new ModelGenerationService(
      store,
      backendConfig,
    );
    this.keepAliveDisposer = reaction(
      () => store.generating,
      (generating) => this.webSocketClient.setKeepAlive(generating),
      { fireImmediately: true },
    );
  }

  start(): void {
    this.webSocketClient.start();
  }

  private onReconnect(): void {
    this.updateService.onReconnect();
    this.occurrencesService.onReconnect();
    this.pwaStore.checkForUpdates();
  }

  private onDisconnect(): void {
    this.store.onDisconnect();
    this.highlightingService.onDisconnect();
    this.validationService.onDisconnect();
    this.occurrencesService.onDisconnect();
    this.modelGenerationService.onDisconnect();
  }

  onTransaction(transaction: Transaction): void {
    // `ContentAssistService.prototype.onTransaction` needs the dirty change desc
    // _before_ the current edit, so we call it before `updateService`.
    this.contentAssistService.onTransaction(transaction);
    this.updateService.onTransaction(transaction);
    this.occurrencesService.onTransaction(transaction);
    this.modelGenerationService.onTransaction(transaction);
  }

  private onPush(
    resource: string,
    stateId: string,
    service: XtextWebPushService,
    push: unknown,
  ) {
    const { resourceName, xtextStateId } = this.updateService;
    if (resource !== resourceName) {
      log.error(
        'Unknown resource name: expected: %s got: %s',
        resourceName,
        resource,
      );
      return;
    }
    if (stateId !== xtextStateId) {
      log.error(
        'Unexpected xtext state id: expected: %s got: %s',
        xtextStateId,
        stateId,
      );
      // The current push message might be stale (referring to a previous state),
      // so this is not neccessarily an error and there is no need to force-reconnect.
      return;
    }
    switch (service) {
      case 'highlight':
        this.highlightingService.onPush(push);
        return;
      case 'validate':
        this.validationService.onPush(push);
        return;
      case 'semantics':
        this.semanticsService.onPush(push);
        return;
      default:
        throw new Error('Unknown service');
    }
  }

  contentAssist(context: CompletionContext): Promise<CompletionResult> {
    return this.contentAssistService.contentAssist(context);
  }

  hoverTooltip(pos: number): Promise<Tooltip | null> {
    return this.hoverService.hoverTooltip(pos);
  }

  goToDefinition(pos?: number): void {
    this.occurrencesService.goToDefinition(pos).catch((err: unknown) => {
      log.error({ err }, 'Error while fetching occurrences');
    });
  }

  startModelGeneration(randomSeed?: number): void {
    this.modelGenerationService.start(randomSeed);
  }

  cancelModelGeneration(): void {
    this.modelGenerationService.cancel();
  }

  formatText(): void {
    this.updateService.formatText().catch((err: unknown) => {
      log.error({ err }, 'Error while formatting text');
    });
  }

  updateConcretize(): void {
    this.updateService.updateConcretize().catch((err: unknown) => {
      log.error({ err }, 'Error while setting concretize flag on server');
    });
  }

  dispose(): void {
    this.keepAliveDisposer();
    this.webSocketClient.disconnect();
  }
}
