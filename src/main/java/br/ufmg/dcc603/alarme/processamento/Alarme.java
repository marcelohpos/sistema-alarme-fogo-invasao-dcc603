package br.ufmg.dcc603.alarme.processamento;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import br.ufmg.dcc603.alarme.monitoramento.Sensor;
import br.ufmg.dcc603.alarme.operador.Operador;

/**
 * Representa um alarme gerado a partir da ativacao de sensores em uma zona (Figuras 3, 4 e 5 - Sprint 2).
 */
public class Alarme {

    public static final String PREFIXO_PADRAO_ID = "ALM-";
    private static final AtomicInteger SEQUENCIAL_ID = new AtomicInteger(1);

    private String id;
    private TipoAlarme tipo;
    private StatusAlarme status;
    private Date timestamp;
    private Zona zona;
    private final List<Sensor> sensoresOrigem;
    private Operador operadorResponsavel;

    public Alarme(String novoId, TipoAlarme tipoAlarme, Zona zonaOcorrencia, Sensor sensorInicial, Date instanteCriacao) {
        // Validar parametros obrigatorios de criacao do alarme.
        if (novoId == null || novoId.trim().isEmpty()) {
            throw new IllegalArgumentException("O identificador do alarme nao pode ser nulo ou vazio.");
        }
        if (tipoAlarme == null) {
            throw new IllegalArgumentException("O tipo do alarme nao pode ser nulo.");
        }
        if (zonaOcorrencia == null) {
            throw new IllegalArgumentException("A zona do alarme nao pode ser nula.");
        }
        if (sensorInicial == null) {
            throw new IllegalArgumentException("O alarme deve possuir ao menos um sensor de origem.");
        }
        if (instanteCriacao == null) {
            throw new IllegalArgumentException("O timestamp do alarme nao pode ser nulo.");
        }

        id = novoId.trim();
        tipo = tipoAlarme;
        status = StatusAlarme.PENDENTE;
        timestamp = new Date(instanteCriacao.getTime());
        zona = zonaOcorrencia;
        sensoresOrigem = new ArrayList<Sensor>();
        sensoresOrigem.add(sensorInicial);
        operadorResponsavel = null;
    }

    public Alarme(String novoId, TipoAlarme tipoAlarme, Zona zonaOcorrencia, Sensor sensorInicial) {
        this(novoId, tipoAlarme, zonaOcorrencia, sensorInicial, new Date());
    }

    public Alarme(TipoAlarme tipoAlarme, Zona zonaOcorrencia, Sensor sensorInicial) {
        this(gerarProximoId(), tipoAlarme, zonaOcorrencia, sensorInicial, new Date());
    }

    private static String gerarProximoId() {
        int numeroAtual = SEQUENCIAL_ID.getAndIncrement();
        return String.format("%s%03d", PREFIXO_PADRAO_ID, numeroAtual);
    }

    public static void reiniciarContadorIds() {
        SEQUENCIAL_ID.set(1);
    }

    public void confirmar() {
        // Permitir confirmacao apenas a partir de PENDENTE (ou manter idempotencia em CONFIRMADO).
        if (status == StatusAlarme.FALSO) {
            throw new IllegalStateException("Nao e possivel confirmar um alarme ja descartado como FALSO.");
        }
        if (status == StatusAlarme.ENCERRADO) {
            throw new IllegalStateException("Nao e possivel confirmar um alarme ja ENCERRADO.");
        }
        status = StatusAlarme.CONFIRMADO;
    }

    public void descartar() {
        // Impedir descarte de alarmes ja confirmados ou encerrados (RF03, UC06, Figura 5).
        if (status == StatusAlarme.CONFIRMADO) {
            throw new IllegalStateException("Nao e possivel descartar como falso um alarme ja CONFIRMADO.");
        }
        if (status == StatusAlarme.ENCERRADO) {
            throw new IllegalStateException("Nao e possivel descartar um alarme ja ENCERRADO.");
        }
        status = StatusAlarme.FALSO;
    }

    public void encerrar() {
        // Conforme o Diagrama de Estados (Figura 5), apenas alarmes CONFIRMADO ou FALSO podem ser encerrados.
        if (status == StatusAlarme.PENDENTE) {
            throw new IllegalStateException("Um alarme PENDENTE deve ser confirmado ou descartado antes de ser encerrado.");
        }
        status = StatusAlarme.ENCERRADO;
    }

    public void adicionarSensorOrigem(Sensor novoSensor) {
        if (novoSensor == null) {
            throw new IllegalArgumentException("O sensor de origem adicional nao pode ser nulo.");
        }
        if (!sensoresOrigem.contains(novoSensor)) {
            sensoresOrigem.add(novoSensor);
        }
    }

    public String getId() {
        return id;
    }

    public TipoAlarme getTipo() {
        return tipo;
    }

    public StatusAlarme getStatus() {
        return status;
    }

    public Date getTimestamp() {
        return new Date(timestamp.getTime());
    }

    public Zona getZona() {
        return zona;
    }

    public List<Sensor> getSensoresOrigem() {
        return Collections.unmodifiableList(sensoresOrigem);
    }

    public Operador getOperadorResponsavel() {
        return operadorResponsavel;
    }

    public void setOperadorResponsavel(Operador novoOperador) {
        operadorResponsavel = novoOperador;
    }

    @Override
    public String toString() {
        return id + " [" + tipo + " | " + status + " | Zona: " + zona.getNome() + "]";
    }
}
