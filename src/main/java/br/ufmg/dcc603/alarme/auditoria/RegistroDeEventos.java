package br.ufmg.dcc603.alarme.auditoria;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Mantem a trilha imutavel de eventos disparados, confirmacoes, acionamentos e acoes do operador (RF15, RNF05).
 */
public class RegistroDeEventos implements IRegistro {

    private final List<Evento> eventos;

    public RegistroDeEventos() {
        eventos = new ArrayList<Evento>();
    }

    @Override
    public void registrar(Evento novoEvento) {
        // Rejeitar eventos nulos para preservar a integridade do log de auditoria.
        if (novoEvento == null) {
            throw new IllegalArgumentException("Nao e permitido registrar um evento nulo.");
        }
        eventos.add(novoEvento);
    }

    @Override
    public List<Evento> listar() {
        return Collections.unmodifiableList(eventos);
    }

    public List<Evento> filtrarPorZona(String nomeZona) {
        List<Evento> filtrados = new ArrayList<Evento>();
        if (nomeZona == null) {
            return Collections.unmodifiableList(filtrados);
        }

        // Selecionar apenas eventos pertencentes a zona informada.
        for (Evento item : eventos) {
            if (item.getZona().equalsIgnoreCase(nomeZona.trim())) {
                filtrados.add(item);
            }
        }
        return Collections.unmodifiableList(filtrados);
    }

    public int obterTotalEventos() {
        return eventos.size();
    }
}
