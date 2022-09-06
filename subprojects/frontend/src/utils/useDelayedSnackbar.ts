import {
  useSnackbar,
  type SnackbarKey,
  type SnackbarMessage,
  type OptionsObject,
} from 'notistack';
import { useCallback } from 'react';

export default function useDelayedSnackbar(
  defaultDelay = 0,
): (
  message: SnackbarMessage,
  options?: OptionsObject | undefined,
  delay?: number | undefined,
) => () => void {
  const { enqueueSnackbar, closeSnackbar } = useSnackbar();
  return useCallback(
    (
      message: SnackbarMessage,
      options?: OptionsObject | undefined,
      delay = defaultDelay,
    ) => {
      let key: SnackbarKey | undefined;
      let timeout: number | undefined = setTimeout(() => {
        timeout = undefined;
        key = enqueueSnackbar(message, options);
      }, delay);
      return () => {
        if (timeout !== undefined) {
          clearTimeout(timeout);
        }
        if (key !== undefined) {
          closeSnackbar(key);
        }
      };
    },
    [defaultDelay, enqueueSnackbar, closeSnackbar],
  );
}
