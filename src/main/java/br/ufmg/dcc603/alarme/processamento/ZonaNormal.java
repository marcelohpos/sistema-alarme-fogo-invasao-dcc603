package br.ufmg.dcc603.alarme.processamento;

import br.ufmg.dcc603.alarme.monitoramento.Sensor;

/**
 * Zona padrao que exige confirmacao por multiplos sensores ativos para mitigar falsos positivos (RF05, RF06, UC03).
 */
public class ZonaNormal extends Zona {

    public static final int PADRAO_SENSORES_NECESSARIOS = 2;

    private int sensoresNecessarios;

    public ZonaNormal(String novoId, String novoNome, int minimoSensores) {
        super(novoId, novoNome);
        if (minimoSensores < 1) {
            throw new IllegalArgumentException("A quantidade minima de sensores necessarios deve ser ao menos 1.");
        }
        sensoresNecessarios = minimoSensores;
    }

    public ZonaNormal(String novoId, String novoNome) {
        this(novoId, novoNome, PADRAO_SENSORES_NECESSARIOS);
    }

    @Override
    public boolean processarAlarme(Alarme alarmeAvaliado) {
        if (alarmeAvaliado == null) {
            throw new IllegalArgumentException("O alarme avaliado na zona normal nao pode ser nulo.");
        }

        // Contar sensores ativos do mesmo tipo na zona para decidir se o limiar de confirmacao foi atingido.
        int ativosMesmoTipo = contarSensoresAtivosPorTipo(alarmeAvaliado.getTipo());
        if (ativosMesmoTipo >= sensoresNecessarios) {
            alarmeAvaliado.confirmar();
            return true;
        }
        return false;
    }

    public int contarSensoresAtivos() {
        int contador = 0;

        // Percorrer todos os sensores cadastrados na zona somando os que estao ativos.
        for (Sensor item : sensores) {
            if (item.estaAtivo()) {
                contador++;
            }
        }
        return contador;
    }

    public int contarSensoresAtivosPorTipo(TipoAlarme tipoProcurado) {
        int contador = 0;
        if (tipoProcurado == null) {
            return contador;
        }

        // Somar apenas sensores ativos que correspondam ao mesmo tipo do alarme em avaliacao (UC03 passo 2).
        for (Sensor item : sensores) {
            if (item.estaAtivo()) {
                if (item.obterTipoAlarme() == tipoProcurado) {
                    contador++;
                }
            }
        }
        return contador;
    }

    public int getSensoresNecessarios() {
        return sensoresNecessarios;
    }

    public void setSensoresNecessarios(int novoMinimo) {
        if (novoMinimo < 1) {
            throw new IllegalArgumentException("A quantidade minima de sensores necessarios deve ser ao menos 1.");
        }
        sensoresNecessarios = novoMinimo;
    }

    @Override
    public boolean ehCritica() {
        return false;
    }
}
