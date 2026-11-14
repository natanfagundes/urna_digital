# Roteiro do Relatório de Extensão — Urna Digital

> Este é um **modelo** para o relatório da disciplina. Os campos entre `[colchetes]` devem ser
> preenchidos pela equipe com os dados reais da ação (público, datas, números e resultados).

## 1. Identificação

| Item | Preenchimento |
|---|---|
| Título da ação | Urna Digital: eleições escolares e educação cidadã com software livre |
| Disciplina | Engenharia de Software |
| Instituição | `[nome da faculdade/universidade]` |
| Equipe | `[nomes e matrículas]` |
| Professor(a) orientador(a) | `[nome]` |
| Parceiro externo | `[escola / grêmio / associação]` |
| Período | `[datas]` |
| Repositório | https://github.com/natanfagundes/urna_digital |

## 2. Público-alvo

`[Ex.: alunos do 6º ao 9º ano da Escola X (aprox. N alunos), professores e a comissão eleitoral do grêmio.]`

## 3. Justificativa

Escolas e associações costumam fazer eleições (grêmio estudantil, representantes de turma,
diretoria de associação) com cédulas de papel, o que exige contagem manual demorada e sujeita a erros.
Ao mesmo tempo, muitos jovens votam pela primeira vez sem entender como funcionam a urna, o voto
de legenda, o voto branco/nulo e o sistema proporcional.

A Urna Digital atende às duas necessidades: oferece à comunidade uma ferramenta gratuita para
eleições reais e serve de apoio a oficinas de educação cidadã. Para a equipe, é uma oportunidade
de aplicar o ciclo completo de Engenharia de Software (requisitos, projeto, implementação, testes,
documentação e implantação) a um problema real.

## 4. Objetivos

**Geral:** desenvolver e implantar, junto à comunidade, um simulador de urna eletrônica para eleições
escolares e para o ensino do processo eleitoral.

**Específicos:**
1. Levantar os requisitos com o parceiro externo (cargos, número de eleitores, necessidades de acessibilidade).
2. Desenvolver o software com boas práticas de Engenharia de Software (camadas, testes automatizados, versionamento).
3. Garantir acessibilidade para eleitores com deficiência visual ou baixa visão.
4. Realizar ao menos uma oficina de educação cidadã usando o modo didático.
5. Apoiar a realização de uma eleição real na instituição parceira.
6. Avaliar a usabilidade e o aprendizado dos participantes.

## 5. Objetivos de Desenvolvimento Sustentável (ONU)

- **ODS 4 — Educação de qualidade:** educação para a cidadania e participação democrática.
- **ODS 16 — Paz, justiça e instituições eficazes:** processos decisórios participativos e transparentes.
- **ODS 10 — Redução das desigualdades:** acessibilidade para pessoas com deficiência.

## 6. Metodologia

| Etapa | Atividades | Produto |
|---|---|---|
| 1. Diagnóstico | Reunião com o parceiro; entender como a eleição é feita hoje | Lista de necessidades |
| 2. Requisitos | Documentar requisitos e casos de uso; validar com o parceiro | [requisitos.md](requisitos.md), [casos-de-uso.md](casos-de-uso.md) |
| 3. Desenvolvimento iterativo | Versões incrementais, revisadas a cada encontro | Código no GitHub |
| 4. Testes | Testes automatizados (JUnit) + teste com usuários reais | Relatório de testes |
| 5. Capacitação | Treinar a comissão eleitoral / mesários | [manual-do-usuario.md](manual-do-usuario.md) |
| 6. Oficina | Aula sobre o processo eleitoral com o modo didático | Lista de presença, questionários |
| 7. Eleição real | Apoio no dia da votação | Zerésima e Boletim de Urna |
| 8. Avaliação | Questionários e conversa com o parceiro | Indicadores (seção 8) |

Histórico de versões do software (ver commits no GitHub):

| Versão | Entregas |
|---|---|
| 1.0 | Urna municipal fixa (vereador e prefeito), liberação de eleitores, Zerésima, Boletim |
| 2.0 | Eleição configurável, importação de planilha, fotos, votação gravada e retomável, acessibilidade, modo didático, JUnit, documentação |

## 7. Plano de oficina (50 minutos)

| Tempo | Atividade |
|---|---|
| 0–10 min | Conversa inicial: quem já votou? Para que serve a eleição? O que é voto branco e nulo? (registrar respostas) |
| 10–15 min | Apresentação da urna: mesário, eleitor, sigilo do voto |
| 15–35 min | Votação simulada com o **Exemplo: grêmio estudantil** e o **modo didático**. Cada aluno vota; um aluno faz o papel de mesário |
| 35–45 min | Encerramento e leitura do Boletim de Urna. Discussão: por que um candidato com menos votos que outro pode ser eleito no sistema proporcional? |
| 45–50 min | Questionário final (mesmas perguntas do início) |

Perguntas sugeridas para o questionário (antes e depois):
1. O voto nulo anula a eleição se for maioria? (Resposta: não.)
2. Votar em branco ajuda algum candidato? (Resposta: não.)
3. O que é voto de legenda?
4. É possível descobrir em quem uma pessoa votou na urna? (Resposta: não, o voto é sigiloso.)
5. De 1 a 5, quanto você entendeu de como funciona a eleição?

## 8. Indicadores

| Indicador | Meta | Resultado |
|---|---|---|
| Participantes nas oficinas | `[meta]` | `[resultado]` |
| Eleitores na eleição real | `[meta]` | `[resultado]` |
| Acertos no questionário (antes → depois) | aumento de `[x]`% | `[resultado]` |
| Tempo de apuração (papel × urna) | `[meta]` | `[resultado]` |
| Satisfação do parceiro (1 a 5) | ≥ 4 | `[resultado]` |

## 9. Resultados e discussão

`[Descrever o que aconteceu: participação, dificuldades, sugestões dos usuários, mudanças feitas no
software a partir do retorno da comunidade, fotos da ação (com autorização).]`

## 10. Conclusão

`[Aprendizados da equipe, impacto para a comunidade e próximos passos.]`

## Anexos sugeridos

- Zerésima e Boletim de Urna da eleição real (pasta `relatorios/`)
- Capturas de tela ([docs/imagens](imagens))
- Lista de presença e questionários
- Termo de parceria / declaração da instituição parceira
