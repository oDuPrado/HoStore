# ⚡ Quick Start - Sistema de Migração e NFCe

## 🚀 Comece em 5 Minutos

### 1️⃣ Compilar o Projeto

```bash
cd /caminho/para/HoStore
mvn clean package -DskipTests
```

**Resultado esperado:**
```
[INFO] BUILD SUCCESS
[INFO] Building jar: target/HoStore-1.0.0-jar-with-dependencies.jar
```

---

### 2️⃣ Executar a Aplicação

```bash
# Primeira vez (cria novo banco)
java -jar target/HoStore-1.0.0-jar-with-dependencies.jar

# OU

# Atualizar (migra banco existente)
java -jar target/HoStore-1.0.0-jar-with-dependencies.jar
```

**O que acontece:**
- ✅ Banco é criado/atualizado automaticamente
- ✅ 7 migrações são executadas
- ✅ Dados padrão são inseridos
- ✅ Aplicação está pronta

---

### 3️⃣ Verificar Banco de Dados

```bash
# Ver se banco foi criado
ls -la data/hostore.db

# Ver status das migrações (via SQL)
sqlite3 data/hostore.db "SELECT * FROM db_migrations;"
```

**Saída esperada:**
```
001|Adicionar campos fiscais em produtos|2026-01-26 15:30:00|...
002|Criar tabelas de referência fiscal|2026-01-26 15:30:05|...
003|Criar tabela de configuração NFCe|2026-01-26 15:30:10|...
004|Criar tabelas de documentos fiscais|2026-01-26 15:30:15|...
005|Criar tabela de sequências fiscais|2026-01-26 15:30:20|...
006|Adicionar campos fiscais em vendas|2026-01-26 15:30:25|...
007|Popular dados de referência fiscal|2026-01-26 15:30:30|...
```

---

### 4️⃣ Gerar Sua Primeira NFCe

**Via Código Java:**

```java
import service.NfceGeneratorService;

public class MinhaApp {
    public static void main(String[] args) throws Exception {
        // Supondo venda ID = 1
        String chave = NfceGeneratorService.gerarNfce(1);
        System.out.println("✅ NFCe: " + chave);
    }
}
```

**Compilar e rodar:**
```bash
javac -cp target/HoStore-1.0.0-jar-with-dependencies.jar MinhaApp.java
java -cp .:target/HoStore-1.0.0-jar-with-dependencies.jar MinhaApp
```

**Saída:**
```
✅ NFCe gerada com sucesso!
  Número: 1
  Chave de Acesso: 4314902001101000000651000000010100123456789
  Documento ID: a1b2c3d4-e5f6-7890-abcd-ef1234567890
```

---

### 5️⃣ Testar Automaticamente

```bash
# Executar testes (se existirem)
mvn test

# Ou compilar sem testes
mvn clean package -DskipTests
```

---

## 🎯 Comandos Úteis

### Banco de Dados

```bash
# Ver tabelas
sqlite3 data/hostore.db ".tables"

# Ver esquema de uma tabela
sqlite3 data/hostore.db ".schema documentos_fiscais"

# Ver dados
sqlite3 data/hostore.db "SELECT * FROM unidades_ref LIMIT 5;"

# Ligar/desligar modo verbose
sqlite3 data/hostore.db ".mode line"
```

### Compilação

```bash
# Apenas compilar
mvn clean compile

# Compilar + empacotar
mvn clean package -DskipTests

# Executar com logs
mvn clean package -X

# Limpar tudo
mvn clean
```

### Executar

```bash
# Rodar JAR
java -jar target/HoStore-1.0.0-jar-with-dependencies.jar

# Com mais memória
java -Xmx512m -jar target/HoStore-1.0.0-jar-with-dependencies.jar

# Com logs detalhados
java -Dhostore.debug=true -jar target/HoStore-1.0.0-jar-with-dependencies.jar
```

---

## ❓ Problemas Comuns

### ❌ "Banco de dados bloqueado"
```bash
# Solução:
rm data/hostore.db-wal
rm data/hostore.db-shm
```

### ❌ "Tabela não encontrada"
```bash
# Solução:
rm data/hostore.db
# Rodar novamente para recriar
```

### ❌ "BUILD FAILURE"
```bash
# Solução:
mvn clean
mvn install -DskipTests
mvn package -DskipTests
```

### ❌ "Cannot find symbol"
```bash
# Solução:
mvn clean compile
# Verificar se tem erros no código
```

---

## 📊 Verificação Rápida

### Checklist Pós-Execução

- [ ] Banco de dados criado (`data/hostore.db`)
- [ ] 7 migrações na tabela `db_migrations`
- [ ] Tabelas de referência preenchidas
  - [ ] 9 unidades em `unidades_ref`
  - [ ] 9 origens em `origem_ref`
  - [ ] 4 CFOP em `cfop_ref`
  - [ ] 6 CSOSN em `csosn_ref`
- [ ] NFCe pode ser gerada
- [ ] Aplicação inicia sem erros

---

## 💾 Arquivos Gerados

Após primeira execução, você terá:

```
HoStore/
├── data/
│   ├── hostore.db           ← Banco de dados
│   ├── hostore.db-wal       ← WAL file (intermo)
│   ├── hostore.db-shm       ← Shared memory (intermo)
│   ├── cache/
│   │   └── sync_state.properties
│   └── export/
├── target/
│   ├── hocore-1.0.0.jar
│   └── HoStore-1.0.0-jar-with-dependencies.jar
└── ... (outros arquivos)
```

---

## 🔗 Próximos Passos

1. **Ler a documentação:**
   - [MANUAL_USO_SISTEMA_NFCE.md](MANUAL_USO_SISTEMA_NFCE.md)
   - [GUIA_TESTES_MIGRACAO_NFCE.md](GUIA_TESTES_MIGRACAO_NFCE.md)

2. **Fazer testes:**
   - Testar com múltiplas NFCe
   - Testar migração de banco antigo
   - Testar com seus dados

3. **Integração:**
   - Configurar dados da empresa
   - Conectar com UI da aplicação
   - Testar fluxo completo

4. **Produção:**
   - Fazer backup do banco
   - Testar em ambiente similar
   - Migrar com confiança

---

## 📞 Suporte Rápido

| Problema | Solução | Comando |
|----------|---------|---------|
| Banco corrompido | Recriar | `rm data/hostore.db` |
| Migração pendente | Executar | Restart app |
| Testar NFCe | Gerar | `NfceGeneratorService.gerarNfce(1)` |
| Ver logs BD | SQL | `sqlite3 data/hostore.db` |

---

## ✅ Tudo Pronto!

Você está pronto para:
- ✅ Usar o HoStore com suporte a NFCe
- ✅ Gerar documentos fiscais
- ✅ Manter banco antigo sem perder dados
- ✅ Estender o sistema com novas features

**Divirta-se!** 🚀

---

**Quick Start v1.0** | 26 de Janeiro de 2026
