/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

export async function saveBlob(
  blob: Blob,
  name: string,
  mimeType: string,
  id?: string,
): Promise<void> {
  if ('showSaveFilePicker' in window) {
    const options: FilePickerOptions = {
      suggestedName: name,
    };
    if (id !== undefined) {
      options.id = id;
    }
    const extensionIndex = name.lastIndexOf('.');
    if (extensionIndex >= 0) {
      options.types = [
        {
          description: `${name.substring(extensionIndex + 1)} files`,
          accept: {
            [mimeType]: [name.substring(extensionIndex)],
          },
        },
      ];
    }
    const handle = await window.showSaveFilePicker(options);
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
    link.style.display = 'none';
    document.body.appendChild(link);
    link.click();
  } finally {
    window.URL.revokeObjectURL(url);
    document.body.removeChild(link);
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
