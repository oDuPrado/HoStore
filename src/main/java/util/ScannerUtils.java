// Caminho sugerido: src/util/ScannerUtils.java
package util;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * Utilitário genérico para captura de código de barras via scanner USB (que emula teclado).
 * 
 * <p>Para usar em qualquer cadastro (Booster, Produto, Venda etc.):
 *   <ol>
 *     <li>Chame ScannerUtils.lerCodigoBarras(parentFrame, "Título", callback).</li>
 *     <li>Implemente BarcodeCallback.onCodigoLido(String codigo).</li>
 *   </ol>
 * </p>
 *
 * <p>O scanner “digita” os dígitos no campo de texto e envia ENTER no final.
 * Este util capta cada caractere, acumula num buffer e, ao detectar ENTER,
 * fecha o diálogo e dispara o callback.</p>
 */
public class ScannerUtils {

    /**
     * Abre um diálogo modal para aguardar a leitura de código de barras.
     *
     * <p><b>Como funciona:</b>
     * <ul>
     *   <li>Cria um JDialog bloqueante (modal).</li>
     *   <li>Coloca um JLabel com instruções e um JTextField para receber as “tecladas” do scanner.</li>
     *   <li>Na implementação do KeyListener, cada caractere é acumulado. Quando se detecta ENTER,
     *       fecha-se o diálogo e o callback é chamado com o conteúdo lido.</li>
     *   <li>Seu callback pode então preencher um JLabel, um JTextField (readonly), ou executar
     *       uma busca no banco de dados.</li>
     * </ul>
     *
     * @param owner    Janela pai (ex: this se você estiver num JFrame ou JDialog). Usado para centralizar.
     * @param titulo   Título do diálogo (ex: "Ler Código de Barras").
     * @param callback Implementação de BarcodeCallback que receberá o texto final (sem ENTER).
     */
    public static void lerCodigoBarras(Window owner, String titulo, BarcodeCallback callback) {
    JDialog dialog = new JDialog(owner, titulo, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        // 2) Painel principal com BoxLayout vertical e borda para espaçamento
        JPanel painel = new JPanel();
        painel.setLayout(new BoxLayout(painel, BoxLayout.Y_AXIS));
        painel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 3) Texto de instrução para o usuário
        JLabel lblInstrucao = new JLabel("Aponte o leitor de código de barras e aguarde...");
        lblInstrucao.setAlignmentX(Component.CENTER_ALIGNMENT);
        painel.add(lblInstrucao);

        // 4) Espaçamento entre componentes
        painel.add(Box.createRigidArea(new Dimension(0, 5)));

        // 5) Campo de texto onde o scanner “digita” o código. 
        //    Pode ser deixado visível ou setVisible(false) para ficar oculto.
        JTextField campoLeitura = new JTextField(20);
        campoLeitura.setMaximumSize(new Dimension(Integer.MAX_VALUE, campoLeitura.getPreferredSize().height));
        campoLeitura.setAlignmentX(Component.CENTER_ALIGNMENT);
        painel.add(campoLeitura);

        // 6) Adiciona o painel ao diálogo, faz pack() e centraliza
        dialog.getContentPane().add(painel);
        dialog.pack();
        dialog.setLocationRelativeTo(owner);

        // 7) Listener que captura cada caractere “digitado” pelo scanner
        campoLeitura.addKeyListener(new KeyAdapter() {
            // Buffer para ir acumulando cada caractere
            private final StringBuilder buffer = new StringBuilder();

            @Override
            public void keyTyped(KeyEvent e) {
                char ch = e.getKeyChar();

                // Se for ENTER (scanner normalmente envia ENTER no fim)
                if (ch == KeyEvent.VK_ENTER) {
                    // Monta a string, limpa o buffer e fecha o diálogo
                    String codigo = buffer.toString().trim();
                    buffer.setLength(0); // zera o buffer para a próxima leitura
                    dialog.dispose();

                    // Chama o callback passando o código lido
                    SwingUtilities.invokeLater(() -> callback.onCodigoLido(codigo));
                } else {
                    // Senão, acumula o caractere no buffer
                    buffer.append(ch);
                }
            }
        });

        // 8) Ao abrir o diálogo, pede foco para o campo imediatamente
        SwingUtilities.invokeLater(campoLeitura::requestFocusInWindow);

        // 9) Exibe o diálogo de forma bloqueante até o scanner enviar ENTER
        dialog.setVisible(true);
    }

    /**
     * Interface de callback que receberá o código de barras lido.
     *
     * <p>Exemplo de uso:
     * <pre>
     * ScannerUtils.lerCodigoBarras(this, "Ler Código", codigo -> {
     *     // aqui você pode preencher um JLabel, fazer busca no banco etc.
     *     tfCodigoBarras.setText(codigo);
     * });
     * </pre>
     * </p>
     */
    public interface BarcodeCallback {
        /**
         * Chamado quando o usuário (scanner) enviar ENTER após digitar o código.
         *
         * @param codigo Sequência de caracteres lidos (sem o ENTER).
         */
        void onCodigoLido(String codigo);
    }
}
