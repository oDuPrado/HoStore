**HoStore — Resumo Profissional para LinkedIn**

Resumo rápido
- Produto: HoStore — sistema completo de gestão de ponto de venda, estoque, vendas e relatórios, com componentes de UI e integração com banco local.
- Objetivo do arquivo: apresentação objetiva para recrutadores no LinkedIn e outras plataformas; destacar escopo, tecnologias, responsabilidades e como avaliar o código.

Sobre o projeto
- Arquitetura: aplicação Java modular organizada em pacotes `api`, `app`, `controller`, `dao`, `service`, `ui`, `util` — separação clara entre camadas de apresentação, lógica e persistência.
- Persistência: exemplos de acesso a SQLite (classe `ConsultaSQLiteSimples.java`), configuração de cache/export e arquivos de dados em `data/`.
- UI: módulos focados em vendas, estoque, financeiro, relatórios e ajustes — solução pensada para uso em PDV/lojas.

Meu papel e contribuições
- Função esperada: design e implementação de camadas backend (`dao`, `service`, `controller`), integração com UI, persistência local, e criação de utilitários e configuração para impressão e exportação.
- Destaques de contribuição (avalie no código): implementação de fluxos de vendas, tratamento de estoque, relatórios e integração com arquivos de configuração (`printConfig.properties`, `cache/sync_state.properties`).

Tecnologias principais
- Linguagem: Java (projeto Maven)
- Persistência: SQLite (acesso direto por classes utilitárias)
- Build: Maven (`pom.xml`)
- Infra/hosting: arquivos de hosting e Firebase estão presentes para front-ends/landing pages.

Destaques técnicos (para recrutadores)
- Código organizado por camadas (bom para manutenção e testes unitários).
- Múltiplos módulos UI especializados (venda, estoque, financeiro, relatórios) demonstram compreensão de domínio de negócio.
- Inclusão de dados de exemplo, mapeamentos e scripts de suporte em `data/` e `backup/`.

Como inspecionar e rodar rapidamente (para avaliadores)
- Build: `mvn clean package`
- Executar em IDE: abrir como projeto Maven e rodar a classe `main` do módulo `app` (ou executar via configuração de execução do IDE).
- Teste rápido de persistência: localizar `ConsultaSQL.java` em `src/main/java` e executar para validar conexão local SQLite.

Onde olhar primeiro no código
- `src/main/java/controller` — fluxos de negócio e orquestração.
- `src/main/java/dao` — regras de acesso a dados e consultas.
- `src/main/java/ui` — telas e interação do usuário (núcleo do produto).
- `data/` e `DOCUMENTAÇÃO/` — exemplos, guias e requisitos.

Contato e chamada para ação
- Recrutadores: se quiserem uma breve demonstração ou walkthrough do código, posso apresentar os principais fluxos (30 minutos). Para contato direto, use o LinkedIn no perfil vinculado ao repositório ou envie e-mail para o contato presente no README.

Observação final
- Este arquivo foi criado para ser usado como descrição objetiva em LinkedIn ou anexado ao portfólio. Posso adaptar o tom (mais técnico, mais executivo, em inglês) se desejar.
