declare const DEBUG: boolean;

declare const PACKAGE_NAME: string;

declare const PACKAGE_VERSION: string;

declare module '*.module.scss' {
  const cssVariables: { [key in string]?: string };
  // eslint-disable-next-line import/no-default-export
  export default cssVariables;
}
