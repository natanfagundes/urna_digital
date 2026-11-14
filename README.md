# Urna Eletrônica em Java

Projeto de Extensão da disciplina de Engenharia de Software.
Uma simulação da urna eletrônica brasileira (eleição municipal) feita em Java (Swing),
sem bibliotecas externas.

## Como executar

Pré-requisito: **JDK 11 ou superior**.

- **Programa:** clique duas vezes em `executar.bat`.
- **Testes automatizados:** clique duas vezes em `executar-testes.bat`.
- **Pela linha de comando:**

```bash
javac -encoding UTF-8 -d out src/urna/*.java src/urna/model/*.java src/urna/service/*.java src/urna/view/*.java
```

```bash
java -cp out urna.Main
```

Também dá para abrir a pasta no IntelliJ, Eclipse ou VS Code e executar a classe `urna.Main`.

## Roteiro de uso (como numa seção eleitoral)

1. **Abertura:** menu *Mesário → Emitir Zerésima*. O relatório prova que todos os candidatos
   começam com zero votos (só pode ser emitido antes do primeiro voto).
2. **Liberar o eleitor:** *Mesário → Liberar eleitor* (Ctrl+L) e digite o título (ex.: `1001`).
   A urna recusa títulos que não são da seção e eleitores que já votaram.
3. **Votar:** primeiro **Vereador** (5 dígitos), depois **Prefeito** (2 dígitos).
   Use o teclado da urna ou o do computador:
   números, **Enter** = CONFIRMA, **Backspace** = CORRIGE, **B** = BRANCO.
4. Ao terminar aparece **FIM** e a urna volta a ficar bloqueada, aguardando o próximo eleitor.
5. **Encerramento:** *Mesário → Encerrar votação* (senha do mesário: **`1234`**).
   O **Boletim de Urna** é exibido com a apuração e os eleitos.

A Zerésima e o Boletim são salvos na pasta `relatorios/`, cada um com um código
**SHA-256** que permite verificar se o arquivo foi alterado depois de emitido.

## Regras implementadas

| Situação | Resultado |
|---|---|
| Número de candidato existente | Voto nominal (mostra nome, partido, vice e foto) |
| Vereador com número inexistente, mas partido válido (2 primeiros dígitos) | Voto de legenda |
| Número sem candidato e sem partido | Voto nulo |
| Tecla BRANCO com o campo vazio | Voto em branco |

- **Sigilo do voto:** a urna guarda *quem votou* (títulos) separado de *em quem se votou*
  (contadores), sem ligação entre os dois. Não há apuração parcial durante a votação.
- **Prefeito:** é eleito o mais votado (com aviso em caso de empate).
- **Vereador (sistema proporcional, simplificado), 9 vagas:**
  1. Quociente eleitoral (QE) = votos válidos ÷ vagas.
  2. Cada partido recebe (votos do partido ÷ QE) cadeiras.
  3. As vagas que sobram vão para o partido com a maior média (votos ÷ (cadeiras + 1)).
  4. As cadeiras do partido vão para seus candidatos mais votados, que precisam ter
     pelo menos 10% do QE em votos nominais. Se faltarem candidatos, a vaga fica sem ser preenchida.

## Dados de exemplo (fictícios)

| Partido | Prefeito (vice) | Vereadores |
|---|---|---|
| 12 PTI | Ana Souza (Carlos Lima) | 12123 João Pereira, 12456 Larissa Martins |
| 25 PEV | Bruno Ferreira (Juliana Rocha) | 25111 Paulo Oliveira, 25222 Camila Ribeiro |
| 40 PMS | Mariana Alves (Pedro Santos) | 40001 Lucas Barbosa, 40002 Beatriz Costa |
| 77 PSS | Ricardo Gomes (Fernanda Dias) | 77777 Marcos Teixeira, 77100 Patrícia Nunes |

**Eleitores:** 15 eleitores com títulos de **1001 a 1015** (lista completa em
*Mesário → Eleitores da seção*).

## Estrutura do projeto

```
src/urna/
├── Main.java                   Ponto de entrada (senha do mesário)
├── model/                      Entidades do domínio
│   ├── Cargo.java              Enum: VEREADOR, PREFEITO (dígitos, vice, legenda)
│   ├── Partido.java            Número (2 dígitos), sigla, nome
│   ├── Candidato.java          Número, nome, partido, cargo, vice
│   ├── Eleitor.java            Título e nome
│   └── Voto.java               NOMINAL, LEGENDA, BRANCO, NULO (voto anônimo)
├── service/                    Regras de negócio
│   ├── CadastroEleitoral.java  Partidos, candidatos e eleitores
│   ├── Urna.java               Liberação, votação, contagem, Zerésima e Boletim
│   ├── Apuracao.java           Eleitos: maioria simples e quociente eleitoral
│   └── ArquivoBoletim.java     Gravação dos relatórios com hash SHA-256
└── view/
    ├── TelaUrna.java           Interface (tela do eleitor, teclado, menu do mesário)
    └── FotoCandidato.java      Retrato do candidato desenhado com as iniciais
test/urna/
└── TesteUrna.java              15 testes automatizados das regras
```

A separação em camadas (modelo / serviço / interface) deixa as regras da eleição
independentes da tela. Por isso dá para testar tudo automaticamente sem abrir a janela.
