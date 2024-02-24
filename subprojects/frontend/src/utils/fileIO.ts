/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

export async function saveBlob(
  blob: Blob,
  name: string,
  options: FilePickerOptions,
): Promise<void> {
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
    return;
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
