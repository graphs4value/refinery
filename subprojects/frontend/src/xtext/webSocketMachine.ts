import ms from 'ms';
import { actions, assign, createMachine, RaiseAction } from 'xstate';

const { raise } = actions;

const ERROR_WAIT_TIMES = ['200', '1s', '5s', '30s'].map(ms);

export interface WebSocketContext {
  errors: string[];
}

export type WebSocketEvent =
  | { type: 'CONNECT' }
  | { type: 'DISCONNECT' }
  | { type: 'OPENED' }
  | { type: 'TAB_VISIBLE' }
  | { type: 'TAB_HIDDEN' }
  | { type: 'PAGE_HIDE' }
  | { type: 'PAGE_SHOW' }
  | { type: 'PAGE_FREEZE' }
  | { type: 'PAGE_RESUME' }
  | { type: 'ONLINE' }
  | { type: 'OFFLINE' }
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
      errors: [],
    },
    type: 'parallel',
    states: {
      connection: {
        initial: 'disconnected',
        entry: 'clearErrors',
        states: {
          disconnected: {
            id: 'disconnected',
            entry: ['clearErrors', 'notifyDisconnect'],
          },
          timedOut: {
            id: 'timedOut',
            always: [
              {
                target: 'temporarilyOffline',
                in: '#offline',
              },
              { target: 'socketCreated', in: '#tabVisible' },
            ],
            on: {
              PAGE_HIDE: 'pageHidden',
              PAGE_FREEZE: 'pageHidden',
            },
          },
          errorWait: {
            id: 'errorWait',
            always: [
              {
                target: 'temporarilyOffline',
                in: '#offline',
              },
            ],
            after: {
              ERROR_WAIT_TIME: 'timedOut',
            },
            on: {
              PAGE_HIDE: 'pageHidden',
              PAGE_FREEZE: 'pageHidden',
            },
          },
          temporarilyOffline: {
            entry: ['clearErrors', 'notifyDisconnect'],
            always: [{ target: 'timedOut', in: '#online' }],
            on: {
              PAGE_HIDE: 'pageHidden',
              PAGE_FREEZE: 'pageHidden',
            },
          },
          pageHidden: {
            entry: 'clearErrors',
            on: {
              PAGE_SHOW: 'timedOut',
              PAGE_RESUME: 'timedOut',
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
                    always: [{ target: '#timedOut', in: '#tabHidden' }],
                    after: {
                      OPEN_TIMEOUT: {
                        actions: 'raiseTimeoutError',
                      },
                    },
                    on: {
                      OPENED: {
                        target: 'opened',
                        actions: ['clearErrors', 'notifyReconnect'],
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
                initial: 'active',
                states: {
                  active: {
                    always: [{ target: 'inactive', in: '#tabHidden' }],
                  },
                  inactive: {
                    always: [{ target: 'active', in: '#tabVisible' }],
                    after: {
                      IDLE_TIMEOUT: '#timedOut',
                    },
                  },
                },
              },
            },
            on: {
              CONNECT: undefined,
              ERROR: { target: '#errorWait', actions: 'pushError' },
              PAGE_HIDE: 'pageHidden',
              PAGE_FREEZE: 'pageHidden',
            },
          },
        },
        on: {
          CONNECT: '.timedOut',
          DISCONNECT: '.disconnected',
        },
      },
      tab: {
        initial: 'visibleOrUnknown',
        states: {
          visibleOrUnknown: {
            id: 'tabVisible',
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
      network: {
        initial: 'onlineOrUnknown',
        states: {
          onlineOrUnknown: {
            id: 'online',
            on: {
              OFFLINE: 'offline',
            },
          },
          offline: {
            id: 'offline',
            on: {
              ONLINE: 'onlineOrUnknown',
            },
          },
        },
      },
    },
  },
  {
    delays: {
      IDLE_TIMEOUT: ms('5m'),
      OPEN_TIMEOUT: ms('10s'),
      PING_PERIOD: ms('10s'),
      ERROR_WAIT_TIME: ({ errors: { length: retryCount } }) => {
        const { length } = ERROR_WAIT_TIMES;
        const index = retryCount < length ? retryCount : length - 1;
        return ERROR_WAIT_TIMES[index];
      },
    },
    actions: {
      pushError: assign((context, { message }) => ({
        ...context,
        errors: [...context.errors, message],
      })),
      clearErrors: assign((context) => ({
        ...context,
        errors: [],
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
