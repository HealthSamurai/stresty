const path = require('path');

module.exports = {
  entry: {
    "monaco": './dev/javascript/monaco.js',
    // Package each language's worker and give these filenames in `getWorkerUrl`
    "editor.worker": './node_modules/monaco-editor/esm/vs/editor/editor.worker.js',
  },
  output: {
    globalObject: 'self',
    filename: '[name].js',
    path: path.resolve(__dirname, 'resources/public/assets/js/monaco')
  },
  module: {
    rules: [{
      test: /\.css$/,
      use: ['style-loader', 'css-loader']
    }, {
      test: /\.ttf$/,
      use: ['file-loader']
    }]
  }
};
