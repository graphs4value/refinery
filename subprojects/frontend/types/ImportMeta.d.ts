/*
 * Copyright (c) 2019-present, Yuxi (Evan) You and Vite contributors
 * Copyright (c) 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: MIT
 */

interface ImportMeta {
  env: {
    BASE_URL: string;
    DEV: boolean;
    MODE: string;
    PROD: boolean;
    VITE_PACKAGE_NAME: string;
    VITE_PACKAGE_VERSION: string;
  };
}
