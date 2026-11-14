package urna;

import urna.model.Candidato;
import urna.model.Cargo;
import urna.model.Partido;
import urna.model.Voto;
import urna.service.Apuracao;
import urna.service.CadastroEleitoral;
import urna.service.Urna;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Testes automatizados das regras da urna (sem bibliotecas externas).
 * Cada método "teste..." é executado e o resultado é exibido no console.
 */
public class TesteUrna {

    private static int aprovados;
    private static final List<String> falhas = new ArrayList<>();

    public static void main(String[] args) {
        executar("Voto nominal em candidato existente", TesteUrna::testeVotoNominal);
        executar("Voto de legenda para vereador", TesteUrna::testeVotoLegenda);
        executar("Voto nulo para número inexistente", TesteUrna::testeVotoNulo);
        executar("Prefeito não aceita voto de legenda", TesteUrna::testePrefeitoSemLegenda);
        executar("Contagem de brancos, nulos e válidos", TesteUrna::testeContagem);
        executar("Eleitor não pode votar duas vezes", TesteUrna::testeVotoDuplo);
        executar("Título inexistente é recusado", TesteUrna::testeTituloInexistente);
        executar("Não registra voto sem eleitor liberado", TesteUrna::testeVotoSemLiberacao);
        executar("Não registra voto após encerramento", TesteUrna::testeVotoAposEncerramento);
        executar("Não encerra com eleitor votando", TesteUrna::testeEncerrarComEleitorVotando);
        executar("Boletim só após encerramento", TesteUrna::testeBoletimSoAposEncerrar);
        executar("Senha do mesário", TesteUrna::testeSenha);
        executar("Candidato com número inválido é recusado", TesteUrna::testeCandidatoInvalido);
        executar("Prefeito eleito é o mais votado", TesteUrna::testePrefeitoEleito);
        executar("Distribuição de cadeiras de vereador", TesteUrna::testeCadeiras);

        System.out.println();
        System.out.println(aprovados + " teste(s) aprovado(s), " + falhas.size() + " falha(s).");
        for (String f : falhas) {
            System.out.println("  FALHOU: " + f);
        }
        if (!falhas.isEmpty()) {
            System.exit(1);
        }
    }

    // ------------------------------------------------------------------
    // Testes
    // ------------------------------------------------------------------

    static void testeVotoNominal() {
        Urna urna = novaUrna();
        Voto v = urna.interpretar(Cargo.VEREADOR, "12123");
        verificar(v.getTipo() == Voto.Tipo.NOMINAL, "deveria ser nominal");
        verificar(v.getCandidato().getNome().equals("João Pereira"), "candidato errado");
    }

    static void testeVotoLegenda() {
        Urna urna = novaUrna();
        Voto v = urna.interpretar(Cargo.VEREADOR, "12999");
        verificar(v.getTipo() == Voto.Tipo.LEGENDA, "deveria ser legenda");
        verificar(v.getPartido().getNumero() == 12, "partido errado");
    }

    static void testeVotoNulo() {
        Urna urna = novaUrna();
        verificar(urna.interpretar(Cargo.VEREADOR, "99999").getTipo() == Voto.Tipo.NULO, "vereador deveria ser nulo");
        verificar(urna.interpretar(Cargo.PREFEITO, "99").getTipo() == Voto.Tipo.NULO, "prefeito deveria ser nulo");
    }

    static void testePrefeitoSemLegenda() {
        CadastroEleitoral c = CadastroEleitoral.comDadosDeExemplo();
        c.cadastrarPartido(55, "PXX", "Partido sem prefeito");
        Urna urna = new Urna(c, "1234");
        verificar(urna.interpretar(Cargo.PREFEITO, "55").getTipo() == Voto.Tipo.NULO,
                "partido sem candidato a prefeito deveria dar nulo");
    }

    static void testeContagem() {
        Urna urna = novaUrna();
        votar(urna, "1001", "12123", "12");
        votar(urna, "1002", "12999", null);   // legenda + branco
        votar(urna, "1003", "99999", "99");   // nulo + nulo

        verificar(urna.getVotosValidos(Cargo.VEREADOR) == 2, "válidos vereador");
        verificar(urna.getVotosNulos(Cargo.VEREADOR) == 1, "nulos vereador");
        verificar(urna.getVotosBrancos(Cargo.PREFEITO) == 1, "brancos prefeito");
        verificar(urna.getVotosNulos(Cargo.PREFEITO) == 1, "nulos prefeito");
        verificar(urna.getTotalVotos(Cargo.PREFEITO) == 3, "total prefeito");
        Partido pti = urna.getCadastro().buscarPartido(12).get();
        verificar(urna.getVotosPartido(Cargo.VEREADOR, pti) == 2, "votos do partido (nominal + legenda)");
        verificar(urna.getEleitoresQueVotaram() == 3, "comparecimento");
        verificar(urna.getAbstencoes() == urna.getEleitoresAptos() - 3, "abstenções");
    }

    static void testeVotoDuplo() {
        Urna urna = novaUrna();
        votar(urna, "1001", "12123", "12");
        esperarErro(() -> urna.liberarEleitor("1001"), "deveria impedir voto duplo");
    }

    static void testeTituloInexistente() {
        Urna urna = novaUrna();
        esperarErro(() -> urna.liberarEleitor("0000"), "título inexistente deveria ser recusado");
    }

    static void testeVotoSemLiberacao() {
        Urna urna = novaUrna();
        esperarErro(() -> urna.registrar(Voto.branco(Cargo.PREFEITO)), "deveria exigir liberação");
    }

    static void testeVotoAposEncerramento() {
        Urna urna = novaUrna();
        urna.encerrar();
        esperarErro(() -> urna.liberarEleitor("1001"), "não deveria liberar após encerrar");
    }

    static void testeEncerrarComEleitorVotando() {
        Urna urna = novaUrna();
        urna.liberarEleitor("1001");
        esperarErro(urna::encerrar, "não deveria encerrar com eleitor votando");
    }

    static void testeBoletimSoAposEncerrar() {
        Urna urna = novaUrna();
        esperarErro(urna::gerarBoletim, "boletim antes do encerramento");
        urna.encerrar();
        verificar(urna.gerarBoletim().contains("BOLETIM DE URNA"), "boletim após encerrar");
    }

    static void testeSenha() {
        Urna urna = novaUrna();
        verificar(urna.validarSenhaMesario("1234"), "senha correta");
        verificar(!urna.validarSenhaMesario("0000"), "senha incorreta");
    }

    static void testeCandidatoInvalido() {
        Partido p = new Partido(12, "PTI", "Teste");
        esperarErro(() -> new Candidato(123, "X", p, Cargo.VEREADOR), "vereador com 3 dígitos");
        esperarErro(() -> new Candidato(25111, "X", p, Cargo.VEREADOR), "número de outro partido");
        esperarErro(() -> new Candidato(12, "X", p, Cargo.PREFEITO), "prefeito sem vice");
    }

    static void testePrefeitoEleito() {
        Urna urna = novaUrna();
        votar(urna, "1001", null, "25");
        votar(urna, "1002", null, "25");
        votar(urna, "1003", null, "12");
        List<Candidato> vencedores = new Apuracao(urna).vencedores(Cargo.PREFEITO);
        verificar(vencedores.size() == 1 && vencedores.get(0).getNumero() == 25, "prefeito eleito");
    }

    static void testeCadeiras() {
        // 3 vagas, 10 votos válidos -> QE = round(10/3) = 3
        Urna urna = new Urna(CadastroEleitoral.comDadosDeExemplo(), "1234", 3);
        String[] votosVereador = {"12123", "12123", "12123", "12456", "12999",   // PTI: 5
                "25111", "25111", "25222",                                       // PEV: 3
                "40001", "40001"};                                               // PMS: 2
        for (int i = 0; i < votosVereador.length; i++) {
            votar(urna, String.valueOf(1001 + i), votosVereador[i], null);
        }
        Apuracao apuracao = new Apuracao(urna);
        verificar(apuracao.quocienteEleitoral(Cargo.VEREADOR, 3) == 3, "quociente eleitoral");

        Map<Partido, Integer> cadeiras = apuracao.distribuirCadeiras(Cargo.VEREADOR, 3);
        CadastroEleitoral c = urna.getCadastro();
        // PTI: 5/3 = 1, PEV: 3/3 = 1, sobra 1 -> maior média: PTI 5/2=2.5, PEV 3/2=1.5, PMS 2/1=2 -> PTI
        verificar(cadeiras.get(c.buscarPartido(12).get()) == 2, "PTI deveria ter 2 cadeiras");
        verificar(cadeiras.get(c.buscarPartido(25).get()) == 1, "PEV deveria ter 1 cadeira");
        verificar(cadeiras.get(c.buscarPartido(40).get()) == 0, "PMS deveria ter 0 cadeiras");

        List<Candidato> eleitos = apuracao.eleitosProporcional(Cargo.VEREADOR, 3);
        verificar(eleitos.size() == 3, "3 eleitos");
        verificar(eleitos.get(0).getNumero() == 12123, "mais votado deveria ser o primeiro");
    }

    // ------------------------------------------------------------------
    // Auxiliares
    // ------------------------------------------------------------------

    private static Urna novaUrna() {
        return new Urna(CadastroEleitoral.comDadosDeExemplo(), "1234");
    }

    /** Vota como o eleitor do título. {@code null} = voto em branco. */
    private static void votar(Urna urna, String titulo, String vereador, String prefeito) {
        urna.liberarEleitor(titulo);
        urna.registrar(vereador == null ? Voto.branco(Cargo.VEREADOR) : urna.interpretar(Cargo.VEREADOR, vereador));
        urna.registrar(prefeito == null ? Voto.branco(Cargo.PREFEITO) : urna.interpretar(Cargo.PREFEITO, prefeito));
        urna.finalizarEleitor();
    }

    private static void verificar(boolean condicao, String mensagem) {
        if (!condicao) {
            throw new AssertionError(mensagem);
        }
    }

    private static void esperarErro(Runnable acao, String mensagem) {
        try {
            acao.run();
        } catch (IllegalArgumentException | IllegalStateException esperado) {
            return;
        }
        throw new AssertionError(mensagem);
    }

    private static void executar(String nome, Runnable teste) {
        try {
            teste.run();
            aprovados++;
            System.out.println("[OK]     " + nome);
        } catch (Throwable t) {
            falhas.add(nome + " -> " + t.getMessage());
            System.out.println("[FALHOU] " + nome + " -> " + t.getMessage());
        }
    }
}
