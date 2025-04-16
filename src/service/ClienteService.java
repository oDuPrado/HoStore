package service;

import dao.ClienteDAO;
import model.ClienteModel;
import com.opencsv.*;
import com.google.gson.Gson;

import java.io.*;
import java.util.List;
import java.util.UUID;

public class ClienteService {

    private static final ClienteDAO dao = new ClienteDAO();
    private static final Gson GSON = new Gson();

    public static List<ClienteModel> loadAll()                { return dao.findAll(); }
    public static void upsert(ClienteModel m)                 { dao.upsert(m); }
    public static void deleteById(String id)                  { dao.delete(id); }
    public static boolean cpfDuplicado(String cpf,String id)  { return dao.cpfExists(cpf,id); }

    /* ---------- EXPORT ---------- */
    public static void exportCsv(File f) throws Exception {
        try (CSVWriter w = new CSVWriter(new FileWriter(f))) {
            w.writeNext(new String[]{"id","nome","telefone","cpf","data_nasc","tipo",
                                     "endereco","cidade","estado","observacoes"});
            for (ClienteModel c : loadAll()) {
                w.writeNext(new String[]{
                    c.getId(), c.getNome(), c.getTelefone(), c.getCpf(), c.getDataNasc(),
                    c.getTipo(), c.getEndereco(), c.getCidade(), c.getEstado(), c.getObservacoes()
                });
            }
        }
    }
    public static void exportJson(File f) throws Exception {
        try(FileWriter w = new FileWriter(f)){
            GSON.toJson(loadAll(), w);
        }
    }

    /* ---------- IMPORT ---------- */
    public static int importCsv(File f) throws Exception {
        int count = 0;
        try (CSVReader r = new CSVReader(new FileReader(f))) {
            String[] row;
            r.readNext(); // cabeçalho
            while ((row = r.readNext()) != null) {
                ClienteModel c = new ClienteModel();
                c.setId(row[0].isEmpty()? geraId(row[5]) : row[0]);
                c.setNome(row[1]);
                c.setTelefone(row[2]);
                c.setCpf(row[3]);
                c.setDataNasc(row[4]);
                c.setTipo(row[5]);
                c.setEndereco(row[6]);
                c.setCidade(row[7]);
                c.setEstado(row[8]);
                c.setObservacoes(row[9]);
                if (!cpfDuplicado(c.getCpf(), c.getId())) {
                    upsert(c);
                    count++;
                }
            }
        }
        return count;
    }
    private static String geraId(String tipo) {
        char p;
        if (tipo == null) tipo = "";
    
        switch (tipo.toUpperCase()) {
            case "JOGADOR":
                p = 'J';
                break;
            case "AMBOS":
                p = 'A';
                break;
            default:
                p = 'C';
        }
    
        return p + "-" + UUID.randomUUID().toString().substring(0, 5);
    }
    public static int importJson(File f) throws Exception {
        try (Reader reader = new FileReader(f)) {
            ClienteModel[] clientes = GSON.fromJson(reader, ClienteModel[].class);
            int count = 0;
            for (ClienteModel c : clientes) {
                if (!cpfDuplicado(c.getCpf(), c.getId())) {
                    upsert(c);
                    count++;
                }
            }
            return count;
        }
    }
        
}
