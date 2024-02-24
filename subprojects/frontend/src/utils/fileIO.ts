/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

export interface OpenResult {
  name: string;
  handle: FileSystemFileHandle | undefined;
}

export interface OpenTextFileResult extends OpenResult {
  text: string;
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
  const text = await file.text();
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
  const writable = await handle.createWritable();
  try {
    await writable.write(text);
  } finally {
    await writable.close();
  }
}

export async function saveBlob(
  blob: Blob,
  name: string,
  options: FilePickerOptions,
): Promise<OpenResult | undefined> {
  if ('showSaveFilePicker' in window) {
    const handle = await window.showSaveFilePicker({
      ...options,
      suggestedName: name,
    });
    const writable = await handle.createWritable();
    try {
      await writable.write(blob);
    } finally {
      await writable.close();
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
  return undefined;
}

export async function copyBlob(blob: Blob): Promise<void> {
  const { clipboard } = navigator;
  if ('write' in clipboard) {
    await clipboard.write([
      new ClipboardItem({
        [blob.type]: blob,
      }),
    ]);
  }
}
