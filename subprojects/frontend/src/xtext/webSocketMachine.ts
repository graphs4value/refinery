/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import ms from 'ms';
import { actions, assign, createMachine } from 'xstate';

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
  | { type: 'GENERATION_STARTED' }
  | { type: 'GENERATION_ENDED' }
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
                    always: [{ target: '#timedOut', in: '#mayDisconnect' }],
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
                    always: [{ target: 'inactive', in: '#mayDisconnect' }],
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
              // The `always` transition will move to `#mayDisconnect`
              // if disconnection is possible.
              TAB_HIDDEN: '#keepAlive',
            },
          },
          hidden: {
            on: {
              TAB_VISIBLE: 'visibleOrUnknown',
            },
            initial: 'mayDisconnect',
            states: {
              mayDisconnect: {
                id: 'mayDisconnect',
                always: { target: 'keepAlive', in: '#generationRunning' },
              },
              keepAlive: {
                id: 'keepAlive',
                always: { target: 'mayDisconnect', in: '#generationIdle' },
              },
            },
          },
        },
      },
      generation: {
        initial: 'idle',
        states: {
          idle: {
            id: 'generationIdle',
            on: {
              GENERATION_STARTED: 'running',
            },
          },
          running: {
            id: 'generationRunning',
            on: {
              GENERATION_ENDED: 'idle',
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
        return ERROR_WAIT_TIMES[index] ?? 0;
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
      raiseTimeoutError: raise({
        type: 'ERROR',
        message: 'Open timeout',
      }),
      raisePromiseRejectionError: (_context, { data }) =>
        raise<WebSocketContext, WebSocketEvent>({
          type: 'ERROR',
          message: String(data),
        }),
    },
  },
);
