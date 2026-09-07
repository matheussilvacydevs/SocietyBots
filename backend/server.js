require('dotenv').config();

const express = require('express');
const authRouter = require('./routes/auth');

const app = express();

const HOST = process.env.HOST || '0.0.0.0';
const PORT = Number(process.env.PORT || 2207);

app.disable('x-powered-by');

app.use(express.json({
    limit: '1mb'
}));

app.get('/health', (_req, res) => {
    res.status(200).json({
        ok: true,
        service: 'Society Bots API',
        firebaseProject: process.env.FIREBASE_PROJECT_ID,
        port: PORT
    });
});

app.use('/api/auth', authRouter);

app.use((req, res) => {
    res.status(404).json({
        ok: false,
        error: 'Endpoint não encontrado.',
        path: req.path
    });
});

app.use((error, _req, res, _next) => {
    console.error('[SERVER]', error);

    res.status(500).json({
        ok: false,
        error: 'Erro interno do servidor.'
    });
});

const server = app.listen(PORT, HOST, () => {
    console.log('');
    console.log('╔══════════════════════════════════════════════╗');
    console.log('║       SOCIETY BOTS • BACKEND ONLINE         ║');
    console.log('╚══════════════════════════════════════════════╝');
    console.log('');
    console.log(`🌐 http://${HOST}:${PORT}`);
    console.log(`🔥 Firebase: ${process.env.FIREBASE_PROJECT_ID}`);
    console.log('');
});

server.on('error', error => {
    console.error('❌ Falha ao iniciar servidor:', error);
    process.exit(1);
});
