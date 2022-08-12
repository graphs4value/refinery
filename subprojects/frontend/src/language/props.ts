/* eslint-disable import/prefer-default-export -- Lezer needs non-default exports */

import { NodeProp } from '@lezer/common';

export const implicitCompletion = new NodeProp({
  deserialize(s: string) {
    return s === 'true';
  },
});
