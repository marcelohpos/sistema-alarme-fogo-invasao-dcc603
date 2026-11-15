package br.ufmg.dcc603.alarme.auditoria;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Representa um registro imutavel de evento relevante do sistema para auditoria (RF02, RF15, RNF05).
 */
public class Evento {

    public static final String FORMATO_PADRAO_DATA = "yyyy-MM-dd HH:mm:ss";

    private final Date timestamp;
    private final String tipo;
    private final String zona;
    private final String origem;
    private final String descricao;

    public Evento(Date instanteEvento, String tipoEvento, String nomeZona, String origemEvento, String descricaoEvento) {
        // Garantir validade dos dados obrigatorios do evento auditavel.
        if (instanteEvento == null) {
            throw new IllegalArgumentException("O timestamp do evento nao pode ser nulo.");
        }
        if (tipoEvento == null || tipoEvento.trim().isEmpty()) {
            throw new IllegalArgumentException("O tipo do evento nao pode ser nulo ou vazio.");
        }
        if (nomeZona == null || nomeZona.trim().isEmpty()) {
            throw new IllegalArgumentException("A zona do evento nao pode ser nula ou vazia.");
        }
        if (origemEvento == null || origemEvento.trim().isEmpty()) {
            throw new IllegalArgumentException("A origem do evento nao pode ser nula ou vazia.");
        }
        if (descricaoEvento == null || descricaoEvento.trim().isEmpty()) {
            throw new IllegalArgumentException("A descricao do evento nao pode ser nula ou vazia.");
        }

        timestamp = new Date(instanteEvento.getTime());
        tipo = tipoEvento.trim();
        zona = nomeZona.trim();
        origem = origemEvento.trim();
        descricao = descricaoEvento.trim();
    }

    public Evento(String tipoEvento, String nomeZona, String origemEvento, String descricaoEvento) {
        this(new Date(), tipoEvento, nomeZona, origemEvento, descricaoEvento);
    }

    public Date getTimestamp() {
        return new Date(timestamp.getTime());
    }

    public String getTipo() {
        return tipo;
    }

    public String getZona() {
        return zona;
    }

    public String getOrigem() {
        return origem;
    }

    public String getDescricao() {
        return descricao;
    }

    public String formatarHorario() {
        SimpleDateFormat formatador = new SimpleDateFormat(FORMATO_PADRAO_DATA);
        return formatador.format(timestamp);
    }

    @Override
    public String toString() {
        return "[" + formatarHorario() + "] [" + tipo + "] Zona: " + zona
                + " | Origem: " + origem + " | " + descricao;
    }
}
