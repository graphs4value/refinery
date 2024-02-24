/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

interface FilePickerOptions {
  id?: string;
  types?: {
    description?: string;
    accept: Record<string, string[]>;
  }[];
}

interface FilePickerSaveOptions extends FilePickerOptions {
  suggestedName?: string;
}

interface Window {
  showOpenFilePicker?: (
    options?: FilePickerOpenOptions,
  ) => Promise<FileSystemFileHandle[]>;
  showSaveFilePicker?: (
    options?: FilePickerSaveOptions,
  ) => Promise<FileSystemFileHandle>;
}

interface FileSystemHandlePermissionDescriptor {
  mode?: 'read' | 'readwrite';
}

interface FileSystemHandle {
  queryPermission?: (
    options?: FileSystemHandlePermissionDescriptor,
  ) => Promise<PermissionStatus>;

  requestPermission?: (
    options?: FileSystemHandlePermissionDescriptor,
  ) => Promise<PermissionStatus>;
}
