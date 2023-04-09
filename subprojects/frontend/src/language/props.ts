/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

/* eslint-disable import/prefer-default-export -- Lezer needs non-default exports */

import { NodeProp } from '@lezer/common';

export const implicitCompletion = new NodeProp({
  deserialize(s: string) {
    return s === 'true';
  },
});
