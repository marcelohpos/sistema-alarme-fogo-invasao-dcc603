package br.ufmg.dcc603.alarme.monitoramento;

import br.ufmg.dcc603.alarme.processamento.TipoAlarme;
import br.ufmg.dcc603.alarme.processamento.Zona;

/**
 * Classe abstrata base para sensores fisicos monitorados em uma zona do edificio (Figura 3 - Sprint 2, RF01).
 */
public abstract class Sensor implements IMonitoramento {

    private String id;
    private EstadoSensor estado;
    private Zona zona;

    protected Sensor(String novoId) {
        // Validar o identificador obrigatorio do sensor.
        if (novoId == null || novoId.trim().isEmpty()) {
            throw new IllegalArgumentException("O identificador do sensor nao pode ser nulo ou vazio.");
        }
        id = novoId.trim();
        estado = EstadoSensor.INATIVO;
        zona = null;
    }

    protected Sensor(String novoId, Zona zonaAssociada) {
        this(novoId);
        if (zonaAssociada != null) {
            zonaAssociada.adicionarSensor(this);
        }
    }

    @Override
    public void ativar() {
        estado = EstadoSensor.ATIVO;
    }

    @Override
    public void desativar() {
        estado = EstadoSensor.INATIVO;
    }

    @Override
    public boolean estaAtivo() {
        return estado == EstadoSensor.ATIVO;
    }

    @Override
    public Zona getZona() {
        return zona;
    }

    public void setZona(Zona novaZona) {
        zona = novaZona;
    }

    public String getId() {
        return id;
    }

    public void setId(String novoId) {
        if (novoId == null || novoId.trim().isEmpty()) {
            throw new IllegalArgumentException("O identificador do sensor nao pode ser nulo ou vazio.");
        }
        id = novoId.trim();
    }

    public EstadoSensor getEstado() {
        return estado;
    }

    public void setEstado(EstadoSensor novoEstado) {
        if (novoEstado == null) {
            throw new IllegalArgumentException("O estado do sensor nao pode ser nulo.");
        }
        estado = novoEstado;
    }

    @Override
    public abstract TipoAlarme obterTipoAlarme();

    @Override
    public String toString() {
        return id + " (" + obterTipoAlarme() + " - " + estado + ")";
    }
}
