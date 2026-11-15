package br.ufmg.dcc603.alarme.monitoramento;

import br.ufmg.dcc603.alarme.processamento.TipoAlarme;
import br.ufmg.dcc603.alarme.processamento.Zona;

/**
 * Sensor especializado na deteccao de calor ou fumaca em uma zona (Figura 3 - Sprint 2).
 */
public class SensorFogo extends Sensor {

    public SensorFogo(String novoId) {
        super(novoId);
    }

    public SensorFogo(String novoId, Zona zonaAssociada) {
        super(novoId, zonaAssociada);
    }

    @Override
    public TipoAlarme obterTipoAlarme() {
        return TipoAlarme.FOGO;
    }
}
