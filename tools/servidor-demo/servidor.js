// Servidor de DEMONSTRAÇÃO do Hemare Mobile.
// NÃO é o backend do site: só devolve, no mesmo formato do docs/API.md, as rotas públicas que o app usa
// (/saude, /locais, /hospital/estoque-publico, /noticias/feed e /auth/login). Serve para mostrar a integração
// do app com uma API HTTP/JSON sem precisar de PostgreSQL. Sem dependências: node servidor.js [porta]
const http = require('http');

const PORTA = Number(process.argv[2] || process.env.PORT || 3000);
const agora = Date.now();
const h = (n) => new Date(agora - n * 3600 * 1000).toISOString();

const LOCAIS = [
    { id: 9001, nome: '[DEMO] Hemocentro Central do Servidor', cidade: 'Recife', estado: 'PE', endereco: 'Rua da Demonstração, 100', telefone: '(81) 3000-0001', tipo: 'hemocentro' },
    { id: 9002, nome: '[DEMO] Banco de Sangue Norte', cidade: 'Natal', estado: 'RN', endereco: 'Av. Exemplo, 200', telefone: '(84) 3000-0002', tipo: 'hemocentro' },
    { id: 9003, nome: '[DEMO] Hospital Escola Sul', cidade: 'Porto Alegre', estado: 'RS', endereco: 'Rua Teste, 300', telefone: null, tipo: 'hospital' }
];

const ESTOQUE = [
    { hospital: '[DEMO] Hospital Escola Sul', cidade: 'Porto Alegre', estado: 'RS', tipo_sanguineo: 'O-', nivel: 'emergencia', atualizado_em: h(2) },
    { hospital: '[DEMO] Hospital Escola Sul', cidade: 'Porto Alegre', estado: 'RS', tipo_sanguineo: 'A+', nivel: 'estavel', atualizado_em: h(2) },
    { hospital: '[DEMO] Banco de Sangue Norte', cidade: 'Natal', estado: 'RN', tipo_sanguineo: 'B-', nivel: 'critico', atualizado_em: h(5) },
    { hospital: '[DEMO] Banco de Sangue Norte', cidade: 'Natal', estado: 'RN', tipo_sanguineo: 'O+', nivel: 'alerta', atualizado_em: h(5) }
];

const NOTICIAS = [
    { id: 1, titulo: 'Servidor de demonstração: estoque de sangue O- em emergência no Sul', resumo: 'Notícia de teste enviada pela API de demonstração para provar a integração do app.', fonte: 'Servidor demo', url: 'https://www.gov.br/saude/pt-br', imagem: null, categoria: 'estoque', uf: 'RS', publicadaEm: h(1) },
    { id: 2, titulo: 'Servidor de demonstração: campanha de doação de sangue começa na semana que vem', resumo: 'Segunda notícia de teste, vinda do JSON da rota /noticias/feed.', fonte: 'Servidor demo', url: 'https://www.gov.br/saude/pt-br', imagem: null, categoria: 'campanhas', uf: 'RN', publicadaEm: h(6) },
    { id: 3, titulo: 'Servidor de demonstração: o que fazer antes de doar sangue pela primeira vez', resumo: 'Terceira notícia de teste.', fonte: 'Servidor demo', url: 'https://www.gov.br/saude/pt-br', imagem: null, categoria: 'doacao', uf: null, publicadaEm: h(30) }
];

const CATEGORIAS = [
    { id: 'doacao', rotulo: 'Doação de sangue' }, { id: 'estoque', rotulo: 'Estoques e urgência' },
    { id: 'campanhas', rotulo: 'Campanhas' }, { id: 'orgaos', rotulo: 'Órgãos, medula e leite' }, { id: 'saude', rotulo: 'Saúde' }
];

function json(res, codigo, corpo) {
    res.writeHead(codigo, { 'Content-Type': 'application/json; charset=utf-8' });
    res.end(JSON.stringify(corpo));
}

const servidor = http.createServer((req, res) => {
    const url = new URL(req.url, 'http://localhost');
    console.log(new Date().toLocaleTimeString(), req.method, req.url);
    const rota = url.pathname;

    if (req.method === 'GET' && rota === '/saude') return json(res, 200, { ok: true, modo: 'demonstracao' });
    if (req.method === 'GET' && rota === '/locais') return json(res, 200, LOCAIS);
    if (req.method === 'GET' && rota === '/hospital/estoque-publico') return json(res, 200, ESTOQUE);
    if (req.method === 'GET' && rota === '/noticias/feed') {
        const pagina = Number(url.searchParams.get('pagina') || 1);
        return json(res, 200, {
            itens: pagina === 1 ? NOTICIAS : [], total: NOTICIAS.length, pagina, porPagina: 20, temMais: false,
            categorias: CATEGORIAS.map((c) => ({ ...c, total: NOTICIAS.filter((n) => n.categoria === c.id).length })),
            atualizadoEm: h(0)
        });
    }
    if (req.method === 'POST' && rota === '/auth/login') {
        let corpo = '';
        req.on('data', (d) => { corpo += d; });
        req.on('end', () => {
            let dados = {};
            try { dados = JSON.parse(corpo || '{}'); } catch (e) { /* corpo inválido */ }
            if (dados.email === 'demo@hemare.com' && dados.senha === 'demo12345') {
                return json(res, 200, {
                    mensagem: 'Login realizado!', token: 'token-de-demonstracao', expiraEm: h(-24),
                    usuario: { id: 1, nome: 'Conta de Demonstração', email: 'demo@hemare.com', tipo: 'doador' }
                });
            }
            return json(res, 401, { erro: 'E-mail ou senha incorretos.' });
        });
        return;
    }
    return json(res, 404, { erro: 'Rota não existe neste servidor de demonstração.' });
});

servidor.listen(PORTA, '0.0.0.0', () => {
    console.log('Servidor de DEMONSTRAÇÃO do Hemare em http://localhost:' + PORTA);
    console.log('No emulador Android use: http://10.0.2.2:' + PORTA);
    console.log('Login de teste: demo@hemare.com / demo12345');
});
