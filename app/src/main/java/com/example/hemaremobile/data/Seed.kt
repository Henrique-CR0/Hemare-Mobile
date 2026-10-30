package com.example.hemaremobile.data

import com.example.hemaremobile.domain.*
import java.time.LocalDate

/**
 * Dados de exemplo (mock): o app ainda não fala com o backend do Hemare, então começa com estas contas, hospitais,
 * doadores e pedidos para que cada tela tenha o que mostrar. Nada daqui é pessoa real.
 *
 * Contas de teste (senha entre parênteses):
 *   doador@hemare.com (doador123)   hospital@hemare.com (hospital123, aprovado)
 *   pendente@hemare.com (hospital123, em verificação)   admin@hemare.com (admin1234)
 */
object Seed {
    private class Construtor {
        var id = 1
        val usuarios = mutableListOf<Usuario>()
        val doadores = mutableMapOf<Int, Doador>()
        val hospitais = mutableMapOf<Int, Hospital>()
        val doacoes = mutableListOf<Doacao>()
        var ultimoHash = Cadeia.GENESIS

        fun usuario(nome: String, email: String, senha: String, tipo: TipoConta, criado: LocalDate): Int {
            val novo = id++
            usuarios.add(Usuario(novo, nome, email, Senhas.hash(senha, email), tipo, criado))
            return novo
        }

        fun doador(
            nome: String, email: String, tipo: String, sexo: Char, cidade: String, estado: String, vis: String = "anonimo",
            tel: String? = null, codigo: String, indicadoPor: Int? = null, raro: RaroInfo = RaroInfo(), senha: String = "exemplo123"
        ): Int {
            val criado = Datas.hoje().minusDays(40)
            val uid = usuario(nome, email, senha, TipoConta.DOADOR, criado)
            doadores[uid] = Doador(
                id = uid, tipoSanguineo = tipo, sexo = sexo, genero = null, nomeSocial = null, cidade = cidade, estado = estado,
                dataNascimento = LocalDate.of(1990, 5, 12), pesoKg = 70, telefone = tel, cpf = null, visibilidade = vis,
                contatoEmergenciaNome = null, contatoEmergenciaTelefone = null, querLembrete = false, codigoIndicacao = codigo,
                indicadoPor = indicadoPor, ultimoAlerta = null, pcd = PcdInfo(), raro = raro, foto = null
            )
            return uid
        }

        fun hospital(
            nome: String, email: String, senha: String, cnpj: String, cnes: String, cidade: String, estado: String, endereco: String,
            aprovado: Boolean, recusa: String? = null, lat: Double?, lng: Double?
        ): Int {
            val uid = usuario(nome, email, senha, TipoConta.HOSPITAL, Datas.hoje().minusDays(90))
            hospitais[uid] = Hospital(uid, cnpj, cnes, "50000-000", endereco, "100", "Centro", null, cidade, estado, aprovado, recusa, lat, lng)
            return uid
        }

        fun doou(doadorId: Int, hospitalId: Int, diasAtras: Int, campanha: Boolean = false) {
            val data = Datas.hoje().minusDays(diasAtras.toLong())
            val hash = Cadeia.calcularHash(ultimoHash, doadorId, hospitalId, data)
            ultimoHash = hash
            doacoes.add(Doacao(id++, doadorId, hospitalId, data, hash, campanha))
        }
    }

    private val LOCAIS = listOf(
        listOf("Fundação Hemope", "Recife", "PE", "R. Joaquim Nabuco, 171 - Graças", "(81) 3182-4600", -8.0476, -34.8770),
        listOf("Fundação Pró-Sangue - Hemocentro SP", "São Paulo", "SP", "Av. Dr. Enéas C. de Aguiar, 155 - Cerqueira César", "(11) 4573-7800", -23.5505, -46.6333),
        listOf("HEMORIO", "Rio de Janeiro", "RJ", "R. Frei Caneca, 8 - Centro", "(21) 3916-8300", -22.9068, -43.1729),
        listOf("Fundação Hemominas", "Belo Horizonte", "MG", "Alameda Ezequiel Dias, 321 - Santa Efigênia", "(31) 3768-7400", -19.9167, -43.9345),
        listOf("HEMOAM", "Manaus", "AM", "Av. Constantino Nery, 4397 - Chapada", "(92) 3655-0100", -3.1190, -60.0217),
        listOf("HEMOBA", "Salvador", "BA", "Ladeira do Hospital Geral, s/n - Brotas", "(71) 3116-5652", -12.9777, -38.5016),
        listOf("HEMOCE", "Fortaleza", "CE", "Av. José Bastos, 3390 - Rodolfo Teófilo", "(85) 3101-2296", -3.7319, -38.5267),
        listOf("Hemocentro de Brasília - FHB", "Brasília", "DF", "SMHN Quadra 03, Conj. A, Bl. 3 - Asa Norte", "(61) 3327-4413", -15.7939, -47.8828),
        listOf("HEMORGS", "Porto Alegre", "RS", "Av. Bento Gonçalves, 3722 - Partenon", "(51) 3288-4069", -30.0346, -51.2177),
        listOf("HEMEPAR", "Curitiba", "PR", "Trav. João Prosdocimo, 145 - Alto da XV", "(41) 3281-4000", -25.4284, -49.2733),
        listOf("HEMOSC", "Florianópolis", "SC", "Av. Othon Gama D'Eça, 756 - Centro", "(48) 3251-9711", -27.5954, -48.5480),
        listOf("HEMOAL", "Maceió", "AL", "Av. Jorge de Lima, 58 - Trapiche da Barra", "(82) 3315-2107", -9.6498, -35.7089),
        listOf("HEMONORTE", "Natal", "RN", "Av. Alexandrino de Alencar, 1800 - Tirol", "(84) 3232-6767", -5.7945, -35.2110),
        listOf("HEMOPI", "Teresina", "PI", "R. 1º de Maio, 235 - Centro", "(86) 3221-8319", -5.0892, -42.8019),
        listOf("HEMOPA", "Belém", "PA", "Trav. Padre Eutíquio, 2109 - Batista Campos", "(91) 3110-6300", -1.4558, -48.4902),
        listOf("HEMOSE", "Aracaju", "SE", "Av. Tancredo Neves, s/n - Capucho", "(79) 3225-8500", -10.9472, -37.0731),
        listOf("HEMOMAR", "São Luís", "MA", "R. 5 de Janeiro, s/n - Jordoa", "(98) 3218-7100", -2.5307, -44.3068),
        listOf("MT Hemocentro", "Cuiabá", "MT", "R. 13 de Junho, 1055 - Porto", "(65) 3623-0044", -15.6014, -56.0979),
        listOf("HEMOSUL", "Campo Grande", "MS", "Av. Fernando Corrêa da Costa, 1304 - Centro", "(67) 3312-1500", -20.4697, -54.6201),
        listOf("HEMOGO", "Goiânia", "GO", "Av. Anhanguera, 5195 - Setor Coimbra", "(62) 3201-4560", -16.6869, -49.2648)
    )

    fun criar(): Estado {
        val b = Construtor()
        val hoje = Datas.hoje()

        // ----- hospitais (3 aprovados, 1 pendente) -----
        val hRecife = b.hospital("Hospital das Clínicas (exemplo)", "hospital@hemare.com", "hospital123", "11.222.333/0001-81", "2530651", "Recife", "PE", "Av. Prof. Moraes Rego", true, lat = -8.0476, lng = -34.8770)
        val hSp = b.hospital("Hemocentro Paulista (exemplo)", "hemocentro.sp@hemare.com", "hospital123", "11.444.777/0001-61", "2077485", "São Paulo", "SP", "Av. Dr. Arnaldo", true, lat = -23.5505, lng = -46.6333)
        val hBh = b.hospital("Santa Casa de Belo Horizonte (exemplo)", "santacasa.bh@hemare.com", "hospital123", "17.217.985/0001-04", "2695324", "Belo Horizonte", "MG", "Av. Francisco Sales", true, lat = -19.9167, lng = -43.9345)
        b.hospital("Hospital Aguardando Aprovação (exemplo)", "pendente@hemare.com", "hospital123", "33.000.167/0001-01", "2237601", "Curitiba", "PR", "R. Marechal Deodoro", false, lat = -25.4284, lng = -49.2733)

        // ----- admin -----
        b.usuario("Administrador Hemare", "admin@hemare.com", "admin1234", TipoConta.ADMIN, hoje.minusDays(200))

        // ----- doadores -----
        val demo = b.doador("Maria Doadora", "doador@hemare.com", "O+", 'F', "Recife", "PE", vis = "identificado", tel = "(81) 99999-0001", codigo = "HEMARE26", senha = "doador123")
        val ana = b.doador("Ana Paula Ferreira", "ana@exemplo.com", "A+", 'F', "Recife", "PE", vis = "identificado", tel = "(81) 99999-0002", codigo = "ANAPAULA", indicadoPor = demo)
        val bruno = b.doador("Bruno Lima", "bruno@exemplo.com", "O-", 'M', "Recife", "PE", vis = "identificado", tel = "(81) 99999-0003", codigo = "BRUNO234")
        val carla = b.doador("Carla Souza", "carla@exemplo.com", "B+", 'F', "Recife", "PE", codigo = "CARLA567")
        val diego = b.doador("Diego Alves", "diego@exemplo.com", "AB+", 'M', "São Paulo", "SP", vis = "identificado", tel = "(11) 98888-0001", codigo = "DIEGO789")
        val eduarda = b.doador("Eduarda Rocha", "eduarda@exemplo.com", "O-", 'F', "São Paulo", "SP", vis = "identificado", tel = "(11) 98888-0002", codigo = "EDUARDA2")
        val fabio = b.doador("Fábio Mendes", "fabio@exemplo.com", "B-", 'M', "São Paulo", "SP", codigo = "FABIO345")
        val gabi = b.doador("Gabriela Nunes", "gabriela@exemplo.com", "A-", 'F', "Belo Horizonte", "MG", vis = "identificado", tel = "(31) 97777-0001", codigo = "GABI4567")
        val hugo = b.doador("Hugo Barros", "hugo@exemplo.com", "AB-", 'M', "Belo Horizonte", "MG", codigo = "HUGO6789")
        val irene = b.doador("Irene Castro", "irene@exemplo.com", "O+", 'F', "Belo Horizonte", "MG", vis = "identificado", tel = "(31) 97777-0002", codigo = "IRENE234")
        val joao = b.doador(
            "João Dourado", "joao@exemplo.com", "Rh nulo (sangue dourado)", 'M', "Recife", "PE", vis = "identificado", tel = "(81) 99999-0004", codigo = "JOAO5678",
            raro = RaroInfo("rh-nulo", "estado", true, "confirmado", "RAROJOAO", null)
        )

        // Doações (a cadeia de confiança liga cada uma à anterior)
        b.doou(demo, hRecife, 100)
        b.doou(ana, hRecife, 200); b.doou(ana, hRecife, 30)
        b.doou(bruno, hRecife, 75); b.doou(bruno, hSp, 150)
        b.doou(carla, hRecife, 20)
        b.doou(diego, hSp, 45); b.doou(diego, hSp, 160)
        b.doou(eduarda, hSp, 10)
        b.doou(fabio, hSp, 95)
        b.doou(gabi, hBh, 70); b.doou(gabi, hBh, 190)
        b.doou(hugo, hBh, 15)
        b.doou(irene, hBh, 130)
        b.doou(joao, hRecife, 300)

        // ----- estoque, necessidades -----
        val estoque = mapOf(
            hRecife to mapOf("O-" to "critico", "O+" to "alerta", "A+" to "alerta", "A-" to "estavel", "B+" to "estavel", "B-" to "estavel", "AB+" to "estavel", "AB-" to "estavel"),
            hSp to mapOf("B-" to "emergencia", "O-" to "alerta", "O+" to "estavel", "A+" to "estavel", "A-" to "estavel", "B+" to "estavel", "AB+" to "estavel", "AB-" to "estavel"),
            hBh to mapOf("AB-" to "alerta", "O-" to "estavel", "O+" to "estavel", "A+" to "estavel", "A-" to "estavel", "B+" to "estavel", "B-" to "estavel", "AB+" to "estavel")
        )
        val necessidades = listOf(
            Necessidade(b.id++, hRecife, "O-", "critico", "aberta"),
            Necessidade(b.id++, hRecife, "A+", "alerta", "aberta"),
            Necessidade(b.id++, hSp, "B-", "emergencia", "aberta")
        )

        // ----- agenda: vagas dos próximos 14 dias úteis -----
        val vagas = mutableListOf<Vaga>()
        listOf(hRecife, hSp, hBh).forEach { h ->
            for (dia in 1..14) {
                val data = hoje.plusDays(dia.toLong())
                if (data.dayOfWeek.value in 1..5) {
                    Datas.gerarHorarios("08:00", "11:00", 30).forEach { hora -> vagas.add(Vaga(b.id++, h, data, hora, 2)) }
                }
            }
        }

        // ----- apadrinhamento -----
        val casos = listOf(
            Caso(b.id++, hRecife, "Paciente Aurora", "O+", "Anemia falciforme", 30, Apadrinhamento.sugerirMeta(30), true, null, listOf(bruno, ana)),
            Caso(b.id++, hRecife, "Pequeno Davi", "A+", "Talassemia", 21, Apadrinhamento.sugerirMeta(21), true, hoje.minusDays(10), listOf(ana)),
            Caso(b.id++, hSp, "Dona Lúcia", "B+", "Tratamento oncológico", 45, Apadrinhamento.sugerirMeta(45), true, null, emptyList())
        )

        // ----- campanha de reposição -----
        val campanhas = listOf(
            CampanhaReg(
                "DEMO2026", hRecife, "Seu Antônio", 5, hoje.plusDays(12), true,
                listOf(Promessa(carla, hoje.plusDays(3), false), Promessa(bruno, hoje.plusDays(5), false))
            )
        )

        // ----- locais públicos de doação -----
        val locais = LOCAIS.mapIndexed { i, l ->
            LocalDoacao(i + 1, l[0] as String, l[1] as String, l[2] as String, l[3] as String, l[4] as String, l[5] as Double, l[6] as Double)
        }

        return Estado(
            usuarios = b.usuarios, doadores = b.doadores, hospitais = b.hospitais, locais = locais, doacoes = b.doacoes,
            estoque = estoque, necessidades = necessidades, vagas = vagas, agendamentos = emptyList(), casos = casos,
            campanhas = campanhas, pedidosRaros = emptyList(), noticias = NoticiasExemplo.lista(), noticiasOcultas = emptySet(),
            coleta = null, sessaoUsuarioId = null, proximoId = b.id + 1000
        )
    }
}
