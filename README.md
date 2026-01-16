# 🏪 HoStore - Sistema ERP para Lojas TCG

[![Java](https://img.shields.io/badge/Java-17+-orange?logo=java)](https://www.oracle.com/java/)
[![Maven](https://img.shields.io/badge/Build-Maven-blue?logo=apache-maven)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-MIT-green)](LICENSE)
[![Status](https://img.shields.io/badge/Status-Em%20Desenvolvimento-yellow)]()

> Sistema de Gestão Empresarial (ERP) desktop especializado em Trading Card Games. Controle completo de vendas, estoque, financeiro e relatórios para lojas física de Pokémon, Magic, Yu-Gi-Oh!, Digimon, One Piece e mais.

---

## 📋 Índice Rápido

- [✨ Funcionalidades](#-funcionalidades-principais)
- [💻 Requisitos](#-requisitos)
- [📦 Instalação](#-instalação)
- [🚀 Como Usar](#-como-usar)
- [🗂️ Estrutura](#-estrutura-do-projeto)
- [⌨️ Atalhos](#️-atalhos-de-teclado)
- [📚 Documentação](#-documentação-completa)

---

## ✨ Funcionalidades Principais

### 🛒 Vendas
- **Carrinho Dinâmico**: Adicione/remova produtos em tempo real
- **Descontos Flexíveis**: Por item, total, percentual ou fixo
- **Múltiplos Pagamentos**: Dinheiro, Cartão, PIX, Transferência, Combinado
- **Parcelamento**: Cálculo automático com datas configuráveis
- **Comprovantes**: Emissão em PDF ou impressão direta
- **Devoluções**: Sistema completo com reintegração ao estoque
- **Estornos**: Com reversão automática de movimentações
- **Auditoria**: Registro completo de todas as ações

### 📦 Estoque
- **Categorias Especializadas**: Cartas, Boosters, Decks, ETBs, Acessórios, Alimentícios
- **Busca Avançada**: Por nome, categoria, faixa de preço
- **Alertas Automáticos**: Estoque baixo, produtos vencidos
- **Pedidos de Compra**: Integrados com entrada de produtos
- **Movimentação**: Rastreada e auditada
- **Dashboard**: Resumo visual com KPIs

### 💰 Financeiro
- **Contas a Pagar**: Registrar, parcelar, pagamentos
- **Contas a Receber**: Acompanhar vendas parceladas
- **Crédito de Loja**: Gerenciar créditos de clientes
- **Fluxo de Caixa**: Relatórios completos
- **Impostos**: Integração com ICMS, IPI, PIS, COFINS

### 📊 Relatórios
- **Dashboard com KPIs**: Vendas, estoque, caixa em tempo real
- **Vendas**: Por período, cliente, produto, margem
- **Estoque**: Movimentação, validade, ABC
- **Financeiro**: Fluxo, contas, resultado
- **Exportação**: Excel, PDF, CSV

### 🎴 APIs de TCG
- **Pokémon**: Todos os sets e cartas
- **Magic**: Integração Scryfall
- **Yu-Gi-Oh!**: YGOPRODeck
- **Digimon**: digimoncard.io
- **One Piece**: optcgapi.com
- Cache local + sincronização automática

### 👤 Sistema
- **Login com Autenticação**: Controle de usuários
- **Permissões**: Por função
- **Backup Automático**: Diário em ./data/backup/
- **Logs Completos**: Auditoria de todas as ações

---

## 💻 Requisitos

### Mínimos
- **SO**: Windows 10+, macOS 10.15+, Linux (Ubuntu 20.04+)
- **Java**: JDK 17+
- **RAM**: 2 GB
- **Armazenamento**: 500 MB
- **Resolução**: 1024x768+

### Desenvolvimento
- Maven 3.8.0+
- IDE: VS Code, IntelliJ, Eclipse
- Git 2.30+

---

## 📦 Instalação

### Clone e Compile

```bash
# Clone o repositório
git clone https://github.com/oDuPrado/HoStore.git
cd HoStore

# Compile com Maven
mvn clean package
```

### Execute

```bash
# Opção 1: Com Maven
mvn exec:java@run

# Opção 2: JAR direto
java -jar target/HoStore-1.0.0-jar-with-dependencies.jar

# Opção 3: IDE
# Abra app/Main.java e execute
```

### Primeiro Uso
- Sistema cria banco automaticamente
- Usuário padrão: `admin` / `admin` ⚠️ **Alterar senha!**

---

## 🚀 Como Usar

### Nova Venda (30 segundos)
1. `Vendas → Nova Venda`
2. Selecione cliente
3. Busque e adicione produtos
4. Revise e finalize
5. Escolha pagamento
6. PDF gerado automaticamente

### Novo Produto no Estoque
1. `Estoque → Novo Item`
2. Selecione categoria (Carta, Booster, etc.)
3. Preencha dados específicos
4. Configure preços
5. Confirme

### Entrada de Compra
1. `Estoque → Entrada de Produtos`
2. Selecione pedido (ou novo)
3. Confirm quantidades
4. Vincula nota fiscal
5. Estoque atualiza automaticamente

---

## 🗂️ Estrutura do Projeto

```
HoStore/
├── src/main/java/
│   ├── api/                 # Integração com TCG APIs
│   ├── app/Main.java        # Ponto de entrada
│   ├── controller/          # Controladores (7 classes)
│   ├── dao/                 # Acesso dados (50+ DAOs)
│   ├── model/               # Modelos (60+ Models)
│   ├── service/             # Lógica negócio (26 Services)
│   └── ui/                  # Interface Swing + FlatLaf
│       ├── venda/           # Vendas
│       ├── estoque/         # Estoque
│       ├── financeiro/      # Financeiro
│       ├── clientes/        # Clientes
│       ├── relatorios/      # Relatórios
│       └── ajustes/         # Configurações
├── data/
│   ├── backup/              # Backups automáticos
│   ├── cache/               # Cache de APIs
│   └── export/              # Exportações
├── pom.xml                  # Dependências Maven
└── README_COMPLETE.md       # Documentação completa
```

### Tecnologias

| Componente | Tecnologia | Versão |
|-----------|-----------|--------|
| **Linguagem** | Java | 17+ |
| **UI** | Swing + FlatLaf | 3.6 |
| **DB** | SQLite | 3.42 |
| **PDF** | PDFBox | 3.0.2 |
| **Excel** | POI | 5.2.3 |
| **JSON** | Gson | 2.10.1 |
| **Build** | Maven | 3.8+ |

---

## ⌨️ Atalhos de Teclado

| Atalho | Ação |
|--------|------|
| `Ctrl+N` | Nova venda |
| `Ctrl+S` | Salvar |
| `Ctrl+P` | Imprimir |
| `F2` | Focar busca (estoque) |
| `F3` | Focar tabela (estoque) |
| `Del` | Excluir selecionado |
| `Ctrl+E` | Nova entrada (estoque) |
| `Esc` | Cancelar/Fechar |

---

## 📚 Documentação Completa

Para documentação técnica e detalhada, veja:

- 📄 **[README_COMPLETE.md](README_COMPLETE.md)** - Guia completo (80+ seções)
- 📦 **[Estoque.md](Estrturas/Estoque.md)** - Módulo de estoque detalhado
- 🛒 **[Vendas.md](Estrturas/vendas.md)** - Fluxos de venda e regras
- 🔧 **[estrutura.txt](estrutura.txt)** - Árvore de arquivos completa

---

## 🤝 Contribuindo

```bash
# 1. Fork e clone
git clone https://github.com/SEU_USUARIO/HoStore.git

# 2. Crie branch
git checkout -b feature/MinhaFeature

# 3. Commit
git commit -m "Adiciona MinhaFeature"

# 4. Push
git push origin feature/MinhaFeature

# 5. Pull Request
# Abra um PR no GitHub
```

---

## 📞 Suporte

### FAQ Rápido

**P: Como faço backup?**
R: Automático diariamente em `./data/backup/`. Menu: `Ajustes → Backup Manual`.

**P: Qual é o limite de produtos?**
R: Sem limite. Testado com ~50k itens sem perda de performance.

**P: Posso usar em múltiplas lojas?**
R: Não na v1.0. Planejado para v2.0 com sistema de franquia.

**P: Dados vão para nuvem?**
R: Não. SQLite local. Nuvem será opcional em v3.2 com criptografia.

---

## 📝 Licença

MIT License - [Veja LICENSE](LICENSE)

---

## 👨‍💻 Autor

**oDuPrado** - [@GitHub](https://github.com/oDuPrado)

---

## 🗂️ Status das Versões

| Versão | Status | Recursos |
|--------|--------|----------|
| 1.0.0 | ✅ Atual | Vendas, Estoque, Fiscal Básico |
| 2.1.0 | 🚀 Próxima | Fiscal Completo, Múltiplos Usuários |
| 3.2.0 | 🎯 Planejado | Franquias, Mobile, Cloud Sync |
| 4.0.0 | 🔮 Futuro | JavaFX, API REST, Web |

---

**Desenvolvido com ❤️ para a comunidade TCG** 🎴
