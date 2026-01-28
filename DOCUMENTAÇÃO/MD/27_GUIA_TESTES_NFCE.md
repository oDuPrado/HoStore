# 🧪 Guia de Testes - Sistema de Migração e NFCe

## 📋 Pré-requisitos

- Java 17+
- Maven 3.8.1+
- SQLite 3
- HoStore compilado (`mvn clean package`)

---

## 🚀 Teste 1: Inicialização com Banco Novo

### Objetivo
Verificar que o banco é criado corretamente na primeira execução.

### Passos

1. **Remover banco existente:**
   ```bash
   rm data/hostore.db
   ```

2. **Executar a aplicação:**
   ```bash
   java -jar target/HoStore-1.0.0-jar-with-dependencies.jar
   ```

3. **Verificar criação:**
   ```bash
   # O banco deve ser criado automaticamente
   ls -la data/hostore.db
   ```

### Resultado Esperado
```
✅ Banco criado com sucesso
✅ Todas as tabelas criadas
✅ Dados padrão inseridos
✅ Migrações executadas (V001-V007)
```

---

## 🚀 Teste 2: Inicialização com Banco Existente

### Objetivo
Verificar que o banco existente é migrado sem perder dados.

### Passos

1. **Manter banco da execução anterior:**
   ```bash
   # Banco já deve existir de teste 1
   ```

2. **Executar novamente:**
   ```bash
   java -jar target/HoStore-1.0.0-jar-with-dependencies.jar
   ```

3. **Verificar migrações:**
   ```sql
   SELECT * FROM db_migrations;
   ```

### Resultado Esperado
```
✅ Banco não foi recriado
✅ Tabela db_migrations contém as 7 migrações
✅ Nenhuma migração foi executada duas vezes
✅ Dados existentes foram preservados
```

---

## 🚀 Teste 3: Gerar NFCe para uma Venda

### Objetivo
Testar geração completa de NFCe.

### Pré-requisito
Ter uma venda registrada no banco com ID = 1

### Código de Teste

```java
import service.NfceGeneratorService;

public class TestNfceGenerator {
    public static void main(String[] args) {
        try {
            // Gerar NFCe para venda ID 1
            String chaveAcesso = NfceGeneratorService.gerarNfce(1);
            
            System.out.println("✅ NFCe Gerada com Sucesso!");
            System.out.println("Chave de Acesso: " + chaveAcesso);
            System.out.println("Comprimento: " + chaveAcesso.length());
            
            // Validar formato da chave
            if (chaveAcesso.matches("\\d{44}")) {
                System.out.println("✅ Formato de chave válido");
            } else {
                System.out.println("❌ Formato de chave inválido");
            }
            
        } catch (Exception e) {
            System.err.println("❌ Erro ao gerar NFCe:");
            e.printStackTrace();
        }
    }
}
```

### Execução
```bash
# Compilar
javac -cp target/hocore-1.0.0.jar TestNfceGenerator.java

# Executar
java -cp .:target/hocore-1.0.0.jar TestNfceGenerator
```

### Resultado Esperado
```
✅ NFCe Gerada com Sucesso!
Chave de Acesso: 4314902001101000000651000000010100123456789
Comprimento: 44
✅ Formato de chave válido
```

---

## 🚀 Teste 4: Verificar Sequência de NFCe

### Objetivo
Validar que os números de NFCe são sequenciais.

### SQL de Verificação
```sql
-- Verificar sequência
SELECT 
    id,
    modelo,
    serie,
    ultimo_numero
FROM sequencias_nfce;

-- Resultado esperado:
-- SEQ_NFCE_001 | NFCe | 1 | 1
```

### Esperado
```
✅ Número começou em 1
✅ Incrementou para 2, 3, 4... após cada geração
✅ Série está correta (1)
```

---

## 🚀 Teste 5: Verificar Dados de Referência

### SQL de Verificação
```sql
-- Unidades
SELECT COUNT(*) FROM unidades_ref;
-- Esperado: 9

-- Origem
SELECT COUNT(*) FROM origem_ref;
-- Esperado: 9

-- CFOP
SELECT COUNT(*) FROM cfop_ref;
-- Esperado: 4

-- CSOSN
SELECT COUNT(*) FROM csosn_ref;
-- Esperado: 6

-- Verificar dados específicos
SELECT descricao FROM unidades_ref WHERE codigo = 'UN';
-- Esperado: Unidade

SELECT descricao FROM origem_ref WHERE codigo = '0';
-- Esperado: Nacional

SELECT descricao FROM cfop_ref WHERE codigo = '5102';
-- Esperado: Venda para Consumidor Final
```

### Resultado Esperado
```
✅ 9 unidades de medida
✅ 9 origens de produtos
✅ 4 CFOPs padrão
✅ 6 CSOSNs padrão
✅ Dados corretos
```

---

## 🚀 Teste 6: Migração de Bancos com Dados Existentes

### Objetivo
Validar que novos campos foram adicionados sem perder dados.

### Preparação
1. Criar banco com versão anterior (antes das migrações)
2. Inserir dados de teste
3. Executar aplicação

### SQL de Verificação
```sql
-- Verificar novos campos em produtos
PRAGMA table_info(produtos);
-- Deve incluir: ncm, cfop, csosn, origem, unidade

-- Verificar novos campos em vendas  
PRAGMA table_info(vendas);
-- Deve incluir: numero_nfce, status_fiscal

-- Verificar dados preservados
SELECT COUNT(*) FROM produtos;
-- Deve manter todos os produtos existentes

SELECT COUNT(*) FROM vendas;
-- Deve manter todas as vendas existentes
```

### Resultado Esperado
```
✅ Todos os novos campos adicionados
✅ Dados existentes preservados
✅ Nenhuma tabela foi recriada
✅ Sem perda de dados
```

---

## 🚀 Teste 7: Gerar Múltiplas NFCe

### Objetivo
Testar sequência correta com múltiplas gerações.

### Código
```java
import service.NfceGeneratorService;

public class TestMultiplasNfce {
    public static void main(String[] args) {
        try {
            System.out.println("Gerando 3 NFCes sequenciais...\n");
            
            for (int i = 1; i <= 3; i++) {
                String chave = NfceGeneratorService.gerarNfce(i);
                System.out.println(i + ". Chave: " + chave);
            }
            
            System.out.println("\n✅ Todas as NFCes geradas com sucesso!");
            
        } catch (Exception e) {
            System.err.println("❌ Erro:");
            e.printStackTrace();
        }
    }
}
```

### Resultado Esperado
```
Gerando 3 NFCes sequenciais...

1. Chave: 4314902001101000000651000000010100123456789
2. Chave: 4314902001101000000651000000020100456789123
3. Chave: 4314902001101000000651000000030100789123456

✅ Todas as NFCes geradas com sucesso!
```

---

## 📊 Checklist de Testes

| Teste | Status | Data | Resultado |
|-------|--------|------|-----------|
| 1. Banco Novo | ⬜ | - | - |
| 2. Banco Existente | ⬜ | - | - |
| 3. Gerar NFCe | ⬜ | - | - |
| 4. Sequência | ⬜ | - | - |
| 5. Dados Padrão | ⬜ | - | - |
| 6. Migração de Dados | ⬜ | - | - |
| 7. Múltiplas NFCe | ⬜ | - | - |

---

## 🔍 Diagnóstico

### Verificar Migrações Executadas
```sql
SELECT 
    version,
    name,
    executed_at
FROM db_migrations
ORDER BY version;
```

### Verificar Estrutura de Tabelas
```sql
-- Ver todas as tabelas
SELECT name FROM sqlite_master WHERE type='table' ORDER BY name;

-- Ver colunas de uma tabela
PRAGMA table_info(tabela_nome);
```

### Verificar Índices
```sql
SELECT * FROM sqlite_master WHERE type='index';
```

---

## 🐛 Troubleshooting

### Erro: "Banco de dados bloqueado"
```bash
# Fechar todas as conexões
# Remover arquivo .db-wal se existir
rm data/hostore.db-wal
```

### Erro: "Tabela não encontrada"
```bash
# Recriar banco
rm data/hostore.db
# Executar aplicação novamente
```

### Erro: "Campo não existe"
```sql
-- Verificar schema
PRAGMA table_info(tabela_nome);
-- Pode ser necessário executar a migração manualmente
```

---

## 📝 Notas

- Os testes devem ser executados em ordem
- Cada teste depende do anterior
- Usar sempre a versão compilada mais recente
- Verificar logs da aplicação para detalhes

---

**Guia de Testes - v1.0**  
**Última atualização: 26 de Janeiro de 2026**
