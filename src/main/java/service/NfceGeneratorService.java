package service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import util.DB;
import model.*;

/**
 * NfceGeneratorService: Serviço completo de geração e emissão de NFCe
 * 
 * Funcionalidades:
 * - Gerar número de NFCe com sequência
 * - Construir XML válido
 * - Gerar chave de acesso
 * - Assinar digitalmente (placeholder para certificado)
 * - Armazenar no banco de dados
 */
public class NfceGeneratorService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * Gera NFCe a partir de uma venda
     */
    public static String gerarNfce(int vendaId) throws Exception {
        try (Connection conn = DB.get()) {
            conn.setAutoCommit(false);
            
            // 1. Carregar dados da venda
            VendaModel venda = carregarVenda(conn, vendaId);
            if (venda == null) {
                throw new Exception("Venda não encontrada: " + vendaId);
            }
            
            // 2. Carregar itens da venda
            List<VendaItemModel> itens = carregarItensVenda(conn, vendaId);
            
            // 3. Carregar configuração fiscal
            ConfigNfceModel config = carregarConfiguracao(conn);
            
            // 4. Gerar número de NFCe
            int numeroNfce = obterProximoNumeroNfce(conn, config);
            
            // 5. Construir XML
            String xml = construirXmlNfce(venda, itens, config, numeroNfce);
            
            // 6. Calcular chave de acesso
            String chaveAcesso = calcularChaveAcesso(config.getCnpj(), 
                                                     config.getSerieNfce(), 
                                                     numeroNfce,
                                                     xml);
            
            // 7. Assinar XML (placeholder)
            String xmlAssinado = assinarXml(xml, chaveAcesso);
            
            // 8. Criar registro de documento fiscal
            String docId = UUID.randomUUID().toString();
            inserirDocumentoFiscal(conn, docId, vendaId, numeroNfce, 
                                  config.getSerieNfce(), chaveAcesso, 
                                  xmlAssinado, venda);
            
            // 9. Atualizar venda com NFCe
            atualizarVendaComNfce(conn, vendaId, numeroNfce);
            
            conn.commit();
            
            System.out.println("✅ NFCe gerada com sucesso!");
            System.out.println("  Número: " + numeroNfce);
            System.out.println("  Chave de Acesso: " + chaveAcesso);
            System.out.println("  Documento ID: " + docId);
            
            return chaveAcesso;
            
        } catch (Exception e) {
            System.err.println("❌ Erro ao gerar NFCe: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Carrega dados da venda do banco
     */
    private static VendaModel carregarVenda(Connection conn, int vendaId) throws SQLException {
        String sql = """
            SELECT id, cliente_id, data_venda, total_produtos, total_desconto, 
                   total_acrescimo, total_final, status_fiscal
            FROM vendas WHERE id = ?
            """;
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, vendaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    VendaModel venda = new VendaModel();
                    venda.id = rs.getInt("id");
                    venda.clienteId = rs.getInt("cliente_id");
                    venda.dataVenda = rs.getString("data_venda");
                    venda.totalProdutos = rs.getDouble("total_produtos");
                    venda.totalDesconto = rs.getDouble("total_desconto");
                    venda.totalAcrescimo = rs.getDouble("total_acrescimo");
                    venda.totalFinal = rs.getDouble("total_final");
                    venda.statusFiscal = rs.getString("status_fiscal");
                    return venda;
                }
            }
        }
        return null;
    }

    /**
     * Carrega itens da venda
     */
    private static List<VendaItemModel> carregarItensVenda(Connection conn, int vendaId) throws SQLException {
        List<VendaItemModel> itens = new ArrayList<>();
        
        String sql = """
            SELECT id, venda_id, produto_id, qtd, valor_unit, desconto, 
                   acrescimo, total_item
            FROM vendas_itens WHERE venda_id = ?
            """;
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, vendaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    VendaItemModel item = new VendaItemModel();
                    item.id = rs.getInt("id");
                    item.vendaId = rs.getInt("venda_id");
                    item.produtoId = rs.getString("produto_id");
                    item.qtd = rs.getInt("qtd");
                    item.valorUnit = rs.getDouble("valor_unit");
                    item.desconto = rs.getDouble("desconto");
                    item.acrescimo = rs.getDouble("acrescimo");
                    item.totalItem = rs.getDouble("total_item");
                    
                    // Carregar dados fiscais do produto
                    carregarDadosFiscaisProduto(conn, item);
                    
                    itens.add(item);
                }
            }
        }
        
        return itens;
    }

    /**
     * Carrega NCM, CFOP, CSOSN, origem e unidade do produto
     */
    private static void carregarDadosFiscaisProduto(Connection conn, VendaItemModel item) throws SQLException {
        String sql = """
            SELECT ncm, cfop, csosn, origem, unidade, nome, descricao
            FROM produtos WHERE id = ?
            """;
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, item.produtoId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    item.ncm = rs.getString("ncm");
                    item.cfop = rs.getString("cfop");
                    item.csosn = rs.getString("csosn");
                    item.origem = rs.getString("origem");
                    item.unidade = rs.getString("unidade");
                    item.descricao = rs.getString("descricao");
                    
                    // Usar defaults se não preenchido
                    if (item.ncm == null || item.ncm.isEmpty()) item.ncm = "00000000";
                    if (item.cfop == null || item.cfop.isEmpty()) item.cfop = "5102";
                    if (item.csosn == null || item.csosn.isEmpty()) item.csosn = "102";
                    if (item.origem == null || item.origem.isEmpty()) item.origem = "0";
                    if (item.unidade == null || item.unidade.isEmpty()) item.unidade = "UN";
                }
            }
        }
    }

    /**
     * Carrega configuração fiscal da empresa
     */
    private static ConfigNfceModel carregarConfiguracao(Connection conn) throws SQLException {
        String sql = """
            SELECT id, emitir_nfce, csc_nfce, id_csc_nfce, cert_a1_path, 
                   cert_a1_senha, serie_nfce, numero_inicial_nfce, ambiente, 
                   regime_tributario, nome_empresa, cnpj, inscricao_estadual, uf,
                   nome_fantasia, endereco_logradouro, endereco_numero, 
                   endereco_complemento, endereco_bairro, endereco_municipio, endereco_cep
            FROM config_nfce LIMIT 1
            """;
        
        try (Statement st = conn.createStatement()) {
            try (ResultSet rs = st.executeQuery(sql)) {
                if (rs.next()) {
                    ConfigNfceModel config = new ConfigNfceModel();
                    config.setId(rs.getString("id"));
                    config.setEmitirNfce(rs.getInt("emitir_nfce"));
                    config.setCsc(rs.getString("csc_nfce"));
                    config.setIdCsc(rs.getInt("id_csc_nfce"));
                    config.setCertA1Path(rs.getString("cert_a1_path"));
                    config.setCertA1Senha(rs.getString("cert_a1_senha"));
                    config.setSerieNfce(rs.getInt("serie_nfce"));
                    config.setNumeroInicialNfce(rs.getInt("numero_inicial_nfce"));
                    config.setAmbiente(rs.getString("ambiente"));
                    config.setRegimeTributario(rs.getString("regime_tributario"));
                    config.setNomeEmpresa(rs.getString("nome_empresa"));
                    config.setCnpj(rs.getString("cnpj"));
                    config.setInscricaoEstadual(rs.getString("inscricao_estadual"));
                    config.setUf(rs.getString("uf"));
                    config.setNomeFantasia(rs.getString("nome_fantasia"));
                    config.setEnderecoLogradouro(rs.getString("endereco_logradouro"));
                    config.setEnderecoNumero(rs.getString("endereco_numero"));
                    config.setEnderecoComplemento(rs.getString("endereco_complemento"));
                    config.setEnderecoBairro(rs.getString("endereco_bairro"));
                    config.setEnderecoMunicipio(rs.getString("endereco_municipio"));
                    config.setEnderecoCep(rs.getString("endereco_cep"));
                    
                    return config;
                }
            }
        }
        
        // Config padrão se não existir
        return new ConfigNfceModel();
    }

    /**
     * Obtém próximo número de NFCe
     */
    private static int obterProximoNumeroNfce(Connection conn, ConfigNfceModel config) throws SQLException {
        String ambiente = normalizarAmbiente(config.getAmbiente());
        int serie = config.getSerieNfce();
        String sql = """
            SELECT ultimo_numero FROM sequencias_fiscais
            WHERE modelo = 'NFCe' AND codigo_modelo = 65 AND serie = ? AND ambiente = ?
            """;
        
        int numeroAtual = 0;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, serie);
            ps.setString(2, ambiente);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    numeroAtual = rs.getInt("ultimo_numero");
                }
            }
        }
        
        // Incrementar e atualizar
        int proximoNumero = numeroAtual + 1;
        String updateSql = """
            UPDATE sequencias_fiscais SET ultimo_numero = ? 
            WHERE modelo = 'NFCe' AND codigo_modelo = 65 AND serie = ? AND ambiente = ?
            """;
        
        try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
            ps.setInt(1, proximoNumero);
            ps.setInt(2, serie);
            ps.setString(3, ambiente);
            ps.executeUpdate();
        }
        
        return proximoNumero;
    }

    private static String normalizarAmbiente(String ambiente) {
        if (ambiente == null) return "HOMOLOG";
        String norm = ambiente.trim().toUpperCase();
        if (norm.startsWith("PROD")) return "PRODUCAO";
        if (norm.startsWith("HOM")) return "HOMOLOG";
        if ("OFF".equals(norm) || "OFFLINE".equals(norm)) return "OFF";
        return "HOMOLOG";
    }

    /**
     * Constrói XML da NFCe
     */
    private static String construirXmlNfce(VendaModel venda, List<VendaItemModel> itens,
                                          ConfigNfceModel config, int numeroNfce) throws Exception {
        
        DocumentoFiscalModel doc = new DocumentoFiscalModel();
        doc.numero = numeroNfce;
        doc.serie = config.getSerieNfce();
        doc.totalProdutos = venda.totalProdutos;
        doc.totalDesconto = venda.totalDesconto;
        doc.totalAcrescimo = venda.totalAcrescimo;
        doc.totalFinal = venda.totalFinal;
        
        List<DocumentoFiscalModel.ItemComImpostos> itensComImpostos = new ArrayList<>();
        for (VendaItemModel item : itens) {
            DocumentoFiscalModel.ItemComImpostos itemComImp = new DocumentoFiscalModel.ItemComImpostos();
            itemComImp.setProdutoId(item.produtoId);
            itemComImp.setDescricao(item.descricao);
            itemComImp.setQuantidade(item.qtd);
            itemComImp.setValorUnit(item.valorUnit);
            itemComImp.setDesconto(item.desconto);
            itemComImp.setAcrescimo(item.acrescimo);
            itemComImp.setTotalItem(item.totalItem);
            itemComImp.setNcm(item.ncm);
            itemComImp.setCfop(item.cfop);
            itemComImp.setCsosn(item.csosn);
            itemComImp.setOrigem(item.origem);
            itemComImp.setUnidade(item.unidade);
            
            itensComImpostos.add(itemComImp);
        }
        
        XmlBuilderNfce builder = new XmlBuilderNfce(doc, config, itensComImpostos);
        return builder.construir();
    }

    /**
     * Calcula chave de acesso (CNJ 65AAMMDDSSNNNNNNNNCDC onde CDC é dígito verificador)
     */
    private static String calcularChaveAcesso(String cnpj, int serie, int numero, String xml) throws Exception {
        String cnpjLimpo = cnpj.replaceAll("\\D", "");
        String dataBusca = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMM"));
        String serieFormatado = String.format("%02d", serie);
        String numeroFormatado = String.format("%09d", numero);
        
        // Calcular DV
        String sequencia = "65" + dataBusca + cnpjLimpo + serieFormatado + numeroFormatado + "00";
        int dv = calcularDVChave(sequencia);
        
        return sequencia.substring(0, 43) + dv;
    }

    /**
     * Calcula dígito verificador da chave
     */
    private static int calcularDVChave(String sequencia) {
        int[] multiplicador = {2, 3, 4, 5, 6, 7, 8, 9, 2, 3, 4, 5, 6, 7, 8, 9, 
                              2, 3, 4, 5, 6, 7, 8, 9, 2, 3, 4, 5, 6, 7, 8, 9, 
                              2, 3, 4, 5, 6, 7, 8, 9, 2, 3, 4};
        
        int soma = 0;
        for (int i = 0; i < sequencia.length(); i++) {
            soma += Integer.parseInt(String.valueOf(sequencia.charAt(i))) * multiplicador[i];
        }
        
        int resto = soma % 11;
        return resto == 0 ? 0 : 11 - resto;
    }

    /**
     * Assina digitalmente o XML (placeholder - implementar com certificado real)
     */
    private static String assinarXml(String xml, String chaveAcesso) throws Exception {
        System.out.println("⚠️ Assinatura digital: usar certificado real em produção");
        
        // Placeholder: retornar XML com tag de assinatura simulada
        String xmlComChave = xml.replace("</infNFe>", 
            "    <!-- Chave de Acesso: " + chaveAcesso + " -->\n  </infNFe>");
        
        return xmlComChave;
    }

    /**
     * Insere documento fiscal no banco
     */
    private static void inserirDocumentoFiscal(Connection conn, String docId, int vendaId, 
                                              int numeroNfce, int serie, String chaveAcesso, 
                                              String xml, VendaModel venda) throws SQLException {
        String sql = """
            INSERT INTO documentos_fiscais 
            (id, venda_id, modelo, serie, numero, ambiente, status, chave_acesso, 
             xml, total_produtos, total_desconto, total_acrescimo, total_final, 
             criado_em, criado_por)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, docId);
            ps.setInt(2, vendaId);
            ps.setString(3, "NFCe");
            ps.setInt(4, serie);
            ps.setInt(5, numeroNfce);
            ps.setString(6, "homologacao");
            ps.setString(7, "pendente");
            ps.setString(8, chaveAcesso);
            ps.setString(9, xml);
            ps.setDouble(10, venda.totalProdutos);
            ps.setDouble(11, venda.totalDesconto);
            ps.setDouble(12, venda.totalAcrescimo);
            ps.setDouble(13, venda.totalFinal);
            ps.setString(14, LocalDateTime.now().format(DATE_FORMAT));
            ps.setString(15, "SISTEMA");
            
            ps.executeUpdate();
        }
    }

    /**
     * Atualiza venda com número de NFCe
     */
    private static void atualizarVendaComNfce(Connection conn, int vendaId, int numeroNfce) throws SQLException {
        String sql = """
            UPDATE vendas SET numero_nfce = ?, status_fiscal = 'emitida', atualizado_em = ?
            WHERE id = ?
            """;
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, numeroNfce);
            ps.setString(2, LocalDateTime.now().format(DATE_FORMAT));
            ps.setInt(3, vendaId);
            
            ps.executeUpdate();
        }
    }

    // ==================== Classes de Modelo Internos ====================
    
    @SuppressWarnings("unused")
    private static class VendaModel {
        int id;
        int clienteId;
        String dataVenda;
        double totalProdutos;
        double totalDesconto;
        double totalAcrescimo;
        double totalFinal;
        String statusFiscal;
    }
    
    @SuppressWarnings("unused")
    private static class VendaItemModel {
        int id;
        int vendaId;
        String produtoId;
        int qtd;
        double valorUnit;
        double desconto;
        double acrescimo;
        double totalItem;
        String ncm;
        String cfop;
        String csosn;
        String origem;
        String unidade;
        String descricao;
    }
}
