var path = require("path");
const MiniCssExtractPlugin = require("mini-css-extract-plugin");
const CssMinimizerPlugin = require("css-minimizer-webpack-plugin");

module.exports = {
  resolve: {
    extensions: [".js", ".jsx", ".ts", ".tsx", ".css"],
  },
  entry: ["./src/index.js", "./src/css/app.css"],
  devtool: "eval",
  cache: true,
  mode: "development",
  output: {
    path: __dirname,
    filename: "../target/classes/static/built/bundle.js",
  },
  plugins: [
    new MiniCssExtractPlugin({
      filename: "../target/classes/static/built/css/app.css",
    }),
  ],
  module: {
    rules: [
      {
        test: path.join(__dirname, "."),
        exclude: /(node_modules)/,
        use: [
          {
            loader: "babel-loader",
            options: {
              presets: ["@babel/preset-env", "@babel/preset-react"],
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
        use: [
          {
            loader: "url-loader",
            options: {},
          },
        ],
      },
      {
        test: /\.tsx?$/,
        exclude: /node_modules/,
        loader: "ts-loader",
      },
    ],
  },
  optimization: {
    minimize: true,
    minimizer: [new CssMinimizerPlugin()],
    removeEmptyChunks: true,
    mergeDuplicateChunks: true,
    usedExports: true,
    innerGraph: true,
  },
};
