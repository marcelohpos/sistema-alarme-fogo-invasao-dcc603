package br.ufmg.dcc603.alarme.monitoramento;

import br.ufmg.dcc603.alarme.processamento.TipoAlarme;
import br.ufmg.dcc603.alarme.processamento.Zona;

/**
 * Sensor especializado na deteccao de violacao ou intrusao em uma zona (Figura 3 - Sprint 2).
 */
public class SensorInvasao extends Sensor {

    public SensorInvasao(String novoId) {
        super(novoId);
    }

    public SensorInvasao(String novoId, Zona zonaAssociada) {
        super(novoId, zonaAssociada);
    }

    @Override
    public TipoAlarme obterTipoAlarme() {
        return TipoAlarme.INVASAO;
    }
}
