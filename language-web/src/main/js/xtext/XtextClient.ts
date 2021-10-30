import type {
  CompletionContext,
  CompletionResult,
} from '@codemirror/autocomplete';
import type { Transaction } from '@codemirror/state';

import type { EditorStore } from '../editor/EditorStore';
import { ContentAssistService } from './ContentAssistService';
import { getLogger } from '../logging';
import { UpdateService } from './UpdateService';
import { ValidationService } from './ValidationService';
import { XtextWebSocketClient } from './XtextWebSocketClient';

const log = getLogger('xtext.XtextClient');

export class XtextClient {
  webSocketClient: XtextWebSocketClient;

  updateService: UpdateService;

  contentAssistService: ContentAssistService;

  validationService: ValidationService;

  constructor(store: EditorStore) {
    this.webSocketClient = new XtextWebSocketClient(
      async () => {
        this.updateService.xtextStateId = null;
        await this.updateService.updateFullText();
      },
      async (resource, stateId, service, push) => {
        await this.onPush(resource, stateId, service, push);
      },
    );
    this.updateService = new UpdateService(store, this.webSocketClient);
    this.contentAssistService = new ContentAssistService(this.updateService);
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
      case 'validate':
        this.validationService.onPush(push);
        return;
      case 'highlight':
        // TODO
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
