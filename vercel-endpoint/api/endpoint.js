module.exports = function handler(req, res) {
  res.setHeader('Cache-Control', 'no-store, no-cache, must-revalidate');

  return res.status(200).json({
    ok: true,
    baseUrl: 'https://traveling-appointed-sticks-invisible.trycloudflare.com'
  });
};
