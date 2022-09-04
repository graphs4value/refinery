import { actions, assign, createMachine, RaiseAction } from 'xstate';

const { raise } = actions;

const ERROR_WAIT_TIMES = [200, 1000, 5000, 30_000];

export interface WebSocketContext {
  webSocketURL: string | undefined;
  errors: string[];
  retryCount: number;
}

export type WebSocketEvent =
  | { type: 'CONFIGURE'; webSocketURL: string }
  | { type: 'CONNECT' }
  | { type: 'DISCONNECT' }
  | { type: 'OPENED' }
  | { type: 'TAB_VISIBLE' }
  | { type: 'TAB_HIDDEN' }
  | { type: 'ERROR'; message: string };

export default createMachine(
  {
    id: 'webSocket',
    predictableActionArguments: true,
    schema: {
      context: {} as WebSocketContext,
      events: {} as WebSocketEvent,
    },
    tsTypes: {} as import('./webSocketMachine.typegen').Typegen0,
    context: {
      webSocketURL: undefined,
      errors: [],
      retryCount: 0,
    },
    type: 'parallel',
    states: {
      connection: {
        initial: 'disconnected',
        states: {
          disconnected: {
            id: 'disconnected',
            on: {
              CONFIGURE: { actions: 'configure' },
            },
          },
          timedOut: {
            id: 'timedOut',
            on: {
              TAB_VISIBLE: 'socketCreated',
            },
          },
          errorWait: {
            id: 'errorWait',
            after: {
              ERROR_WAIT_TIME: [
                { target: 'timedOut', in: '#tabHidden' },
                { target: 'socketCreated' },
              ],
            },
          },
          socketCreated: {
            type: 'parallel',
            entry: 'openWebSocket',
            exit: ['cancelPendingRequests', 'closeWebSocket'],
            states: {
              open: {
                initial: 'opening',
                states: {
                  opening: {
                    after: {
                      OPEN_TIMEOUT: {
                        actions: 'raiseTimeoutError',
                      },
                    },
                    on: {
                      OPENED: {
                        target: 'opened',
                        actions: ['clearError', 'notifyReconnect'],
                      },
                    },
                  },
                  opened: {
                    initial: 'pongReceived',
                    states: {
                      pongReceived: {
                        after: {
                          PING_PERIOD: 'pingSent',
                        },
                      },
                      pingSent: {
                        invoke: {
                          src: 'pingService',
                          onDone: 'pongReceived',
                          onError: {
                            actions: 'raisePromiseRejectionError',
                          },
                        },
                      },
                    },
                  },
                },
              },
              idle: {
                initial: 'getTabState',
                states: {
                  getTabState: {
                    always: [
                      { target: 'inactive', in: '#tabHidden' },
                      'active',
                    ],
                  },
                  active: {
                    on: {
                      TAB_HIDDEN: 'inactive',
                    },
                  },
                  inactive: {
                    after: {
                      IDLE_TIMEOUT: '#timedOut',
                    },
                    on: {
                      TAB_VISIBLE: 'active',
                    },
                  },
                },
              },
            },
            on: {
              CONNECT: undefined,
              ERROR: {
                target: '#errorWait',
                actions: 'increaseRetryCount',
              },
            },
          },
        },
        on: {
          CONNECT: { target: '.socketCreated', cond: 'hasWebSocketURL' },
          DISCONNECT: { target: '.disconnected', actions: 'clearError' },
        },
      },
      tab: {
        initial: 'visibleOrUnknown',
        states: {
          visibleOrUnknown: {
            on: {
              TAB_HIDDEN: 'hidden',
            },
          },
          hidden: {
            id: 'tabHidden',
            on: {
              TAB_VISIBLE: 'visibleOrUnknown',
            },
          },
        },
      },
      error: {
        initial: 'init',
        states: {
          init: {
            on: {
              ERROR: { actions: 'pushError' },
            },
          },
        },
      },
    },
  },
  {
    guards: {
      hasWebSocketURL: ({ webSocketURL }) => webSocketURL !== undefined,
    },
    delays: {
      IDLE_TIMEOUT: 300_000,
      OPEN_TIMEOUT: 5000,
      PING_PERIOD: 10_000,
      ERROR_WAIT_TIME: ({ retryCount }) => {
        const { length } = ERROR_WAIT_TIMES;
        const index = retryCount < length ? retryCount : length - 1;
        return ERROR_WAIT_TIMES[index];
      },
    },
    actions: {
      configure: assign((context, { webSocketURL }) => ({
        ...context,
        webSocketURL,
      })),
      pushError: assign((context, { message }) => ({
        ...context,
        errors: [...context.errors, message],
      })),
      increaseRetryCount: assign((context) => ({
        ...context,
        retryCount: context.retryCount + 1,
      })),
      clearError: assign((context) => ({
        ...context,
        errors: [],
        retryCount: 0,
      })),
      // Workaround from https://github.com/statelyai/xstate/issues/1414#issuecomment-699972485
      raiseTimeoutError: raise({
        type: 'ERROR',
        message: 'Open timeout',
      }) as RaiseAction<WebSocketEvent>,
      raisePromiseRejectionError: (_context, { data }) =>
        raise({
          type: 'ERROR',
          message: data,
        }) as RaiseAction<WebSocketEvent>,
    },
  },
);
