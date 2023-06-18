/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

export default class CancelledError extends Error {
  constructor(message = 'Operation cancelled') {
    super(message);
  }
}
