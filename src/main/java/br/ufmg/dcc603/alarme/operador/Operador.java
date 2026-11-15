package br.ufmg.dcc603.alarme.operador;

import br.ufmg.dcc603.alarme.processamento.Alarme;
import br.ufmg.dcc603.alarme.processamento.CentralDeMonitoramento;

/**
 * Representa o operador humano autenticado na area central de controle (Figura 3 - Sprint 2, RF07).
 */
public class Operador {

    private String id;
    private String nome;
    private CentralDeMonitoramento central;

    public Operador(String novoId, String novoNome, CentralDeMonitoramento centralOperada) {
        if (novoId == null || novoId.trim().isEmpty()) {
            throw new IllegalArgumentException("O identificador do operador nao pode ser nulo ou vazio.");
        }
        if (novoNome == null || novoNome.trim().isEmpty()) {
            throw new IllegalArgumentException("O nome do operador nao pode ser nulo ou vazio.");
        }
        if (centralOperada == null) {
            throw new IllegalArgumentException("A central de monitoramento operada nao pode ser nula.");
        }
        id = novoId.trim();
        nome = novoNome.trim();
        central = centralOperada;
    }

    public void responderAlarme(Alarme alarmeAlvo) {
        central.registrarRespostaPessoal(this, alarmeAlvo);
    }

    public void transmitirAlarme(Alarme alarmeAlvo) {
        central.transmitirAlarmeManual(this, alarmeAlvo);
    }

    public void descartarAlarme(Alarme alarmeAlvo) {
        central.descartarAlarmeManual(this, alarmeAlvo);
    }

    public void encerrarAlarme(Alarme alarmeAlvo) {
        central.encerrarAlarme(this, alarmeAlvo);
    }

    public String getId() {
        return id;
    }

    public void setId(String novoId) {
        if (novoId == null || novoId.trim().isEmpty()) {
            throw new IllegalArgumentException("O identificador do operador nao pode ser nulo ou vazio.");
        }
        id = novoId.trim();
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String novoNome) {
        if (novoNome == null || novoNome.trim().isEmpty()) {
            throw new IllegalArgumentException("O nome do operador nao pode ser nulo ou vazio.");
        }
        nome = novoNome.trim();
    }

    public CentralDeMonitoramento getCentral() {
        return central;
    }

    public void setCentral(CentralDeMonitoramento novaCentral) {
        if (novaCentral == null) {
            throw new IllegalArgumentException("A central de monitoramento operada nao pode ser nula.");
        }
        central = novaCentral;
    }

    @Override
    public String toString() {
        return nome + " (" + id + ")";
    }
}
