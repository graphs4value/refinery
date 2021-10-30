import type {
  CompletionContext,
  CompletionResult,
} from '@codemirror/autocomplete';
import type { Transaction } from '@codemirror/state';

import type { EditorStore } from '../editor/EditorStore';
import { ContentAssistService } from './ContentAssistService';
import { HighlightingService } from './HighlightingService';
import { UpdateService } from './UpdateService';
import { getLogger } from '../utils/logger';
import { ValidationService } from './ValidationService';
import { XtextWebSocketClient } from './XtextWebSocketClient';

const log = getLogger('xtext.XtextClient');

export class XtextClient {
  private webSocketClient: XtextWebSocketClient;

  private updateService: UpdateService;

  private contentAssistService: ContentAssistService;

  private highlightingService: HighlightingService;

  private validationService: ValidationService;

  constructor(store: EditorStore) {
    this.webSocketClient = new XtextWebSocketClient(
      () => this.updateService.onConnect(),
      (resource, stateId, service, push) => this.onPush(resource, stateId, service, push),
    );
    this.updateService = new UpdateService(store, this.webSocketClient);
    this.contentAssistService = new ContentAssistService(this.updateService);
    this.highlightingService = new HighlightingService(store, this.updateService);
    this.validationService = new ValidationService(store, this.updateService);
  }

  onTransaction(transaction: Transaction): void {
    // `ContentAssistService.prototype.onTransaction` needs the dirty change desc
    // _before_ the current edit, so we call it before `updateService`.
    this.contentAssistService.onTransaction(transaction);
    this.updateService.onTransaction(transaction);
  }

  private async onPush(resource: string, stateId: string, service: string, push: unknown) {
    const { resourceName, xtextStateId } = this.updateService;
    if (resource !== resourceName) {
      log.error('Unknown resource name: expected:', resourceName, 'got:', resource);
      return;
    }
    if (stateId !== xtextStateId) {
      log.error('Unexpected xtext state id: expected:', xtextStateId, 'got:', resource);
      await this.updateService.updateFullText();
    }
    switch (service) {
      case 'highlight':
        this.highlightingService.onPush(push);
        return;
      case 'validate':
        this.validationService.onPush(push);
        return;
      default:
        log.error('Unknown push service:', service);
        break;
    }
  }

  contentAssist(context: CompletionContext): Promise<CompletionResult> {
    return this.contentAssistService.contentAssist(context);
  }
}
