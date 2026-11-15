package br.ufmg.dcc603.alarme.monitoramento;

import br.ufmg.dcc603.alarme.processamento.TipoAlarme;
import br.ufmg.dcc603.alarme.processamento.Zona;

/**
 * Interface arquitetural do componente de Monitoramento de Sensores (Figura 2 - Sprint 2).
 */
public interface IMonitoramento {

    void ativar();

    void desativar();

    boolean estaAtivo();

    Zona getZona();

    TipoAlarme obterTipoAlarme();
}
