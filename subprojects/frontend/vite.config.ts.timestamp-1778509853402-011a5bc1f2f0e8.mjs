// vite.config.ts
import path3 from "node:path";
import { fileURLToPath } from "node:url";
import { lezer } from "file:///home/kmono/refinery/.yarn/cache/@lezer-generator-npm-1.8.0-1de52721b4-c9dab9a27b.zip/node_modules/@lezer/generator/dist/rollup-plugin-lezer.js";
import react from "file:///home/kmono/refinery/.yarn/__virtual__/@vitejs-plugin-react-swc-virtual-fdfd72865b/0/cache/@vitejs-plugin-react-swc-npm-4.3.0-39d765192d-36228152f5.zip/node_modules/@vitejs/plugin-react-swc/index.js";
import { defineConfig } from "file:///home/kmono/refinery/.yarn/__virtual__/vite-virtual-1c5cf21f9b/0/cache/vite-npm-7.3.1-330baf2f0d-5c7548f5f4.zip/node_modules/vite/dist/node/index.js";
import { VitePWA } from "file:///home/kmono/refinery/.yarn/__virtual__/vite-plugin-pwa-virtual-6d84312263/0/cache/vite-plugin-pwa-npm-1.2.0-db78434d4b-d037591fc6.zip/node_modules/vite-plugin-pwa/dist/index.js";
import svgr from "file:///home/kmono/refinery/.yarn/__virtual__/vite-plugin-svgr-virtual-053c316068/0/cache/vite-plugin-svgr-npm-4.5.0-eec66448a2-3e1959fec6.zip/node_modules/vite-plugin-svgr/dist/index.js";

// src/xtext/BackendConfig.ts
import { z } from "file:///home/kmono/refinery/.yarn/cache/zod-npm-4.3.6-a096e305e6-860d25a81a.zip/node_modules/zod/v4/index.js";
var ENDPOINT = "config.json";
var BackendConfig = z.object({
  apiBase: z.url().optional(),
  webSocketURL: z.url().optional(),
  chatURL: z.url().optional()
});

// config/backendConfigVitePlugin.ts
function backendConfigVitePlugin(backendConfig) {
  return {
    name: "backend-config",
    apply: "serve",
    configureServer(server) {
      const config = JSON.stringify(backendConfig);
      server.middlewares.use((req, res, next) => {
        if (req.url === `/${ENDPOINT}`) {
          res.setHeader("Content-Type", "application/json");
          res.end(config);
        } else {
          next();
        }
      });
    }
  };
}

// config/detectDevModeOptions.ts
var API_ENDPOINT = "api";
var XTEXT_ENDPOINT = "xtext-service";
var CHAT_ENDPOINT = "chat";
function detectListenOptions(name, fallbackHost, fallbackPort) {
  const host = process.env[`REFINERY_${name}_HOST`] ?? fallbackHost;
  const rawPort = process.env[`REFINERY_${name}_PORT`];
  const port = rawPort === void 0 ? fallbackPort : parseInt(rawPort, 10);
  const secure = port === 443;
  return { host, port, secure };
}
function listenURL({ host, port, secure }, protocol = "http") {
  return `${secure ? `${protocol}s` : protocol}://${host}:${port}`;
}
function detectDevModeOptions() {
  const mode2 = process.env["MODE"] ?? "development";
  const isDevelopment2 = mode2 === "development";
  if (!isDevelopment2) {
    return {
      mode: mode2,
      isDevelopment: isDevelopment2,
      devModePlugins: [],
      serverOptions: {}
    };
  }
  const listen = detectListenOptions("LISTEN", "localhost", 1313);
  const api = detectListenOptions("API", "127.0.0.1", 1312);
  const apiURL = listenURL(api);
  const chat = detectListenOptions("CHAT", "127.0.0.1", 1314);
  const chatURL = listenURL(chat);
  const publicAddress = detectListenOptions("PUBLIC", listen.host, listen.port);
  const publicURL = listenURL(publicAddress);
  if (listen.secure) {
    throw new Error(`Preview on secure port ${listen.port} is not supported`);
  }
  const backendConfig = {
    apiBase: `${publicURL}/${API_ENDPOINT}/v1`,
    webSocketURL: `${listenURL(publicAddress, "ws")}/${XTEXT_ENDPOINT}`,
    chatURL: `${publicURL}/${CHAT_ENDPOINT}/v1`
  };
  return {
    mode: mode2,
    isDevelopment: isDevelopment2,
    devModePlugins: [backendConfigVitePlugin(backendConfig)],
    serverOptions: {
      host: listen.host,
      port: listen.port,
      strictPort: true,
      headers: {
        // Enable strict origin isolation, see e.g.,
        // https://github.com/vitejs/vite/issues/3909#issuecomment-1065893956
        "Cross-Origin-Opener-Policy": "same-origin",
        "Cross-Origin-Embedder-Policy": "require-corp",
        "Cross-Origin-Resource-Policy": "cross-origin"
      },
      proxy: {
        [`/${API_ENDPOINT}`]: {
          target: apiURL,
          secure: api.secure
        },
        [`/${XTEXT_ENDPOINT}`]: {
          target: apiURL,
          ws: true,
          secure: api.secure
        },
        [`/${CHAT_ENDPOINT}`]: {
          target: chatURL,
          secure: chat.secure
        }
      },
      hmr: {
        host: publicAddress.host,
        clientPort: publicAddress.port,
        path: "/vite"
      }
    }
  };
}

// config/fetchPackageMetadata.ts
import { readFile } from "node:fs/promises";
import path from "node:path";
import z2 from "file:///home/kmono/refinery/.yarn/cache/zod-npm-4.3.6-a096e305e6-860d25a81a.zip/node_modules/zod/v4/index.js";
var PackageInfo = z2.object({
  name: z2.string().min(1),
  version: z2.string().min(1)
});
async function fetchPackageMetadata(thisDir2) {
  const contents = await readFile(path.join(thisDir2, "package.json"), "utf8");
  const { name: packageName, version: packageVersion } = PackageInfo.parse(
    JSON.parse(contents)
  );
  process.env["VITE_PACKAGE_NAME"] ??= packageName;
  process.env["VITE_PACKAGE_VERSION"] ??= packageVersion;
}

// config/graphvizUMDVitePlugin.ts
import { readFile as readFile2 } from "node:fs/promises";
import path2 from "node:path";
import pnpapi from "pnpapi";
var issuerFileName = "worker.cjs";
function graphvizUMDVitePlugin() {
  let command = "build";
  let root;
  let url;
  return {
    name: "graphviz-umd",
    enforce: "post",
    configResolved(config) {
      ({ command, root } = config);
    },
    async buildStart() {
      const issuer = root === void 0 ? issuerFileName : path2.join(root, issuerFileName);
      const resolvedPath = pnpapi.resolveRequest("@hpcc-js/wasm/graphviz", issuer)?.replace(/\.cjs$/, ".umd.js");
      if (resolvedPath === void 0) {
        return;
      }
      if (command === "serve") {
        url = `/@fs/${resolvedPath}`;
      } else {
        const content = await readFile2(resolvedPath, null);
        url = this.emitFile({
          name: path2.basename(resolvedPath),
          type: "asset",
          source: content
        });
      }
    },
    renderStart() {
      if (url !== void 0 && command !== "serve") {
        url = this.getFileName(url);
      }
    },
    transformIndexHtml() {
      if (url === void 0) {
        return void 0;
      }
      return [
        {
          tag: "script",
          attrs: {
            src: url,
            type: "javascript/worker"
          },
          injectTo: "head"
        }
      ];
    }
  };
}

// config/manifest.ts
var manifest = {
  lang: "en-US",
  name: "Refinery",
  short_name: "Refinery",
  description: "An efficient graph solver for generating well-formed models",
  theme_color: "#f5f5f5",
  display_override: ["window-controls-overlay"],
  display: "standalone",
  background_color: "#21252b",
  icons: [
    {
      src: "icon-192x192.png",
      sizes: "192x192",
      type: "image/png",
      purpose: "any maskable"
    },
    {
      src: "icon-512x512.png",
      sizes: "512x512",
      type: "image/png",
      purpose: "any maskable"
    },
    {
      src: "icon-any.svg",
      sizes: "any",
      type: "image/svg+xml",
      purpose: "any maskable"
    },
    {
      src: "mask-icon.svg",
      sizes: "any",
      type: "image/svg+xml",
      purpose: "monochrome"
    }
  ]
};
var manifest_default = manifest;

// config/minifyHTMLVitePlugin.ts
import { minify } from "file:///home/kmono/refinery/.yarn/cache/html-minifier-terser-npm-7.2.0-b9eba92a3b-ffc97c1729.zip/node_modules/html-minifier-terser/src/htmlminifier.js";
function minifyHTMLVitePlugin(options) {
  return {
    name: "minify-html",
    apply: "build",
    enforce: "post",
    transformIndexHtml(html) {
      return minify(html, {
        collapseWhitespace: true,
        collapseBooleanAttributes: true,
        minifyCSS: true,
        removeComments: true,
        removeAttributeQuotes: true,
        removeRedundantAttributes: true,
        sortAttributes: true,
        ...options ?? {}
      });
    }
  };
}

// config/preloadFontsVitePlugin.ts
import micromatch from "file:///home/kmono/refinery/.yarn/cache/micromatch-npm-4.0.8-c9570e4aca-166fa6eb92.zip/node_modules/micromatch/index.js";
function preloadFontsVitePlugin(fontsGlob2) {
  return {
    name: "refinery-preload-fonts",
    apply: "build",
    enforce: "post",
    transformIndexHtml(_html, { bundle }) {
      return micromatch(Object.keys(bundle ?? {}), fontsGlob2).map((href) => ({
        tag: "link",
        attrs: {
          href,
          rel: "preload",
          type: "font/woff2",
          as: "font",
          crossorigin: "anonymous"
        }
      }));
    }
  };
}

// vite.config.ts
var __vite_injected_original_import_meta_url = "file:///home/kmono/refinery/subprojects/frontend/vite.config.ts";
var thisDir = path3.dirname(fileURLToPath(__vite_injected_original_import_meta_url));
var { mode, isDevelopment, devModePlugins, serverOptions } = detectDevModeOptions();
process.env["NODE_ENV"] ??= mode;
var fontsGlob = [
  "open-sans-latin-*.ttf",
  "open-sans-latin-400-{normal,italic}-*.woff2",
  "open-sans-latin-700-*.woff2",
  "open-sans-latin-wdth-{normal,italic}-*.woff2",
  "jetbrains-mono-latin-wght-{normal,italic}-*.woff2"
];
var viteConfig = {
  logLevel: "info",
  mode,
  root: thisDir,
  cacheDir: path3.join(thisDir, "build/vite/cache"),
  plugins: [
    react(),
    lezer(),
    svgr(),
    preloadFontsVitePlugin(fontsGlob),
    minifyHTMLVitePlugin(),
    graphvizUMDVitePlugin(),
    VitePWA({
      strategies: "generateSW",
      registerType: "prompt",
      injectRegister: null,
      workbox: {
        globPatterns: ["**/*.{css,html,js}", ...fontsGlob],
        dontCacheBustURLsMatching: /\.(?:css|js|woff2?)$/,
        navigateFallbackDenylist: [
          new RegExp(`^\\/${API_ENDPOINT}$`),
          new RegExp(`^\\/${XTEXT_ENDPOINT}$`),
          new RegExp(`^\\/${CHAT_ENDPOINT}$`)
        ],
        runtimeCaching: [
          {
            urlPattern: ENDPOINT,
            handler: "StaleWhileRevalidate"
          }
        ]
      },
      includeAssets: ["apple-touch-icon.png", "favicon.svg"],
      manifest: manifest_default
    }),
    devModePlugins
  ],
  base: "",
  define: {
    __DEV__: JSON.stringify(isDevelopment)
    // For MobX
  },
  resolve: {
    alias: {
      "@tools.refinery/client/chat": path3.join(
        thisDir,
        "../client-js/",
        isDevelopment ? "src/chat/index.ts" : "dist/chat.mjs"
      ),
      "@tools.refinery/client": path3.join(
        thisDir,
        "../client-js/",
        isDevelopment ? "src/index.ts" : "dist/index.mjs"
      )
    }
  },
  build: {
    assetsDir: ".",
    // If we don't control inlining manually, web fonts will be randomly inlined
    // into the CSS, which degrades performance.
    assetsInlineLimit: 0,
    outDir: path3.join("build/vite", mode),
    emptyOutDir: true,
    sourcemap: isDevelopment,
    minify: !isDevelopment,
    rollupOptions: {
      output: {
        chunkFileNames: ({ isDynamicEntry, isEntry }) => isDynamicEntry || isEntry ? "[name]-[hash].js" : "[hash].js",
        experimentalMinChunkSize: 20 * 1024
      }
    }
  },
  server: serverOptions
};
var vite_config_default = defineConfig(async () => {
  await fetchPackageMetadata(thisDir);
  return viteConfig;
});
export {
  vite_config_default as default
};
//# sourceMappingURL=data:application/json;base64,ewogICJ2ZXJzaW9uIjogMywKICAic291cmNlcyI6IFsidml0ZS5jb25maWcudHMiLCAic3JjL3h0ZXh0L0JhY2tlbmRDb25maWcudHMiLCAiY29uZmlnL2JhY2tlbmRDb25maWdWaXRlUGx1Z2luLnRzIiwgImNvbmZpZy9kZXRlY3REZXZNb2RlT3B0aW9ucy50cyIsICJjb25maWcvZmV0Y2hQYWNrYWdlTWV0YWRhdGEudHMiLCAiY29uZmlnL2dyYXBodml6VU1EVml0ZVBsdWdpbi50cyIsICJjb25maWcvbWFuaWZlc3QudHMiLCAiY29uZmlnL21pbmlmeUhUTUxWaXRlUGx1Z2luLnRzIiwgImNvbmZpZy9wcmVsb2FkRm9udHNWaXRlUGx1Z2luLnRzIl0sCiAgInNvdXJjZVJvb3QiOiAiZmlsZTovLy9ob21lL2ttb25vL3JlZmluZXJ5L3N1YnByb2plY3RzL2Zyb250ZW5kLyIsCiAgInNvdXJjZXNDb250ZW50IjogWyJjb25zdCBfX3ZpdGVfaW5qZWN0ZWRfb3JpZ2luYWxfZGlybmFtZSA9IFwiL2hvbWUva21vbm8vcmVmaW5lcnkvc3VicHJvamVjdHMvZnJvbnRlbmRcIjtjb25zdCBfX3ZpdGVfaW5qZWN0ZWRfb3JpZ2luYWxfZmlsZW5hbWUgPSBcIi9ob21lL2ttb25vL3JlZmluZXJ5L3N1YnByb2plY3RzL2Zyb250ZW5kL3ZpdGUuY29uZmlnLnRzXCI7Y29uc3QgX192aXRlX2luamVjdGVkX29yaWdpbmFsX2ltcG9ydF9tZXRhX3VybCA9IFwiZmlsZTovLy9ob21lL2ttb25vL3JlZmluZXJ5L3N1YnByb2plY3RzL2Zyb250ZW5kL3ZpdGUuY29uZmlnLnRzXCI7LypcbiAqIFNQRFgtRmlsZUNvcHlyaWdodFRleHQ6IDIwMjEtMjAyNCBUaGUgUmVmaW5lcnkgQXV0aG9ycyA8aHR0cHM6Ly9yZWZpbmVyeS50b29scy8+XG4gKlxuICogU1BEWC1MaWNlbnNlLUlkZW50aWZpZXI6IEVQTC0yLjBcbiAqL1xuXG5pbXBvcnQgcGF0aCBmcm9tICdub2RlOnBhdGgnO1xuaW1wb3J0IHsgZmlsZVVSTFRvUGF0aCB9IGZyb20gJ25vZGU6dXJsJztcblxuaW1wb3J0IHsgbGV6ZXIgfSBmcm9tICdAbGV6ZXIvZ2VuZXJhdG9yL3JvbGx1cCc7XG5pbXBvcnQgcmVhY3QgZnJvbSAnQHZpdGVqcy9wbHVnaW4tcmVhY3Qtc3djJztcbmltcG9ydCB7IGRlZmluZUNvbmZpZywgdHlwZSBVc2VyQ29uZmlnIGFzIFZpdGVDb25maWcgfSBmcm9tICd2aXRlJztcbmltcG9ydCB7IFZpdGVQV0EgfSBmcm9tICd2aXRlLXBsdWdpbi1wd2EnO1xuaW1wb3J0IHN2Z3IgZnJvbSAndml0ZS1wbHVnaW4tc3Zncic7XG5cbmltcG9ydCB7IENPTkZJR19FTkRQT0lOVCB9IGZyb20gJy4vY29uZmlnL2JhY2tlbmRDb25maWdWaXRlUGx1Z2luJztcbmltcG9ydCBkZXRlY3REZXZNb2RlT3B0aW9ucywge1xuICBBUElfRU5EUE9JTlQsXG4gIENIQVRfRU5EUE9JTlQsXG4gIFhURVhUX0VORFBPSU5ULFxufSBmcm9tICcuL2NvbmZpZy9kZXRlY3REZXZNb2RlT3B0aW9ucyc7XG5pbXBvcnQgZmV0Y2hQYWNrYWdlTWV0YWRhdGEgZnJvbSAnLi9jb25maWcvZmV0Y2hQYWNrYWdlTWV0YWRhdGEnO1xuaW1wb3J0IGdyYXBodml6VU1EVml0ZVBsdWdpbiBmcm9tICcuL2NvbmZpZy9ncmFwaHZpelVNRFZpdGVQbHVnaW4nO1xuaW1wb3J0IG1hbmlmZXN0IGZyb20gJy4vY29uZmlnL21hbmlmZXN0JztcbmltcG9ydCBtaW5pZnlIVE1MVml0ZVBsdWdpbiBmcm9tICcuL2NvbmZpZy9taW5pZnlIVE1MVml0ZVBsdWdpbic7XG5pbXBvcnQgcHJlbG9hZEZvbnRzVml0ZVBsdWdpbiBmcm9tICcuL2NvbmZpZy9wcmVsb2FkRm9udHNWaXRlUGx1Z2luJztcblxuY29uc3QgdGhpc0RpciA9IHBhdGguZGlybmFtZShmaWxlVVJMVG9QYXRoKGltcG9ydC5tZXRhLnVybCkpO1xuXG5jb25zdCB7IG1vZGUsIGlzRGV2ZWxvcG1lbnQsIGRldk1vZGVQbHVnaW5zLCBzZXJ2ZXJPcHRpb25zIH0gPVxuICBkZXRlY3REZXZNb2RlT3B0aW9ucygpO1xuXG5wcm9jZXNzLmVudlsnTk9ERV9FTlYnXSA/Pz0gbW9kZTtcblxuY29uc3QgZm9udHNHbG9iID0gW1xuICAnb3Blbi1zYW5zLWxhdGluLSoudHRmJyxcbiAgJ29wZW4tc2Fucy1sYXRpbi00MDAte25vcm1hbCxpdGFsaWN9LSoud29mZjInLFxuICAnb3Blbi1zYW5zLWxhdGluLTcwMC0qLndvZmYyJyxcbiAgJ29wZW4tc2Fucy1sYXRpbi13ZHRoLXtub3JtYWwsaXRhbGljfS0qLndvZmYyJyxcbiAgJ2pldGJyYWlucy1tb25vLWxhdGluLXdnaHQte25vcm1hbCxpdGFsaWN9LSoud29mZjInLFxuXTtcblxuY29uc3Qgdml0ZUNvbmZpZzogVml0ZUNvbmZpZyA9IHtcbiAgbG9nTGV2ZWw6ICdpbmZvJyxcbiAgbW9kZSxcbiAgcm9vdDogdGhpc0RpcixcbiAgY2FjaGVEaXI6IHBhdGguam9pbih0aGlzRGlyLCAnYnVpbGQvdml0ZS9jYWNoZScpLFxuICBwbHVnaW5zOiBbXG4gICAgcmVhY3QoKSxcbiAgICBsZXplcigpLFxuICAgIHN2Z3IoKSxcbiAgICBwcmVsb2FkRm9udHNWaXRlUGx1Z2luKGZvbnRzR2xvYiksXG4gICAgbWluaWZ5SFRNTFZpdGVQbHVnaW4oKSxcbiAgICBncmFwaHZpelVNRFZpdGVQbHVnaW4oKSxcbiAgICBWaXRlUFdBKHtcbiAgICAgIHN0cmF0ZWdpZXM6ICdnZW5lcmF0ZVNXJyxcbiAgICAgIHJlZ2lzdGVyVHlwZTogJ3Byb21wdCcsXG4gICAgICBpbmplY3RSZWdpc3RlcjogbnVsbCxcbiAgICAgIHdvcmtib3g6IHtcbiAgICAgICAgZ2xvYlBhdHRlcm5zOiBbJyoqLyoue2NzcyxodG1sLGpzfScsIC4uLmZvbnRzR2xvYl0sXG4gICAgICAgIGRvbnRDYWNoZUJ1c3RVUkxzTWF0Y2hpbmc6IC9cXC4oPzpjc3N8anN8d29mZjI/KSQvLFxuICAgICAgICBuYXZpZ2F0ZUZhbGxiYWNrRGVueWxpc3Q6IFtcbiAgICAgICAgICBuZXcgUmVnRXhwKGBeXFxcXC8ke0FQSV9FTkRQT0lOVH0kYCksXG4gICAgICAgICAgbmV3IFJlZ0V4cChgXlxcXFwvJHtYVEVYVF9FTkRQT0lOVH0kYCksXG4gICAgICAgICAgbmV3IFJlZ0V4cChgXlxcXFwvJHtDSEFUX0VORFBPSU5UfSRgKSxcbiAgICAgICAgXSxcbiAgICAgICAgcnVudGltZUNhY2hpbmc6IFtcbiAgICAgICAgICB7XG4gICAgICAgICAgICB1cmxQYXR0ZXJuOiBDT05GSUdfRU5EUE9JTlQsXG4gICAgICAgICAgICBoYW5kbGVyOiAnU3RhbGVXaGlsZVJldmFsaWRhdGUnLFxuICAgICAgICAgIH0sXG4gICAgICAgIF0sXG4gICAgICB9LFxuICAgICAgaW5jbHVkZUFzc2V0czogWydhcHBsZS10b3VjaC1pY29uLnBuZycsICdmYXZpY29uLnN2ZyddLFxuICAgICAgbWFuaWZlc3QsXG4gICAgfSksXG4gICAgZGV2TW9kZVBsdWdpbnMsXG4gIF0sXG4gIGJhc2U6ICcnLFxuICBkZWZpbmU6IHtcbiAgICBfX0RFVl9fOiBKU09OLnN0cmluZ2lmeShpc0RldmVsb3BtZW50KSwgLy8gRm9yIE1vYlhcbiAgfSxcbiAgcmVzb2x2ZToge1xuICAgIGFsaWFzOiB7XG4gICAgICAnQHRvb2xzLnJlZmluZXJ5L2NsaWVudC9jaGF0JzogcGF0aC5qb2luKFxuICAgICAgICB0aGlzRGlyLFxuICAgICAgICAnLi4vY2xpZW50LWpzLycsXG4gICAgICAgIGlzRGV2ZWxvcG1lbnQgPyAnc3JjL2NoYXQvaW5kZXgudHMnIDogJ2Rpc3QvY2hhdC5tanMnLFxuICAgICAgKSxcbiAgICAgICdAdG9vbHMucmVmaW5lcnkvY2xpZW50JzogcGF0aC5qb2luKFxuICAgICAgICB0aGlzRGlyLFxuICAgICAgICAnLi4vY2xpZW50LWpzLycsXG4gICAgICAgIGlzRGV2ZWxvcG1lbnQgPyAnc3JjL2luZGV4LnRzJyA6ICdkaXN0L2luZGV4Lm1qcycsXG4gICAgICApLFxuICAgIH0sXG4gIH0sXG4gIGJ1aWxkOiB7XG4gICAgYXNzZXRzRGlyOiAnLicsXG4gICAgLy8gSWYgd2UgZG9uJ3QgY29udHJvbCBpbmxpbmluZyBtYW51YWxseSwgd2ViIGZvbnRzIHdpbGwgYmUgcmFuZG9tbHkgaW5saW5lZFxuICAgIC8vIGludG8gdGhlIENTUywgd2hpY2ggZGVncmFkZXMgcGVyZm9ybWFuY2UuXG4gICAgYXNzZXRzSW5saW5lTGltaXQ6IDAsXG4gICAgb3V0RGlyOiBwYXRoLmpvaW4oJ2J1aWxkL3ZpdGUnLCBtb2RlKSxcbiAgICBlbXB0eU91dERpcjogdHJ1ZSxcbiAgICBzb3VyY2VtYXA6IGlzRGV2ZWxvcG1lbnQsXG4gICAgbWluaWZ5OiAhaXNEZXZlbG9wbWVudCxcbiAgICByb2xsdXBPcHRpb25zOiB7XG4gICAgICBvdXRwdXQ6IHtcbiAgICAgICAgY2h1bmtGaWxlTmFtZXM6ICh7IGlzRHluYW1pY0VudHJ5LCBpc0VudHJ5IH0pID0+XG4gICAgICAgICAgaXNEeW5hbWljRW50cnkgfHwgaXNFbnRyeSA/ICdbbmFtZV0tW2hhc2hdLmpzJyA6ICdbaGFzaF0uanMnLFxuICAgICAgICBleHBlcmltZW50YWxNaW5DaHVua1NpemU6IDIwICogMTAyNCxcbiAgICAgIH0sXG4gICAgfSxcbiAgfSxcbiAgc2VydmVyOiBzZXJ2ZXJPcHRpb25zLFxufTtcblxuZXhwb3J0IGRlZmF1bHQgZGVmaW5lQ29uZmlnKGFzeW5jICgpID0+IHtcbiAgYXdhaXQgZmV0Y2hQYWNrYWdlTWV0YWRhdGEodGhpc0Rpcik7XG4gIHJldHVybiB2aXRlQ29uZmlnO1xufSk7XG4iLCAiY29uc3QgX192aXRlX2luamVjdGVkX29yaWdpbmFsX2Rpcm5hbWUgPSBcIi9ob21lL2ttb25vL3JlZmluZXJ5L3N1YnByb2plY3RzL2Zyb250ZW5kL3NyYy94dGV4dFwiO2NvbnN0IF9fdml0ZV9pbmplY3RlZF9vcmlnaW5hbF9maWxlbmFtZSA9IFwiL2hvbWUva21vbm8vcmVmaW5lcnkvc3VicHJvamVjdHMvZnJvbnRlbmQvc3JjL3h0ZXh0L0JhY2tlbmRDb25maWcudHNcIjtjb25zdCBfX3ZpdGVfaW5qZWN0ZWRfb3JpZ2luYWxfaW1wb3J0X21ldGFfdXJsID0gXCJmaWxlOi8vL2hvbWUva21vbm8vcmVmaW5lcnkvc3VicHJvamVjdHMvZnJvbnRlbmQvc3JjL3h0ZXh0L0JhY2tlbmRDb25maWcudHNcIjsvKlxuICogU1BEWC1GaWxlQ29weXJpZ2h0VGV4dDogMjAyMS0yMDIzIFRoZSBSZWZpbmVyeSBBdXRob3JzIDxodHRwczovL3JlZmluZXJ5LnRvb2xzLz5cbiAqXG4gKiBTUERYLUxpY2Vuc2UtSWRlbnRpZmllcjogRVBMLTIuMFxuICovXG5cbi8qIGVzbGludC1kaXNhYmxlIEB0eXBlc2NyaXB0LWVzbGludC9uby1yZWRlY2xhcmUgLS0gRGVjbGFyZSB0eXBlcyB3aXRoIHRoZWlyIGNvbXBhbmlvbiBvYmplY3RzICovXG5cbmltcG9ydCB7IHogfSBmcm9tICd6b2QvdjQnO1xuXG5leHBvcnQgY29uc3QgRU5EUE9JTlQgPSAnY29uZmlnLmpzb24nO1xuXG5jb25zdCBCYWNrZW5kQ29uZmlnID0gei5vYmplY3Qoe1xuICBhcGlCYXNlOiB6LnVybCgpLm9wdGlvbmFsKCksXG4gIHdlYlNvY2tldFVSTDogei51cmwoKS5vcHRpb25hbCgpLFxuICBjaGF0VVJMOiB6LnVybCgpLm9wdGlvbmFsKCksXG59KTtcblxudHlwZSBCYWNrZW5kQ29uZmlnID0gei5pbmZlcjx0eXBlb2YgQmFja2VuZENvbmZpZz47XG5cbmV4cG9ydCBkZWZhdWx0IEJhY2tlbmRDb25maWc7XG4iLCAiY29uc3QgX192aXRlX2luamVjdGVkX29yaWdpbmFsX2Rpcm5hbWUgPSBcIi9ob21lL2ttb25vL3JlZmluZXJ5L3N1YnByb2plY3RzL2Zyb250ZW5kL2NvbmZpZ1wiO2NvbnN0IF9fdml0ZV9pbmplY3RlZF9vcmlnaW5hbF9maWxlbmFtZSA9IFwiL2hvbWUva21vbm8vcmVmaW5lcnkvc3VicHJvamVjdHMvZnJvbnRlbmQvY29uZmlnL2JhY2tlbmRDb25maWdWaXRlUGx1Z2luLnRzXCI7Y29uc3QgX192aXRlX2luamVjdGVkX29yaWdpbmFsX2ltcG9ydF9tZXRhX3VybCA9IFwiZmlsZTovLy9ob21lL2ttb25vL3JlZmluZXJ5L3N1YnByb2plY3RzL2Zyb250ZW5kL2NvbmZpZy9iYWNrZW5kQ29uZmlnVml0ZVBsdWdpbi50c1wiOy8qXG4gKiBTUERYLUZpbGVDb3B5cmlnaHRUZXh0OiAyMDIxLTIwMjMgVGhlIFJlZmluZXJ5IEF1dGhvcnMgPGh0dHBzOi8vcmVmaW5lcnkudG9vbHMvPlxuICpcbiAqIFNQRFgtTGljZW5zZS1JZGVudGlmaWVyOiBFUEwtMi4wXG4gKi9cblxuaW1wb3J0IHR5cGUgeyBQbHVnaW5PcHRpb24gfSBmcm9tICd2aXRlJztcblxuaW1wb3J0IEJhY2tlbmRDb25maWcsIHsgRU5EUE9JTlQgfSBmcm9tICcuLi9zcmMveHRleHQvQmFja2VuZENvbmZpZyc7XG5cbmV4cG9ydCBkZWZhdWx0IGZ1bmN0aW9uIGJhY2tlbmRDb25maWdWaXRlUGx1Z2luKFxuICBiYWNrZW5kQ29uZmlnOiBCYWNrZW5kQ29uZmlnLFxuKTogUGx1Z2luT3B0aW9uIHtcbiAgcmV0dXJuIHtcbiAgICBuYW1lOiAnYmFja2VuZC1jb25maWcnLFxuICAgIGFwcGx5OiAnc2VydmUnLFxuICAgIGNvbmZpZ3VyZVNlcnZlcihzZXJ2ZXIpIHtcbiAgICAgIGNvbnN0IGNvbmZpZyA9IEpTT04uc3RyaW5naWZ5KGJhY2tlbmRDb25maWcpO1xuICAgICAgc2VydmVyLm1pZGRsZXdhcmVzLnVzZSgocmVxLCByZXMsIG5leHQpID0+IHtcbiAgICAgICAgaWYgKHJlcS51cmwgPT09IGAvJHtFTkRQT0lOVH1gKSB7XG4gICAgICAgICAgcmVzLnNldEhlYWRlcignQ29udGVudC1UeXBlJywgJ2FwcGxpY2F0aW9uL2pzb24nKTtcbiAgICAgICAgICByZXMuZW5kKGNvbmZpZyk7XG4gICAgICAgIH0gZWxzZSB7XG4gICAgICAgICAgbmV4dCgpO1xuICAgICAgICB9XG4gICAgICB9KTtcbiAgICB9LFxuICB9O1xufVxuXG5leHBvcnQgdHlwZSB7IGRlZmF1bHQgYXMgQmFja2VuZENvbmZpZyB9IGZyb20gJy4uL3NyYy94dGV4dC9CYWNrZW5kQ29uZmlnJztcbmV4cG9ydCB7IEVORFBPSU5UIGFzIENPTkZJR19FTkRQT0lOVCB9IGZyb20gJy4uL3NyYy94dGV4dC9CYWNrZW5kQ29uZmlnJztcbiIsICJjb25zdCBfX3ZpdGVfaW5qZWN0ZWRfb3JpZ2luYWxfZGlybmFtZSA9IFwiL2hvbWUva21vbm8vcmVmaW5lcnkvc3VicHJvamVjdHMvZnJvbnRlbmQvY29uZmlnXCI7Y29uc3QgX192aXRlX2luamVjdGVkX29yaWdpbmFsX2ZpbGVuYW1lID0gXCIvaG9tZS9rbW9uby9yZWZpbmVyeS9zdWJwcm9qZWN0cy9mcm9udGVuZC9jb25maWcvZGV0ZWN0RGV2TW9kZU9wdGlvbnMudHNcIjtjb25zdCBfX3ZpdGVfaW5qZWN0ZWRfb3JpZ2luYWxfaW1wb3J0X21ldGFfdXJsID0gXCJmaWxlOi8vL2hvbWUva21vbm8vcmVmaW5lcnkvc3VicHJvamVjdHMvZnJvbnRlbmQvY29uZmlnL2RldGVjdERldk1vZGVPcHRpb25zLnRzXCI7LypcbiAqIFNQRFgtRmlsZUNvcHlyaWdodFRleHQ6IDIwMjEtMjAyNSBUaGUgUmVmaW5lcnkgQXV0aG9ycyA8aHR0cHM6Ly9yZWZpbmVyeS50b29scy8+XG4gKlxuICogU1BEWC1MaWNlbnNlLUlkZW50aWZpZXI6IEVQTC0yLjBcbiAqL1xuXG5pbXBvcnQgdHlwZSB7IFBsdWdpbk9wdGlvbiwgU2VydmVyT3B0aW9ucyB9IGZyb20gJ3ZpdGUnO1xuXG5pbXBvcnQgYmFja2VuZENvbmZpZ1ZpdGVQbHVnaW4sIHtcbiAgdHlwZSBCYWNrZW5kQ29uZmlnLFxufSBmcm9tICcuL2JhY2tlbmRDb25maWdWaXRlUGx1Z2luJztcblxuZXhwb3J0IGNvbnN0IEFQSV9FTkRQT0lOVCA9ICdhcGknO1xuZXhwb3J0IGNvbnN0IFhURVhUX0VORFBPSU5UID0gJ3h0ZXh0LXNlcnZpY2UnO1xuZXhwb3J0IGNvbnN0IENIQVRfRU5EUE9JTlQgPSAnY2hhdCc7XG5cbmV4cG9ydCBpbnRlcmZhY2UgRGV2TW9kZU9wdGlvbnMge1xuICBtb2RlOiBzdHJpbmc7XG4gIGlzRGV2ZWxvcG1lbnQ6IGJvb2xlYW47XG4gIGRldk1vZGVQbHVnaW5zOiBQbHVnaW5PcHRpb25bXTtcbiAgc2VydmVyT3B0aW9uczogU2VydmVyT3B0aW9ucztcbn1cblxuaW50ZXJmYWNlIExpc3Rlbk9wdGlvbnMge1xuICBob3N0OiBzdHJpbmc7XG4gIHBvcnQ6IG51bWJlcjtcbiAgc2VjdXJlOiBib29sZWFuO1xufVxuXG5mdW5jdGlvbiBkZXRlY3RMaXN0ZW5PcHRpb25zKFxuICBuYW1lOiBzdHJpbmcsXG4gIGZhbGxiYWNrSG9zdDogc3RyaW5nLFxuICBmYWxsYmFja1BvcnQ6IG51bWJlcixcbik6IExpc3Rlbk9wdGlvbnMge1xuICBjb25zdCBob3N0ID0gcHJvY2Vzcy5lbnZbYFJFRklORVJZXyR7bmFtZX1fSE9TVGBdID8/IGZhbGxiYWNrSG9zdDtcbiAgY29uc3QgcmF3UG9ydCA9IHByb2Nlc3MuZW52W2BSRUZJTkVSWV8ke25hbWV9X1BPUlRgXTtcbiAgY29uc3QgcG9ydCA9IHJhd1BvcnQgPT09IHVuZGVmaW5lZCA/IGZhbGxiYWNrUG9ydCA6IHBhcnNlSW50KHJhd1BvcnQsIDEwKTtcbiAgY29uc3Qgc2VjdXJlID0gcG9ydCA9PT0gNDQzO1xuICByZXR1cm4geyBob3N0LCBwb3J0LCBzZWN1cmUgfTtcbn1cblxuZnVuY3Rpb24gbGlzdGVuVVJMKFxuICB7IGhvc3QsIHBvcnQsIHNlY3VyZSB9OiBMaXN0ZW5PcHRpb25zLFxuICBwcm90b2NvbCA9ICdodHRwJyxcbik6IHN0cmluZyB7XG4gIHJldHVybiBgJHtzZWN1cmUgPyBgJHtwcm90b2NvbH1zYCA6IHByb3RvY29sfTovLyR7aG9zdH06JHtwb3J0fWA7XG59XG5cbmV4cG9ydCBkZWZhdWx0IGZ1bmN0aW9uIGRldGVjdERldk1vZGVPcHRpb25zKCk6IERldk1vZGVPcHRpb25zIHtcbiAgY29uc3QgbW9kZSA9IHByb2Nlc3MuZW52WydNT0RFJ10gPz8gJ2RldmVsb3BtZW50JztcbiAgY29uc3QgaXNEZXZlbG9wbWVudCA9IG1vZGUgPT09ICdkZXZlbG9wbWVudCc7XG5cbiAgaWYgKCFpc0RldmVsb3BtZW50KSB7XG4gICAgcmV0dXJuIHtcbiAgICAgIG1vZGUsXG4gICAgICBpc0RldmVsb3BtZW50LFxuICAgICAgZGV2TW9kZVBsdWdpbnM6IFtdLFxuICAgICAgc2VydmVyT3B0aW9uczoge30sXG4gICAgfTtcbiAgfVxuXG4gIGNvbnN0IGxpc3RlbiA9IGRldGVjdExpc3Rlbk9wdGlvbnMoJ0xJU1RFTicsICdsb2NhbGhvc3QnLCAxMzEzKTtcbiAgLy8gTWFrZSBzdXJlIHdlIGFsd2F5cyB1c2UgSVB2NCB0byBjb25uZWN0IHRvIHRoZSBiYWNrZW5kLFxuICAvLyBiZWNhdXNlIGl0IGRvZXNuJ3QgbGlzdGVuIG9uIElQdjYuXG4gIGNvbnN0IGFwaSA9IGRldGVjdExpc3Rlbk9wdGlvbnMoJ0FQSScsICcxMjcuMC4wLjEnLCAxMzEyKTtcbiAgY29uc3QgYXBpVVJMID0gbGlzdGVuVVJMKGFwaSk7XG4gIGNvbnN0IGNoYXQgPSBkZXRlY3RMaXN0ZW5PcHRpb25zKCdDSEFUJywgJzEyNy4wLjAuMScsIDEzMTQpO1xuICBjb25zdCBjaGF0VVJMID0gbGlzdGVuVVJMKGNoYXQpO1xuICBjb25zdCBwdWJsaWNBZGRyZXNzID0gZGV0ZWN0TGlzdGVuT3B0aW9ucygnUFVCTElDJywgbGlzdGVuLmhvc3QsIGxpc3Rlbi5wb3J0KTtcbiAgY29uc3QgcHVibGljVVJMID0gbGlzdGVuVVJMKHB1YmxpY0FkZHJlc3MpO1xuXG4gIGlmIChsaXN0ZW4uc2VjdXJlKSB7XG4gICAgLy8gU2luY2Ugbm9kZWpzIDIwLCB3ZSdkIG5lZWQgdG8gcGFzcyBpbiBIVFRQUyBvcHRpb25zIG1hbnVhbGx5LlxuICAgIHRocm93IG5ldyBFcnJvcihgUHJldmlldyBvbiBzZWN1cmUgcG9ydCAke2xpc3Rlbi5wb3J0fSBpcyBub3Qgc3VwcG9ydGVkYCk7XG4gIH1cblxuICBjb25zdCBiYWNrZW5kQ29uZmlnOiBCYWNrZW5kQ29uZmlnID0ge1xuICAgIGFwaUJhc2U6IGAke3B1YmxpY1VSTH0vJHtBUElfRU5EUE9JTlR9L3YxYCxcbiAgICB3ZWJTb2NrZXRVUkw6IGAke2xpc3RlblVSTChwdWJsaWNBZGRyZXNzLCAnd3MnKX0vJHtYVEVYVF9FTkRQT0lOVH1gLFxuICAgIGNoYXRVUkw6IGAke3B1YmxpY1VSTH0vJHtDSEFUX0VORFBPSU5UfS92MWAsXG4gIH07XG5cbiAgcmV0dXJuIHtcbiAgICBtb2RlLFxuICAgIGlzRGV2ZWxvcG1lbnQsXG4gICAgZGV2TW9kZVBsdWdpbnM6IFtiYWNrZW5kQ29uZmlnVml0ZVBsdWdpbihiYWNrZW5kQ29uZmlnKV0sXG4gICAgc2VydmVyT3B0aW9uczoge1xuICAgICAgaG9zdDogbGlzdGVuLmhvc3QsXG4gICAgICBwb3J0OiBsaXN0ZW4ucG9ydCxcbiAgICAgIHN0cmljdFBvcnQ6IHRydWUsXG4gICAgICBoZWFkZXJzOiB7XG4gICAgICAgIC8vIEVuYWJsZSBzdHJpY3Qgb3JpZ2luIGlzb2xhdGlvbiwgc2VlIGUuZy4sXG4gICAgICAgIC8vIGh0dHBzOi8vZ2l0aHViLmNvbS92aXRlanMvdml0ZS9pc3N1ZXMvMzkwOSNpc3N1ZWNvbW1lbnQtMTA2NTg5Mzk1NlxuICAgICAgICAnQ3Jvc3MtT3JpZ2luLU9wZW5lci1Qb2xpY3knOiAnc2FtZS1vcmlnaW4nLFxuICAgICAgICAnQ3Jvc3MtT3JpZ2luLUVtYmVkZGVyLVBvbGljeSc6ICdyZXF1aXJlLWNvcnAnLFxuICAgICAgICAnQ3Jvc3MtT3JpZ2luLVJlc291cmNlLVBvbGljeSc6ICdjcm9zcy1vcmlnaW4nLFxuICAgICAgfSxcbiAgICAgIHByb3h5OiB7XG4gICAgICAgIFtgLyR7QVBJX0VORFBPSU5UfWBdOiB7XG4gICAgICAgICAgdGFyZ2V0OiBhcGlVUkwsXG4gICAgICAgICAgc2VjdXJlOiBhcGkuc2VjdXJlLFxuICAgICAgICB9LFxuICAgICAgICBbYC8ke1hURVhUX0VORFBPSU5UfWBdOiB7XG4gICAgICAgICAgdGFyZ2V0OiBhcGlVUkwsXG4gICAgICAgICAgd3M6IHRydWUsXG4gICAgICAgICAgc2VjdXJlOiBhcGkuc2VjdXJlLFxuICAgICAgICB9LFxuICAgICAgICBbYC8ke0NIQVRfRU5EUE9JTlR9YF06IHtcbiAgICAgICAgICB0YXJnZXQ6IGNoYXRVUkwsXG4gICAgICAgICAgc2VjdXJlOiBjaGF0LnNlY3VyZSxcbiAgICAgICAgfSxcbiAgICAgIH0sXG4gICAgICBobXI6IHtcbiAgICAgICAgaG9zdDogcHVibGljQWRkcmVzcy5ob3N0LFxuICAgICAgICBjbGllbnRQb3J0OiBwdWJsaWNBZGRyZXNzLnBvcnQsXG4gICAgICAgIHBhdGg6ICcvdml0ZScsXG4gICAgICB9LFxuICAgIH0sXG4gIH07XG59XG4iLCAiY29uc3QgX192aXRlX2luamVjdGVkX29yaWdpbmFsX2Rpcm5hbWUgPSBcIi9ob21lL2ttb25vL3JlZmluZXJ5L3N1YnByb2plY3RzL2Zyb250ZW5kL2NvbmZpZ1wiO2NvbnN0IF9fdml0ZV9pbmplY3RlZF9vcmlnaW5hbF9maWxlbmFtZSA9IFwiL2hvbWUva21vbm8vcmVmaW5lcnkvc3VicHJvamVjdHMvZnJvbnRlbmQvY29uZmlnL2ZldGNoUGFja2FnZU1ldGFkYXRhLnRzXCI7Y29uc3QgX192aXRlX2luamVjdGVkX29yaWdpbmFsX2ltcG9ydF9tZXRhX3VybCA9IFwiZmlsZTovLy9ob21lL2ttb25vL3JlZmluZXJ5L3N1YnByb2plY3RzL2Zyb250ZW5kL2NvbmZpZy9mZXRjaFBhY2thZ2VNZXRhZGF0YS50c1wiOy8qXG4gKiBTUERYLUZpbGVDb3B5cmlnaHRUZXh0OiAyMDIxLTIwMjMgVGhlIFJlZmluZXJ5IEF1dGhvcnMgPGh0dHBzOi8vcmVmaW5lcnkudG9vbHMvPlxuICpcbiAqIFNQRFgtTGljZW5zZS1JZGVudGlmaWVyOiBFUEwtMi4wXG4gKi9cblxuaW1wb3J0IHsgcmVhZEZpbGUgfSBmcm9tICdub2RlOmZzL3Byb21pc2VzJztcbmltcG9ydCBwYXRoIGZyb20gJ25vZGU6cGF0aCc7XG5cbmltcG9ydCB6IGZyb20gJ3pvZC92NCc7XG5cbmNvbnN0IFBhY2thZ2VJbmZvID0gei5vYmplY3Qoe1xuICBuYW1lOiB6LnN0cmluZygpLm1pbigxKSxcbiAgdmVyc2lvbjogei5zdHJpbmcoKS5taW4oMSksXG59KTtcblxuZXhwb3J0IGRlZmF1bHQgYXN5bmMgZnVuY3Rpb24gZmV0Y2hQYWNrYWdlTWV0YWRhdGEoXG4gIHRoaXNEaXI6IHN0cmluZyxcbik6IFByb21pc2U8dm9pZD4ge1xuICBjb25zdCBjb250ZW50cyA9IGF3YWl0IHJlYWRGaWxlKHBhdGguam9pbih0aGlzRGlyLCAncGFja2FnZS5qc29uJyksICd1dGY4Jyk7XG4gIGNvbnN0IHsgbmFtZTogcGFja2FnZU5hbWUsIHZlcnNpb246IHBhY2thZ2VWZXJzaW9uIH0gPSBQYWNrYWdlSW5mby5wYXJzZShcbiAgICBKU09OLnBhcnNlKGNvbnRlbnRzKSxcbiAgKTtcbiAgcHJvY2Vzcy5lbnZbJ1ZJVEVfUEFDS0FHRV9OQU1FJ10gPz89IHBhY2thZ2VOYW1lO1xuICBwcm9jZXNzLmVudlsnVklURV9QQUNLQUdFX1ZFUlNJT04nXSA/Pz0gcGFja2FnZVZlcnNpb247XG59XG4iLCAiY29uc3QgX192aXRlX2luamVjdGVkX29yaWdpbmFsX2Rpcm5hbWUgPSBcIi9ob21lL2ttb25vL3JlZmluZXJ5L3N1YnByb2plY3RzL2Zyb250ZW5kL2NvbmZpZ1wiO2NvbnN0IF9fdml0ZV9pbmplY3RlZF9vcmlnaW5hbF9maWxlbmFtZSA9IFwiL2hvbWUva21vbm8vcmVmaW5lcnkvc3VicHJvamVjdHMvZnJvbnRlbmQvY29uZmlnL2dyYXBodml6VU1EVml0ZVBsdWdpbi50c1wiO2NvbnN0IF9fdml0ZV9pbmplY3RlZF9vcmlnaW5hbF9pbXBvcnRfbWV0YV91cmwgPSBcImZpbGU6Ly8vaG9tZS9rbW9uby9yZWZpbmVyeS9zdWJwcm9qZWN0cy9mcm9udGVuZC9jb25maWcvZ3JhcGh2aXpVTURWaXRlUGx1Z2luLnRzXCI7LypcbiAqIFNQRFgtRmlsZUNvcHlyaWdodFRleHQ6IDIwMjMgVGhlIFJlZmluZXJ5IEF1dGhvcnMgPGh0dHBzOi8vcmVmaW5lcnkudG9vbHMvPlxuICpcbiAqIFNQRFgtTGljZW5zZS1JZGVudGlmaWVyOiBFUEwtMi4wXG4gKi9cblxuaW1wb3J0IHsgcmVhZEZpbGUgfSBmcm9tICdub2RlOmZzL3Byb21pc2VzJztcbmltcG9ydCBwYXRoIGZyb20gJ25vZGU6cGF0aCc7XG5cbmltcG9ydCBwbnBhcGkgZnJvbSAncG5wYXBpJztcbmltcG9ydCB0eXBlIHsgUGx1Z2luT3B0aW9uLCBSZXNvbHZlZENvbmZpZyB9IGZyb20gJ3ZpdGUnO1xuXG4vLyBVc2UgYSBDSlMgZmlsZSBhcyB0aGUgUG5QIHJlc29sdXRpb24gaXNzdWVyIHRvIGZvcmNlIHJlc29sdXRpb24gdG8gYSBub24tRVNNIGV4cG9ydC5cbmNvbnN0IGlzc3VlckZpbGVOYW1lID0gJ3dvcmtlci5janMnO1xuXG5leHBvcnQgZGVmYXVsdCBmdW5jdGlvbiBncmFwaHZpelVNRFZpdGVQbHVnaW4oKTogUGx1Z2luT3B0aW9uIHtcbiAgbGV0IGNvbW1hbmQ6IFJlc29sdmVkQ29uZmlnWydjb21tYW5kJ10gPSAnYnVpbGQnO1xuICBsZXQgcm9vdDogc3RyaW5nIHwgdW5kZWZpbmVkO1xuICBsZXQgdXJsOiBzdHJpbmcgfCB1bmRlZmluZWQ7XG5cbiAgcmV0dXJuIHtcbiAgICBuYW1lOiAnZ3JhcGh2aXotdW1kJyxcbiAgICBlbmZvcmNlOiAncG9zdCcsXG4gICAgY29uZmlnUmVzb2x2ZWQoY29uZmlnKSB7XG4gICAgICAoeyBjb21tYW5kLCByb290IH0gPSBjb25maWcpO1xuICAgIH0sXG4gICAgYXN5bmMgYnVpbGRTdGFydCgpIHtcbiAgICAgIGNvbnN0IGlzc3VlciA9XG4gICAgICAgIHJvb3QgPT09IHVuZGVmaW5lZCA/IGlzc3VlckZpbGVOYW1lIDogcGF0aC5qb2luKHJvb3QsIGlzc3VlckZpbGVOYW1lKTtcbiAgICAgIC8vIFNpbmNlIGh0dHBzOi8vZ2l0aHViLmNvbS9ocGNjLXN5c3RlbXMvaHBjYy1qcy13YXNtL2NvbW1pdC8xNWUxYWNlNWVkYWU3Zjk0NzE0ZTU0N2EzYWMyMGUwZTE3Y2Q2YjBjLFxuICAgICAgLy8gaHBjYy1qcyBoYXMgYm90aCBhIGAuY2pzYCBhbmQgYSBgLnVtZC5qc2AgYnVpbGQuIFBuUEFQSSB3aWxsIGZpbmQgdGhlIGZvcm1lciwgYnV0IHdlIG5lZWQgdGhlIGxhdHRlci5cbiAgICAgIGNvbnN0IHJlc29sdmVkUGF0aCA9IHBucGFwaVxuICAgICAgICAucmVzb2x2ZVJlcXVlc3QoJ0BocGNjLWpzL3dhc20vZ3JhcGh2aXonLCBpc3N1ZXIpXG4gICAgICAgID8ucmVwbGFjZSgvXFwuY2pzJC8sICcudW1kLmpzJyk7XG4gICAgICBpZiAocmVzb2x2ZWRQYXRoID09PSB1bmRlZmluZWQpIHtcbiAgICAgICAgcmV0dXJuO1xuICAgICAgfVxuICAgICAgaWYgKGNvbW1hbmQgPT09ICdzZXJ2ZScpIHtcbiAgICAgICAgdXJsID0gYC9AZnMvJHtyZXNvbHZlZFBhdGh9YDtcbiAgICAgIH0gZWxzZSB7XG4gICAgICAgIGNvbnN0IGNvbnRlbnQgPSBhd2FpdCByZWFkRmlsZShyZXNvbHZlZFBhdGgsIG51bGwpO1xuICAgICAgICB1cmwgPSB0aGlzLmVtaXRGaWxlKHtcbiAgICAgICAgICBuYW1lOiBwYXRoLmJhc2VuYW1lKHJlc29sdmVkUGF0aCksXG4gICAgICAgICAgdHlwZTogJ2Fzc2V0JyxcbiAgICAgICAgICBzb3VyY2U6IGNvbnRlbnQsXG4gICAgICAgIH0pO1xuICAgICAgfVxuICAgIH0sXG4gICAgcmVuZGVyU3RhcnQoKSB7XG4gICAgICBpZiAodXJsICE9PSB1bmRlZmluZWQgJiYgY29tbWFuZCAhPT0gJ3NlcnZlJykge1xuICAgICAgICB1cmwgPSB0aGlzLmdldEZpbGVOYW1lKHVybCk7XG4gICAgICB9XG4gICAgfSxcbiAgICB0cmFuc2Zvcm1JbmRleEh0bWwoKSB7XG4gICAgICBpZiAodXJsID09PSB1bmRlZmluZWQpIHtcbiAgICAgICAgcmV0dXJuIHVuZGVmaW5lZDtcbiAgICAgIH1cbiAgICAgIHJldHVybiBbXG4gICAgICAgIHtcbiAgICAgICAgICB0YWc6ICdzY3JpcHQnLFxuICAgICAgICAgIGF0dHJzOiB7XG4gICAgICAgICAgICBzcmM6IHVybCxcbiAgICAgICAgICAgIHR5cGU6ICdqYXZhc2NyaXB0L3dvcmtlcicsXG4gICAgICAgICAgfSxcbiAgICAgICAgICBpbmplY3RUbzogJ2hlYWQnLFxuICAgICAgICB9LFxuICAgICAgXTtcbiAgICB9LFxuICB9O1xufVxuIiwgImNvbnN0IF9fdml0ZV9pbmplY3RlZF9vcmlnaW5hbF9kaXJuYW1lID0gXCIvaG9tZS9rbW9uby9yZWZpbmVyeS9zdWJwcm9qZWN0cy9mcm9udGVuZC9jb25maWdcIjtjb25zdCBfX3ZpdGVfaW5qZWN0ZWRfb3JpZ2luYWxfZmlsZW5hbWUgPSBcIi9ob21lL2ttb25vL3JlZmluZXJ5L3N1YnByb2plY3RzL2Zyb250ZW5kL2NvbmZpZy9tYW5pZmVzdC50c1wiO2NvbnN0IF9fdml0ZV9pbmplY3RlZF9vcmlnaW5hbF9pbXBvcnRfbWV0YV91cmwgPSBcImZpbGU6Ly8vaG9tZS9rbW9uby9yZWZpbmVyeS9zdWJwcm9qZWN0cy9mcm9udGVuZC9jb25maWcvbWFuaWZlc3QudHNcIjsvKlxuICogU1BEWC1GaWxlQ29weXJpZ2h0VGV4dDogMjAyMS0yMDIzIFRoZSBSZWZpbmVyeSBBdXRob3JzIDxodHRwczovL3JlZmluZXJ5LnRvb2xzLz5cbiAqXG4gKiBTUERYLUxpY2Vuc2UtSWRlbnRpZmllcjogRVBMLTIuMFxuICovXG5cbmltcG9ydCB0eXBlIHsgTWFuaWZlc3RPcHRpb25zIH0gZnJvbSAndml0ZS1wbHVnaW4tcHdhJztcblxuY29uc3QgbWFuaWZlc3Q6IFBhcnRpYWw8TWFuaWZlc3RPcHRpb25zPiA9IHtcbiAgbGFuZzogJ2VuLVVTJyxcbiAgbmFtZTogJ1JlZmluZXJ5JyxcbiAgc2hvcnRfbmFtZTogJ1JlZmluZXJ5JyxcbiAgZGVzY3JpcHRpb246ICdBbiBlZmZpY2llbnQgZ3JhcGggc29sdmVyIGZvciBnZW5lcmF0aW5nIHdlbGwtZm9ybWVkIG1vZGVscycsXG4gIHRoZW1lX2NvbG9yOiAnI2Y1ZjVmNScsXG4gIGRpc3BsYXlfb3ZlcnJpZGU6IFsnd2luZG93LWNvbnRyb2xzLW92ZXJsYXknXSxcbiAgZGlzcGxheTogJ3N0YW5kYWxvbmUnLFxuICBiYWNrZ3JvdW5kX2NvbG9yOiAnIzIxMjUyYicsXG4gIGljb25zOiBbXG4gICAge1xuICAgICAgc3JjOiAnaWNvbi0xOTJ4MTkyLnBuZycsXG4gICAgICBzaXplczogJzE5MngxOTInLFxuICAgICAgdHlwZTogJ2ltYWdlL3BuZycsXG4gICAgICBwdXJwb3NlOiAnYW55IG1hc2thYmxlJyxcbiAgICB9LFxuICAgIHtcbiAgICAgIHNyYzogJ2ljb24tNTEyeDUxMi5wbmcnLFxuICAgICAgc2l6ZXM6ICc1MTJ4NTEyJyxcbiAgICAgIHR5cGU6ICdpbWFnZS9wbmcnLFxuICAgICAgcHVycG9zZTogJ2FueSBtYXNrYWJsZScsXG4gICAgfSxcbiAgICB7XG4gICAgICBzcmM6ICdpY29uLWFueS5zdmcnLFxuICAgICAgc2l6ZXM6ICdhbnknLFxuICAgICAgdHlwZTogJ2ltYWdlL3N2Zyt4bWwnLFxuICAgICAgcHVycG9zZTogJ2FueSBtYXNrYWJsZScsXG4gICAgfSxcbiAgICB7XG4gICAgICBzcmM6ICdtYXNrLWljb24uc3ZnJyxcbiAgICAgIHNpemVzOiAnYW55JyxcbiAgICAgIHR5cGU6ICdpbWFnZS9zdmcreG1sJyxcbiAgICAgIHB1cnBvc2U6ICdtb25vY2hyb21lJyxcbiAgICB9LFxuICBdLFxufTtcblxuZXhwb3J0IGRlZmF1bHQgbWFuaWZlc3Q7XG4iLCAiY29uc3QgX192aXRlX2luamVjdGVkX29yaWdpbmFsX2Rpcm5hbWUgPSBcIi9ob21lL2ttb25vL3JlZmluZXJ5L3N1YnByb2plY3RzL2Zyb250ZW5kL2NvbmZpZ1wiO2NvbnN0IF9fdml0ZV9pbmplY3RlZF9vcmlnaW5hbF9maWxlbmFtZSA9IFwiL2hvbWUva21vbm8vcmVmaW5lcnkvc3VicHJvamVjdHMvZnJvbnRlbmQvY29uZmlnL21pbmlmeUhUTUxWaXRlUGx1Z2luLnRzXCI7Y29uc3QgX192aXRlX2luamVjdGVkX29yaWdpbmFsX2ltcG9ydF9tZXRhX3VybCA9IFwiZmlsZTovLy9ob21lL2ttb25vL3JlZmluZXJ5L3N1YnByb2plY3RzL2Zyb250ZW5kL2NvbmZpZy9taW5pZnlIVE1MVml0ZVBsdWdpbi50c1wiOy8qXG4gKiBTUERYLUZpbGVDb3B5cmlnaHRUZXh0OiAyMDIxLTIwMjMgVGhlIFJlZmluZXJ5IEF1dGhvcnMgPGh0dHBzOi8vcmVmaW5lcnkudG9vbHMvPlxuICpcbiAqIFNQRFgtTGljZW5zZS1JZGVudGlmaWVyOiBFUEwtMi4wXG4gKi9cblxuaW1wb3J0IHsgbWluaWZ5LCB0eXBlIE9wdGlvbnMgYXMgVGVyc2VyT3B0aW9ucyB9IGZyb20gJ2h0bWwtbWluaWZpZXItdGVyc2VyJztcbmltcG9ydCB0eXBlIHsgUGx1Z2luT3B0aW9uIH0gZnJvbSAndml0ZSc7XG5cbmV4cG9ydCBkZWZhdWx0IGZ1bmN0aW9uIG1pbmlmeUhUTUxWaXRlUGx1Z2luKFxuICBvcHRpb25zPzogVGVyc2VyT3B0aW9ucyxcbik6IFBsdWdpbk9wdGlvbiB7XG4gIHJldHVybiB7XG4gICAgbmFtZTogJ21pbmlmeS1odG1sJyxcbiAgICBhcHBseTogJ2J1aWxkJyxcbiAgICBlbmZvcmNlOiAncG9zdCcsXG4gICAgdHJhbnNmb3JtSW5kZXhIdG1sKGh0bWwpIHtcbiAgICAgIHJldHVybiBtaW5pZnkoaHRtbCwge1xuICAgICAgICBjb2xsYXBzZVdoaXRlc3BhY2U6IHRydWUsXG4gICAgICAgIGNvbGxhcHNlQm9vbGVhbkF0dHJpYnV0ZXM6IHRydWUsXG4gICAgICAgIG1pbmlmeUNTUzogdHJ1ZSxcbiAgICAgICAgcmVtb3ZlQ29tbWVudHM6IHRydWUsXG4gICAgICAgIHJlbW92ZUF0dHJpYnV0ZVF1b3RlczogdHJ1ZSxcbiAgICAgICAgcmVtb3ZlUmVkdW5kYW50QXR0cmlidXRlczogdHJ1ZSxcbiAgICAgICAgc29ydEF0dHJpYnV0ZXM6IHRydWUsXG4gICAgICAgIC4uLihvcHRpb25zID8/IHt9KSxcbiAgICAgIH0pO1xuICAgIH0sXG4gIH07XG59XG4iLCAiY29uc3QgX192aXRlX2luamVjdGVkX29yaWdpbmFsX2Rpcm5hbWUgPSBcIi9ob21lL2ttb25vL3JlZmluZXJ5L3N1YnByb2plY3RzL2Zyb250ZW5kL2NvbmZpZ1wiO2NvbnN0IF9fdml0ZV9pbmplY3RlZF9vcmlnaW5hbF9maWxlbmFtZSA9IFwiL2hvbWUva21vbm8vcmVmaW5lcnkvc3VicHJvamVjdHMvZnJvbnRlbmQvY29uZmlnL3ByZWxvYWRGb250c1ZpdGVQbHVnaW4udHNcIjtjb25zdCBfX3ZpdGVfaW5qZWN0ZWRfb3JpZ2luYWxfaW1wb3J0X21ldGFfdXJsID0gXCJmaWxlOi8vL2hvbWUva21vbm8vcmVmaW5lcnkvc3VicHJvamVjdHMvZnJvbnRlbmQvY29uZmlnL3ByZWxvYWRGb250c1ZpdGVQbHVnaW4udHNcIjsvKlxuICogU1BEWC1GaWxlQ29weXJpZ2h0VGV4dDogMjAyMS0yMDIzIFRoZSBSZWZpbmVyeSBBdXRob3JzIDxodHRwczovL3JlZmluZXJ5LnRvb2xzLz5cbiAqXG4gKiBTUERYLUxpY2Vuc2UtSWRlbnRpZmllcjogRVBMLTIuMFxuICovXG5cbmltcG9ydCBtaWNyb21hdGNoIGZyb20gJ21pY3JvbWF0Y2gnO1xuaW1wb3J0IHR5cGUgeyBQbHVnaW5PcHRpb24gfSBmcm9tICd2aXRlJztcblxuZXhwb3J0IGRlZmF1bHQgZnVuY3Rpb24gcHJlbG9hZEZvbnRzVml0ZVBsdWdpbihcbiAgZm9udHNHbG9iOiBzdHJpbmcgfCBzdHJpbmdbXSxcbik6IFBsdWdpbk9wdGlvbiB7XG4gIHJldHVybiB7XG4gICAgbmFtZTogJ3JlZmluZXJ5LXByZWxvYWQtZm9udHMnLFxuICAgIGFwcGx5OiAnYnVpbGQnLFxuICAgIGVuZm9yY2U6ICdwb3N0JyxcbiAgICB0cmFuc2Zvcm1JbmRleEh0bWwoX2h0bWwsIHsgYnVuZGxlIH0pIHtcbiAgICAgIHJldHVybiBtaWNyb21hdGNoKE9iamVjdC5rZXlzKGJ1bmRsZSA/PyB7fSksIGZvbnRzR2xvYikubWFwKChocmVmKSA9PiAoe1xuICAgICAgICB0YWc6ICdsaW5rJyxcbiAgICAgICAgYXR0cnM6IHtcbiAgICAgICAgICBocmVmLFxuICAgICAgICAgIHJlbDogJ3ByZWxvYWQnLFxuICAgICAgICAgIHR5cGU6ICdmb250L3dvZmYyJyxcbiAgICAgICAgICBhczogJ2ZvbnQnLFxuICAgICAgICAgIGNyb3Nzb3JpZ2luOiAnYW5vbnltb3VzJyxcbiAgICAgICAgfSxcbiAgICAgIH0pKTtcbiAgICB9LFxuICB9O1xufVxuIl0sCiAgIm1hcHBpbmdzIjogIjtBQU1BLE9BQU9BLFdBQVU7QUFDakIsU0FBUyxxQkFBcUI7QUFFOUIsU0FBUyxhQUFhO0FBQ3RCLE9BQU8sV0FBVztBQUNsQixTQUFTLG9CQUFtRDtBQUM1RCxTQUFTLGVBQWU7QUFDeEIsT0FBTyxVQUFVOzs7QUNMakIsU0FBUyxTQUFTO0FBRVgsSUFBTSxXQUFXO0FBRXhCLElBQU0sZ0JBQWdCLEVBQUUsT0FBTztBQUFBLEVBQzdCLFNBQVMsRUFBRSxJQUFJLEVBQUUsU0FBUztBQUFBLEVBQzFCLGNBQWMsRUFBRSxJQUFJLEVBQUUsU0FBUztBQUFBLEVBQy9CLFNBQVMsRUFBRSxJQUFJLEVBQUUsU0FBUztBQUM1QixDQUFDOzs7QUNOYyxTQUFSLHdCQUNMLGVBQ2M7QUFDZCxTQUFPO0FBQUEsSUFDTCxNQUFNO0FBQUEsSUFDTixPQUFPO0FBQUEsSUFDUCxnQkFBZ0IsUUFBUTtBQUN0QixZQUFNLFNBQVMsS0FBSyxVQUFVLGFBQWE7QUFDM0MsYUFBTyxZQUFZLElBQUksQ0FBQyxLQUFLLEtBQUssU0FBUztBQUN6QyxZQUFJLElBQUksUUFBUSxJQUFJLFFBQVEsSUFBSTtBQUM5QixjQUFJLFVBQVUsZ0JBQWdCLGtCQUFrQjtBQUNoRCxj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O0FBQUEsUUFDQTtBQUFBLFFBQ0EsZ0JBQWdCLGlCQUFpQjtBQUFBLE1BQ25DO0FBQUEsSUFDRjtBQUFBLEVBQ0Y7QUFBQSxFQUNBLE9BQU87QUFBQSxJQUNMLFdBQVc7QUFBQTtBQUFBO0FBQUEsSUFHWCxtQkFBbUI7QUFBQSxJQUNuQixRQUFRQSxNQUFLLEtBQUssY0FBYyxJQUFJO0FBQUEsSUFDcEMsYUFBYTtBQUFBLElBQ2IsV0FBVztBQUFBLElBQ1gsUUFBUSxDQUFDO0FBQUEsSUFDVCxlQUFlO0FBQUEsTUFDYixRQUFRO0FBQUEsUUFDTixnQkFBZ0IsQ0FBQyxFQUFFLGdCQUFnQixRQUFRLE1BQ3pDLGtCQUFrQixVQUFVLHFCQUFxQjtBQUFBLFFBQ25ELDBCQUEwQixLQUFLO0FBQUEsTUFDakM7QUFBQSxJQUNGO0FBQUEsRUFDRjtBQUFBLEVBQ0EsUUFBUTtBQUNWO0FBRUEsSUFBTyxzQkFBUSxhQUFhLFlBQVk7QUFDdEMsUUFBTSxxQkFBcUIsT0FBTztBQUNsQyxTQUFPO0FBQ1QsQ0FBQzsiLAogICJuYW1lcyI6IFsicGF0aCIsICJtb2RlIiwgImlzRGV2ZWxvcG1lbnQiLCAieiIsICJ6IiwgInRoaXNEaXIiLCAicmVhZEZpbGUiLCAicGF0aCIsICJwYXRoIiwgInJlYWRGaWxlIiwgImZvbnRzR2xvYiIsICJwYXRoIl0KfQo=
