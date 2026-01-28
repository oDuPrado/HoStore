# 🔍 MATRIZ DE REFERÊNCIA RÁPIDA - NFC-e HoStore

**Objetivo**: Localizar rapidamente arquivo, método ou conceito  
**Tempo de Leitura**: 3 minutos para consultá-lo quando precisar

---

## 📍 LOCALIZADOR DE ARQUIVOS JAVA

### Por Funcionalidade

| Funcionalidade | Arquivo | Classe | Método Principal |
|---|---|---|---|
| Calcular ICMS | `service/FiscalCalcService.java` | `FiscalCalcService` | `calcICMS()` |
| Calcular IPI | `service/FiscalCalcService.java` | `FiscalCalcService` | `calcIPI()` |
| Calcular PIS/COFINS | `service/FiscalCalcService.java` | `FiscalCalcService` | `calcPIS()`, `calcCOFINS()` |
| Cálculo Completo | `service/FiscalCalcService.java` | `FiscalCalcService` | `calcularImpostosCompletos()` |
| Montagem XML | `service/XmlBuilderNfce.java` | `XmlBuilderNfce` | `construir()` |
| Assinatura Digital | `service/XmlAssinaturaService.java` | `XmlAssinaturaService` | `assinarXml()` |
| Comunicação SEFAZ | `service/SefazClientSoap.java` | `SefazClientSoap` | `enviarLoteNfce()` |
| Geração DANFE | `service/DanfeNfceGenerator.java` | `DanfeNfceGenerator` | `gerarDANFETexto()` |
| Job Background | `service/FiscalWorker.java` | `FiscalWorker` | `iniciar()` |
| BD - Sequência | `dao/SequenciaFiscalDAO.java` | `SequenciaFiscalDAO` | `nextNumero()` |
| BD - Documento | `dao/DocumentoFiscalDAO.java` | `DocumentoFiscalDAO` | `inserir()` |
| BD - ICMS | `dao/ImpostoICMSDAO.java` | `ImpostoICMSDAO` | `buscarPorNcmEUf()` |
| BD - IPI | `dao/ImpostoIPIDAO.java` | `ImpostoIPIDAO` | `buscarPorNcm()` |
| BD - PIS/COFINS | `dao/ImpostoPisCofinsDAO.java` | `ImpostoPisCofinsDAO` | `buscarPorNcm()` |
| BD - Config | `dao/ConfiguracaoNfeNfceDAO.java` | `ConfiguracaoNfeNfceDAO` | `salvar()` |

### Por Tipo

| Tipo | Arquivos | Quantidade |
|---|---|---|
| **Serviços** | FiscalCalc*, XmlBuilder*, XmlAssinatura*, SefazClient*, DanfeGenerator, FiscalWorker | 6 |
| **DAOs** | SequenciaFiscal, Imposto*, Configuracao*, DocumentoFiscal* | 8 |
| **Modelos** | DocumentoFiscal*, ConfiguracaoNfeNfce*, Imposto* | 5 |
| **Documentação** | IMPLEMENTACAO_NFCE_STATUS, CHECKLIST_*, RESUMO_*, INVENTARIO_*, QUICK_START_* | 5 |

---

## 🎯 MATRIZ DE ESTADOS

### Estados Documento Fiscal

```
PENDENTE
   ↓
   [calcularImpostos + gerarXml]
   ↓
XML_GERADO
   ↓
   [assinarXml]
   ↓
ASSINADA
   ↓
   [enviarSefaz]
   ↓
ENVIADA
   ├─ [autorizado] → AUTORIZADA ✅
   ├─ [rejeitado] → REJEITADA ❌
   └─ [erro técnico] → ERRO (retry em 2^n min)
   
ERRO (com retry)
   ↓
   [nova tentativa após backoff]
   ↓
PENDENTE (recolocado em fila)
```

**Transições**:
- `pendente → xml_gerado` (FiscalWorker.processarPendentes)
- `xml_gerado → assinada` (manual ou automático)
- `assinada → enviada` (FiscalWorker.processarAssinados)
- `enviada → autorizada|rejeitada` (resposta SEFAZ)
- `enviada → erro` (timeout/conexão)
- `erro → pendente` (retry automático)

---

## 🔗 MATRIZ DE DEPENDÊNCIAS SERVIÇO

```
FiscalCalcService
├─ [usa] ImpostoICMSDAO.buscarPorNcmEUf()
├─ [usa] ImpostoIPIDAO.buscarPorNcm()
├─ [usa] ImpostoPisCofinsDAO.buscarPorNcm()
└─ [retorna] ImpostosItem

XmlBuilderNfce
├─ [usa] FiscalCalcService.calcularImpostosCompletos()
├─ [usa] DocumentoFiscalModel (dados documento)
├─ [usa] ConfiguracaoNfeNfceModel (dados config loja)
└─ [retorna] String XML

XmlAssinaturaService
├─ [carrega] Certificado A1 (PKCS#12)
├─ [usa] XmlBuilderNfce (XML de entrada)
└─ [retorna] String XML assinado

SefazClientSoap
├─ [usa] XmlAssinaturaService (XML assinado)
├─ [comunica] SEFAZ webservice (SOAP/HTTP)
└─ [retorna] RespostaSefaz (protocolo, recibo, etc)

DanfeNfceGenerator
├─ [usa] DocumentoFiscalModel
├─ [usa] ConfiguracaoNfeNfceModel
├─ [usa] SefazClientSoap (se protocolo já tiver)
└─ [retorna] String texto ou byte[] PDF

FiscalWorker (Timer job)
├─ [chama] DocumentoFiscalDAO.listarPorStatus()
├─ [para cada] FiscalCalcService
├─ [para cada] XmlBuilderNfce
├─ [para cada] XmlAssinaturaService
├─ [para cada] SefazClientSoap
├─ [para cada] DocumentoFiscalDAO.atualizarStatus()
└─ [a cada] 5 minutos (configurable)
```

---

## 🗂️ MATRIZ DE TABELAS BD

| Tabela | Colunas Principais | DAO | Uso |
|---|---|---|---|
| `ncm` | id, codigo (8 dígitos), descricao | (util) | Referência produtos |
| `cfop` | id, codigo (4 dígitos), descricao | (util) | Op. fiscal (5102=venda) |
| `csosn` | id, codigo, descricao | (util) | Situação ICMS (102, 500) |
| `origem` | id, codigo, descricao | (util) | Produto (0=BR, 1=Est) |
| `sequencias_fiscais` | id, modelo, serie, numero, ambiente, proxNum | SequenciaFiscalDAO | Numeração automática |
| `documentos_fiscais` | id, venda_id, modelo, serie, numero, status, chave, protocolo, xml, erro | DocumentoFiscalDAO | Header NFC-e |
| `documentos_fiscais_itens` | id, doc_id, ncm, cfop, csosn, qtd, valor, desconto | DocumentoFiscalItemDAO | Linhas NFC-e |
| `documentos_fiscais_pagamentos` | id, doc_id, tipo, valor | DocumentoFiscalPagamentoDAO | Formas de pago |
| `imposto_icms` | id, estado, estado_dest, ncm, aliq_cons, aliq_cont, reducao_base | ImpostoICMSDAO | Alíquota ICMS |
| `imposto_ipi` | id, ncm, aliquota, cnpj_produtor | ImpostoIPIDAO | Alíquota IPI |
| `imposto_pis_cofins` | id, ncm, cst_pis, aliq_pis, cst_cofins, aliq_cofins | ImpostoPisCofinsDAO | Alíquota PIS/COFINS |
| `configuracao_nfe_nfce` | id (única), emitir_nfe, emitir_nfce, cert_path, csc, ambiente | ConfiguracaoNfeNfceDAO | Config loja |

---

## 🔑 MATRIZ DE CHAVES E CÓDIGOS

### CSOSN (Código Situação Tributária ICMS - Simples Nacional)

| Código | Regime | Significado | Uso |
|---|---|---|---|
| 102 | SN | Optante Simples - Contribuinte | Padrão HoStore |
| 500 | SN | Optante Simples - NÃO Contribuinte | Se comprador SN |

### CFOP (Código Fiscal de Operação)

| Código | Descrição | Uso |
|---|---|---|
| 5102 | Venda para Consumidor Final | Varejo (padrão NFC-e) |
| 5103 | Venda para Consumidor Final - Comércio | Varejo outro estado |

### Origem (Produto)

| Código | Significado |
|---|---|
| 0 | Produto Nacional (Brasil) |
| 1 | Estrangeiro (Importado) |

### Ambiente (SEFAZ)

| Código | Significado | Certificado |
|---|---|---|
| HOMOLOGACAO | Teste/desenvolvimento | A1 Teste (SRF) |
| PRODUCAO | Produção/real | A1 Produção |

---

## 💻 MATRIZ DE MÉTODOS ESSENCIAIS

### FiscalCalcService

```java
// Retorna objeto com: tipo, cst, aliquota, valor
ImpostoCalculado calcICMS(String ncm, String ufOrigem, 
                          String ufDestino, Double baseCalculo, 
                          String tipoOperacao)

// Retorna objeto com: ncm, baseCalculo, icms, ipi, pis, cofins, totalImpostos
ImpostosItem calcularImpostosCompletos(String ncm, String ufOrigem, 
                                       String ufDestino, Double valorItem)
```

### XmlBuilderNfce

```java
// Retorna XML válido RFB 5.00 (String)
String construir()
```

### XmlAssinaturaService

```java
// Constructor: carrega certificado A1
XmlAssinaturaService(String caminhoP12, String senha)

// Retorna XML com <Signature> XMLDSig
String assinarXml(String xmlDesassinado)

// Valida se certificado não expirou
void validarCertificado() throws Exception
```

### SefazClientSoap

```java
// Envia XML assinado para SEFAZ
// Retorna objeto com: sucesso, status, protocolo, recibo, xmlResposta
RespostaSefaz enviarLoteNfce(String xmlAssinado, boolean producao)

// Consulta status por recibo (nRec)
RespostaSefaz consultarRecibo(String nRec, boolean producao)
```

### DanfeNfceGenerator

```java
// Retorna String com DANFE formatado 80mm
String gerarDANFETexto()

// Salva DANFE em arquivo
void salvarEmArquivo(String caminhoSaida)

// Retorna URL SEFAZ com chave e hash CSC
String gerarURLQRCode(String csc, String idCSC)
```

### FiscalWorker

```java
// Obtém instância singleton
static FiscalWorker getInstance()

// Inicia job Timer (5 minutos)
void iniciar()

// Para job Timer
void parar()

// Executa processamento imediatamente (debug)
void forcarProcessamento()

// Retorna se worker está rodando
boolean estaRodando()
```

---

## 🎯 MATRIZ DE CENÁRIOS (O que fazer quando...)

| Cenário | Ação | Arquivo |
|---|---|---|
| Preciso calcular impostos | → `FiscalCalcService.calcularImpostosCompletos()` | FiscalCalcService.java |
| Preciso gerar XML | → `XmlBuilderNfce.construir()` | XmlBuilderNfce.java |
| Preciso assinar XML | → `XmlAssinaturaService.assinarXml()` | XmlAssinaturaService.java |
| Preciso enviar SEFAZ | → `SefazClientSoap.enviarLoteNfce()` | SefazClientSoap.java |
| Preciso imprimir DANFE | → `DanfeNfceGenerator.gerarDANFETexto()` | DanfeNfceGenerator.java |
| Preciso processar automático | → `FiscalWorker.iniciar()` | FiscalWorker.java |
| Preciso próximo número fiscal | → `SequenciaFiscalDAO.nextNumero()` | SequenciaFiscalDAO.java |
| Preciso alíquota ICMS | → `ImpostoICMSDAO.buscarPorNcmEUf()` | ImpostoICMSDAO.java |
| Preciso salvar documento | → `DocumentoFiscalDAO.inserir()` | DocumentoFiscalDAO.java |
| Preciso config fiscal | → `ConfiguracaoNfeNfceDAO.obter()` | ConfiguracaoNfeNfceDAO.java |

---

## 🚀 MATRIZ DE FLUXO RÁPIDO

### Fluxo Manual (UI)
```
1. Usuário em VendaDialog → Finalizar Venda
2. → DocumentoFiscalService.criarDocumentoPendenteParaVenda()
3. → DocumentoFiscal criado com status=pendente
4. → Usuário vai em "Painel Documentos Fiscais"
5. → Clica "Enviar SEFAZ"
6. → Service chama: calcularImpostos() + gerarXml() + assinarXml() + enviarSefaz()
7. → Documento muda para status=enviada/autorizada
```

### Fluxo Automático (Job)
```
1. VendaService.finalizarVenda() cria DocumentoFiscal (status=pendente)
2. FiscalWorker.iniciar() inicia Timer (a cada 5 min)
3. Worker processa automaticamente:
   - Calcula impostos
   - Gera XML
   - Assina XML
   - Envia SEFAZ
4. Documento progride: pendente → xml_gerado → assinada → enviada → autorizada
5. Se erro: status=erro, retry com backoff 2^n
```

---

## 📈 MATRIZ DE PERFORMANCE

| Operação | Tempo Esperado | Crítico |
|---|---|---|
| Calcular impostos (1 item) | <100ms | Não |
| Gerar XML (10 itens) | <500ms | Não |
| Assinar XML | <1s | Sim (certificado carregado) |
| Enviar SEFAZ | 5-30s | Sim (rede/SEFAZ) |
| Imprimir DANFE | <500ms | Não |
| Job FiscalWorker | Cada 5 min | Não (background) |

---

## 🆘 MATRIZ DE DIAGNÓSTICO

| Erro | Causa Provável | Solução |
|---|---|---|
| `cannot find symbol class FiscalCalcService` | Arquivo não existe | Verificar src/main/java/service/ |
| `NullPointerException em calcICMS` | Tabela ICMS vazia | Fallback retorna 0 (OK) |
| `Signature not found in keystore` | Certificado não carregou | Check path + senha |
| `SEFAZ timeout` | Rede/SEFAZ offline | Retry automático 2^n |
| `XML validation error` | XML malformado | Check XmlBuilderNfce tags |

---

## 📋 CHECKLIST DE IMPLEMENTAÇÃO (status)

- [x] Compilação: mvn clean compile
- [x] FiscalCalcService: 6/6 métodos
- [x] XmlBuilderNfce: 9/9 métodos
- [x] XmlAssinaturaService: 3/3 métodos (RSA placeholder)
- [x] SefazClientSoap: 4/4 métodos
- [x] DanfeNfceGenerator: 4/4 métodos
- [x] FiscalWorker: 7/7 métodos
- [x] DAOs: 8/8 completos
- [x] Modelos: 9/9 completos
- [x] Documentação: 5 arquivos

---

## 🎓 Para Aprender Mais

| Tópico | Ler | Localização |
|---|---|---|
| Visão geral | RESUMO_IMPLEMENTACAO_NFCE.md | DOCUMENTAÇÃO/MD/ |
| Status etapas | IMPLEMENTACAO_NFCE_STATUS.md | DOCUMENTAÇÃO/MD/ |
| Próximos passos | CHECKLIST_IMPLEMENTACAO_NFCE.md | DOCUMENTAÇÃO/MD/ |
| Lista arquivos | INVENTARIO_ARQUIVOS_NFCE.md | DOCUMENTAÇÃO/MD/ |
| Testes rápidos | QUICK_START_NFCE_TESTES.md | DOCUMENTAÇÃO/MD/ |
| Referência | Este arquivo (MATRIZ_REFERENCIA_NFCE.md) | DOCUMENTAÇÃO/MD/ |

---

**Use este documento como referência rápida durante desenvolvimento**

Marcadores: `Ctrl+F` → buscar função, arquivo ou estado
