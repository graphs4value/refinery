/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { reaction } from 'mobx';
import { type SnackbarKey, type VariantType, useSnackbar } from 'notistack';
import { useEffect, useState } from 'react';

import type EditorStore from './EditorStore';

function MessageObserver({
  editorStore,
}: {
  editorStore: EditorStore;
}): React.ReactNode {
  const [message, setMessage] = useState(
    editorStore.delayedErrors.semanticsError ?? '',
  );
  // Instead of making this component an `observer`,
  // we only update the message is one is present to make sure that the
  // disappear animation has a chance to complete.
  useEffect(
    () =>
      reaction(
        () => editorStore.delayedErrors.semanticsError,
        (newMessage) => {
          if (newMessage !== undefined) {
            setMessage(newMessage);
          }
        },
        { fireImmediately: false },
      ),
    [editorStore],
  );
  return message;
}

export default function AnalysisErrorNotification({
  editorStore,
}: {
  editorStore: EditorStore;
}): null {
  const { enqueueSnackbar, closeSnackbar } = useSnackbar();
  useEffect(() => {
    let key: SnackbarKey | undefined;
    let lastVariant: VariantType | undefined;
    const disposer = reaction(
      (): VariantType | undefined => {
        const {
          delayedErrors: { semanticsError, propagationRejected },
        } = editorStore;
        if (semanticsError === undefined) {
          return undefined;
        }
        return propagationRejected ? 'error' : 'info';
      },
      (variant) => {
        if (variant !== undefined) {
          if (key === undefined || lastVariant !== variant) {
            if (key !== undefined) {
              closeSnackbar(key);
            }
            key = enqueueSnackbar({
              message: <MessageObserver editorStore={editorStore} />,
              variant,
              persist: true,
            });
            lastVariant = variant;
          }
        } else if (key !== undefined) {
          closeSnackbar(key);
          key = undefined;
        }
      },
      { fireImmediately: true },
    );
    return () => {
      disposer();
      if (key !== undefined) {
        closeSnackbar(key);
      }
    };
  }, [editorStore, enqueueSnackbar, closeSnackbar]);
  return null;
}
