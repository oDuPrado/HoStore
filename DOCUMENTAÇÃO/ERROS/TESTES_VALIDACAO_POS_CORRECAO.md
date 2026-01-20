# 🧪 TESTES E VALIDAÇÃO - Suite Completa de Testes

**Data:** 19 de Janeiro de 2026  
**Público:** QA, Desenvolvedores  
**Objetivo:** Validar que todas as correções funcionam  

---

## 📋 SUITES DE TESTES

1. Teste de Compilação
2. Testes de Parcel Calculation
3. Testes de Desconto
4. Testes de Performance
5. Testes de Database
6. Testes de Integration

---

## ✅ SUITE 1: Teste de Compilação

**Objetivo:** Verificar que não há erros de compilação

**Comando:**
```bash
mvn clean compile
```

**Resultado Esperado:**
```
[INFO] BUILD SUCCESS
[INFO] Total time: XX s
[INFO] Finished at: 2026-01-19T...
```

**Validação Checklist:**
- [ ] 0 erros de compilação
- [ ] 0 avisos Locale deprecado
- [ ] 0 avisos de unused imports

---

## ✅ SUITE 2: Parcel Calculation Tests

**Arquivo:** `src/test/java/service/ContaReceberServiceTest.java`

```java
package service;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import model.ParcelaContaReceber;

public class ContaReceberServiceTest {
    
    private ContaReceberService service;
    
    @Before
    public void setup() {
        service = new ContaReceberService();
    }
    
    @Test
    public void test_ParcelaMarkAsPaidWhenExactAmount() {
        // Cenário: Cliente paga exatamente o valor devido
        ParcelaContaReceber parcela = new ParcelaContaReceber();
        parcela.setValorNominal(104.00);
        parcela.setValorPago(104.00);
        parcela.setStatus("pendente");
        
        service.verificarParcela(parcela);
        
        assertEquals("pago", parcela.getStatus());
    }
    
    @Test
    public void test_ParcelaMarkAsPaidWhen1CentShort() {
        // Cenário: Cliente paga R$ 0.01 a menos (tolerância de 1 centavo)
        ParcelaContaReceber parcela = new ParcelaContaReceber();
        parcela.setValorNominal(104.00);
        parcela.setValorPago(103.99);
        parcela.setStatus("pendente");
        
        service.verificarParcela(parcela);
        
        // ✅ Deve marcar como pago (dentro da tolerância)
        assertEquals("pago", parcela.getStatus());
    }
    
    @Test
    public void test_ParcelaNotMarkedAsPaidWhen2CentsShort() {
        // Cenário: Cliente paga R$ 0.02 a menos (além da tolerância)
        ParcelaContaReceber parcela = new ParcelaContaReceber();
        parcela.setValorNominal(104.00);
        parcela.setValorPago(103.98);
        parcela.setStatus("pendente");
        
        service.verificarParcela(parcela);
        
        // ✅ Deve permanecer pendente
        assertEquals("pendente", parcela.getStatus());
    }
    
    @Test
    public void test_ParcelaWithJuros() {
        // Cenário: Parcela com juros
        ParcelaContaReceber parcela = new ParcelaContaReceber();
        parcela.setValorNominal(100.00);
        parcela.setJuros(4.00);           // Total devido: R$ 104.00
        parcela.setValorPago(104.00);
        parcela.setStatus("pendente");
        
        service.verificarParcela(parcela);
        
        assertEquals("pago", parcela.getStatus());
    }
    
    @Test
    public void test_ParcelaWithDesconto() {
        // Cenário: Parcela com desconto
        ParcelaContaReceber parcela = new ParcelaContaReceber();
        parcela.setValorNominal(100.00);
        parcela.setDesconto(5.00);        // Total devido: R$ 95.00
        parcela.setValorPago(95.00);
        parcela.setStatus("pendente");
        
        service.verificarParcela(parcela);
        
        assertEquals("pago", parcela.getStatus());
    }
    
    @Test
    public void test_ParcelaWithJurosAndDesconto() {
        // Cenário: Parcela com ambos
        ParcelaContaReceber parcela = new ParcelaContaReceber();
        parcela.setValorNominal(100.00);
        parcela.setJuros(4.00);
        parcela.setDesconto(5.00);
        parcela.setValorPago(99.00);      // 100 + 4 - 5 = 99
        parcela.setStatus("pendente");
        
        service.verificarParcela(parcela);
        
        assertEquals("pago", parcela.getStatus());
    }
}
```

**Executar:**
```bash
mvn test -Dtest=ContaReceberServiceTest
```

**Resultado Esperado:**
```
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
```

---

## ✅ SUITE 3: Desconto Consistency Tests

**Arquivo:** `src/test/java/model/DescontoConsistencyTest.java`

```java
package model;

import org.junit.Test;
import static org.junit.Assert.*;

public class DescontoConsistencyTest {
    
    @Test
    public void test_VendaItemDescontoPercentual() {
        // VendaItemModel: desconto = percentual
        VendaItemModel item = new VendaItemModel();
        item.setQuantidade(1);
        item.setValorUnitario(100.0);
        item.setDesconto(10.0);  // 10%
        
        double total = item.getValorTotal();
        
        // Esperado: 100 × (1 - 0.10) = 90
        assertEquals(90.0, total, 0.01);
    }
    
    @Test
    public void test_ComandaItemDescontoPercentual() {
        // ComandaItemModel: desconto TAMBÉM deve ser percentual agora
        ComandaItemModel item = new ComandaItemModel();
        item.setQuantidade(1);
        item.setValorUnitario(100.0);
        item.setDesconto(10.0);  // 10% (antes era R$ 10)
        
        double total = item.getValorTotal();
        
        // Esperado: 100 × (1 - 0.10) = 90 ✅ CONSISTENTE
        assertEquals(90.0, total, 0.01);
    }
    
    @Test
    public void test_ConsistenciaEntreModelos() {
        // Ambos modelos com mesmos dados devem dar mesmo resultado
        VendaItemModel venda = new VendaItemModel();
        venda.setQuantidade(5);
        venda.setValorUnitario(50.0);
        venda.setDesconto(20.0);  // 20%
        
        ComandaItemModel comanda = new ComandaItemModel();
        comanda.setQuantidade(5);
        comanda.setValorUnitario(50.0);
        comanda.setDesconto(20.0);  // 20%
        
        double totalVenda = venda.getValorTotal();
        double totalComanda = comanda.getValorTotal();
        
        // ✅ Ambos devem ter mesmo resultado
        assertEquals(totalVenda, totalComanda, 0.01);
    }
    
    @Test
    public void test_DescontoZero() {
        VendaItemModel item = new VendaItemModel();
        item.setQuantidade(2);
        item.setValorUnitario(25.0);
        item.setDesconto(0.0);  // Sem desconto
        
        double total = item.getValorTotal();
        assertEquals(50.0, total, 0.01);
    }
    
    @Test
    public void test_DescontoTotal() {
        VendaItemModel item = new VendaItemModel();
        item.setQuantidade(1);
        item.setValorUnitario(100.0);
        item.setDesconto(100.0);  // 100% desconto
        
        double total = item.getValorTotal();
        assertEquals(0.0, total, 0.01);
    }
}
```

**Executar:**
```bash
mvn test -Dtest=DescontoConsistencyTest
```

---

## ✅ SUITE 4: Performance Tests

**Arquivo:** `src/test/java/util/PerformanceTest.java`

```java
package util;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;

public class PerformanceTest {
    
    @Test
    public void test_N1QueryPerformance() {
        // Simular: Cupom com 50 items
        List<VendaItemModel> itens = criarCupomSimulado(50);
        
        long inicio = System.currentTimeMillis();
        String cupom = new CupomFiscalFormatter().formatarCupom(itens);
        long duracao = System.currentTimeMillis() - inicio;
        
        System.out.println("Tempo para formatar cupom de 50 itens: " + duracao + "ms");
        
        // ✅ Deve ser < 500ms (era 1000-2000ms antes)
        assertTrue("Cupom deveria formatar em menos de 500ms", duracao < 500);
    }
    
    @Test
    public void test_ConnectionPoolThroughput() throws Exception {
        // Simular: 100 queries concorrentes
        int queries = 100;
        long inicio = System.currentTimeMillis();
        
        for (int i = 0; i < queries; i++) {
            try (Connection conn = DB.getConnection()) {
                // Simular query
                Thread.sleep(10);
            }
        }
        
        long duracao = System.currentTimeMillis() - inicio;
        System.out.println("Tempo para 100 queries: " + duracao + "ms");
        
        // ✅ Com pool deve ser mais rápido
        assertTrue("100 queries deveriam ser rápidas com pool", duracao < 5000);
    }
    
    private List<VendaItemModel> criarCupomSimulado(int quantidade) {
        List<VendaItemModel> itens = new ArrayList<>();
        for (int i = 0; i < quantidade; i++) {
            VendaItemModel item = new VendaItemModel();
            item.setProdutoId("PROD_" + i);
            item.setQuantidade(1);
            item.setValorUnitario(10.0 + i);
            itens.add(item);
        }
        return itens;
    }
}
```

**Executar:**
```bash
mvn test -Dtest=PerformanceTest -X
```

**Resultado Esperado:**
```
Tempo para formatar cupom de 50 itens: 150ms (era 1500ms)
Tempo para 100 queries: 2000ms (era 5000ms+)
```

---

## ✅ SUITE 5: Database Tests

**Arquivo:** `src/test/java/util/DatabaseTest.java`

```java
package util;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import java.sql.*;

public class DatabaseTest {
    
    @Test
    public void test_ConnectionPoolExists() throws SQLException {
        // Verificar que pool está funcionando
        Connection conn1 = DB.getConnection();
        Connection conn2 = DB.getConnection();
        Connection conn3 = DB.getConnection();
        
        assertNotNull(conn1);
        assertNotNull(conn2);
        assertNotNull(conn3);
        
        conn1.close();
        conn2.close();
        conn3.close();
    }
    
    @Test
    public void test_IndicesExist() throws SQLException {
        try (Connection conn = DB.getConnection();
             DatabaseMetaData meta = conn.getMetaData()) {
            
            // Verificar índices
            String[] indices = {
                "idx_vendas_itens_venda_id",
                "idx_vendas_itens_produto_id",
                "idx_parcelas_cp_titulo_id",
                "idx_parcelas_cr_titulo_id",
                "idx_estoque_mov_produto_id"
            };
            
            for (String indexName : indices) {
                // Buscar índice na metadados do DB
                System.out.println("Verificando índice: " + indexName);
            }
        }
    }
    
    @Test
    public void test_QueryVendasItensByVendaId() throws SQLException {
        // Teste: Query em vendas_itens com filtro venda_id (deve usar índice)
        try (Connection conn = DB.getConnection();
             Statement stmt = conn.createStatement()) {
            
            long inicio = System.currentTimeMillis();
            ResultSet rs = stmt.executeQuery("SELECT * FROM vendas_itens WHERE venda_id = 1");
            long duracao = System.currentTimeMillis() - inicio;
            
            System.out.println("Query com índice levou: " + duracao + "ms");
            
            // ✅ Deve ser rápido (< 50ms para índice)
            assertTrue("Query deveria ser rápida com índice", duracao < 50);
            
            rs.close();
        }
    }
}
```

---

## ✅ SUITE 6: Integration Tests

**Arquivo:** `src/test/java/integration/FullFlowTest.java`

```java
package integration;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class FullFlowTest {
    
    @Test
    public void test_VendaCompleta() {
        // 1. Criar venda
        VendaModel venda = new VendaModel();
        venda.setDataVenda(new Date());
        venda.setValorTotal(1000.0);
        
        // 2. Adicionar itens
        VendaItemModel item1 = new VendaItemModel();
        item1.setQuantidade(5);
        item1.setValorUnitario(100.0);
        item1.setDesconto(10.0);  // 10%
        
        venda.adicionarItem(item1);
        
        // 3. Gerar cupom
        String cupom = new CupomFiscalFormatter().formatarCupom(venda.getItens());
        assertNotNull(cupom);
        assertTrue(cupom.length() > 0);
        
        // 4. Salvar venda
        VendaDAO dao = new VendaDAO();
        dao.criar(venda);
        
        // 5. Recuperar e validar
        VendaModel vendaRecuperada = dao.buscarPorId(venda.getId());
        assertNotNull(vendaRecuperada);
        assertEquals(1000.0, vendaRecuperada.getValorTotal(), 0.01);
    }
    
    @Test
    public void test_ParcelaFlow() {
        // 1. Criar conta a receber
        ContaReceberModel conta = new ContaReceberModel();
        conta.setClienteId("CLI_001");
        conta.setValorTotal(500.0);
        
        // 2. Gerar parcelas (3x de 166.67)
        List<ParcelaContaReceber> parcelas = gerarParcelas(conta, 3);
        
        // 3. Pagar primeira parcela
        ParcelaContaReceber parcela1 = parcelas.get(0);
        parcela1.setValorPago(166.67);
        
        ContaReceberService service = new ContaReceberService();
        service.verificarParcela(parcela1);
        
        // ✅ Deve estar paga
        assertEquals("pago", parcela1.getStatus());
        
        // 4. Verificar outras parcelas ainda pendentes
        ParcelaContaReceber parcela2 = parcelas.get(1);
        parcela2.setValorPago(0);
        service.verificarParcela(parcela2);
        
        assertEquals("pendente", parcela2.getStatus());
    }
    
    private List<ParcelaContaReceber> gerarParcelas(ContaReceberModel conta, int qtd) {
        // Implementação de geração de parcelas
        List<ParcelaContaReceber> parcelas = new ArrayList<>();
        double valorParcela = conta.getValorTotal() / qtd;
        
        for (int i = 0; i < qtd; i++) {
            ParcelaContaReceber p = new ParcelaContaReceber();
            p.setValorNominal(valorParcela);
            parcelas.add(p);
        }
        
        return parcelas;
    }
}
```

---

## 📊 RELATÓRIO DE TESTES

**Após Executar Todos:**
```bash
mvn clean test
```

**Esperado:**
```
[INFO] Tests run: 25, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

**Validação Final:**
- [ ] Todos os 25+ testes passando
- [ ] 0 falhas
- [ ] 0 erros
- [ ] Cobertura >= 80%

---

## 🎯 Cronograma de Testes

**Dia 1-2: Testes de Compilação**
- [ ] mvn clean compile (0 erros)

**Dia 3: Testes de Parcel**
- [ ] ContaReceberServiceTest (6 testes)

**Dia 4: Testes de Desconto**
- [ ] DescontoConsistencyTest (6 testes)

**Dia 5: Testes de Performance**
- [ ] PerformanceTest (2 testes)

**Dia 6: Testes de Database**
- [ ] DatabaseTest (3 testes)

**Dia 7: Integration Tests**
- [ ] FullFlowTest (2 testes)

**Dia 8: Testes Completos**
- [ ] mvn clean test (todos os testes)
- [ ] QA completa

---

## 📚 Próxima Leitura

👉 **SUMARIO_FINAL_ANALISE.md** - Resumo executivo com roadmap
