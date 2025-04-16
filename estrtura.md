/lib                     ← (dependências externas, se houver)
/scripts                ← (scripts extras ou utilitários)
/data                   ← (pasta de dados persistidos - SQLite DB, arquivos exportados etc.)

/src
│
├── app/
│   └── Main.java                      ← ponto de entrada da aplicação
│
├── controller/                        ← (reservado, ainda vazio)
│
├── dao/                               ← acesso direto ao banco (SQLite)
│   ├── ClienteDAO.java                ← CRUD de clientes
│   ├── VendaDAO.java                  ← CRUD da venda
│   └── VendaItemDAO.java              ← CRUD dos itens de venda
│
├── model/                             ← estrutura das entidades
│   ├── Carta.java                     ← entidade de carta/estoque
│   ├── ClienteModel.java              ← entidade de cliente
│   ├── VendaModel.java                ← entidade da venda
│   └── VendaItemModel.java            ← entidade dos itens da venda
│
├── service/                           ← regras de negócio (validação, integração, lógica de app)
│   ├── ClienteService.java            ← lógica de clientes
│   ├── EstoqueService.java            ← controle de estoque (cartas)
│   └── VendaService.java              ← lógica da venda (inicia, adiciona item, totaliza, finaliza)
│
├── ui/                                ← interface visual
│   ├── dialog/                        ← modais (janela flutuante)
│   │   ├── ClienteCadastroDialog.java ← modal de cliente
│   │   ├── VendaFinalizarDialog.java  ← finalizar venda (pagamento, total etc.)
│   │   └── VendaNovaDialog.java       ← nova venda (seleção de cliente e cartas)
│   │
│   ├── DashboardPanel.java            ← painel inicial / home do sistema
│   ├── PainelClientes.java            ← tela principal de clientes
│   ├── PainelVendas.java              ← tela principal de vendas
│   └── TelaPrincipal.java             ← janela principal do app
│
├── util/                              ← utilitários diversos
│   ├── DB.java                        ← conexão com banco SQLite
│   └── PythonCaller.java              ← ponte de integração com scripts Python
