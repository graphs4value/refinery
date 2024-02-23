/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

interface FilePickerOptions {
  suggestedName?: string;
  id?: string;
  types?: {
    description?: string;
    accept: Record<string, string[]>;
  }[];
}

interface Window {
  showOpenFilePicker?: (
    options?: FilePickerOptions,
  ) => Promise<FileSystemFileHandle>;
  showSaveFilePicker?: (
    options?: FilePickerOptions,
  ) => Promise<FileSystemFileHandle>;
}
