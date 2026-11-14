# Documentação Técnica — Urna Digital

Documento para desenvolvedores, avaliadores e quem for manter o projeto. Para a visão de negócio, veja
[Requisitos](requisitos.md) e [Casos de uso](casos-de-uso.md); para os diagramas UML, [Arquitetura](arquitetura.md).

## Sumário

1. [Tecnologias](#1-tecnologias)
2. [Ambiente de desenvolvimento](#2-ambiente-de-desenvolvimento)
3. [Estrutura do repositório](#3-estrutura-do-repositório)
4. [Compilar e executar](#4-compilar-e-executar)
5. [Testes](#5-testes)
6. [Empacotamento e distribuição](#6-empacotamento-e-distribuição)
7. [Integração contínua](#7-integração-contínua)
8. [Pacotes e classes](#8-pacotes-e-classes)
9. [Funcionamento interno](#9-funcionamento-interno)
10. [Interface gráfica](#10-interface-gráfica)
11. [Como estender](#11-como-estender)
12. [Convenções de código](#12-convenções-de-código)
13. [Limitações e melhorias futuras](#13-limitações-e-melhorias-futuras)
14. [Solução de problemas](#14-solução-de-problemas)

---

## 1. Tecnologias

| Tecnologia | Uso |
|---|---|
| **Java 11+** (compilado com `--release 11`) | Linguagem. Roda em qualquer JDK/JRE 11 ou superior |
| **Swing / AWT** | Interface gráfica |
| `javax.sound.sampled` | Sons das teclas, gerados em tempo real (sem arquivos de áudio) |
| `javax.imageio` | Fotos dos candidatos |
| `java.security.MessageDigest` | SHA-256 (senha, integridade dos arquivos) |
| **JUnit 5.11** | Testes automatizados (única dependência, só em teste) |
| **Maven** | Build, testes e geração do `.jar` |
| **jpackage / jlink** (JDK 17+) | Geração do `.exe` com Java embutido |
| **GitHub Actions** | Integração contínua |

O código de produção **não tem dependências externas**: só a biblioteca padrão do Java.

## 2. Ambiente de desenvolvimento

- **JDK 17 ou superior** recomendado (o código é compatível com 11, mas o `jpackage` exige 14+ e o CI usa 17).
- **IDE:** IntelliJ IDEA, NetBeans, Eclipse ou VS Code. Abra a pasta do projeto: o `pom.xml` é reconhecido
  e o JUnit é baixado automaticamente.
- **Maven** (opcional fora da IDE): `mvn -v` deve funcionar no terminal.
- **Codificação:** todos os arquivos são UTF-8. Ao compilar à mão, use sempre `-encoding UTF-8`.

## 3. Estrutura do repositório

```
├── pom.xml                         build Maven (Java 11, JUnit 5, jar executável)
├── executar.bat                    compila com javac e abre o programa (Windows)
├── executar-testes.bat             roda os testes com Maven (Windows)
├── gerar-exe.bat                   gera dist/UrnaDigital-windows.zip com jpackage
├── empacotamento/urna.ico          ícone do .exe
├── exemplos/eleitores_modelo.csv   planilha modelo de eleitores
├── .github/workflows/testes.yml    CI: testes, .jar e .exe
├── docs/                           documentação
├── src/main/java/urna/
│   ├── Main.java
│   ├── model/                      entidades do domínio
│   ├── service/                    regras de negócio
│   ├── persistencia/               leitura e gravação de arquivos
│   ├── util/                       funções auxiliares
│   └── view/                       interface Swing
└── src/test/java/urna/             testes JUnit (mesmos pacotes)
```

Pastas geradas (ignoradas pelo Git): `out/`, `target/`, `build-exe/`, `dist/`, `eleicoes/*/votacao/`, `relatorios/`.

## 4. Compilar e executar

**Com Maven**

```bash
mvn package
```

```bash
java -jar target/urna-digital.jar
```

**Sem Maven (só JDK)**

```bash
javac -encoding UTF-8 -d out/main $(find src/main/java -name "*.java")
```

```bash
java -cp out/main urna.Main
```

No Windows, `executar.bat` faz os dois passos.

**Pela IDE:** execute a classe `urna.Main`.

### Propriedades do sistema

| Propriedade | Padrão | Efeito |
|---|---|---|
| `urna.dados` | `eleicoes` (relativo ao diretório atual) | Pasta onde as eleições são salvas. O `.exe` usa `$APPDIR\..\eleicoes`, isto é, ao lado do executável |

Exemplo: `java -Durna.dados=D:\eleicoes -jar target/urna-digital.jar`

## 5. Testes

São 47 testes JUnit 5, organizados com a mesma estrutura de pacotes do código:

| Classe de teste | O que cobre |
|---|---|
| `model.CandidatoTest` | Validação de número (dígitos, prefixo do partido), vice obrigatório, regras de `Cargo`, `Partido` e `Eleitor` |
| `model.EleicaoTest` | Cadastros sem repetição, remoção protegida, ordem dos cargos, pendências, senha, bloqueio |
| `service.UrnaTest` | Interpretação do voto, contagem, transação por eleitor, desistência, voto duplo, encerramento, falha de gravação, retomada |
| `service.ApuracaoTest` | Majoritário (1 e 2 vagas, empate), proporcional (QE, cadeiras, maior média, votação mínima) |
| `service.RelatoriosTest` | Conteúdo da Zerésima e do Boletim |
| `persistencia.CsvTest` | Ida e volta com aspas e `;`, separador vírgula |
| `persistencia.RepositorioEleicaoTest` | Salvar/carregar eleição completa, cópia de fotos, erro com nome do arquivo |
| `persistencia.ArmazenamentoEmArquivoTest` | Retomada após "queda", sigilo no arquivo, detecção de adulteração, arquivamento |
| `persistencia.ImportadorEleitoresTest` | Cabeçalho, linhas inválidas e duplicadas |
| `util.TextosTest` | `slug`, plural de votos, SHA-256 |

```bash
mvn test
```

Sem Maven, com o [JUnit Console Launcher](https://junit.org/junit5/docs/current/user-guide/#running-tests-console-launcher):

```bash
javac -encoding UTF-8 -cp out/main:junit-platform-console-standalone.jar -d out/test $(find src/test/java -name "*.java")
```

```bash
java -jar junit-platform-console-standalone.jar execute -cp out/main:out/test --scan-classpath
```

(No Windows, troque `:` por `;` no classpath.)

**Dublê de teste:** `service.ArmazenamentoEmMemoria` implementa `ArmazenamentoVotacao` na memória, conta as
gravações e pode simular falha de disco (`falhar = true`). Os testes de serviço não tocam no disco; os de
persistência usam `@TempDir`.

A interface gráfica não tem testes automatizados. Ela foi verificada renderizando as telas fora da tela
(`printAll` num `BufferedImage`) e por teste manual.

## 6. Empacotamento e distribuição

| Formato | Como gerar | Para quem |
|---|---|---|
| `.jar` | `mvn package` → `target/urna-digital.jar` | Quem já tem Java instalado (`java -jar`) |
| `.exe` com Java embutido | `gerar-exe.bat` → `dist/UrnaDigital-windows.zip` (~25 MB) | Usuários Windows sem Java |

O `gerar-exe.bat`:

1. descobre a pasta do JDK por `JAVA_HOME` ou pela propriedade `java.home` (o `jpackage` não costuma estar no PATH);
2. compila com `--release 11` e monta o `.jar` com `jar --main-class`;
3. roda `jpackage --type app-image` com `--add-modules java.desktop` (Swing, som e imagens; o `java.base` vem
   junto) e `--jlink-options "--strip-debug --no-man-pages --no-header-files"` para reduzir o tamanho;
4. passa `-Durna.dados=$APPDIR\..\eleicoes` (o `$APPDIR` é expandido pelo lançador do jpackage);
5. compacta a pasta `dist/UrnaDigital` com o PowerShell.

O `.exe` **não é assinado**, por isso o Windows SmartScreen mostra um aviso. Assinar exigiria um certificado
de assinatura de código.

> Se for adicionar código que use outro módulo do JDK (por exemplo `java.sql` ou `java.net.http`),
> inclua o módulo em `--add-modules` no `gerar-exe.bat`, senão o `.exe` falha com `NoClassDefFoundError`.
> Para descobrir os módulos usados: `jdeps --print-module-deps target/urna-digital.jar`.

## 7. Integração contínua

`.github/workflows/testes.yml` roda a cada `push` e `pull_request`:

| Job | Executor | Passos | Artefato |
|---|---|---|---|
| `testes` | ubuntu-latest, Temurin 17 | `mvn -B verify` (compila, testa e gera o `.jar`) | `urna-digital` (o `.jar`) |
| `exe-windows` | windows-latest, Temurin 17 (só se `testes` passar) | `gerar-exe.bat` | `UrnaDigital-windows` (pasta com o `.exe`) |

O `gerar-exe.bat` não pausa quando a variável `CI` está definida (o GitHub Actions a define).

## 8. Pacotes e classes

### `urna.model` — entidades

| Classe | Responsabilidade |
|---|---|
| `Eleicao` | Raiz do agregado: nome, senha (resumo SHA-256), cargos ordenados, partidos, candidatos, eleitores. Valida unicidade e referências. Depois de `bloquear()`, todo método de alteração lança `IllegalStateException` |
| `Cargo` | Imutável. Nome (identidade, sem diferenciar maiúsculas), dígitos (2–5), `Tipo` (MAJORITARIO/PROPORCIONAL), vagas, rótulo do vice. `aceitaVotoLegenda()` = proporcional e mais de 2 dígitos |
| `Partido` | Imutável. Número de 2 dígitos (identidade), sigla e nome |
| `Candidato` | Imutável. Valida dígitos e prefixo do partido. `comFoto(Path)` devolve uma cópia com outra foto |
| `Eleitor` | Título/matrícula (só dígitos, identidade) e nome |
| `Voto` | Imutável, criado por fábricas: `nominal`, `legenda`, `branco`, `nulo`. Não referencia o eleitor |

### `urna.service` — regras de negócio

| Classe | Responsabilidade |
|---|---|
| `Urna` | Ciclo da votação: liberar, interpretar, registrar, finalizar, cancelar e encerrar. Consultas de contagem e ranking |
| `Contagem` | Contadores por cargo (nominais, legenda, brancos, nulos), indexados pelo nome do cargo e pelo número |
| `EstadoVotacao` | Fotografia imutável: contagem, títulos que votaram (`TreeSet`), abertura e encerramento |
| `ArmazenamentoVotacao` | Interface de gravação do estado. `NENHUM` = não grava (testes e demonstrações) |
| `Apuracao` | Eleitos majoritários e proporcionais, quociente eleitoral, cadeiras e empate |
| `Relatorios` | Texto da Zerésima e do Boletim de Urna |
| `Exemplos` | Eleições de exemplo (municipal e grêmio), com senha `1234` |

### `urna.persistencia` — arquivos

| Classe | Responsabilidade |
|---|---|
| `RepositorioEleicao` | Salva e carrega a configuração (`eleicao.properties` + 4 CSV + `fotos/`) |
| `ArmazenamentoEmArquivo` | Implementa `ArmazenamentoVotacao` em `votacao/estado.txt` (atômico e com SHA-256); `arquivar()` |
| `ArquivoRelatorio` | Grava Zerésima e Boletim em `relatorios/` com SHA-256 no final |
| `ImportadorEleitores` | Importa CSV de eleitores e devolve importados e erros por linha |
| `Csv` | Leitura e escrita de CSV (`;`, UTF-8 com BOM, aspas) |

### `urna.util`

`Hash.sha256(String)` e `Textos` (`slug`, `votos`, `vazio`).

### `urna.view` — interface

| Classe | Responsabilidade |
|---|---|
| `TelaInicial` | Escolha da eleição; decide entre configurar, retomar, ver resultado ou zerar |
| `TelaConfiguracao` | Abas de cadastro, salvar e iniciar votação |
| `TelaUrna` | A urna: tela do eleitor, teclado, menus do mesário, acessibilidade e modo didático |
| `FotoCandidato` | Mostra a imagem (recorte proporcional, com cache) ou um avatar com as iniciais |
| `Som` | Gera bipes senoidais (22 050 Hz, 16 bits) numa thread própria |
| `Voz` | Leitura em voz alta pelo sintetizador do sistema operacional |
| `Didatica` | Textos do modo didático |
| `ModeloTabela<T>` | `TableModel` genérico com colunas definidas por funções |

## 9. Funcionamento interno

### 9.1 Ciclo de vida

```
Configuração (Eleicao editável) ──Urna.nova()──▶ Votação (Eleicao bloqueada) ──encerrar()──▶ Encerrada
        ▲                                                │
        └────────── zerar (arquivar votacao/) ◀──────────┘
```

`Urna.nova` exige `verificarPendencias()` vazia, bloqueia a eleição e grava o estado inicial.
`Urna.retomar` carrega o estado e confere a compatibilidade: todo cargo, candidato, partido e título citado
no arquivo precisa existir na eleição.

### 9.2 Interpretação do voto (`Urna.interpretar`)

```
dígitos têm o tamanho do cargo?           não → IllegalArgumentException
existe candidato com esse número?          sim → NOMINAL
cargo aceita legenda e os 2 primeiros
dígitos são de um partido?                 sim → LEGENDA
caso contrário                                  → NULO
```

O voto em branco não passa por aqui: a interface cria `Voto.branco(cargo)` quando a tecla BRANCO é usada
com o campo vazio.

### 9.3 Votação como transação

1. `liberarEleitor(titulo)` define `eleitorAtual` e limpa `votosPendentes`.
2. `registrar(voto)` só adiciona à lista de pendentes (um voto por cargo).
3. `finalizarEleitor()` exige todos os cargos votados e então:
   - copia a contagem e aplica os votos pendentes na **cópia**;
   - grava o novo `EstadoVotacao` pelo `ArmazenamentoVotacao`;
   - **só depois** troca a contagem em memória e marca o eleitor.

   Se a gravação lançar `UncheckedIOException`, a memória continua como antes e o eleitor não é marcado.
4. `cancelarEleitor()` descarta os pendentes.

Resultado: o estado em disco e em memória nunca contém um eleitor "pela metade".

### 9.4 Sigilo

- `Voto` não tem referência ao eleitor.
- `Contagem` guarda só totais, nunca a sequência dos votos.
- Os títulos ficam num `TreeSet`, gravados em ordem numérica, não na ordem de votação.
- Como o estado só é gravado no fim de cada eleitor, não há gravações intermediárias que permitam correlacionar.

> Limitação: quem observar o arquivo **entre dois eleitores** consegue ver a diferença de totais. Na urna real
> isso é mitigado pelo Registro Digital do Voto embaralhado. Aqui, o arquivo deve ficar num computador sob
> controle do mesário.

### 9.5 Formato de `votacao/estado.txt`

```
URNA-DIGITAL-VOTACAO;1
ABERTURA;<LocalDateTime ISO>
ENCERRAMENTO;<LocalDateTime ISO>       (só após encerrar)
VOTOU;<título>                         (uma linha por eleitor, em ordem)
NOMINAL;<cargo>;<número>;<quantidade>
LEGENDA;<cargo>;<número do partido>;<quantidade>
BRANCO;<cargo>;<quantidade>
NULO;<cargo>;<quantidade>
SHA256;<hash das linhas anteriores, unidas por \n, com \n final>
```

- **Gravação atômica:** escreve `estado.tmp` e faz `Files.move(..., REPLACE_EXISTING, ATOMIC_MOVE)`. Se o sistema
  de arquivos não suportar movimento atômico, cai para o movimento simples.
- **Leitura:** confere cabeçalho e SHA-256. Se não conferir, lança `IllegalStateException("...alterado ou está corrompido...")`.
- O SHA-256 detecta alteração acidental ou ingênua. Não é uma assinatura: quem conhece o formato pode recalcular o hash.

### 9.6 Formato da configuração

| Arquivo | Colunas |
|---|---|
| `eleicao.properties` | `nome`, `senhaMesarioSha256` |
| `cargos.csv` | `ordem;nome;digitos;tipo;vagas;rotulo_vice` (`tipo` = `MAJORITARIO` ou `PROPORCIONAL`) |
| `partidos.csv` | `numero;sigla;nome` |
| `candidatos.csv` | `cargo;numero;nome;partido;vice;foto` (`foto` relativa à pasta da eleição) |
| `eleitores.csv` | `titulo;nome` |

Os CSV são escritos com `;`, UTF-8 com BOM e CRLF, para o Excel em português abrir direto. Na leitura, o
separador é detectado pela primeira linha (`;` ou `,`). Ao salvar, as fotos são copiadas para
`fotos/<cargo>_<numero>.<ext>`. Erros de carga informam o arquivo e a linha.

### 9.7 Senha do mesário

Guarda-se `SHA-256("urna-digital:" + senha)` e nunca a senha. O tamanho mínimo é 4 caracteres.

> Limitação: o hash usa um prefixo fixo e não um *salt* aleatório nem um algoritmo lento (PBKDF2/bcrypt).
> É suficiente para o propósito educacional, mas não para dados sensíveis.

### 9.8 Apuração (`Apuracao`)

**Majoritário:** ordena por votos (desempate alfabético só para exibição) e elege os primeiros `vagas`
candidatos com pelo menos 1 voto. `empateNaUltimaVaga` devolve os candidatos empatados com o último eleito,
quando o primeiro não eleito tem o mesmo número de votos.

**Proporcional:**

```
QE      = validos == 0 ? 0 : max(1, round(validos / vagas))
minimo  = max(1, ceil(QE × 0,10))                 votação nominal mínima do candidato
aptos(p) = candidatos de p com votos ≥ minimo, por votos

para cada partido p:  cadeiras[p] = min(votos(p) / QE, |aptos(p)|)
enquanto houver vagas:
    p = partido com maior votos(p) / (cadeiras[p] + 1), entre os que ainda têm aptos
    se nenhum: para (vagas ficam sem preencher)
    cadeiras[p]++
eleitos = os cadeiras[p] primeiros de aptos(p), para cada p
```

`votos(p)` = nominais dos candidatos de `p` + legenda de `p`.

## 10. Interface gráfica

### 10.1 Navegação

```
TelaInicial ──nova/exemplo──▶ TelaConfiguracao ──iniciar──▶ TelaUrna
     │                                                         │
     └──abrir pasta com votação──▶ (retomar) ──────────────────┘
TelaUrna ──Voltar à tela inicial──▶ TelaInicial
```

### 10.2 Estados da `TelaUrna`

`BLOQUEADA → VOTANDO → FIM → BLOQUEADA`, e `BLOQUEADA → ENCERRADA`. Todo método de tecla confere o estado
antes de agir. A tela "FIM" dura 3 s (6 s no modo didático), por um `javax.swing.Timer`.

### 10.3 Teclado

Um `KeyEventDispatcher` global (registrado no construtor e removido no `dispose()`) trata dígitos (inclusive
do teclado numérico), Enter, Backspace e B. Ele ignora teclas com Ctrl ou Alt, para não roubar os atalhos
dos menus (Ctrl+L, F2 a F5).

### 10.4 Tema e tamanho da fonte

Cada componente recebe a fonte por `fonte(componente, estilo, tamanhoBase)`, que guarda o tamanho base numa
*client property*. Ao trocar a escala (1,0 ou 1,4), `reaplicarFontes` percorre a árvore de componentes e
recalcula as fontes. O alto contraste troca fundo e texto (preto e amarelo) por `pintarTextos`. Com a fonte
grande, a janela cresce até o limite do monitor.

### 10.5 Voz

| Sistema | Implementação |
|---|---|
| Windows | Um processo `powershell` permanente, iniciado com `-EncodedCommand` (script em UTF-16LE/Base64, sem problemas de aspas). Ele carrega `System.Speech`, escolhe uma voz `pt-BR` se houver e fala cada linha recebida pela entrada padrão (UTF-8), cancelando a fala anterior |
| macOS | `say <texto>`, um processo por frase |
| Linux | `espeak-ng -v pt-br` (ou `espeak -v pt`) |

O texto é passado como argumento ou pela entrada padrão, **nunca** interpretado por um shell. Se o
sintetizador falhar, a voz se desativa sem afetar a urna. Um *shutdown hook* encerra o processo.

## 11. Como estender

| Quero... | Onde mexer |
|---|---|
| **Novo cargo, chapa ou candidato** | Nada no código: use a tela de configuração ou edite os CSV |
| **Nova eleição de exemplo** | Crie um método em `Exemplos` e um botão em `TelaInicial` |
| **Mudar uma regra de apuração** (ex.: cláusula de 80% nas sobras) | `Apuracao.distribuirCadeiras` e novos casos em `ApuracaoTest` |
| **Novo tipo de voto** | `Voto.Tipo`, `Urna.interpretar`, `Contagem.registrar`, formato do `estado.txt` (suba a versão do cabeçalho), `Relatorios` e `TelaUrna.mostrarVotoPendente` |
| **Gravar em banco de dados ou na nuvem** | Implemente `ArmazenamentoVotacao` e passe-o para `Urna.nova/retomar`. A `Urna` não muda |
| **Novo relatório** (ex.: CSV de resultado) | Método em `Relatorios` e item de menu em `TelaUrna` |
| **Segundo turno** | Em `Apuracao`, verificar maioria absoluta (> 50% dos válidos) e devolver os 2 primeiros. A nova votação seria uma nova `Eleicao` com o cargo e os 2 candidatos |
| **Outro idioma** | Os textos estão em `TelaUrna`, `TelaConfiguracao`, `TelaInicial`, `Didatica` e `Relatorios`. O próximo passo seria movê-los para um `ResourceBundle` |

Sempre que mudar uma regra: escreva o teste primeiro, rode `mvn test` e atualize a rastreabilidade em
[requisitos.md](requisitos.md).

## 12. Convenções de código

- **Idioma:** nomes de classes, métodos e mensagens em português, de acordo com o domínio do problema.
- **Camadas:** `model` não conhece ninguém; `service` não conhece arquivos nem Swing; só `view` usa Swing.
- **Imutabilidade:** `Cargo`, `Partido`, `Candidato`, `Voto` e `EstadoVotacao` são imutáveis.
- **Exceções:**
  - `IllegalArgumentException` para dado inválido (a mensagem é mostrada ao usuário);
  - `IllegalStateException` para operação fora de hora;
  - `UncheckedIOException` para falha de disco durante a votação;
  - `IOException` para carregar e salvar configuração.
- **Mensagens de erro** escritas para o usuário final, explicando a regra violada.
- **Testes:** uma classe de teste por classe de produção, com `@DisplayName` descrevendo a regra.
- **Commits:** mensagem em português no imperativo, com o resumo na primeira linha.

## 13. Limitações e melhorias futuras

| Limitação | Possível melhoria |
|---|---|
| Sobras proporcionais simplificadas (sem a cláusula de 80% do QE) | Implementar a regra completa do Código Eleitoral |
| Sem desempate por idade | Adicionar data de nascimento ao `Candidato` |
| Sem segundo turno | Ver a seção 11 |
| Uma urna por computador, sem totalização | Exportar o boletim em formato estruturado e criar um totalizador |
| Hash de senha sem *salt* e integridade sem assinatura | PBKDF2 para a senha; assinatura digital (par de chaves) para os boletins |
| Interface sem testes automatizados | Testes com AssertJ Swing ou separação em padrão MVP |
| Textos fixos no código | `ResourceBundle` para internacionalização |
| `.exe` não assinado | Certificado de assinatura de código |

## 14. Solução de problemas

| Sintoma | Causa e solução |
|---|---|
| `javac` não é reconhecido | JDK não instalado ou fora do PATH. Instale o Temurin 17+ e reabra o terminal |
| `jpackage` não encontrado | Ele fica em `<JDK>\bin`. O `gerar-exe.bat` o encontra sozinho; se falhar, defina `JAVA_HOME` |
| Acentos estranhos nas mensagens ou nos arquivos | Compile com `-encoding UTF-8`. No console do Windows, use `chcp 65001` |
| `NoClassDefFoundError` só no `.exe` | Falta um módulo em `--add-modules` (ver seção 6) |
| `mvn` não encontrado | Instale o Maven ou rode pela IDE |
| "arquivo de votação foi alterado ou está corrompido" | O `estado.txt` foi editado. Restaure um backup da pasta `votacao/`; não edite esse arquivo à mão |
| CI falha só no job `exe-windows` | Veja o log do passo "Gerar o .exe". Normalmente é um módulo faltando ou um caminho com espaços |
