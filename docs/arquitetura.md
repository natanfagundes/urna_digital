# Arquitetura — Urna Digital

## Camadas

O sistema é dividido em camadas. Cada uma só conhece as camadas abaixo dela, por isso as regras
da eleição podem ser testadas sem abrir nenhuma janela.

```mermaid
flowchart TB
    view["<b>view</b> — interface Swing<br/>TelaInicial, TelaConfiguracao, TelaUrna,<br/>FotoCandidato, Som, Voz, Didatica"]
    service["<b>service</b> — regras de negócio<br/>Urna, Apuracao, Relatorios, Contagem,<br/>EstadoVotacao, ArmazenamentoVotacao, Exemplos"]
    persistencia["<b>persistencia</b> — arquivos<br/>RepositorioEleicao, ArmazenamentoEmArquivo,<br/>ArquivoRelatorio, ImportadorEleitores, Csv"]
    model["<b>model</b> — entidades<br/>Eleicao, Cargo, Partido, Candidato, Eleitor, Voto"]
    util["<b>util</b> — Hash, Textos"]

    view --> service
    view --> persistencia
    persistencia --> service
    service --> model
    persistencia --> model
    model --> util
    persistencia --> util
```

`ArmazenamentoVotacao` é uma **interface** da camada de serviço. A `Urna` depende só dela, e a
implementação concreta (`ArmazenamentoEmArquivo`) fica na persistência (inversão de dependência).
Nos testes, a urna recebe um armazenamento em memória.

## Diagrama de classes (domínio e serviço)

```mermaid
classDiagram
    direction LR
    class Eleicao {
        -String nome
        -String hashSenhaMesario
        -boolean bloqueada
        +adicionarCargo(Cargo)
        +cadastrarPartido(int, String, String)
        +cadastrarCandidato(Candidato)
        +cadastrarEleitor(String, String)
        +verificarPendencias() List~String~
        +validarSenhaMesario(String) boolean
        +bloquear()
    }
    class Cargo {
        -String nome
        -int quantidadeDigitos
        -Tipo tipo
        -int vagas
        -String rotuloVice
        +aceitaVotoLegenda() boolean
    }
    class Partido {
        -int numero
        -String sigla
        -String nome
    }
    class Candidato {
        -int numero
        -String nome
        -String nomeVice
        -Path foto
    }
    class Eleitor {
        -String titulo
        -String nome
    }
    class Voto {
        -Tipo tipo
        +nominal(Candidato) Voto
        +legenda(Cargo, Partido) Voto
        +branco(Cargo) Voto
        +nulo(Cargo) Voto
    }
    class Urna {
        -Eleitor eleitorAtual
        -List~Voto~ votosPendentes
        +nova(Eleicao, ArmazenamentoVotacao) Urna
        +retomar(Eleicao, ArmazenamentoVotacao) Urna
        +liberarEleitor(String) Eleitor
        +interpretar(Cargo, String) Voto
        +registrar(Voto)
        +finalizarEleitor()
        +cancelarEleitor()
        +encerrar()
    }
    class Contagem {
        +registrar(Voto)
        +validos(Cargo) int
    }
    class EstadoVotacao {
        -Set~String~ titulosQueVotaram
        -LocalDateTime abertura
        -LocalDateTime encerramento
    }
    class ArmazenamentoVotacao {
        <<interface>>
        +salvar(EstadoVotacao)
        +carregar() Optional~EstadoVotacao~
    }
    class ArmazenamentoEmArquivo
    class Apuracao {
        +eleitos(Cargo) List~Candidato~
        +quocienteEleitoral(Cargo) int
        +distribuirCadeiras(Cargo) Map
        +empateNaUltimaVaga(Cargo) List~Candidato~
    }

    Eleicao "1" *-- "*" Cargo
    Eleicao "1" *-- "*" Partido
    Eleicao "1" *-- "*" Candidato
    Eleicao "1" *-- "*" Eleitor
    Candidato --> Cargo
    Candidato --> Partido
    Voto --> Cargo
    Voto --> Candidato
    Voto --> Partido
    Urna --> Eleicao
    Urna --> Contagem
    Urna --> ArmazenamentoVotacao
    ArmazenamentoVotacao ..> EstadoVotacao
    EstadoVotacao --> Contagem
    ArmazenamentoEmArquivo ..|> ArmazenamentoVotacao
    Apuracao --> Urna
```

## Sequência: um eleitor votando

```mermaid
sequenceDiagram
    actor M as Mesário
    actor E as Eleitor
    participant T as TelaUrna
    participant U as Urna
    participant A as ArmazenamentoEmArquivo

    M->>T: Liberar eleitor (título)
    T->>U: liberarEleitor(titulo)
    U-->>T: Eleitor (ou erro: já votou / não encontrado)
    loop para cada cargo
        E->>T: digita os números
        T->>U: interpretar(cargo, dígitos)
        U-->>T: Voto (nominal / legenda / nulo)
        T-->>E: mostra nome, partido, vice e foto
        E->>T: CONFIRMA
        T->>U: registrar(voto) — fica pendente
    end
    T->>U: finalizarEleitor()
    U->>A: salvar(novo estado) — gravação atômica com SHA-256
    A-->>U: ok
    U-->>T: votos contados
    T-->>E: FIM
```

## Estados da urna (tela)

```mermaid
stateDiagram-v2
    [*] --> Bloqueada: iniciar ou retomar votação
    Bloqueada --> Votando: mesário libera eleitor
    Votando --> Votando: confirma cargo (há mais cargos)
    Votando --> Fim: confirma o último cargo
    Votando --> Bloqueada: mesário cancela (desistência)
    Fim --> Bloqueada: após alguns segundos
    Bloqueada --> Encerrada: mesário encerra (senha)
    Encerrada --> [*]
```

## Arquivos de uma eleição

```
eleicoes/gremio-estudantil-2026/
├── eleicao.properties     nome e SHA-256 da senha do mesário
├── cargos.csv             ordem;nome;digitos;tipo;vagas;rotulo_vice
├── partidos.csv           numero;sigla;nome
├── candidatos.csv         cargo;numero;nome;partido;vice;foto
├── eleitores.csv          titulo;nome
├── fotos/                 fotos copiadas dos candidatos
├── votacao/estado.txt     votação em andamento (ver abaixo)
└── relatorios/            zeresima_*.txt e boletim_urna_*.txt
```

Os CSV usam `;` e UTF-8 com BOM, então abrem direto no Excel em português.

### `votacao/estado.txt`

```
URNA-DIGITAL-VOTACAO;1
ABERTURA;2026-11-14T08:00:00
VOTOU;2026001            ← em ordem numérica, não na ordem em que votaram
VOTOU;2026007
NOMINAL;Presidente do Grêmio;10;1
BRANCO;Presidente do Grêmio;1
...
SHA256;<resumo de todas as linhas acima>
```

- **Sigilo:** só há totais. Não existe nenhuma ligação entre `VOTOU` e os votos.
- **Atomicidade:** grava em `estado.tmp` e depois renomeia por cima de `estado.txt`.
- **Integridade:** ao carregar, recalcula o SHA-256. Se não conferir, recusa o arquivo.

## Decisões de projeto

| Decisão | Motivo |
|---|---|
| Java + Swing, sem bibliotecas | Roda offline em qualquer computador de escola com Java; nada para instalar além do JDK. |
| Voto como transação (pendente até o FIM) | Uma queda de energia no meio do voto não deixa um eleitor "meio votado". |
| Guardar totais em vez de cada voto | Garante o sigilo por construção, não por cuidado. |
| CSV em vez de banco de dados | O organizador consegue ver, conferir e editar a eleição no Excel. |
| Interface `ArmazenamentoVotacao` | Permite testar a urna sem disco e trocar o armazenamento no futuro. |
| Voz do sistema operacional | Leitura em português sem baixar nada (Windows: System.Speech). |
