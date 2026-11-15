package br.ufmg.dcc603.alarme.app;

import java.io.File;

import javax.swing.SwingUtilities;

import br.ufmg.dcc603.alarme.operador.PainelOperadorSwing;
import br.ufmg.dcc603.alarme.operador.RoteiroDemonstracao;

/**
 * Classe separada contendo exclusivamente o metodo main() de iniciacao do sistema (Idiomas #2 e #3).
 */
public class SistemaAlarmeApp {

    public static void main(String[] args) {
        RoteiroDemonstracao roteiro = new RoteiroDemonstracao();

        // Suportar execucao de roteiro em console (--demo) ou exportacao de captura (--screenshot <arquivo>).
        if (args != null && args.length > 0) {
            if ("--demo".equalsIgnoreCase(args[0]) || "--console".equalsIgnoreCase(args[0])) {
                roteiro.executarCenarioGuiado();
                roteiro.imprimirRelatorioConsole();
                return;
            }
            if ("--screenshot".equalsIgnoreCase(args[0]) && args.length > 1) {
                try {
                    SwingUtilities.invokeAndWait(
                            () -> PainelOperadorSwing.exportarCapturaDemonstracao(roteiro, new File(args[1])));
                } catch (Exception erro) {
                    throw new RuntimeException("Falha ao capturar tela: " + erro.getMessage(), erro);
                }
                return;
            }
        }

        // Iniciar o painel visual da Area Central na thread de eventos do Swing.
        SwingUtilities.invokeLater(() -> {
            PainelOperadorSwing painel = new PainelOperadorSwing(roteiro);
            painel.exibir();
        });
    }
}
