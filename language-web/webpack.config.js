const fs = require('fs');
const path = require('path');

const { DefinePlugin } = require('webpack');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const HtmlWebpackInjectPreload = require('@principalstudio/html-webpack-inject-preload');
const MiniCssExtractPlugin = require('mini-css-extract-plugin');
const { SubresourceIntegrityPlugin } = require('webpack-subresource-integrity');

const packageInfo = require('./package.json');

const currentNodeEnv = process.env.NODE_ENV || 'development';
const devMode = currentNodeEnv !== 'production';
const outputPath = path.resolve(__dirname, 'build/webpack', currentNodeEnv);

const portNumberOrElse = (envName, fallback) => {
  const value = process.env[envName];
  return value ? parseInt(value) : fallback;
};
const listenHost = process.env['LISTEN_HOST'] || 'localhost';
const listenPort = portNumberOrElse('LISTEN_PORT', 1313);
const apiHost = process.env['API_HOST'] || listenHost;
const apiPort = portNumberOrElse('API_PORT', 1312);
const publicHost = process.env['PUBLIC_HOST'] || listenHost;
const publicPort = portNumberOrElse('PUBLIC_PORT', listenPort);

const resolveSources = sources => path.resolve(__dirname, 'src', sources);
const mainJsSources = resolveSources('main/js');
const babelLoaderFilters = {
  include: [mainJsSources],
  exclude: [resolveSources('main/js/xtext')],
};
const babelPresets = [
  [
    '@babel/preset-env',
    {
      targets: 'defaults',
    },
  ],
  '@babel/preset-react',
];
const babelPlugins = [
  '@babel/plugin-transform-runtime',
]
const magicCommentsLoader = {
  loader: 'magic-comments-loader',
  options: {
    webpackChunkName: true,
  }
};

module.exports = {
  mode: devMode ? 'development' : 'production',
  entry: './src/main/js',
  output: {
    path: outputPath,
    publicPath: '/',
    filename: devMode ? '[name].js' : '[name].[contenthash].js',
    chunkFilename: devMode ? '[name].js' : '[name].[contenthash].js',
    assetModuleFilename: devMode ? '[name].js' : '[name].[contenthash][ext]',
    clean: true,
    crossOriginLoading: 'anonymous',
  },
  module: {
    rules: [
      {
        test: /\.jsx?$/i,
        ...babelLoaderFilters,
        use: [
          {
            loader: 'babel-loader',
            options: {
              presets: babelPresets,
              plugins: [
                [
                  '@babel/plugin-proposal-class-properties',
                  {
                    loose: false,
                  },
                  ...babelPlugins,
                ],
              ],
              assumptions: {
                'setPublicClassFields': false,
              },
            },
          },
          magicCommentsLoader,
        ],
      },
      {
        test: /.tsx?$/i,
        ...babelLoaderFilters,
        use: [
          {
            loader: 'babel-loader',
            options: {
              presets: [
                ...babelPresets,
                [
                  '@babel/preset-typescript',
                  {
                    isTSX: true,
                    allExtensions: true,
                    allowDeclareFields: true,
                    onlyRemoveTypeImports: true,
                    optimizeConstEnums: true,
                  },
                ]
              ],
              plugins: babelPlugins,
            },
          },
          magicCommentsLoader,
        ],
      },
      {
        test: /\.scss$/i,
        use: [
          devMode ? 'style-loader' : MiniCssExtractPlugin.loader,
          'css-loader',
          {
            loader: 'sass-loader',
            options: {
              implementation: require.resolve('sass'),
            },
          },
        ],
      },
      {
        test: /\.(gif|png|jpe?g|svg?)$/i,
        use: [
          {
            loader: 'image-webpack-loader',
            options: {
              disable: true,
            }
          },
        ],
        type: 'asset',
      },
      {
        test: /\.woff2?$/i,
        type: 'asset/resource',
      },
    ],
  },
  resolve: {
    modules: [
      'node_modules',
      mainJsSources,
    ],
    extensions: ['.js', '.jsx', '.ts', '.tsx'],
    alias: {
      images: resolveSources('main/images'),
    },
  },
  devtool: devMode ? 'inline-source-map' : 'source-map',
  optimization: {
    providedExports: !devMode,
    sideEffects: devMode ? 'flag' : true,
    splitChunks: {
      chunks: 'all',
      cacheGroups: {
        defaultVendors: {
          test: /[\\/]node_modules[\\/]/,
          priority: -10,
          reuseExistingChunk: true,
          filename: devMode ? 'vendor.[id].js' : 'vendor.[contenthash].js',
        },
        default: {
          minChunks: 2,
          priority: -20,
          reuseExistingChunk: true,
        },
      },
    },
  },
  devServer: {
    client: {
      logging: 'info',
      overlay: true,
      progress: true,
      webSocketURL: {
        hostname: publicHost,
        port: publicPort,
        protocol: publicPort === 443 ? 'wss' : 'ws',
      },
    },
    compress: true,
    host: listenHost,
    port: listenPort,
    proxy: {
      '/xtext-service': {
        target: `${apiPort === 443 ? 'https' : 'http'}://${apiHost}:${apiPort}`,
        ws: true,
      },
    },
  },
  plugins: [
    new DefinePlugin({
      'DEBUG': JSON.stringify(devMode),
      'PACKAGE_NAME': JSON.stringify(packageInfo.name),
      'PACKAGE_VERSION': JSON.stringify(packageInfo.version),
    }),
    new MiniCssExtractPlugin({
      filename: '[name].[contenthash].css',
      chunkFilename: '[name].[contenthash].css',
    }),
    new SubresourceIntegrityPlugin(),
    new HtmlWebpackPlugin({
      template: 'src/main/html/index.html',
      minify: devMode ? false : {
        collapseWhitespace: true,
        removeComments: true,
        removeOptionalTags: true,
        removeRedundantAttributes: true,
        removeScriptTypeAttributes: true,
        removeStyleLinkTypeAttributes: true,
        useShortDoctype: true,
      },
    }),
    new HtmlWebpackInjectPreload({
      files: [
        {
          match: /(roboto-latin-(400|500)-normal|jetbrains-mono-latin-variable).*\.woff2/,
          attributes: {
            as: 'font',
            type: 'font/woff2',
            crossorigin: 'anonymous',
          },
        },
      ],
    }),
  ],
};
