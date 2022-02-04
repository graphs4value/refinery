import { NodeProp } from '@lezer/common';

export const implicitCompletion = new NodeProp({
  deserialize(s: string) {
    return s === 'true';
  },
});
