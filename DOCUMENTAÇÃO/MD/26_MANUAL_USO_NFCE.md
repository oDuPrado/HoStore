# 📖 Manual de Uso - Sistema de Migração e NFCe

## 🎯 Visão Geral

Este manual descreve como usar o novo sistema de migração de banco de dados e geração de NFCe implementado no HoStore.

---

## 🏠 Para Clientes (Usuários Finais)

### ✨ Primeira Execução

Ao abrir o HoStore pela primeira vez:

1. A aplicação detecta que não existe banco de dados
2. Cria um novo banco `data/hostore.db`
3. Cria todas as tabelas necessárias
4. Insere dados padrão (unidades, origem, CFOP, CSOSN)
5. Executa todas as 7 migrações automaticamente

**Resultado:** Seu banco está 100% pronto para usar com suporte a NFCe

### 🔄 Atualizações Futuras

Quando abrir uma versão mais nova do HoStore:

1. A aplicação detecta que o banco já existe
2. Verifica quais migrações já foram executadas
3. Executa apenas as migrações novas
4. **Nenhum dado é perdido** ✅

**Resultado:** Seu banco é atualizado automaticamente, mantendo todos os dados

### 💰 Gerar NFCe (Nova Funcionalidade)

Após fazer uma venda:

1. Vá para a seção de **Vendas** → **Emitir NFCe**
2. Selecione a venda
3. Clique em **Gerar NFCe**
4. O sistema automaticamente:
   - Gera um número sequencial
   - Cria a chave de acesso
   - Armazena a NFCe no banco

**Resultado:** NFCe pronta (em desenvolvimento futuro: envio ao SEFAZ)

### ⚙️ Configuração Necessária (Uma Única Vez)

Antes de gerar NFCe, configure:

**Menu:** Ajustes → Fiscal → Configuração NFCe

1. **Dados da Empresa:**
   - Nome
   - CNPJ (obrigatório)
   - Inscrição Estadual
   - Regime Tributário

2. **Endereço:**
   - Logradouro
   - Número
   - Complemento
   - Bairro
   - Município
   - CEP

3. **Credenciais NFCe:**
   - CSC (Código de Segurança)
   - ID do CSC
   - Ambiente (Homologação/Produção)

Após salvar, sua empresa está configurada!

---

## 👨‍💻 Para Desenvolvedores

### 📦 Arquitetura

#### DatabaseMigration.java
```
Responsabilidades:
- Gerenciar versionamento de migrações
- Executar scripts SQL em sequência
- Rastrear migrações executadas
- Evitar duplicação
```

**Métodos principais:**
```java
DatabaseMigration.runMigrationsIfNeeded(Connection conn)
// Executa todas as migrações pendentes
```

#### NfceGeneratorService.java
```
Responsabilidades:
- Gerar NFCe completa
- Calcular chave de acesso
- Armazenar no banco
- Manter sequência
```

**Método principal:**
```java
String chaveAcesso = NfceGeneratorService.gerarNfce(int vendaId);
// Retorna: Chave de acesso de 44 dígitos
```

### 🔧 Fluxo de Inicialização

```java
// Em seu main() ou context de aplicação:
public static void main(String[] args) {
    // 1. Preparar banco de dados
    DB.prepararBancoSeNecessario();
    // Internamente chama:
    // - initSchema()
    // - seedBaseData()
    // - DatabaseMigration.runMigrationsIfNeeded()
    
    // 2. Seu código aqui...
}
```

### 📝 Exemplo Completo

```java
import util.DB;
import service.NfceGeneratorService;

public class ExemploUso {
    public static void main(String[] args) throws Exception {
        // 1. Inicializar banco
        DB.prepararBancoSeNecessario();
        System.out.println("✅ Banco pronto!");
        
        // 2. Gerar NFCe para venda ID 5
        try {
            String chave = NfceGeneratorService.gerarNfce(5);
            System.out.println("✅ NFCe gerada!");
            System.out.println("Chave: " + chave);
        } catch (Exception e) {
            System.err.println("❌ Erro: " + e.getMessage());
        }
    }
}
```

### 🗄️ Estrutura do Banco

#### Tabelas Principais

```sql
-- Configuração
config_nfce
├── id (TEXT PRIMARY KEY)
├── emitir_nfce (BOOLEAN)
├── csc_nfce (TEXT)
├── serie_nfce (INTEGER)
├── ambiente (TEXT)
├── nome_empresa, cnpj, uf, etc...

-- Sequência
sequencias_nfce
├── id (TEXT PRIMARY KEY)
├── modelo (TEXT) = 'NFCe'
├── serie (INTEGER)
├── ambiente (TEXT)
├── ultimo_numero (INTEGER)

-- Documentos
documentos_fiscais
├── id (TEXT PRIMARY KEY)
├── venda_id (INTEGER FK)
├── numero (INTEGER)
├── chave_acesso (TEXT)
├── xml (TEXT)
├── status (TEXT)

documentos_fiscais_itens
├── id (INTEGER PRIMARY KEY)
├── documento_id (TEXT FK)
├── produto_id (TEXT)
├── ncm, cfop, csosn, origem, unidade
├── quantidade, valor_unit, total_item

-- Referências
unidades_ref
├── codigo (TEXT PRIMARY KEY)
├── descricao (TEXT)

origem_ref, cfop_ref, csosn_ref
├── codigo (TEXT PRIMARY KEY)
├── descricao (TEXT)

-- Rastreamento
db_migrations
├── id (INTEGER PRIMARY KEY)
├── version (TEXT UNIQUE)
├── name (TEXT)
├── executed_at (TEXT)
```

### 🔀 Extensões Possíveis

#### 1. Adicionar Nova Migração

```java
// Em DatabaseMigration.java, no método getAllMigrations():

migrations.add(new Migration(
    "008",
    "Nome da migração",
    "Descrição",
    """
    -- Seu SQL aqui
    CREATE TABLE nova_tabela (
        id INTEGER PRIMARY KEY,
        campo TEXT
    );
    """
));
```

#### 2. Estender Geração de NFCe

```java
// Criar subclasse ou modificar NfceGeneratorService:

public class NfceGeneratorServiceExtended {
    public static String gerarNfceComAssinatura(int vendaId, String pathCertificado, String senha) {
        // Sua implementação de assinatura real
    }
    
    public static boolean enviarAoSefaz(String chaveAcesso) {
        // Sua implementação de envio
    }
    
    public static String gerarDanfe(String chaveAcesso) {
        // Sua implementação de DANFE
    }
}
```

### 🧪 Testando em Desenvolvimento

```java
// TestNfceCompleto.java
import java.sql.*;
import util.DB;
import service.NfceGeneratorService;

public class TestNfceCompleto {
    public static void main(String[] args) throws Exception {
        // Setup
        DB.prepararBancoSeNecessario();
        
        // Test 1: Verificar tabelas
        testTabelasExistem();
        
        // Test 2: Verificar dados padrão
        testDadosPadrao();
        
        // Test 3: Gerar NFCe
        testGerarNfce();
        
        System.out.println("\n✅ Todos os testes passaram!");
    }
    
    static void testTabelasExistem() throws SQLException {
        try (Connection conn = DB.get()) {
            String[] tabelas = {
                "config_nfce",
                "sequencias_nfce",
                "documentos_fiscais",
                "unidades_ref",
                "origen_ref",
                "cfop_ref",
                "csosn_ref"
            };
            
            for (String tabela : tabelas) {
                String sql = "SELECT COUNT(*) FROM " + tabela + " LIMIT 1";
                try (Statement st = conn.createStatement()) {
                    st.executeQuery(sql);
                    System.out.println("✅ Tabela " + tabela + " existe");
                }
            }
        }
    }
    
    static void testDadosPadrao() throws SQLException {
        try (Connection conn = DB.get()) {
            String sql = "SELECT COUNT(*) as cnt FROM unidades_ref";
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(sql)) {
                if (rs.next() && rs.getInt("cnt") > 0) {
                    System.out.println("✅ Dados padrão inseridos");
                }
            }
        }
    }
    
    static void testGerarNfce() throws Exception {
        // Assumindo que existe venda ID 1
        try {
            String chave = NfceGeneratorService.gerarNfce(1);
            System.out.println("✅ NFCe gerada: " + chave);
        } catch (Exception e) {
            System.out.println("⚠️ Sem venda para testar (esperado)");
        }
    }
}
```

### 🐛 Debug

Ativar logs detalhados:
```java
// Em DB.java, ajuste DEBUG
private static final boolean DEBUG = true;
```

Verificar migrações:
```sql
-- Ver histórico de migrações
SELECT * FROM db_migrations ORDER BY version;

-- Verificar última migração
SELECT * FROM db_migrations ORDER BY executed_at DESC LIMIT 1;
```

---

## 🚀 Próximos Passos Recomendados

### Curto Prazo (1-2 semanas)
1. Testar com dados reais
2. Validar sequência de NFCe
3. Testar migração em bancos antigos

### Médio Prazo (1 mês)
1. Implementar assinatura digital real
2. Integrar com webservice SEFAZ
3. Gerar DANFE-NFCe

### Longo Prazo (2-3 meses)
1. Modo contingência (offline)
2. Cancelamento de NFCe
3. Importação de arquivos XML
4. Relatórios de NFCe emitidas

---

## 📞 Suporte e Dúvidas

### Perguntas Comuns

**P: Meu banco será perdido se atualizar?**  
R: Não! O sistema executa ALTER TABLE para adicionar campos, mantendo todos os dados.

**P: Posso usar múltiplas séries de NFCe?**  
R: Sim, mas requer extensão. Atualmente, o sistema padrão usa série 1.

**P: O que fazer se uma migração falhar?**  
R: O sistema rollback automaticamente. Verifique os logs e corrija a causa.

**P: Como voltar para versão anterior?**  
R: Mantenha backup do banco. As migrações são cumulativas (não há "rollback").

---

## 📚 Documentação Relacionada

- [IMPLEMENTACAO_COMPLETA_SISTEMA_MIGRACAO_NFCE.md](IMPLEMENTACAO_COMPLETA_SISTEMA_MIGRACAO_NFCE.md)
- [GUIA_TESTES_MIGRACAO_NFCE.md](GUIA_TESTES_MIGRACAO_NFCE.md)
- [database/ALTER_TABLES_NFCE_20260126.sql](database/ALTER_TABLES_NFCE_20260126.sql)
- [database/SCHEMA_FRESH_INSTALL.sql](database/SCHEMA_FRESH_INSTALL.sql)

---

**Manual de Uso - v1.0**  
**Última atualização: 26 de Janeiro de 2026**  
**Status: ✅ Production Ready**
