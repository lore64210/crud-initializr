const webpack = require("webpack");
const MiniCssExtractPlugin = require("mini-css-extract-plugin");
const CssMinimizerPlugin = require("css-minimizer-webpack-plugin");
const CopyPlugin = require("copy-webpack-plugin");

module.exports = (env, argv) => {
  const isProduction = (argv && argv.mode) === "production";

  return {
    resolve: {
      extensions: [".js", ".jsx", ".css"],
    },
    entry: ["./src/index.js", "./src/css/app.css"],
    devtool: isProduction ? false : "eval",
    cache: true,
    mode: isProduction ? "production" : "development",
    output: {
      path: __dirname,
      filename: "../target/classes/static/built/bundle.js",
      assetModuleFilename: "../target/classes/static/built/[hash][ext][query]",
    },
    plugins: [
      new MiniCssExtractPlugin({
        filename: "../target/classes/static/built/css/app.css",
      }),
      new CopyPlugin({
        patterns: [
          {
            from: "public/favicon.ico",
            to: "../target/classes/static/favicon.ico",
          },
        ],
      }),
      new webpack.DefinePlugin({
        "process.env.REACT_APP_API_URL": JSON.stringify(
          process.env.REACT_APP_API_URL || "",
        ),
      }),
    ],
    module: {
      rules: [
        {
          test: /\.(js|jsx)$/,
          exclude: /node_modules/,
          use: [
            {
              loader: "babel-loader",
              options: {
                babelrc: false,
                configFile: false,
                presets: [
                  [
                    "@babel/preset-env",
                    { useBuiltIns: "usage", corejs: 3 },
                  ],
                  "@babel/preset-react",
                ],
              },
            },
          ],
        },
        {
          test: /\.css$/,
          use: [MiniCssExtractPlugin.loader, "css-loader"],
        },
        {
          test: /\.(png|svg|jpg|gif|eot|otf|ttf|woff|woff2)$/,
          type: "asset/resource",
        },
      ],
    },
    optimization: {
      minimize: isProduction,
      minimizer: [new CssMinimizerPlugin()],
      removeEmptyChunks: true,
      mergeDuplicateChunks: true,
      usedExports: true,
      innerGraph: true,
    },
  };
};
