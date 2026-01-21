package ui.estoque.dialog;

import util.UiKit;
import util.AlertUtils;
import util.DB;
import org.apache.poi.ss.usermodel.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import util.ColecaoMapper;
import ui.estoque.dialog.VincularColecaoDialog;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;

/**
 * Dialog responsável por importar cartas a partir da planilha (.xlsx ou .xlsm)
 * da Liga.
 *
 * Fluxo:
 * 1) Seleciona arquivo
 * 2) Mostra prévia (10 primeiras linhas) – opcional
 * 3) Importa em lote (UPSERT) usando transações de 500 registros
 *
 * Inclui logs em todas as etapas para facilitar o debug.
 * Ajustado para ler “Quantidade Existente” em vez de “Quantidade Para
 * Somar/Subtrair”,
 * e para aceitar campos vazios de preço (assumindo 0.0) e de quantidade
 * (assumindo 0).
 */
public class ImportLigaDialog extends JDialog {

    // --- UI -----------------------------------------------------------------
    private final JTextField txtArquivo = new JTextField();
    private final JButton btnEscolher = new JButton("Escolher .xlsx/.xlsm...");
    private final JButton btnImportar = new JButton("Importar");
    private final JButton btnFechar = new JButton("Fechar");
    private final JProgressBar progress = new JProgressBar(0, 100);
    private final JTextArea previewArea = new JTextArea(8, 60);

    // --- Mapeamento de Raridades -------------------------------------------
    private static final Map<String, String> MAP_RARIDADE = new HashMap<>();
    static {
        MAP_RARIDADE.put("comum", "R1");
        MAP_RARIDADE.put("incomum", "R2");
        MAP_RARIDADE.put("rara", "R3");
        MAP_RARIDADE.put("promo", "R4");
        MAP_RARIDADE.put("foil", "R5");
        MAP_RARIDADE.put("foil reverse", "R6");
        MAP_RARIDADE.put("reverse", "R6");
        MAP_RARIDADE.put("secreta", "R7");
    }

    public ImportLigaDialog(Window parent) {
        super(parent, "Importar planilha da Liga", ModalityType.APPLICATION_MODAL);
        UiKit.applyDialogBase(this);
        setLayout(new BorderLayout(10, 10));
        ((JComponent) getContentPane()).setBorder(new EmptyBorder(10, 10, 10, 10));
        initUI();
        pack();
        setLocationRelativeTo(parent);
        System.out.println("[LOG] ImportLigaDialog criado.");
    }

    // -----------------------------------------------------------------------
    // UI
    // -----------------------------------------------------------------------
    private void initUI() {
        System.out.println("[LOG] Inicializando UI do ImportLigaDialog.");
        // Linha de seleção de arquivo
        txtArquivo.setEditable(false);
        JPanel filePanel = new JPanel(new BorderLayout(5, 5));
        filePanel.add(txtArquivo, BorderLayout.CENTER);
        filePanel.add(btnEscolher, BorderLayout.EAST);

        // Área de preview
        previewArea.setEditable(false);
        JScrollPane scroll = UiKit.scroll(previewArea);

        // Botões inferiores
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.add(btnImportar);
        btnPanel.add(btnFechar);

        add(filePanel, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        add(progress, BorderLayout.SOUTH);
        add(btnPanel, BorderLayout.PAGE_END);

        // Listeners
        btnEscolher.addActionListener(e -> {
            System.out.println("[LOG] Botão Escolher clicado.");
            escolherArquivo();
        });
        btnImportar.addActionListener(e -> {
            System.out.println("[LOG] Botão Importar clicado.");
            iniciarImportacao();
        });
        btnFechar.addActionListener(e -> {
            System.out.println("[LOG] Botão Fechar clicado.");
            dispose();
        });

        btnImportar.setEnabled(false); // Só habilita após selecionar arquivo válido
    }

    private void escolherArquivo() {
        System.out.println("[LOG] Abrindo JFileChooser para selecionar planilha.");
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("Planilhas Excel (.xlsx ou .xlsm)", "xlsx", "xlsm"));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            System.out.println("[LOG] Arquivo selecionado: " + f.getAbsolutePath());
            txtArquivo.setText(f.getAbsolutePath());
            carregarPreview(f);
            btnImportar.setEnabled(true);
        } else {
            System.out.println("[LOG] Seleção de arquivo cancelada.");
        }
    }

    private void carregarPreview(File file) {
        System.out.println("[LOG] Iniciando pré-visualização do arquivo: " + file.getAbsolutePath());
        try (FileInputStream fis = new FileInputStream(file);
                Workbook wb = WorkbookFactory.create(fis)) {

            System.out.println("[LOG] Workbook carregado para preview.");
            Sheet sh = wb.getSheetAt(0);
            StringBuilder sb = new StringBuilder();
            DataFormatter fmt = new DataFormatter();
            int linhasPreview = 0;

            for (Row row : sh) {
                if (row.getRowNum() < 6) {
                    // Ignora instruções e cabeçalho
                    continue;
                }
                if (linhasPreview >= 10) {
                    // Limita a 10 linhas
                    break;
                }

                sb.append('|');
                for (int i = 0; i < 9; i++) {
                    Cell c = row.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    String valor = (c != null ? fmt.formatCellValue(c) : "");
                    sb.append(valor).append('|');
                }
                sb.append('\n');
                linhasPreview++;
            }

            previewArea.setText(sb.toString());
            System.out.println("[LOG] Pré-visualização carregada (" + linhasPreview + " linhas).");
        } catch (Exception ex) {
            System.err.println("[ERROR] Falha ao pré-visualizar planilha: " + ex.getMessage());
            AlertUtils.error("Erro ao pré-visualizar a planilha:\n" + ex.getMessage());
        }
    }

    private void iniciarImportacao() {
        String caminho = txtArquivo.getText();
        System.out.println("[LOG] Iniciando importação. Arquivo: " + caminho);
        File arquivo = new File(caminho);
        btnImportar.setEnabled(false);

        new SwingWorker<Void, Integer>() {
            @Override
            protected Void doInBackground() {
                importarArquivo(arquivo);
                return null;
            }

            @Override
            protected void done() {
                btnImportar.setEnabled(true);
                progress.setValue(100);
                System.out.println("[LOG] SwingWorker finalizado.");
            }
        }.execute();
    }

    // -----------------------------------------------------------------------
    // Importação propriamente dita (com logs detalhados)
    // -----------------------------------------------------------------------
    private void importarArquivo(File file) {
        System.out.println("===== INÍCIO DA IMPORTAÇÃO =====");
        int totalLinhasImportadas = 0;
        int totalLinhasIgnoradas = 0;
        int totalInseridas = 0;
        int totalAtualizadas = 0;

        // 1) Abrir arquivo e Workbook
        try (FileInputStream fis = new FileInputStream(file)) {
            System.out.println("[LOG] FileInputStream aberto: " + file.getAbsolutePath());
            Workbook wb = WorkbookFactory.create(fis);
            System.out.println("[LOG] Workbook criado. Primeira aba: " + wb.getSheetName(0));

            // 2) Obter conexão com banco
            Connection conn = DB.get();
            System.out.println("[LOG] Conexão SQLite aberta.");

            Sheet sheet = wb.getSheetAt(0);

            // 3) Localizar linha de cabeçalho (índice 5)
            Row headerRow = sheet.getRow(5); // linha 6 no Excel
            if (headerRow == null) {
                System.err.println("[ERROR] Cabeçalho não encontrado na linha 6 (índice 5).");
                SwingUtilities.invokeLater(() -> AlertUtils.error("Cabeçalho não encontrado na linha 6 (índice 5)."));
                return;
            }
            System.out.println("[LOG] Cabeçalho encontrado na linha 6.");

            // 4) Mapear “texto normalizado de cabeçalho” → índice da coluna
            Map<String, Integer> colIndex = new HashMap<>();
            for (Cell cell : headerRow) {
                String bruto = cell.getStringCellValue();
                String semQuebra = bruto.replace("\n", " ");
                String texto = semQuebra.trim().replaceAll("\\s+", " ");
                colIndex.put(texto, cell.getColumnIndex());
                System.out.printf("[LOG] Cabeçalho detectado: [%d] \"%s\"%n", cell.getColumnIndex(), texto);
            }
            System.out.println("[LOG] Mapeamento de cabeçalhos concluído.");

            // 5) Verificar cabeçalhos obrigatórios
            String[] cabeçalhosObrigatorios = {
                    "Edição Sigla",
                    "Número",
                    "Nome da Carta",
                    "Raridade",
                    "Reverse Foil (0 ou 1)",
                    "Quantidade Existente",
                    "Preço"
            };
            for (String h : cabeçalhosObrigatorios) {
                if (!colIndex.containsKey(h)) {
                    System.err.println("[ERROR] Cabeçalho obrigatório não encontrado: \"" + h + "\"");
                    SwingUtilities
                            .invokeLater(() -> AlertUtils.error("Cabeçalho \"" + h + "\" não encontrado na planilha."));
                    return;
                } else {
                    System.out.println(
                            "[LOG] Cabeçalho obrigatório encontrado: \"" + h + "\" no índice " + colIndex.get(h));
                }
            }

            // 6) Recuperar índices das colunas
            int idxSetSigla = colIndex.get("Edição Sigla");
            int idxNumero = colIndex.get("Número");
            int idxNome = colIndex.get("Nome da Carta");
            int idxRaridade = colIndex.get("Raridade");
            int idxReverse = colIndex.get("Reverse Foil (0 ou 1)");
            int idxQtdExist = colIndex.get("Quantidade Existente");
            int idxPreco = colIndex.get("Preço");
            System.out.printf(
                    "[LOG] Índices mapeados: SetSigla=%d, Número=%d, Nome=%d, Raridade=%d, Reverse=%d, Quantidade Existente=%d, Preço=%d%n",
                    idxSetSigla, idxNumero, idxNome, idxRaridade, idxReverse, idxQtdExist, idxPreco);

            // 7) Preparar SQL de UPSERT
            final String sql = """
                        INSERT INTO cartas (
                            id, nome, set_id, numero, qtd, preco, custo,
                            raridade_id, sub_raridade_id,
                            condicao_id, linguagem_id, consignado, dono,
                            tipo_id, subtipo_id, colecao
                        ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                        ON CONFLICT(id) DO UPDATE SET
                            qtd = cartas.qtd + excluded.qtd,
                            preco = excluded.preco,
                            custo = COALESCE(excluded.custo, cartas.custo),
                            raridade_id = excluded.raridade_id,
                            sub_raridade_id = excluded.sub_raridade_id,
                            condicao_id = excluded.condicao_id,
                            linguagem_id = excluded.linguagem_id,
                            colecao = excluded.colecao;
                    """;

            conn.setAutoCommit(false);
            System.out.println("[LOG] SQL de UPSERT preparado.");

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                DataFormatter fmt = new DataFormatter();
                int batchSize = 0;
                int ultimaLinha = sheet.getLastRowNum();
                System.out.println("[LOG] Última linha da planilha (0-based): " + ultimaLinha);

                // 8) Iterar sobre as linhas de dado (a partir de rn = 6)
                for (int rn = 6; rn <= ultimaLinha; rn++) {
                    Row row = sheet.getRow(rn);
                    System.out.println("[LOG] Processando linha " + (rn + 1));
                    if (row == null) {
                        System.out.println("[IGNORADA] Linha " + (rn + 1) + ": row nula");
                        totalLinhasIgnoradas++;
                        continue;
                    }

                    // (a) Edição Sigla
                    Cell cSet = row.getCell(idxSetSigla, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    String setSigla = (cSet != null ? fmt.formatCellValue(cSet).trim() : "");
                    if (setSigla.isEmpty()) {
                        System.out.println("[IGNORADA] Linha " + (rn + 1) + ": Edição Sigla vazia");
                        totalLinhasIgnoradas++;
                        continue;
                    }

                    // Buscar no banco a coleção correspondente à sigla
                    String setId = null;
                    String nomeColecao = null;

                    try (PreparedStatement psCol = conn.prepareStatement(
                            "SELECT id, nome FROM colecoes WHERE sigla = ? LIMIT 1")) {
                        psCol.setString(1, setSigla);
                        try (ResultSet rsCol = psCol.executeQuery()) {
                            if (rsCol.next()) {
                                setId = rsCol.getString("id"); // Ex: sv10
                                nomeColecao = rsCol.getString("nome"); // Ex: Destined Rivals
                            } else {
                                System.out.println("[IGNORADA] Linha " + (rn + 1) + ": sigla \"" + setSigla
                                        + "\" não encontrada na tabela coleções");
                                totalLinhasIgnoradas++;
                                continue;
                            }
                        }
                    }

                    // (b) Número
                    Cell cNum = row.getCell(idxNumero, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    String numero = (cNum != null ? fmt.formatCellValue(cNum).trim() : "");
                    if (numero.isEmpty()) {
                        System.out.println("[IGNORADA] Linha " + (rn + 1) + ": Número vazio");
                        totalLinhasIgnoradas++;
                        continue;
                    }

                    // (c) Nome da Carta
                    Cell cNome = row.getCell(idxNome, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    String nomeCarta = (cNome != null ? fmt.formatCellValue(cNome).trim() : "");
                    if (nomeCarta.isEmpty()) {
                        System.out.println("[IGNORADA] Linha " + (rn + 1) + ": Nome da Carta vazio");
                        totalLinhasIgnoradas++;
                        continue;
                    }

                    // (d) Raridade
                    Cell cRar = row.getCell(idxRaridade, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    String raridadeTxt = (cRar != null
                            ? fmt.formatCellValue(cRar).trim().toLowerCase()
                            : "");
                    if (raridadeTxt.isEmpty()) {
                        System.out.println("[IGNORADA] Linha " + (rn + 1) + ": Raridade vazia");
                        totalLinhasIgnoradas++;
                        continue;
                    }

                    // (e) Reverse Foil
                    Cell cRev = row.getCell(idxReverse, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    String reverseFlag = (cRev != null
                            ? fmt.formatCellValue(cRev).trim()
                            : "0");

                    // (f) Quantidade Existente
                    Cell cQtdExist = row.getCell(idxQtdExist, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    String qtdExistStr = (cQtdExist != null ? fmt.formatCellValue(cQtdExist).trim() : "");
                    int qtdExist = 0;
                    if (!qtdExistStr.isEmpty()) {
                        try {
                            qtdExist = (int) Double.parseDouble(qtdExistStr.replace(",", "."));
                            System.out.println("[LOG] Quantidade Existente convertida: " + qtdExist);
                        } catch (NumberFormatException e) {
                            System.err.println("[IGNORADA] Linha " + (rn + 1) + ": Quantidade Existente inválida \""
                                    + qtdExistStr + "\"");
                            totalLinhasIgnoradas++;
                            continue;
                        }
                    } else {
                        // Se estiver em branco, assume zero
                        qtdExist = 0;
                        System.out.println("[LOG] Quantidade Existente vazia; usando 0");
                    }

                    // (g) Preço
                    Cell cPreco = row.getCell(idxPreco, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    String precoStr = (cPreco != null ? fmt.formatCellValue(cPreco).trim() : "");
                    double preco = 0.0;
                    if (!precoStr.isEmpty()) {
                        try {
                            preco = Double.parseDouble(precoStr.replace(",", "."));
                            System.out.println("[LOG] Preço convertido: " + preco);
                        } catch (NumberFormatException e) {
                            System.err
                                    .println("[IGNORADA] Linha " + (rn + 1) + ": Preço inválido \"" + precoStr + "\"");
                            totalLinhasIgnoradas++;
                            continue;
                        }
                    } else {
                        // Se estiver em branco, assume zero
                        preco = 0.0;
                        System.out.println("[LOG] Preço vazio; usando 0.0");
                    }

                    // (h) Mapear raridade textual → ID
                    String raridadeId = MAP_RARIDADE.getOrDefault(raridadeTxt, null);
                    if (raridadeId == null) {
                        System.err.println(
                                "[IGNORADA] Linha " + (rn + 1) + ": Raridade desconhecida \"" + raridadeTxt + "\"");
                        totalLinhasIgnoradas++;
                        continue;
                    }
                    System.out.println("[LOG] Raridade mapeada: " + raridadeTxt + " → " + raridadeId);

                    // (i) Verificar sub-rareza (Reverse Foil)
                    String subRaridadeId = ("1".equals(reverseFlag.trim()) ? "SR6" : null);
                    if (subRaridadeId != null) {
                        System.out.println("[LOG] Sub-Raridade: Foil Reverse detectado, subRaridadeId = SR6");
                    }

                    // (j) Gerar ID único da carta
                    String idCarta = setSigla + "-" + numero + (subRaridadeId != null ? "-R" : "");
                    // @CTRL+F: VALORES_DEFAULT_CARTA
                    double custo = preco; // Assume custo igual ao preço
                    String condicaoId = "C1"; // Near Mint
                    String linguagemId = "L1"; // Português
                    int consignado = 0; // Produto da loja
                    String dono = "LOJA"; // Dono padrão
                    String tipoId = "T1"; // Tipo: Pokémon
                    String subtipoId = "S1"; // Subtipo: Básico
                    System.out.println("[LOG] ID gerado para a carta: " + idCarta);

                    // (k.1) Verificar se já existe no banco
                    boolean jaExiste = false;
                    try (PreparedStatement psCheck = conn.prepareStatement("SELECT 1 FROM cartas WHERE id = ?")) {
                        psCheck.setString(1, idCarta);
                        try (ResultSet rs = psCheck.executeQuery()) {
                            jaExiste = rs.next();
                        }
                    }
                    if (jaExiste)
                        totalAtualizadas++;
                    else
                        totalInseridas++;

                    // (k) Preencher PreparedStatement
                    // @CTRL+F: PREPARED_STATEMENT_IMPORT
                    ps.setString(1, idCarta); // id
                    ps.setString(2, nomeCarta); // nome
                    ps.setString(3, setId); // set_id
                    ps.setString(4, numero); // numero
                    ps.setInt(5, qtdExist); // qtd
                    ps.setDouble(6, preco); // preco
                    ps.setDouble(7, custo); // custo
                    ps.setString(8, raridadeId); // raridade_id
                    if (subRaridadeId != null) {
                        ps.setString(9, subRaridadeId);
                    } else {
                        ps.setNull(9, Types.VARCHAR);
                    }
                    ps.setString(10, condicaoId); // condicao_id
                    ps.setString(11, linguagemId); // linguagem_id
                    ps.setInt(12, consignado); // consignado
                    ps.setString(13, dono); // dono
                    ps.setString(14, tipoId); // tipo_id
                    ps.setString(15, subtipoId); // subtipo_id
                    ps.setString(16, nomeColecao); // colecao
                    ps.addBatch();

                    // @CTRL+F: INSERIR_PRODUTO_CORRESPONDENTE
                    try (PreparedStatement pProd = conn.prepareStatement(
                            """
                                        INSERT INTO produtos (id, nome, tipo, quantidade, preco_compra, preco_venda, jogo_id, criado_em, alterado_em)
                                        VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                                        ON CONFLICT(id) DO UPDATE SET
                                            quantidade = produtos.quantidade + excluded.quantidade,
                                            preco_compra = excluded.preco_compra,
                                            preco_venda = excluded.preco_venda,
                                            alterado_em = CURRENT_TIMESTAMP
                                    """)) {
                        pProd.setString(1, idCarta);
                        pProd.setString(2, nomeCarta);
                        pProd.setString(3, "Carta");
                        pProd.setInt(4, qtdExist);
                        pProd.setDouble(5, custo);
                        pProd.setDouble(6, preco);
                        pProd.setString(7, "POKEMON");
                        pProd.executeUpdate();
                    }

                    System.out.println("[LOG] Adicionada ao batch: " + idCarta);

                    totalLinhasImportadas++;
                    batchSize++;

                    // 9) Execute batch a cada 500 registros
                    if (batchSize >= 500) {
                        System.out.println("[LOG] Executando batch de 500 registros...");
                        ps.executeBatch();
                        conn.commit();
                        System.out.println("[LOG] Batch executado e commit realizado.");
                        batchSize = 0;
                    }
                }

                // 10) Flush final do batch
                if (batchSize > 0) {
                    System.out.println("[LOG] Executando último batch com " + batchSize + " registros...");
                    ps.executeBatch();
                    conn.commit();
                    System.out.println("[LOG] Último batch executado e commit realizado.");
                }
            }

            // 11) Resumo final
            String msg = String.format(
                    "✅ Importação concluída!\n\n" +
                            "🆕 Novas cartas inseridas: %d\n" +
                            "🔁 Cartas atualizadas: %d\n" +
                            "⚠️ Cartas ignoradas: %d",
                    totalInseridas, totalAtualizadas, totalLinhasIgnoradas);

            System.out.printf("[LOG] Resumo: importadas=%d | ignoradas=%d%n",
                    totalLinhasImportadas, totalLinhasIgnoradas);
            SwingUtilities.invokeLater(() -> AlertUtils.info(msg));

        } catch (Exception ex) {
            System.err.println("[ERROR] Exceção durante importação: " + ex.getMessage());
            ex.printStackTrace();
            SwingUtilities.invokeLater(() -> AlertUtils.error("Falha na importação:\n" + ex.getMessage()));
        } finally {
            System.out.println("===== FIM DA IMPORTAÇÃO =====");
        }
    }
}
