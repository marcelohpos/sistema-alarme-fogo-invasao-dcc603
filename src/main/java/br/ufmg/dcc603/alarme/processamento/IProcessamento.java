package br.ufmg.dcc603.alarme.processamento;

import java.util.List;

import br.ufmg.dcc603.alarme.monitoramento.Sensor;

/**
 * Interface arquitetural do componente de Processamento de Alarmes (Figura 2 - Sprint 2).
 */
public interface IProcessamento {

    void adicionarZona(Zona novaZona);

    void receberAtivacao(Sensor sensorAcionado);

    void acionarServicoEmergencia(Alarme alarmeConfirmado);

    List<Zona> getZonas();

    List<Alarme> getAlarmes();
}
