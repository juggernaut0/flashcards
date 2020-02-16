const path = require('path');

module.exports = function(env, argv) {
    var config = {
        entry: './build/node_modules/web.js',
        output: {
            filename: 'main.js',
            path: path.resolve(__dirname, 'build/webpack')
        },
        mode: 'development'
    };
    if (argv["mode"] !== "production") {
        config = {
            ...config,
            ...{
                devtool: 'eval-source-map',
                module: {
                    rules: [
                        {
                            test: /\.js$/,
                            use: ["source-map-loader"],
                            enforce: "pre"
                        }
                    ]
                }
            }
        };
    }
    return config;
};