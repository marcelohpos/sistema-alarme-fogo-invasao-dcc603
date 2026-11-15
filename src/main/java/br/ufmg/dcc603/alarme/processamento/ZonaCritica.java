package br.ufmg.dcc603.alarme.processamento;

/**
 * Zona de maior risco/prioridade cujo alarme confirma imediatamente sem exigir multiplos sensores (RF03, RF04).
 */
public class ZonaCritica extends Zona {

    public ZonaCritica(String novoId, String novoNome) {
        super(novoId, novoNome);
    }

    @Override
    public boolean processarAlarme(Alarme alarmeAvaliado) {
        if (alarmeAvaliado == null) {
            throw new IllegalArgumentException("O alarme avaliado na zona critica nao pode ser nulo.");
        }

        // Em zona critica, todo alarme e confirmado imediatamente com prioridade maxima.
        alarmeAvaliado.confirmar();
        return true;
    }

    @Override
    public boolean ehCritica() {
        return true;
    }
}
