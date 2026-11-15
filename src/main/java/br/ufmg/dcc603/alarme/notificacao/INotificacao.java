package br.ufmg.dcc603.alarme.notificacao;

import br.ufmg.dcc603.alarme.processamento.Alarme;
import br.ufmg.dcc603.alarme.processamento.Zona;

/**
 * Interface arquitetural do componente de Notificacao/Emergencia (Figura 2 - Sprint 2).
 */
public interface INotificacao {

    void acionar(Zona zonaAlvo, Alarme alarmeAcionado);
}
