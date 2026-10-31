/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

export const REFINERY_CONTENT_TYPE = 'application/x-refinery';

export const FILE_TYPE_OPTIONS: FilePickerOptions = {
  types: [
    {
      description: 'Refinery files',
      accept: {
        [REFINERY_CONTENT_TYPE]: ['.problem', '.refinery'],
      },
    },
  ],
};

export interface OpenResult {
  name: string;
  handle: FileSystemFileHandle | undefined;
}

export interface OpenTextFileResult extends OpenResult {
  text: string;
}

export class FileIOError extends Error {
  constructor(
    readonly fileName: string,
    cause: unknown,
  ) {
    super('File operation failed', { cause });
  }
}

export async function openTextFile(
  options: FilePickerOptions,
): Promise<OpenTextFileResult> {
  let file: File;
  let handle: FileSystemFileHandle | undefined;
  if ('showOpenFilePicker' in window) {
    [handle] = await window.showOpenFilePicker(options);
    if (handle === undefined) {
      throw new Error('No file was selected');
    }
    file = await handle.getFile();
  } else {
    const input = document.createElement('input');
    input.type = 'file';
    file = await new Promise((resolve, reject) => {
      input.addEventListener('change', () => {
        const { files } = input;
        const result = files?.item(0);
        if (result) {
          resolve(result);
        } else {
          reject(new Error('No file was selected'));
        }
      });
      input.click();
    });
  }
  let text: string;
  try {
    text = await file.text();
  } catch (error) {
    throw new FileIOError(file.name, error);
  }
  return {
    name: file.name,
    text,
    handle,
  };
}

export async function saveTextFile(
  handle: FileSystemFileHandle,
  text: string,
): Promise<void> {
  try {
    const writable = await handle.createWritable();
    try {
      await writable.write(text);
    } finally {
      await writable.close();
    }
  } catch (error) {
    throw new FileIOError(handle.name, error);
  }
}

export async function saveBlob(
  blob: Blob,
  name: string,
  options: FilePickerOptions,
): Promise<OpenResult> {
  if ('showSaveFilePicker' in window) {
    const handle = await window.showSaveFilePicker({
      ...options,
      suggestedName: name,
    });
    try {
      const writable = await handle.createWritable();
      try {
        await writable.write(blob);
      } finally {
        await writable.close();
      }
    } catch (error) {
      throw new FileIOError(handle.name, error);
    }
    return {
      name: handle.name,
      handle,
    };
  }
  const link = document.createElement('a');
  const url = window.URL.createObjectURL(blob);
  try {
    link.href = url;
    link.download = name;
    link.click();
  } finally {
    window.URL.revokeObjectURL(url);
  }
  return {
    name,
    handle: undefined,
  };
}

export async function copyBlob(blob: Blob, type?: string): Promise<void> {
  const { clipboard } = navigator;
  if ('write' in clipboard) {
    await clipboard.write([
      new ClipboardItem({
        [type ?? blob.type]: blob,
      }),
    ]);
  }
}
