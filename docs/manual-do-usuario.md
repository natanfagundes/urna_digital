# Manual do Usuário — Urna Digital

Guia para quem vai organizar uma eleição (escola, grêmio, associação) ou usar a urna em uma aula.

## 1. Instalação

1. Instale o **Java (JDK) 11 ou mais novo**, por exemplo o [Eclipse Temurin](https://adoptium.net/).
2. Baixe o projeto (no GitHub: botão **Code → Download ZIP**) e descompacte.
3. Dê dois cliques em **`executar.bat`** (Windows).
   No Linux/macOS: `javac -encoding UTF-8 -d out/main $(find src/main/java -name "*.java") && java -cp out/main urna.Main`

Não é preciso internet para usar a urna.

## 2. Testar com um exemplo

Na tela inicial, escolha **Exemplo: grêmio estudantil** ou **Exemplo: eleição municipal** e clique em
**Iniciar votação**. A senha do mesário dos exemplos é **`1234`**.

| Exemplo | Eleitores para testar |
|---|---|
| Grêmio estudantil | matrículas de `2026001` a `2026020` |
| Eleição municipal | títulos de `1001` a `1015` |

## 3. Preparar uma eleição de verdade

1. Na tela inicial, clique em **Nova eleição** e dê um nome (ex.: "Grêmio 2026").
2. **Aba Cargos:** adicione os cargos na ordem em que serão votados.
   - **Majoritário:** vence o mais votado. Ex.: Presidente do Grêmio (2 dígitos, com vice).
   - **Proporcional:** as vagas são divididas entre as chapas. Ex.: Conselho de Representantes (3 dígitos, 5 vagas).
3. **Aba Partidos / Chapas:** cadastre cada chapa com um número de 2 dígitos (ex.: 10, 20, 30).
4. **Aba Candidatos:** cadastre os candidatos. O número começa com o número da chapa
   (chapa 10 → presidente 10, conselheiros 101, 102...). A foto é opcional (JPG ou PNG).
5. **Aba Eleitores:** clique em **Salvar planilha modelo**, preencha no Excel (coluna 1: matrícula,
   coluna 2: nome) e use **Importar planilha CSV**. No Excel, salve como *CSV (separado por ponto e vírgula)*.
6. **Aba Eleição:** defina a **senha do mesário** (mínimo 4 caracteres). Guarde-a bem.
7. Clique em **Salvar**. A aba Eleição mostra se ainda falta algo.

A eleição fica na pasta `eleicoes/<nome-da-eleicao>/`. Faça uma cópia dessa pasta antes do dia da votação.

## 4. No dia da votação

1. Abra a eleição e clique em **Iniciar votação**. A partir daqui a configuração fica bloqueada.
2. **Mesário → Emitir Zerésima:** prova, para todos, que a urna começa zerada.
3. Para cada eleitor:
   1. confira o documento;
   2. **Mesário → Liberar eleitor** (ou **Ctrl+L**) e digite a matrícula;
   3. o eleitor vota em todos os cargos até aparecer **FIM**.
4. Se o eleitor desistir no meio: **Mesário → Cancelar eleitor atual**. Nada do que ele digitou é contado.
5. **Mesário → Eleitores** mostra quem já votou.

### E se o computador desligar?

Os votos ficam gravados a cada eleitor que termina. Abra o programa, escolha **Abrir eleição salva**,
selecione a pasta da eleição e clique em **Retomar votação**. Só o eleitor que estava no meio do voto
precisa votar de novo.

## 5. Encerrar e divulgar o resultado

1. **Mesário → Encerrar votação** e digite a senha.
2. O **Boletim de Urna** aparece na tela e é salvo em `eleicoes/<nome>/relatorios/`.
3. Imprima ou projete o boletim. O código SHA-256 no final permite verificar que o arquivo não foi alterado.

O boletim traz os votos e a porcentagem de cada candidato, brancos, nulos, comparecimento, abstenções
e os **eleitos**. Em cargos proporcionais, mostra também o quociente eleitoral e as cadeiras de cada chapa.

## 6. Acessibilidade

| Tecla | Recurso |
|---|---|
| F2 | Alto contraste (fundo preto, letras amarelas) |
| F3 | Fonte grande |
| F4 | Leitura em voz alta (lê o cargo, os números, o candidato e as instruções) |
| F5 | Modo didático |

O eleitor também pode votar pelo teclado do computador: números, **Enter** = CONFIRMA,
**Backspace** = CORRIGE, **B** = BRANCO.

## 7. Usando em sala de aula

Marque **Usar o modo didático** na tela inicial (ou aperte F5). Um painel amarelo explica cada etapa:
por que o mesário libera a urna, a diferença entre voto nominal, de legenda, branco e nulo, o sigilo do voto
e como funciona o quociente eleitoral. Sugestão de roteiro de oficina: veja o
[relatório de extensão](relatorio-extensao.md#7-plano-de-oficina-50-minutos).

## 8. Problemas comuns

| Problema | Solução |
|---|---|
| Ninguém lembra a senha do mesário | A senha não pode ser recuperada, só trocada. **Antes de iniciar a votação:** apague a linha `senhaMesarioSha256` do arquivo `eleicao.properties` e defina uma nova senha na aba Eleição. **Depois de iniciada**, a votação não pode ser encerrada nem zerada sem a senha. Isso é proposital. |
| "Arquivo de votação foi alterado ou está corrompido" | Alguém editou `votacao/estado.txt`. Use a cópia de segurança da pasta, se houver. |
| Acentos estranhos na planilha importada | Salve no Excel como *CSV UTF-8* ou *CSV (separado por ponto e vírgula)*. |
| Não sai som / voz | Confira o volume. A voz usa o sintetizador do sistema; no Linux instale o `espeak-ng`. |
