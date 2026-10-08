const { createProxyMiddleware } = require('http-proxy-middleware');

module.exports = function(app) {
  app.use(
    '/api',
    createProxyMiddleware({
      target: 'http://localhost:8765',
      changeOrigin: true,
      secure: false,
      pathRewrite: {},  // Don't strip /api prefix
    })
  );
  
  app.use(
    '/logout',
    createProxyMiddleware({
      target: 'http://localhost:8765',
      changeOrigin: true,
      secure: false,
    })
  );
};
