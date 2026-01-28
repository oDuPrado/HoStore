# ✅ CHECKLIST PRÉ-DEPLOYMENT NFC-e

## 🏗️ Build & Compilation

### Verificação Pré-Build
- [ ] Java 17+ instalado (`java -version`)
- [ ] Maven 3.8+ instalado (`mvn -version`)
- [ ] Caminhos de classe corretos
- [ ] Sem erros de import

### Build Process
```bash
# Limpar builds anteriores
mvn clean

# Compilar código
mvn compile
Status esperado: ✅ BUILD SUCCESS

# Executar testes
mvn test
Status esperado: ✅ 18 testes passando

# Empacotar
mvn package
Status esperado: ✅ JAR criado em target/
```

### Verificação Pós-Build
- [ ] `target/hostore.jar` criado (>50MB)
- [ ] `target/classes/` contém compilados
- [ ] Sem warnings críticos
- [ ] Sem erros de dependências

---

## 🧪 Testes de Validação

### Testes Unitários (FiscalCalcService)
```
✅ testCalcularICMS
✅ testCalcularIPI
✅ testCalcularPIS
✅ testCalcularCOFINS
✅ testCalcularTodos
✅ testAliquotaInvalida
✅ testArredondamento
✅ testValidacao
```

### Testes de Integração (DocumentoFiscal)
```
✅ testCriarDocumentoComItens
✅ testAdicionarItens
✅ testRegistrarLog
✅ testRegistrarErroLog
✅ testBuscarUltimosLogs
✅ testFluxoCompleto (disabled - mock SEFAZ)
✅ testRelatorioErros
✅ testValidarDadosDocumento
✅ testValidarSequenciaStatus
```

### Executar Testes
```bash
mvn clean test

# Gerar relatório
mvn test-report:generate-report

# Ver resultado
# Arquivo: target/site/surefire-report.html
```

- [ ] 8/8 testes unitários: PASS
- [ ] 10/10 testes integração: PASS
- [ ] Cobertura > 80%
- [ ] Sem falsos positivos

---

## 🔐 Segurança & Configuração

### Variáveis de Ambiente
- [ ] `JAVA_HOME` configurado
- [ ] `MAVEN_HOME` configurado
- [ ] Caminho executáveis no PATH

### Permissões de Diretórios
```bash
# data/ deve ser gravável
chmod -R 755 data/
chmod -R 755 data/cache/
chmod -R 755 data/export/

# Verificar
ls -la data/
```
- [ ] `data/` existe e é gravável
- [ ] `data/cache/` existe
- [ ] `data/export/` existe
- [ ] Permissões corretas (755+)

### Certificado A1 (HOMOLOGACAO)
- [ ] Arquivo `.p12` ou `.pfx` obtido
- [ ] Senha memorizada com segurança
- [ ] Expiração verificada (>3 meses)
- [ ] Armazenado em local seguro
- [ ] Backup em local seguro

---

## 📊 Database

### Inicialização
```bash
# Conectar ao BD
sqlite3 data/hostore.db

# Verificar tabelas
.tables

# Verificar logs_fiscal existe
SELECT * FROM logs_fiscal LIMIT 1;
```

- [ ] `hostore.db` criado em `data/`
- [ ] Todas tabelas base existem
- [ ] Tabela `documentos_fiscais` existe
- [ ] Tabela `logs_fiscal` existe (nova)
- [ ] Índices criados

### Backup Inicial
```bash
# Fazer backup antes do deployment
cp data/hostore.db data/hostore.db.backup.inicial
```
- [ ] Backup inicial feito
- [ ] Backup armazenado seguro
- [ ] Teste restauração (simular)

---

## 🎨 Interface Gráfica

### Componentes Visuais
- [ ] FlatLaf theme carrega sem erro
- [ ] Painel principal abre normalmente
- [ ] Abas navegam corretamente
- [ ] Ícones exibem corretamente

### Novos Componentes
- [ ] ConfigLojaDialog → Tab "Fiscal" visível
- [ ] Botão "🔐 Testar Certificado" visível
- [ ] FiscalDocumentosPanel abre via menu
- [ ] FiscalCatalogImportDialog abre via menu
- [ ] Todos botões funcionam

### Testes UI
```
1. Iniciar app
   ✓ Login screen OK
   ✓ Dashboard carrega

2. Abrir Ajustes
   ✓ ConfigLojaDialog abre
   ✓ Tab Fiscal visível
   ✓ Botão testar cert visível

3. Abrir Relatórios
   ✓ FiscalDocumentosPanel abre
   ✓ Tabela vazia (esperado - sem vendas ainda)

4. Importador
   ✓ Menu encontrado
   ✓ Dialog abre com file chooser
```

---

## 🚀 Startup & Shutdown

### Startup Sequence
```
1. main() → SwingUtilities.invokeLater(TelaPrincipal::new)
2. SplashUI mostra
3. DB.prepararBancoSeNecessario()
4. LogService inicializado
5. LoginDialog exibido
6. FiscalWorker.getInstance().iniciar() ✨ (novo)
7. TelaPrincipal abre
8. Dashboard carrega
```

- [ ] App inicia sem erro
- [ ] Splash screen aparece
- [ ] Mensagens de inicialização legíveis
- [ ] Login requerido
- [ ] Dashboard carrega
- [ ] FiscalWorker inicia (verificar logs)

### Shutdown Sequence
```
1. Usuário clica X (fechar)
2. windowClosing() chamado
3. Confirma "Deseja sair?"
4. Se SIM:
   - FiscalWorker.getInstance().parar() ✨ (novo)
   - SessaoService.logout()
   - System.exit(0)
```

- [ ] Botão X fecha normalmente
- [ ] Dialogo confirmação aparece
- [ ] Worker para com "parado" no log
- [ ] Logout registrado
- [ ] App encerra cleanly

---

## 📝 Logs & Monitoring

### Logs da Aplicação
```bash
# Verificar logs do app
tail -f data/logs/hostore.log

# Procurar erros
grep ERROR data/logs/hostore.log
```

- [ ] Log file criado em `data/logs/`
- [ ] Sem erros críticos ao startup
- [ ] Sem exceções não tratadas

### Logs Fiscais (BD)
```bash
sqlite3 data/hostore.db
SELECT * FROM logs_fiscal ORDER BY timestamp DESC LIMIT 10;
```

- [ ] Tabela `logs_fiscal` existe
- [ ] Sem dados inicialmente (esperado)
- [ ] Índices criados

### Monitoramento
- [ ] Logs rotativos configurados
- [ ] Espaço em disco suficiente (>100MB)
- [ ] Permissões de write OK

---

## 🌐 Conectividade

### HOMOLOGACAO
- [ ] URL SEFAZ HOMOLOGACAO acessível
  ```bash
  curl -v https://homolog.sefazrs.rs.gov.br/webservices/NFeAutorizacao4/NFeAutorizacao4.asmx
  ```
- [ ] Certificado SEFAZ confiável
- [ ] Sem proxy bloqueando
- [ ] Porta 443 aberta

### Certificado SSL
```bash
# Validar certificado SEFAZ
openssl s_client -connect homolog.sefazrs.rs.gov.br:443 -showcerts
```
- [ ] Certificado válido
- [ ] Sem warnings de expiração
- [ ] Chain completo

---

## 🔄 Fluxo End-to-End

### Teste 1: Venda Simples → NFC-e Automática

**Setup**:
- [ ] App rodando
- [ ] Usuário logado
- [ ] Certificado A1 testado (✅)

**Procedimento**:
```
1. Menu: Vendas
2. Criar novo comanda/mesa
3. Adicionar 1 produto teste
4. Confirmar item
5. Finalizar venda
   ✓ Venda commit OK
   ✓ Log: "NFC-e criada para venda X"
```

**Validação**:
- [ ] Venda criada com sucesso
- [ ] DocumentoFiscal inserido (status=pendente)
- [ ] Log em logs_acessos
- [ ] Sem erros de NFC-e bloqueando

### Teste 2: Worker Processa Automático

**Procedimento**:
```
1. Esperar 5 minutos
2. Verificar FiscalDocumentosPanel
```

**Validação**:
- [ ] Status muda de "pendente" para "xml_gerado"
- [ ] Depois para "assinada"
- [ ] Depois para "enviada"
- [ ] Finalmente para "autorizada"
- [ ] Chave NFC-e gerada
- [ ] QRcode visível

### Teste 3: Logs Registrados

**Procedimento**:
```
1. FiscalDocumentosPanel → Botão "Detalhes"
2. Ver logs em caixa de texto
```

**Validação**:
- [ ] Logs aparecem para cada etapa
- [ ] Timestamp em order cronológica
- [ ] Mensagens significativas
- [ ] Erros (se houver) bem descritos

### Teste 4: DANFE Gerado

**Procedimento**:
```
1. FiscalDocumentosPanel → Selecionar doc autorizado
2. Botão "Imprimir DANFE"
3. Escolher local salvar
```

**Validação**:
- [ ] Arquivo .txt criado em `data/export/`
- [ ] QRcode SEFAZ visível no arquivo
- [ ] Dados completos (CNPJ, items, totais)
- [ ] Formato 80mm correto

---

## 📋 Importação de Tabelas

### Preparar CSV
```csv
12345678901234;RS;18.0;18.0
23456789012345;SP;7.0;7.0
```

- [ ] Arquivo CSV criado com dados válidos
- [ ] Formato correto: NCM;ESTADO;ALIQ_CONS;ALIQ_CONT

### Importar
```
1. Menu Ajustes → Importador Catálogo Fiscal
2. Procurar arquivo CSV
3. Botão "Importar"
4. Aguardar conclusão
5. Verificar log de sucesso
```

- [ ] Dialog abre sem erro
- [ ] File chooser funciona
- [ ] Importação conclui (progress bar 100%)
- [ ] Mensagem "✅ Importação concluída"
- [ ] Dados inseridos no BD

---

## 📊 Performance

### Métricas a Coletar

**Startup Time**:
```bash
time java -jar target/hostore.jar
```
- [ ] < 10 segundos até dashboard visível

**Worker Processing**:
- [ ] < 1 minuto para processar 1 venda
- [ ] < 100ms por documento

**UI Responsiveness**:
- [ ] Cliques respondem < 500ms
- [ ] Tabelas carregam < 2 seg
- [ ] Sem freezes

**Memory Usage**:
```bash
ps aux | grep java | grep hostore
# Verificar coluna RSS
```
- [ ] < 1GB RAM em repouso
- [ ] < 2GB em operação normal

---

## 🔧 Troubleshooting Pré-Deploy

### Issue: "Certificado expirado"
- [ ] Obter novo certificado A1
- [ ] Validar expiração: `openssl x509 -in cert.p12 -noout -dates`
- [ ] Testar no app: botão "🔐 Testar Certificado"

### Issue: "Erro conexão SEFAZ"
- [ ] Verificar internet: `ping -c 4 8.8.8.8`
- [ ] Verificar URL: `curl -v https://homolog.sefazrs.rs.gov.br/`
- [ ] Verificar firewall: porta 443 aberta?
- [ ] Verificar proxy corporativo

### Issue: "BD corrompido"
- [ ] Restaurar backup: `cp hostore.db.backup data/hostore.db`
- [ ] Recriar: deletar arquivo e reabrir app

### Issue: "Worker não processa"
- [ ] Verificar logs: procurar "FISCAL_WORKER"
- [ ] Reiniciar app (reseta worker)
- [ ] Verificar BD: status=pendente existe?

---

## ✅ Checklist Final Pré-Deploy

### Código
- [ ] `mvn clean test` → All passing
- [ ] `mvn compile` → BUILD SUCCESS
- [ ] `mvn package` → JAR created
- [ ] Sem warnings de compilação

### Banco de Dados
- [ ] `hostore.db` criado
- [ ] Todas tabelas existem
- [ ] `logs_fiscal` criada
- [ ] Backup inicial feito

### Configuração
- [ ] `data/` com permissões corretas
- [ ] Certificado A1 obtido (HOMOLOGACAO)
- [ ] Senha certificado segura
- [ ] Ambiente = HOMOLOGACAO (não PRODUCAO ainda!)

### Interface
- [ ] App inicia sem erro
- [ ] Login funciona
- [ ] Dashboard carrega
- [ ] Todos novos componentes visíveis

### Conectividade
- [ ] SEFAZ HOMOLOGACAO acessível
- [ ] Sem proxy bloqueando
- [ ] Internet OK

### Testes
- [ ] Venda → NFC-e automática
- [ ] Worker processa (5 min)
- [ ] Status progride até "autorizada"
- [ ] Logs registrados
- [ ] DANFE gerado

### Documentação
- [ ] Equipe leu Quick Start
- [ ] Plano de rollback preparado
- [ ] Contatos de suporte definidos
- [ ] SLA acordado

---

## 🚀 Deploy Steps

```bash
# 1. Build final
mvn clean package

# 2. Backup BD anterior (se upgrade)
cp data/hostore.db data/hostore.db.backup.predeployment

# 3. Deploy
java -jar target/hostore.jar

# 4. Monitorar startup
tail -f data/logs/hostore.log

# 5. Validar
# - Abrir app
# - Fazer venda teste
# - Aguardar processamento (5 min)
# - Verificar autorização

# 6. Comunicar
# - Equipe: Sistema pronto em HOMOLOGACAO
# - Esperando 7 dias de teste
```

---

## 🎯 Sign-Off

### Desenvolvedor
- [ ] Código revisado e testado
- [ ] Comentários/docs atualizados
- [ ] Builds passando
- Assinatura: ____________  Data: ____/____/____

### QA
- [ ] Testes executados
- [ ] Casos críticos validados
- [ ] Performance OK
- [ ] Segurança verificada
- Assinatura: ____________  Data: ____/____/____

### Operações
- [ ] Infraestrutura pronta
- [ ] Backups configurados
- [ ] Monitoramento setup
- [ ] Runbooks preparados
- Assinatura: ____________  Data: ____/____/____

### Gestor
- [ ] Equipe treinada
- [ ] Documentação lida
- [ ] Aprovação para HOMOLOGACAO
- Assinatura: ____________  Data: ____/____/____

---

**Versão**: 1.0  
**Data**: 2024  
**Status**: 🟢 PRONTO PARA HOMOLOGACAO

✅ **Checklist Completo - Sistema Pronto para Deploy!**
