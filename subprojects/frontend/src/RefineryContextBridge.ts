/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { z } from 'zod/v4';

import type BackendConfig from './xtext/BackendConfig';

export type ServerStateChangeCallback = (healthy: boolean) => void;

export const ThemeSource = z.enum(['system', 'light', 'dark'] as const);

export type ThemeSource = z.infer<typeof ThemeSource>;

export const ServerSettings = z.object({
  semanticsTimeoutMs: z.int().positive(),
  modelGenerationTimeoutSec: z.int().positive(),
  maxMemoryBytes: z.int().positive(),
  libraryPaths: z.array(z.string().min(1)),
  classpathJars: z.array(z.string().min(1)),
});

export type ServerSettings = z.infer<typeof ServerSettings>;

export const ServerSettingsResponse = z.object({
  settings: ServerSettings,
  systemMemoryBytes: z.int().positive(),
  defaultMaxMemoryBytes: z.int().positive(),
  pathDelimiter: z.string().min(1),
});

export type ServerSettingsResponse = z.infer<typeof ServerSettingsResponse>;

export const RestartServerResult = z.boolean();

export type RestartServerResult = z.infer<typeof RestartServerResult>;

export const CLISymlinkStatus = z.enum([
  'unsupported',
  'notConfigured',
  'disabled',
  'missing',
  'incorrect',
  'correct',
] as const);

export type CLISymlinkStatus = z.infer<typeof CLISymlinkStatus>;

export const CLISymlinkState = z.object({
  status: CLISymlinkStatus,
  actionInFlight: z.boolean(),
});

export type CLISymlinkState = z.infer<typeof CLISymlinkState>;

export type CLISymlinkStatusChangeCallback = (state: CLISymlinkState) => void;

export const CLISymlinkResult = z.union([
  z.literal(true),
  z.object({
    error: z.literal(true),
    reason: z.enum(['failed', 'occupied', 'busy'] as const),
  }),
]);

export type CLISymlinkResult = z.infer<typeof CLISymlinkResult>;

export const CLISymlinkPromptClaimResult = z.boolean();

export type CLISymlinkPromptClaimResult = z.infer<
  typeof CLISymlinkPromptClaimResult
>;

export const PathSelectionResult = z
  .union([z.string().min(1), z.object({ error: z.literal(true) })])
  .optional();

export type PathSelectionResult = z.infer<typeof PathSelectionResult>;

export type ThemeSourceChangeCallback = (themeSource: ThemeSource) => void;

export const EditorCommand = z.enum([
  'openFile',
  'saveFile',
  'saveFileAs',
  'copyLink',
  'pasteLink',
]);

export type EditorCommand = z.infer<typeof EditorCommand>;

export type EditorCommandCallback = (command: EditorCommand) => void;

export const FileResult = z.object({ name: z.string() });

export type FileResult = z.infer<typeof FileResult>;

export const OpenFileResult = FileResult.extend({ text: z.string() });

export type OpenFileResult = z.infer<typeof OpenFileResult>;

const FileError = z.object({
  error: z.literal(true),
  name: z.string().optional(),
  reason: z.enum(['alreadyOpen', 'invalidUtf8']).optional(),
});

export const FileErrorResult = FileError.optional();

export type FileErrorResult = z.infer<typeof FileErrorResult>;

export const FileResultOrError = z.union([FileResult, FileError]).optional();

export type FileResultOrError = z.infer<typeof FileResultOrError>;

export const OpenFileResultOrError = z
  .union([OpenFileResult, FileError])
  .optional();

export type OpenFileResultOrError = z.infer<typeof OpenFileResultOrError>;

export const ReadFileResult = z
  .union([OpenFileResult, z.object({ hash: z.string() }), FileError])
  .optional();

export type ReadFileResult = z.infer<typeof ReadFileResult>;

export default interface RefineryContextBridge {
  logLevel: string;
  log(obj: object): void;
  getBackendConfig(): Promise<BackendConfig>;
  getServerSettings(): Promise<ServerSettingsResponse>;
  onCLISymlinkStatusChange(callback: CLISymlinkStatusChangeCallback): void;
  claimCLISymlinkPrompt(): Promise<CLISymlinkPromptClaimResult>;
  setCLISymlink(enabled: boolean): Promise<CLISymlinkResult>;
  setCLISymlinkPreference(enabled: boolean): Promise<CLISymlinkResult>;
  getPathForFile(file: unknown): string;
  selectLibraryDirectory(): Promise<PathSelectionResult>;
  selectClasspathJar(): Promise<PathSelectionResult>;
  restartServer(settings?: ServerSettings): Promise<RestartServerResult>;
  onServerStateChange(callback: ServerStateChangeCallback): void;
  setThemeSource(themeSource: ThemeSource): void;
  onThemeSourceChange(callback: ThemeSourceChangeCallback): void;
  onEditorCommand(callback: EditorCommandCallback): void;
  setUnsavedChanges(unsavedChanges: boolean): void;
  readFile(): Promise<ReadFileResult>;
  openFile(newWindow?: boolean): Promise<OpenFileResultOrError>;
  clearFile(): Promise<FileErrorResult>;
  openHash(hash: string): Promise<FileErrorResult>;
  saveFile(text: string): Promise<FileResultOrError>;
  saveFileAs(text: string): Promise<FileResultOrError>;
  openDialog(id: string): void;
  closeDialog(id: string): void;
}
