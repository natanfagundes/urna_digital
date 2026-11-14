package urna.view;

import urna.model.Cargo;
import urna.model.Partido;

/**
 * Explicações do modo didático, usado em aulas e oficinas de educação cidadã.
 * Cada método descreve o que está acontecendo na urna naquele momento.
 */
final class Didatica {

    private Didatica() {
    }

    static String bloqueada() {
        return "A urna está BLOQUEADA. Na seção eleitoral, o mesário confere o documento do eleitor "
                + "e digita o título no terminal do mesário para liberar a urna. Assim, ninguém vota "
                + "duas vezes e só quem está na lista da seção pode votar. "
                + "(Aqui: menu Mesário → Liberar eleitor, ou Ctrl+L.)";
    }

    static String etapa(Cargo cargo) {
        String base = "Agora o eleitor vota para " + cargo.getNome() + ": basta digitar os "
                + cargo.getQuantidadeDigitos() + " números do candidato. ";
        if (cargo.isProporcional()) {
            return base + "Este cargo é PROPORCIONAL: as " + cargo.getVagas() + " vagas são divididas entre os "
                    + "partidos/chapas conforme o total de votos de cada um. Por isso, os " + Partido.DIGITOS
                    + " primeiros números do candidato são o número do partido. Votar num candidato também "
                    + "ajuda o partido dele a ganhar cadeiras.";
        }
        return base + "Este cargo é MAJORITÁRIO: " + (cargo.getVagas() == 1 ? "vence quem tiver mais votos."
                : "as " + cargo.getVagas() + " vagas ficam com os mais votados.")
                + (cargo.possuiVice() ? " Quem vota no titular vota também no " + cargo.getRotuloVice() + " da chapa." : "");
    }

    static String partidoDigitado(Partido partido) {
        return "Os primeiros números digitados já identificam o partido/chapa: " + partido.getSigla()
                + ". Continue digitando o número do candidato.";
    }

    static String nominal() {
        return "VOTO NOMINAL: o número é de um candidato. Antes de confirmar, o eleitor confere nome, "
                + "partido e foto. Se estiver errado, aperta CORRIGE e digita de novo. "
                + "O voto nominal é um voto VÁLIDO.";
    }

    static String legenda() {
        return "VOTO DE LEGENDA: não existe candidato com esse número, mas os primeiros números são de um "
                + "partido/chapa. O voto vai para o partido e conta para a divisão das cadeiras. "
                + "Também é um voto VÁLIDO.";
    }

    static String nulo() {
        return "VOTO NULO: o número não é de nenhum candidato nem de nenhum partido. "
                + "O voto nulo não vai para ninguém e NÃO entra no cálculo do resultado (não é válido). "
                + "Mito comum: muitos votos nulos NÃO anulam a eleição.";
    }

    static String branco() {
        return "VOTO EM BRANCO: o eleitor escolhe não votar em ninguém para este cargo. Assim como o nulo, "
                + "o voto em branco NÃO é válido: não vai para nenhum candidato nem partido. "
                + "Para votar em branco, o campo de número precisa estar vazio.";
    }

    static String brancoComDigitos() {
        return "A tecla BRANCO só funciona com o campo vazio, para evitar que o eleitor anule sem querer "
                + "um número que começou a digitar. Aperte CORRIGE para apagar.";
    }

    static String confirmado() {
        return "Voto confirmado! A urna guarda apenas a soma dos votos de cada candidato, sem nenhuma "
                + "ligação com quem votou. Isso é o SIGILO DO VOTO.";
    }

    static String fim() {
        return "FIM: o eleitor terminou. Só agora os votos dele entram na contagem e são gravados. "
                + "Se faltasse energia no meio do voto, nada dele seria contado e ele poderia votar de novo.";
    }

    static String encerrada() {
        return "VOTAÇÃO ENCERRADA. A urna emite o BOLETIM DE URNA com o total de votos de cada candidato. "
                + "Na eleição real, o boletim é impresso, afixado na porta da seção e enviado para a "
                + "totalização. Qualquer pessoa pode conferir. O código SHA-256 no final do arquivo permite "
                + "verificar se o boletim foi alterado.";
    }
}
