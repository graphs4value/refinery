/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import net from 'node:net';

export default function getFreePort(): Promise<number> {
  return new Promise((resolve, reject) => {
    const server = net.createServer();
    server.listen(0, () => {
      const address = server.address();
      if (address === null || typeof address === 'string') {
        server.close();
        reject(new Error(`Invalid server address: ${address ?? 'null'}`));
      } else {
        server.close((error) => {
          if (error) {
            reject(error);
          } else {
            resolve(address.port);
          }
        });
      }
    });
  });
}
