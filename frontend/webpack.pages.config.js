const path = require("path");
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
      path: path.resolve(__dirname, "dist"),
      filename: "bundle.js",
      assetModuleFilename: "assets/[hash][ext][query]",
      publicPath: "/",
      clean: true,
    },
    plugins: [
      new MiniCssExtractPlugin({
        filename: "css/app.css",
      }),
      new CopyPlugin({
        patterns: [
          { from: "public/favicon.ico", to: "favicon.ico" },
          { from: "pages/index.html", to: "index.html" },
          { from: "pages/_headers", to: "_headers", toType: "file" },
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
