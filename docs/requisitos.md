# Documento de Requisitos — Urna Digital

## 1. Visão geral

A **Urna Digital** é um simulador da urna eletrônica brasileira para:

- realizar **eleições reais de pequeno porte** em escolas, grêmios estudantis e associações;
- servir de **ferramenta didática** em aulas e oficinas de educação cidadã, mostrando como
  funciona o voto (nominal, legenda, branco e nulo), o sigilo e a apuração.

### 1.1 Partes interessadas

| Parte interessada | Interesse |
|---|---|
| Organizador (professor, coordenação, comissão eleitoral) | Configurar a eleição de forma simples, sem programar |
| Mesário | Controlar quem vota e encerrar a votação com segurança |
| Eleitor (alunos, membros da comunidade) | Votar de forma simples, acessível e sigilosa |
| Facilitador de oficina | Explicar o processo eleitoral com apoio visual |
| Comunidade | Conferir o resultado de forma transparente |

## 2. Requisitos funcionais

| ID | Requisito | Prioridade | Verificação |
|---|---|---|---|
| RF01 | Cadastrar a eleição: nome e senha do mesário | Alta | `EleicaoTest` |
| RF02 | Cadastrar cargos com quantidade de dígitos (2 a 5), tipo (majoritário/proporcional), número de vagas e, opcionalmente, vice | Alta | `CandidatoTest`, `EleicaoTest` |
| RF03 | Definir a ordem em que os cargos são votados | Média | `EleicaoTest` |
| RF04 | Cadastrar partidos/chapas com número de 2 dígitos | Alta | `EleicaoTest` |
| RF05 | Cadastrar candidatos (número, nome, partido, cargo, vice e foto), validando o número pelo cargo e pelo partido | Alta | `CandidatoTest` |
| RF06 | Cadastrar eleitores e importá-los de planilha CSV, informando as linhas com erro | Alta | `ImportadorEleitoresTest` |
| RF07 | Salvar e reabrir a eleição em arquivos editáveis no Excel | Alta | `RepositorioEleicaoTest` |
| RF08 | Impedir o início da votação enquanto houver pendências (cargo sem candidato, sem eleitores, sem senha) | Alta | `EleicaoTest`, `UrnaTest` |
| RF09 | Bloquear a configuração da eleição após o início da votação | Alta | `EleicaoTest`, `UrnaTest` |
| RF10 | Emitir a Zerésima antes do primeiro voto | Média | `RelatoriosTest` |
| RF11 | Liberar a urna para um eleitor pelo título/matrícula, recusando quem não está na lista ou já votou | Alta | `UrnaTest` |
| RF12 | Votar em cada cargo digitando o número e confirmando, com as teclas BRANCO, CORRIGE e CONFIRMA | Alta | `UrnaTest` |
| RF13 | Classificar o voto em nominal, legenda (só proporcional), branco ou nulo | Alta | `UrnaTest` |
| RF14 | Mostrar nome, partido, vice e foto do candidato antes da confirmação | Alta | Teste manual |
| RF15 | Cancelar o voto de um eleitor que desistiu, permitindo que vote depois | Média | `UrnaTest` |
| RF16 | Encerrar a votação mediante senha do mesário | Alta | `UrnaTest` |
| RF17 | Emitir o Boletim de Urna com votos, percentuais, brancos, nulos, comparecimento, abstenções e eleitos | Alta | `RelatoriosTest` |
| RF18 | Apurar cargos majoritários (mais votados, com aviso de empate) e proporcionais (quociente eleitoral, maior média e votação mínima de 10% do QE) | Alta | `ApuracaoTest` |
| RF19 | Salvar Zerésima e Boletim em arquivo com código de verificação SHA-256 | Média | Teste manual |
| RF20 | Retomar uma votação interrompida (programa fechado, falta de energia) | Alta | `ArmazenamentoEmArquivoTest`, `UrnaTest` |
| RF21 | Zerar a votação mediante senha, guardando a anterior em arquivo | Média | `ArmazenamentoEmArquivoTest` |
| RF22 | Oferecer modo didático com explicação de cada etapa | Média | Teste manual |
| RF23 | Oferecer alto contraste, fonte ampliada, leitura em voz alta e sons das teclas | Alta | Teste manual |

## 3. Requisitos não funcionais

| ID | Categoria | Requisito |
|---|---|---|
| RNF01 | Sigilo | A urna não pode guardar nenhuma ligação entre o eleitor e o voto: os votos são guardados apenas como totais e a lista de quem votou fica em ordem numérica. |
| RNF02 | Confiabilidade | Os votos de um eleitor só são contados quando ele conclui todos os cargos (transação). A gravação é atômica: uma queda de energia nunca deixa o arquivo pela metade. |
| RNF03 | Integridade | O arquivo de votação e os relatórios têm código SHA-256. Um arquivo de votação alterado manualmente é recusado. |
| RNF04 | Segurança | A senha do mesário nunca é gravada: guarda-se apenas o seu resumo SHA-256. |
| RNF05 | Acessibilidade | Todas as funções da urna podem ser usadas pelo teclado; há leitura em voz alta, alto contraste e fonte ampliada. |
| RNF06 | Usabilidade | A tela de votação imita a urna oficial, que o público já conhece. O organizador não precisa editar arquivos nem programar. |
| RNF07 | Portabilidade | Roda em Windows, Linux e macOS com Java 11 ou superior, sem internet e sem bibliotecas externas. |
| RNF08 | Manutenibilidade | Código em camadas (modelo, serviço, persistência, interface); as regras não dependem da interface e são cobertas por testes automatizados. |
| RNF09 | Desempenho | Confirmar um voto e gravar o estado leva menos de 1 segundo em um computador comum. |

## 4. Regras de negócio

| ID | Regra |
|---|---|
| RN01 | O número do partido/chapa tem 2 dígitos (10 a 99). O número do candidato começa com o número do partido. |
| RN02 | Voto de legenda: número de candidato inexistente cujos 2 primeiros dígitos são de um partido válido. Só vale em cargo proporcional com mais de 2 dígitos. |
| RN03 | Voto em branco só pode ser dado com o campo de número vazio. |
| RN04 | Votos válidos = nominais + legenda. Brancos e nulos não são válidos. |
| RN05 | Quociente eleitoral (QE) = votos válidos ÷ vagas, arredondado (mínimo 1 se houver voto válido). Cada partido recebe (votos do partido ÷ QE) cadeiras; as sobras vão para a maior média. |
| RN06 | Só ocupa cadeira proporcional o candidato com votos nominais ≥ 10% do QE (e pelo menos 1). |
| RN07 | Em cargo majoritário, vencem os mais votados. Em caso de empate na última vaga, o boletim avisa (pela lei, vence o mais idoso). |
| RN08 | A Zerésima só pode ser emitida antes do primeiro voto; o Boletim, só depois do encerramento. |

## 5. Limitações conhecidas (simplificações)

- A distribuição das sobras não aplica todas as regras da legislação atual (como a exigência de
  80% do QE para o partido disputar as sobras).
- O desempate por idade não é automático, porque a urna não guarda a data de nascimento dos candidatos.
- Não há segundo turno.
- A urna roda em um único computador; não há totalização de várias seções.
