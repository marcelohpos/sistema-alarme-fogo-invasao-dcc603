package br.ufmg.dcc603.alarme.notificacao;

import br.ufmg.dcc603.alarme.processamento.Alarme;
import br.ufmg.dcc603.alarme.processamento.Zona;

/**
 * Contrato de comunicacao com servicos externos de emergencia (Figura 3 - Sprint 2, RNF07).
 */
public interface ServicoEmergencia extends INotificacao {

    @Override
    void acionar(Zona zonaAlvo, Alarme alarmeAcionado);
}
