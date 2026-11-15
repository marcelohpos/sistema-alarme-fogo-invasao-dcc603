package br.ufmg.dcc603.alarme.processamento;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import br.ufmg.dcc603.alarme.monitoramento.Sensor;

/**
 * Subdivisao fisica do edificio que agrupa sensores e define a politica polimorfica de confirmacao (Figura 3 - Sprint 2).
 */
public abstract class Zona {

    private String id;
    private String nome;
    protected final List<Sensor> sensores;

    protected Zona(String novoId, String novoNome) {
        // Validar identificacao obrigatoria da zona monitorada.
        if (novoId == null || novoId.trim().isEmpty()) {
            throw new IllegalArgumentException("O identificador da zona nao pode ser nulo ou vazio.");
        }
        if (novoNome == null || novoNome.trim().isEmpty()) {
            throw new IllegalArgumentException("O nome da zona nao pode ser nulo ou vazio.");
        }
        id = novoId.trim();
        nome = novoNome.trim();
        sensores = new ArrayList<Sensor>();
    }

    public void adicionarSensor(Sensor novoSensor) {
        if (novoSensor == null) {
            throw new IllegalArgumentException("Nao e permitido adicionar um sensor nulo a zona.");
        }
        if (!sensores.contains(novoSensor)) {
            sensores.add(novoSensor);
        }
        if (novoSensor.getZona() != this) {
            novoSensor.setZona(this);
        }
    }

    public abstract boolean processarAlarme(Alarme alarmeAvaliado);

    public abstract boolean ehCritica();

    public String getId() {
        return id;
    }

    public void setId(String novoId) {
        if (novoId == null || novoId.trim().isEmpty()) {
            throw new IllegalArgumentException("O identificador da zona nao pode ser nulo ou vazio.");
        }
        id = novoId.trim();
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String novoNome) {
        if (novoNome == null || novoNome.trim().isEmpty()) {
            throw new IllegalArgumentException("O nome da zona nao pode ser nulo ou vazio.");
        }
        nome = novoNome.trim();
    }

    public List<Sensor> getSensores() {
        return Collections.unmodifiableList(sensores);
    }

    @Override
    public String toString() {
        String nivel = "NORMAL";
        if (ehCritica()) {
            nivel = "CRITICA";
        }
        return nome + " (" + id + " - " + nivel + ")";
    }
}
