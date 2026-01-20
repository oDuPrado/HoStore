# 🛠️ GUIA TÉCNICO DE CORREÇÕES - Implementação Passo-a-Passo

**Data:** 19 de Janeiro de 2026  
**Público:** Desenvolvedores  
**Tempo de Leitura:** 1 hora  
**Tempo de Implementação:** 25 horas  

---

## 📋 ÍNDICE DE CORREÇÕES

1. Locale Deprecado (15 min)
2. Dead Code (5 min)
3. Parcel Calculation (45 min)
4. Comparativo Fix (15 min)
5. Desconto Consistency (1h)
6. N+1 Query Fix (30 min)
7. Connection Pool (3h)
8. Database Indexes (1h)

---

## ✅ CORREÇÃO 1: Locale Deprecado (15 minutos)

**Erro:** `new Locale()` descontinuado em Java 19+

**Arquivos Afetados:** 19 arquivos Java

**Solução - Passo 1: Find & Replace**

1. Abra VS Code
2. Pressione `Ctrl+H` (Find & Replace)
3. Encontrar: `new Locale("pt", "BR")`
4. Trocar por: `Locale.of("pt", "BR")`
5. Clique em "Replace All"

**Código - ANTES:**
```java
import java.util.Locale;

public class MoedaUtil {
    private static final Locale LOCALE_BR = new Locale("pt", "BR");  // ❌
}
```

**Código - DEPOIS:**
```java
import java.util.Locale;

public class MoedaUtil {
    private static final Locale LOCALE_BR = Locale.of("pt", "BR");  // ✅
}
```

**Teste:**
```bash
mvn clean compile
# Resultado esperado: BUILD SUCCESS (sem erros de Locale)
```

---

## ✅ CORREÇÃO 2: Dead Code (5 minutos)

**Erro:** Linhas unreachable em PDFGenerator.java

**Arquivo:** `src/main/java/util/PDFGenerator.java`

**Passo 1: Remover Unreachable Code (linha 441)**

**ANTES:**
```java
public int gerarRelatorio() {
    // ... código
    if (condicao) {
        return 1;
    }
    return 0;  // ← Redundante, sempre alcançado
    return 0;  // ❌ NUNCA EXECUTA - REMOVER
}
```

**DEPOIS:**
```java
public int gerarRelatorio() {
    // ... código
    if (condicao) {
        return 1;
    }
    return 0;  // ✅ Único return necessário
}
```

**Passo 2: Remover Método Não Utilizado**

Buscar `botao(String)` no PDFGenerator - se nunca chamado, remover.

---

## ✅ CORREÇÃO 3: Parcel Calculation (45 minutos)

**Erro:** Parcelas marcadas pagas quando ainda devem centavos

**Arquivo:** `src/main/java/service/ContaReceberService.java`

**Passo 1: Adicionar Dependência (pom.xml)**

```xml
<!-- Já deve estar, confirmar -->
<dependency>
    <groupId>junit</groupId>
    <artifactId>junit</artifactId>
    <version>4.13.2</version>
    <scope>test</scope>
</dependency>
```

**Passo 2: Modificar ContaReceberService.java**

**ANTES:**
```java
public void verificarParcela(ParcelaContaReceber parcela) {
    // ❌ ERRADO: usa double, margem menor que um centavo
    if (parcela.getValorPago() + 0.009 >= parcela.getValorNominal() + juros - desconto) {
        parcela.setStatus("pago");
    }
}
```

**DEPOIS:**
```java
import java.math.BigDecimal;
import java.math.RoundingMode;

public class ContaReceberService {
    private static final BigDecimal TOLERANCE_1_CENT = BigDecimal.valueOf(0.01);
    
    public void verificarParcela(ParcelaContaReceber parcela) {
        // ✅ CORRETO: usa BigDecimal, margem de 1 centavo
        BigDecimal paidAmount = BigDecimal.valueOf(parcela.getValorPago())
            .setScale(2, RoundingMode.HALF_UP);
        
        BigDecimal nominalValue = BigDecimal.valueOf(parcela.getValorNominal())
            .add(BigDecimal.valueOf(parcela.getJuros() != null ? parcela.getJuros() : 0))
            .subtract(BigDecimal.valueOf(parcela.getDesconto() != null ? parcela.getDesconto() : 0))
            .setScale(2, RoundingMode.HALF_UP);
        
        BigDecimal difference = paidAmount.subtract(nominalValue).abs();
        
        if (difference.compareTo(TOLERANCE_1_CENT) <= 0) {
            parcela.setStatus("pago");
            parcela.setDataPagamento(new java.util.Date());
        }
    }
}
```

**Passo 3: Adicionar Teste**

Criar arquivo: `src/test/java/service/ContaReceberServiceTest.java`

```java
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class ContaReceberServiceTest {
    
    private ContaReceberService service;
    
    @Before
    public void setup() {
        service = new ContaReceberService();
    }
    
    @Test
    public void testParcelaMarkAsPaidWhenExactAmount() {
        // Cliente paga exatamente R$ 104.00
        ParcelaContaReceber parcela = new ParcelaContaReceber();
        parcela.setValorNominal(104.00);
        parcela.setValorPago(104.00);
        
        service.verificarParcela(parcela);
        assertEquals("pago", parcela.getStatus());
    }
    
    @Test
    public void testParcelaMarkAsPaidWhenDifferenceIsLessThan1Cent() {
        // Cliente paga R$ 103.99 (falta 1 centavo)
        ParcelaContaReceber parcela = new ParcelaContaReceber();
        parcela.setValorNominal(104.00);
        parcela.setValorPago(103.99);
        
        service.verificarParcela(parcela);
        assertEquals("pago", parcela.getStatus());  // ✅ Deve marcar como pago
    }
    
    @Test
    public void testParcelaNotMarkedAsPaidWhenDifferenceMoreThan1Cent() {
        // Cliente paga R$ 103.98 (faltam 2 centavos)
        ParcelaContaReceber parcela = new ParcelaContaReceber();
        parcela.setValorNominal(104.00);
        parcela.setValorPago(103.98);
        
        service.verificarParcela(parcela);
        assertEquals("pendente", parcela.getStatus());  // ✅ Deve permanecer pendente
    }
}
```

**Teste:**
```bash
mvn test -Dtest=ContaReceberServiceTest
# Esperado: 3 testes passando
```

---

## ✅ CORREÇÃO 4: Comparativo Fix (15 minutos)

**Erro:** Divisão por zero quando anterior = 0

**Arquivo:** `src/main/java/model/ComparativoModel.java`

**ANTES:**
```java
public class ComparativoModel {
    public void calcularDelta(double anterior, double atual) {
        // ❌ ERRADO: threshold muito pequeno
        if (Math.abs(anterior) < 0.0000001) {
            this.deltaPct = 1;  // Retorna 100%
        } else {
            this.deltaPct = (atual - anterior) / anterior;
        }
    }
}
```

**DEPOIS:**
```java
public class ComparativoModel {
    public void calcularDelta(double anterior, double atual) {
        // ✅ CORRETO: verifica zero explicitamente
        if (anterior == 0.0) {
            if (atual > 0.0) {
                this.deltaPct = Double.POSITIVE_INFINITY;  // Crescimento infinito
            } else if (atual < 0.0) {
                this.deltaPct = Double.NEGATIVE_INFINITY;  // Queda infinita
            } else {
                this.deltaPct = 0.0;  // Sem mudança
            }
        } else {
            this.deltaPct = (atual - anterior) / anterior;
        }
    }
}
```

**Teste:**
```java
@Test
public void testDeltaPctWhenAnteriorIsZero() {
    ComparativoModel modelo = new ComparativoModel();
    
    modelo.calcularDelta(0.0, 100.0);
    assertEquals(Double.POSITIVE_INFINITY, modelo.getDeltaPct(), 0.0);
}
```

---

## ✅ CORREÇÃO 5: Desconto Consistency (1 hora)

**Erro:** VendaItemModel trata desconto como %, ComandaItemModel como valor absoluto

**Arquivo 1:** `src/main/java/model/VendaItemModel.java`

**Atual:**
```java
public class VendaItemModel {
    private double desconto;  // Percentual (0-100)
    
    public double getValorTotal() {
        double desconto_pct = this.desconto / 100.0;
        return (quantidade * valorUnitario) * (1.0 - desconto_pct);
    }
}
```

**Arquivo 2:** `src/main/java/model/ComandaItemModel.java`

**Atual - PROBLEMA:**
```java
public class ComandaItemModel {
    private double desconto;  // ❌ VALOR ABSOLUTO (em Reais)
    
    public double getValorTotal() {
        return (quantidade * valorUnitario) - desconto;
    }
}
```

**Solução: Padronizar ambos como PERCENTUAL**

**ComandaItemModel - DEPOIS:**
```java
public class ComandaItemModel {
    private double desconto;  // ✅ PERCENTUAL (0-100)
    
    public double getValorTotal() {
        double desconto_pct = this.desconto / 100.0;
        return (quantidade * valorUnitario) * (1.0 - desconto_pct);
    }
}
```

**Atualizar BD Migration:**
```sql
-- Se havia desconto como valor absoluto, converter para percentual
-- SELECT desconto, (desconto / valorUnitario) * 100 as desconto_pct FROM comandas_itens LIMIT 10;
-- UPDATE comandas_itens SET desconto = (desconto / valorUnitario) * 100;
```

---

## ✅ CORREÇÃO 6: N+1 Query Fix (30 minutos)

**Erro:** CupomFiscalFormatter cria DAO dentro de loop

**Arquivo:** `src/main/java/util/CupomFiscalFormatter.java`

**ANTES - PROBLEMA:**
```java
public String formatarCupom(List<VendaItemModel> itens) {
    StringBuilder sb = new StringBuilder();
    
    for (VendaItemModel it : itens) {  // Loop de 50 items
        ProdutoDAO pdao = new ProdutoDAO();  // ❌ 50 DAOs criados!
        ProdutoModel produto = pdao.buscarPorId(it.getProdutoId());  // 50 queries!
        sb.append(produto.getNome());
    }
    
    return sb.toString();
}
```

**DEPOIS - CORRETO:**
```java
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public String formatarCupom(List<VendaItemModel> itens) {
    StringBuilder sb = new StringBuilder();
    
    // PASSO 1: Coletar todos IDs de produtos
    Set<String> produtoIds = itens.stream()
        .map(VendaItemModel::getProdutoId)
        .collect(Collectors.toSet());
    
    // PASSO 2: 1 única query para TODOS os produtos
    ProdutoDAO pdao = new ProdutoDAO();
    Map<String, ProdutoModel> produtoCache = pdao.buscarPorIds(produtoIds)
        .stream()
        .collect(Collectors.toMap(ProdutoModel::getId, p -> p));
    
    // PASSO 3: Usar cache no loop
    for (VendaItemModel it : itens) {
        ProdutoModel produto = produtoCache.get(it.getProdutoId());
        if (produto != null) {
            sb.append(produto.getNome());
        }
    }
    
    return sb.toString();
}
```

**Atualizar ProdutoDAO - Adicionar método:**
```java
public List<ProdutoModel> buscarPorIds(Set<String> ids) {
    if (ids.isEmpty()) return new ArrayList<>();
    
    String placeholders = String.join(",", Collections.nCopies(ids.size(), "?"));
    String query = "SELECT * FROM produtos WHERE id IN (" + placeholders + ")";
    
    try (Connection conn = DB.getConnection();
         PreparedStatement stmt = conn.prepareStatement(query)) {
        
        int index = 1;
        for (String id : ids) {
            stmt.setString(index++, id);
        }
        
        ResultSet rs = stmt.executeQuery();
        List<ProdutoModel> produtos = new ArrayList<>();
        while (rs.next()) {
            produtos.add(mapToModel(rs));
        }
        return produtos;
    } catch (SQLException e) {
        e.printStackTrace();
        return new ArrayList<>();
    }
}
```

**Performance - Antes vs Depois:**
- Antes: 50 queries × 20ms = 1.000ms ⏱️
- Depois: 1 query × 20ms = 20ms ⏱️
- **Melhoria: 50x mais rápido!**

---

## ✅ CORREÇÃO 7: Connection Pool (3 horas)

**Erro:** Sem pool de conexões (cria nova a cada requisição)

**Passo 1: Adicionar Dependência**

`pom.xml`:
```xml
<dependency>
    <groupId>com.zaxxer</groupId>
    <artifactId>HikariCP</artifactId>
    <version>5.1.0</version>
</dependency>
```

**Passo 2: Atualizar DB.java**

**ANTES:**
```java
public class DB {
    private static final String URL = "jdbc:sqlite:data/hostore.db";
    
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);  // ❌ Nova conexão a cada vez
    }
}
```

**DEPOIS:**
```java
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class DB {
    private static final String URL = "jdbc:sqlite:data/hostore.db";
    private static final HikariDataSource dataSource;
    
    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(URL);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(20000);  // 20 segundos
        config.setIdleTimeout(300000);       // 5 minutos
        config.setMaxLifetime(1200000);      // 20 minutos
        
        dataSource = new HikariDataSource(config);
    }
    
    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();  // ✅ Da pool de 10
    }
    
    public static void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }
}
```

**Passo 3: Atualizar Main.java para fechar pool**

```java
public class Main {
    public static void main(String[] args) {
        try {
            // ... aplicação
        } finally {
            DB.close();  // Fechar pool corretamente
        }
    }
}
```

---

## ✅ CORREÇÃO 8: Database Indexes (1 hora)

**Erro:** Sem índices em chaves estrangeiras consultadas frequentemente

**Arquivo:** `src/main/java/util/DB.java` (método de inicialização do banco)

**Adicionar:**
```java
public static void inicializarIndices() {
    String[] indices = {
        "CREATE INDEX IF NOT EXISTS idx_vendas_itens_venda_id ON vendas_itens(venda_id)",
        "CREATE INDEX IF NOT EXISTS idx_vendas_itens_produto_id ON vendas_itens(produto_id)",
        "CREATE INDEX IF NOT EXISTS idx_parcelas_cp_titulo_id ON parcelas_contas_pagar(titulo_id)",
        "CREATE INDEX IF NOT EXISTS idx_parcelas_cr_titulo_id ON parcelas_contas_receber(titulo_id)",
        "CREATE INDEX IF NOT EXISTS idx_estoque_mov_produto_id ON estoque_movimentacoes(produto_id)"
    };
    
    try (Connection conn = getConnection()) {
        for (String index : indices) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(index);
            }
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }
}
```

**Chamar na inicialização:**
```java
public static void main(String[] args) {
    DB.inicializarIndices();
    // ... resto da aplicação
}
```

---

## 📊 CHECKLIST DE IMPLEMENTAÇÃO

- [ ] Correção 1: Locale (15 min)
- [ ] Correção 2: Dead Code (5 min)
- [ ] Correção 3: Parcel Calculation (45 min)
- [ ] Correção 4: Comparativo (15 min)
- [ ] Correção 5: Desconto (1h)
- [ ] Correção 6: N+1 Query (30 min)
- [ ] Correção 7: Connection Pool (3h)
- [ ] Correção 8: Indexes (1h)
- [ ] Testes (2h)
- [ ] Build final: `mvn clean compile`

**Total: 25 horas**

---

## 📚 Próxima Leitura

👉 **TESTES_VALIDACAO_POS_CORRECAO.md** - Suite de testes para validar todas correções
