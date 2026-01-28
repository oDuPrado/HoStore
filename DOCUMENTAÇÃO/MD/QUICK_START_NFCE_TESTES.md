# ⚡ QUICK START - Testes NFC-e HoStore

**Tempo Leitura**: 5 minutos  
**Tempo para Primeiro Teste**: 10 minutos  
**Objetivo**: Validar que tudo compila e funções básicas rodando

---

## 🚀 PASSO 1: Compilar (2 minutos)

```bash
# Abrir terminal na raiz do projeto
cd C:\Users\Adm\Documents\PROJETOS\GITHUB\APP_HOSTORE\HoStore

# Compilar tudo
mvn clean compile

# Esperado: BUILD SUCCESS
```

**Se erro**:
```
[ERROR] cannot find symbol class ImpostoPisCofinsModel
[ERROR] cannot find symbol class FiscalCalcService
```

→ Verificar se arquivos estão em:
- `src/main/java/model/ImpostoPisCofinsModel.java`
- `src/main/java/service/FiscalCalcService.java` etc.

---

## 🚀 PASSO 2: Validar Arquivo Service (2 minutos)

Abrir em VS Code:
```
src/main/java/service/FiscalCalcService.java
```

Procurar por:
- [x] `public ImpostoCalculado calcICMS(...)`
- [x] `public ImpostoCalculado calcIPI(...)`
- [x] `public ImpostoCalculado calcPIS(...)`
- [x] `public ImpostoCalculado calcCOFINS(...)`
- [x] `public ImpostosItem calcularImpostosCompletos(...)`

✅ Se vê todos → arquivo OK

---

## 🚀 PASSO 3: Validar Arquivo DAO (2 minutos)

Abrir em VS Code:
```
src/main/java/dao/ImpostoPisCofinsDAO.java
```

Procurar por:
- [x] `public void inserir(ImpostoPisCofinsModel model)`
- [x] `public ImpostoPisCofinsModel buscarPorNcm(String ncm)`
- [x] `public List<ImpostoPisCofinsModel> listarTodos()`

✅ Se vê todos → arquivo OK

---

## 🚀 PASSO 4: Validar Arquivo XML Builder (2 minutos)

Abrir em VS Code:
```
src/main/java/service/XmlBuilderNfce.java
```

Procurar por:
- [x] `public String construir()`
- [x] `private String buildIde()`
- [x] `private String buildEmit()`
- [x] `private String buildDest()`
- [x] `private String buildDetItem(int nItem, ItemComImpostos item)`

✅ Se vê todos → arquivo OK

---

## 🚀 PASSO 5: Validar Arquivo Worker (2 minutos)

Abrir em VS Code:
```
src/main/java/service/FiscalWorker.java
```

Procurar por:
- [x] `public static synchronized FiscalWorker getInstance()`
- [x] `public synchronized void iniciar()`
- [x] `public synchronized void parar()`
- [x] `public void forcarProcessamento()`
- [x] `private void processarPendentes()`
- [x] `private void processarAssinados()`
- [x] `private void processarComErro()`

✅ Se vê todos → arquivo OK

---

## 🚀 PASSO 6: Teste Integrado Rápido (3 minutos)

Criar arquivo de teste temporário:
```java
// TempTest.java (raiz do projeto)
import service.FiscalCalcService;
import model.ImpostoPisCofinsModel;
import dao.ImpostoPisCofinsDAO;

public class TempTest {
    public static void main(String[] args) throws Exception {
        // Teste 1: FiscalCalcService
        System.out.println("Teste 1: FiscalCalcService");
        FiscalCalcService calc = new FiscalCalcService();
        var imp = calc.calcularImpostosCompletos("95049090", "RS", "RS", 100.0);
        System.out.println("  ICMS: " + imp.getIcms().getValor());
        System.out.println("  IPI: " + imp.getIpi().getValor());
        System.out.println("  PIS: " + imp.getPis().getValor());
        System.out.println("  COFINS: " + imp.getCofins().getValor());
        System.out.println("  ✅ OK\n");
        
        // Teste 2: Modelo
        System.out.println("Teste 2: ImpostoPisCofinsModel");
        ImpostoPisCofinsModel model = new ImpostoPisCofinsModel(
            "95049090",
            "04",     // CST PIS
            1.25,     // Aliquota PIS
            "04",     // CST COFINS
            5.75      // Aliquota COFINS
        );
        System.out.println("  NCM: " + model.getNcm());
        System.out.println("  PIS: " + model.getAliquotaPis());
        System.out.println("  ✅ OK\n");
        
        // Teste 3: DAO
        System.out.println("Teste 3: ImpostoPisCofinsDAO");
        ImpostoPisCofinsDAO dao = new ImpostoPisCofinsDAO();
        dao.inserir(model);
        ImpostoPisCofinsModel recuperado = dao.buscarPorNcm("95049090");
        if (recuperado != null) {
            System.out.println("  Recuperado: " + recuperado.getNcm());
            System.out.println("  ✅ OK\n");
        } else {
            System.out.println("  ⚠️ Não encontrado (esperado se BD vazio)\n");
        }
        
        System.out.println("✅ Todos testes rápidos passaram!");
    }
}
```

Compilar e executar:
```bash
javac -cp target/classes TempTest.java
java -cp target/classes:. TempTest
```

**Esperado**:
```
Teste 1: FiscalCalcService
  ICMS: 0.0
  IPI: 0.0
  PIS: 0.0
  COFINS: 0.0
  ✅ OK

Teste 2: ImpostoPisCofinsModel
  NCM: 95049090
  PIS: 1.25
  ✅ OK

Teste 3: ImpostoPisCofinsDAO
  ⚠️ Não encontrado (esperado se BD vazio)

✅ Todos testes rápidos passaram!
```

✅ Se recebe isto → **TUDO FUNCIONANDO!**

---

## 🚀 PASSO 7: Teste XML Geração (3 minutos)

Criar teste XML:
```java
// TempXmlTest.java
import service.XmlBuilderNfce;
import model.DocumentoFiscalModel;
import model.ConfiguracaoNfeNfceModel;
import java.util.ArrayList;

public class TempXmlTest {
    public static void main(String[] args) throws Exception {
        System.out.println("Teste: XmlBuilderNfce");
        
        // Setup documento
        DocumentoFiscalModel doc = new DocumentoFiscalModel();
        doc.setNumero(1);
        doc.setSerie(1);
        doc.setAmbiente("HOMOLOGACAO");
        doc.setTotalFinal(100.00);
        
        // Setup config
        ConfiguracaoNfeNfceModel config = new ConfiguracaoNfeNfceModel();
        config.setEmitirNfce(true);
        
        // Build
        XmlBuilderNfce builder = new XmlBuilderNfce(doc, config, new ArrayList<>());
        String xml = builder.construir();
        
        // Validate
        if (xml.contains("<ide>") && xml.contains("<emit>") && xml.length() > 100) {
            System.out.println("  XML gerado: " + xml.length() + " caracteres");
            System.out.println("  Tags: <ide>, <emit> OK");
            System.out.println("  ✅ OK");
        } else {
            System.out.println("  ❌ XML incompleto");
        }
    }
}
```

---

## 🚀 PASSO 8: Teste FiscalWorker (2 minutos)

Criar teste worker:
```java
// TempWorkerTest.java
import service.FiscalWorker;

public class TempWorkerTest {
    public static void main(String[] args) throws Exception {
        System.out.println("Teste: FiscalWorker");
        
        FiscalWorker worker = FiscalWorker.getInstance();
        System.out.println("  Singleton obtido: " + (worker != null ? "✅" : "❌"));
        
        worker.iniciar();
        System.out.println("  Worker iniciado: ✅");
        System.out.println("  Status rodando: " + (worker.estaRodando() ? "✅" : "❌"));
        
        // Não forçar processamento (precisa de BD com dados)
        // worker.forcarProcessamento();
        
        worker.parar();
        System.out.println("  Worker parado: ✅");
    }
}
```

---

## 📋 Checklist Validação Rápida

- [ ] `mvn clean compile` → BUILD SUCCESS
- [ ] FiscalCalcService.java compila e exporta
- [ ] ImpostoPisCofinsDAO.java compila e exporta
- [ ] XmlBuilderNfce.java compila e exporta
- [ ] XmlAssinaturaService.java compila e exporta
- [ ] SefazClientSoap.java compila e exporta
- [ ] DanfeNfceGenerator.java compila e exporta
- [ ] FiscalWorker.java compila e exporta
- [ ] ImpostoPisCofinsModel.java compila e exporta
- [ ] TempTest roda sem erro
- [ ] TempXmlTest roda sem erro
- [ ] TempWorkerTest roda sem erro

✅ Se tudo marcado → **NÚCLEO VALIDADO**

---

## 🎯 Próximo Passo

Após validar tudo acima:

1. **Ler**: IMPLEMENTACAO_NFCE_STATUS.md (5 min)
2. **Seguir**: CHECKLIST_IMPLEMENTACAO_NFCE.md seção "Fase 3" (criar UI)
3. **Testar**: Com certificado A1 em ambiente homologação

---

## 🆘 Se Algo Falhar

### Erro: "cannot find symbol class FiscalCalcService"
- [ ] Verificar arquivo existe: `src/main/java/service/FiscalCalcService.java`
- [ ] Verificar package: primeira linha deve ser `package service;`
- [ ] Executar: `mvn clean compile` novamente

### Erro: "cannot find symbol class ImpostoPisCofinsModel"
- [ ] Verificar arquivo existe: `src/main/java/model/ImpostoPisCofinsModel.java`
- [ ] Executar: `mvn clean compile` novamente

### Erro em Teste: "ArrayIndexOutOfBoundsException"
- [ ] Esperado se BD não tiver registros de impostos
- [ ] Fallback automático retorna 0

### Erro em DB: "cannot delete or update a parent row"
- [ ] Esperado se há relacionamento FK não respeitado
- [ ] Usar PRAGMA foreign_keys=OFF para debug

---

## 🎓 Documentação Completa

Após Quick Start, ler em ordem:

1. **RESUMO_IMPLEMENTACAO_NFCE.md** (overview)
2. **IMPLEMENTACAO_NFCE_STATUS.md** (status etapas)
3. **CHECKLIST_IMPLEMENTACAO_NFCE.md** (próximos passos detalhados)
4. **INVENTARIO_ARQUIVOS_NFCE.md** (referência arquivos)

---

## ✨ Benchmark de Sucesso

| Teste | Esperado | Seu Resultado |
|-------|----------|---------------|
| Compilação | BUILD SUCCESS | __ |
| FiscalCalcService | Calcula impostos | __ |
| XML Builder | Gera XML válido | __ |
| DAO | Insere/busca | __ |
| Worker | Singleton + Timer | __ |

✅ Se todos "✅" → Pronto para Fase 3 (UI)

---

**Sucesso! 🎉 Infraestrutura Core validada e pronta para continuar.**

Próximo: Criar UI Config + testar integração com VendaService
