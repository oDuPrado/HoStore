// src/main/java/service/FiscalCatalogService.java
package service;

import dao.FiscalCatalogDAO;
import model.CodigoDescricaoModel;
import util.DB;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class FiscalCatalogService {

    public enum CatalogType {
        NCM("ncm", "NCM",
                "Novo / Editar NCM", "Código (8 dígitos):",
                "Ex: 95049090 (será exibido como 9504.90.90)"),
        CFOP("cfop", "CFOP",
                "Novo / Editar CFOP", "Código (4 dígitos):",
                "Ex: 5102"),
        CSOSN("csosn", "CSOSN",
                "Novo / Editar CSOSN", "Código (3 dígitos):",
                "Ex: 102"),
        ORIGEM("origem", "Origem",
                "Novo / Editar Origem", "Código (1 dígito):",
                "Ex: 0, 1, 2, 3, 8"),
        UNIDADES("unidades", "Unidades",
                "Novo / Editar Unidade", "Código (1 a 6):",
                "Ex: UN, KG, CX, PCT");

        private final String tableName;
        private final String name;
        private final String title;
        private final String codeLabel;
        private final String hint;

        CatalogType(String tableName, String name, String title, String codeLabel, String hint) {
            this.tableName = tableName;
            this.name = name;
            this.title = title;
            this.codeLabel = codeLabel;
            this.hint = hint;
        }

        public String getTableName() { return tableName; }
        public String getName() { return name; }
        public String getTitle() { return title; }
        public String getCodeLabel() { return codeLabel; }
        public String getHint() { return hint; }

        public String validate(String codigo, String descricao) {
            if (codigo == null) codigo = "";
            if (descricao == null) descricao = "";

            codigo = codigo.trim();
            descricao = descricao.trim();

            if (codigo.isEmpty()) return "Código é obrigatório.";
            if (descricao.isEmpty()) return "Descrição é obrigatória.";

            switch (this) {
                case NCM:
                    if (!codigo.matches("\\d{8}")) return "NCM deve ter exatamente 8 dígitos.";
                    break;
                case CFOP:
                    if (!codigo.matches("\\d{4}")) return "CFOP deve ter exatamente 4 dígitos.";
                    break;
                case CSOSN:
                    if (!codigo.matches("\\d{3}")) return "CSOSN deve ter exatamente 3 dígitos.";
                    break;
                case ORIGEM:
                    if (!codigo.matches("\\d{1}")) return "Origem deve ter 1 dígito (0-9).";
                    break;
                case UNIDADES:
                    if (!codigo.matches("[A-Z0-9]{1,6}")) return "Unidade deve ter 1 a 6 caracteres (A-Z / 0-9).";
                    break;
            }
            return null;
        }
    }

    public static class SaveResult {
        public final List<String> deletedOk = new ArrayList<>();
        public final List<String> deletedFailed = new ArrayList<>();
    }

    private static final FiscalCatalogService INSTANCE = new FiscalCatalogService();

    public static FiscalCatalogService getInstance() {
        return INSTANCE;
    }

    private final FiscalCatalogDAO dao = new FiscalCatalogDAO();

    private FiscalCatalogService() {}

    public List<CodigoDescricaoModel> findAll(CatalogType type) {
        try {
            return dao.findAll(type.getTableName());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Salva (UPSERT) tudo que está na UI.
     * Também tenta remover códigos que existiam no banco e não estão mais na UI,
     * mas se estiverem referenciados (FK), a remoção falha e a gente só registra.
     */
    public SaveResult saveAll(CatalogType type, List<CodigoDescricaoModel> newList) {
        // valida de novo aqui (UI valida, mas service não confia em humano)
        for (CodigoDescricaoModel it : newList) {
            String err = type.validate(it.getCodigo(), it.getDescricao());
            if (err != null) throw new RuntimeException(err + " (código=" + it.getCodigo() + ")");
        }

        SaveResult result = new SaveResult();

        try (Connection c = DB.get()) {
            c.setAutoCommit(false);
            try {
                // snapshot do que existe
                List<CodigoDescricaoModel> old = dao.findAll(type.getTableName());

                Set<String> newCodes = newList.stream()
                        .map(x -> x.getCodigo().trim())
                        .collect(Collectors.toSet());

                Set<String> oldCodes = old.stream()
                        .map(x -> x.getCodigo().trim())
                        .collect(Collectors.toSet());

                // 1) upsert todos os atuais
                dao.upsertAll(c, type.getTableName(), newList);

                // 2) tenta deletar os removidos (se FK impedir, registra e segue)
                for (String code : oldCodes) {
                    if (!newCodes.contains(code)) {
                        try {
                            boolean ok = dao.deleteByCodigo(c, type.getTableName(), code);
                            if (ok) result.deletedOk.add(code);
                        } catch (SQLException fkFail) {
                            result.deletedFailed.add(code);
                        }
                    }
                }

                c.commit();
                return result;

            } catch (Exception e) {
                c.rollback();
                throw e;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
