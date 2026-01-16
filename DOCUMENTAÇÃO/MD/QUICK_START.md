# 🚀 Quick Start Guide - HoStore

**Tempo estimado**: 5 minutos | **Nível**: Iniciante

---

## ⚡ TL;DR (Muito Longo; Não Leia)

```bash
# 1. Clonar
git clone https://github.com/oDuPrado/HoStore.git && cd HoStore

# 2. Compilar
mvn clean package

# 3. Executar
java -jar target/HoStore-1.0.0-jar-with-dependencies.jar
```

**Login**: `admin` / `admin` (⚠️ Mude a senha!)

---

## 📋 Checklist Inicial

- [ ] Java 17+ instalado? `java -version`
- [ ] Maven instalado? `mvn -version`
- [ ] Git instalado? `git --version`
- [ ] Pelo menos 2 GB de RAM livre?
- [ ] 500 MB de espaço em disco?

---

## 🎯 5 Passos para Começar

### Passo 1: Clonar o Repositório (30 segundos)

```bash
git clone https://github.com/oDuPrado/HoStore.git
cd HoStore
```

**Resultado esperado**:
```
Cloning into 'HoStore'...
remote: Enumerating objects: 450, done.
Resolving deltas: 100% (182/182), done.
```

---

### Passo 2: Compilar (2-3 minutos)

```bash
mvn clean package
```

**Resultado esperado**:
```
[INFO] Downloading dependencies...
[INFO] Compiling source files...
[INFO] Building jar file...
[INFO] BUILD SUCCESS
```

Se der erro sobre JDK, instale Java 17+:
```bash
# Windows (Chocolatey)
choco install openjdk17

# macOS (Homebrew)
brew install openjdk@17

# Linux (Ubuntu/Debian)
sudo apt-get install openjdk-17-jdk
```

---

### Passo 3: Executar (30 segundos)

```bash
java -jar target/HoStore-1.0.0-jar-with-dependencies.jar
```

**Resultado esperado**:
- Splash screen aparece
- 3-5 segundos de inicialização
- Tela de login

---

### Passo 4: Login (10 segundos)

Na tela de login:
- **Usuário**: `admin`
- **Senha**: `admin`

Clique em "Entrar" ou pressione `Enter`

---

### Passo 5: Dashboard Principal (Pronto!)

Você deve ver:
```
┌──────────────────────────────────────────┐
│ HoStore - Sistema de ERP para TCG        │
├──────────────────────────────────────────┤
│ Vendas Hoje: R$ 0,00                     │
│ Estoque: 0 unidades                      │
│ Clientes Ativos: 0                       │
│ [Vendas] [Estoque] [Financeiro] [...]    │
└──────────────────────────────────────────┘
```

---

## 🎮 Primeira Ação: Criar um Cliente

1. Menu: `Clientes → Novo Cliente`
2. Preencha:
   - **Nome**: João Silva
   - **CPF**: 123.456.789-00
   - **Email**: joao@email.com
3. Clique `Salvar`

✅ **Pronto!** Primeiro cliente criado.

---

## 📦 Segunda Ação: Cadastrar um Produto

1. Menu: `Estoque → Novo Item`
2. Selecione categoria: `Cartas`
3. Preencha:
   - **Nome**: Charizard
   - **Set**: Scarlet & Violet
   - **Preço Custo**: R$ 50
   - **Preço Venda**: R$ 80
   - **Quantidade**: 5
4. Clique `Salvar`

✅ **Pronto!** Primeiro produto em estoque.

---

## 🛒 Terceira Ação: Fazer Primeira Venda

1. Menu: `Vendas → Nova Venda`
2. **Selecione Cliente**: João Silva
3. **Busque Produto**: Charizard
4. **Quantidade**: 1
5. Clique `Adicionar ao Carrinho`
6. Clique `Finalizar`
7. **Forma de Pagamento**: Dinheiro
8. Clique `Confirmar`

✅ **Pronto!** Primeira venda realizada e comprovante PDF gerado!

---

## 📊 Próximos Passos

### Nível Iniciante (Hoje)
- [x] Login e navegação
- [x] Criar cliente
- [x] Cadastrar produto
- [x] Fazer venda

### Nível Intermediário (Semana 1)
- [ ] Adicionar mais produtos
- [ ] Fazer 10 vendas de teste
- [ ] Explorar relatórios
- [ ] Consultar estoque

### Nível Avançado (Semana 2)
- [ ] Configurar fiscal
- [ ] Integrar APIs de TCG
- [ ] Criar pedidos de compra
- [ ] Análise de vendas

---

## 🆘 Troubleshooting Rápido

### ❌ "Erro: Java não encontrado"
```bash
# Solução: Instale Java 17+
java -version  # Deve mostrar 17+
```

### ❌ "Porta 8080 em uso"
```bash
# O HoStore usa SQLite local, não precisa de porta
# Se receber este erro, reinicie o computador
```

### ❌ "Banco de dados corrompido"
```bash
# Solução: Delete o arquivo e recrie
rm hostore.db
# Reinicie a aplicação (vai recria automaticamente)
```

### ❌ "Senha esquecida"
```bash
# Delete banco e inicie novamente com admin/admin
rm hostore.db
```

---

## ⌨️ Atalhos Essenciais

| Tecla | Ação |
|-------|------|
| `Ctrl+N` | Nova venda |
| `Ctrl+S` | Salvar |
| `Ctrl+P` | Imprimir |
| `Esc` | Cancelar |
| `F5` | Atualizar |

---

## 📚 Próximas Leituras

**Depois de fazer sua primeira venda**, leia:

1. **[README.md](README.md)** - Visão geral (5 min)
2. **[Estoque.md](Estrturas/Estoque.md)** - Gerenciar estoque (10 min)
3. **[vendas.md](Estrturas/vendas.md)** - Sistema de vendas (10 min)
4. **[README_COMPLETE.md](README_COMPLETE.md)** - Completo (30 min)

---

## 🎯 Metas Prácticas

### Dia 1
- ✅ Instalar e executar
- ✅ Login
- ✅ Criar cliente
- ✅ Fazer venda

### Semana 1
- ✅ 50 produtos em estoque
- ✅ 100+ vendas
- ✅ 10+ clientes
- ✅ Explorar relatórios

### Mês 1
- ✅ Aprender todos os módulos
- ✅ Integrar APIs de TCG
- ✅ Configurar fiscal
- ✅ Customizar para sua loja

---

## 💡 Dicas Pro

1. **Sincronize TCGs**: Menu → Ajustes → Sincronizar TCG
   - Carrega dados das APIs
   - Cache local para offline

2. **Backup Automático**: Feito diariamente em `./data/backup/`
   - Seguro, restaurável, comprimido

3. **PDFs**: Comprovantes salvos em `./data/export/`
   - Imprima ou envie por email

4. **Temas**: Menu → Ajustes → Aparência
   - Light/Dark mode disponível

5. **Permissões**: Admin pode criar usuários com permissões limitadas

---

## 🔒 Segurança Básica

**Após fazer login pela primeira vez**:

1. Menu: `Ajustes → Alterar Senha`
2. Digite nova senha (16+ caracteres)
3. Salve em local seguro (gestor de senhas)

**Nunca compartilhe a senha!**

---

## 📞 Precisa de Ajuda?

### Rápido (1-2 min)
- Consulte este guia
- Veja os atalhos

### Médio (10-15 min)
- Leia [README.md](README.md)
- Consulte FAQ

### Completo (30+ min)
- Estude [README_COMPLETE.md](README_COMPLETE.md)
- Veja exemplos práticos
- Explore documentação técnica

### Issue no GitHub
- Descreva o problema
- Inclua versão (Help → About)
- Forneça screenshot se possível

---

## 🎉 Parabéns!

Você completou o Quick Start do HoStore! 🎊

Agora você está pronto para:
- ✅ Usar o sistema em sua loja
- ✅ Gerenciar vendas e estoque
- ✅ Gerar relatórios
- ✅ Explorar recursos avançados

---

## 📖 Documentação Completa

Consulte:
- 📚 [INDICE_DOCUMENTACAO.md](INDICE_DOCUMENTACAO.md) - Índice de todos os docs
- 🏗️ [ARQUITETURA.md](ARQUITETURA.md) - Para desenvolvedores
- 🔧 [FUNCIONALIDADES_COMPLETAS.md](FUNCIONALIDADES_COMPLETAS.md) - Lista completa

---

**Versão**: 1.0.0 | **Data**: Janeiro 2026 | **Status**: ✅ Pronto para usar

**Desenvolvido com ❤️ para a comunidade TCG** 🎴

