package br.ufmg.dcc603.alarme.processamento;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import br.ufmg.dcc603.alarme.auditoria.Evento;
import br.ufmg.dcc603.alarme.auditoria.IRegistro;
import br.ufmg.dcc603.alarme.auditoria.RegistroDeEventos;
import br.ufmg.dcc603.alarme.monitoramento.Sensor;
import br.ufmg.dcc603.alarme.notificacao.ServicoEmergencia;
import br.ufmg.dcc603.alarme.notificacao.ServicoEmergenciaSimulado;
import br.ufmg.dcc603.alarme.operador.Operador;

/**
 * Nucleo central de recepcao de sinais de sensores, confirmacao polimorfica, escalonamento e auditoria (Figuras 3 e 4).
 */
public class CentralDeMonitoramento implements IProcessamento {

    public static final String TIPO_EVENTO_ATIVACAO = "ATIVACAO_SENSOR";
    public static final String TIPO_EVENTO_CONFIRMACAO = "CONFIRMACAO_ALARME";
    public static final String TIPO_EVENTO_EMERGENCIA = "ACIONAMENTO_EMERGENCIA";
    public static final String TIPO_EVENTO_TRANSMISSAO_OPERADOR = "TRANSMISSAO_MANUAL_OPERADOR";
    public static final String TIPO_EVENTO_RESPOSTA_OPERADOR = "RESPOSTA_PESSOAL_OPERADOR";
    public static final String TIPO_EVENTO_DESCARTE = "DESCARTE_ALARME_FALSO";
    public static final String TIPO_EVENTO_TIMEOUT = "EXPIRACAO_TEMPO_LIMITE";
    public static final String TIPO_EVENTO_ENCERRAMENTO = "ENCERRAMENTO_ALARME";

    private final List<Zona> zonas;
    private final List<Alarme> alarmes;
    private final ServicoEmergencia servicoEmergencia;
    private final IRegistro registroDeEventos;

    public CentralDeMonitoramento(ServicoEmergencia servicoExterno, IRegistro repositorioEventos) {
        if (servicoExterno == null) {
            throw new IllegalArgumentException("O servico de emergencia nao pode ser nulo.");
        }
        if (repositorioEventos == null) {
            throw new IllegalArgumentException("O registro de eventos nao pode ser nulo.");
        }
        zonas = new ArrayList<Zona>();
        alarmes = new ArrayList<Alarme>();
        servicoEmergencia = servicoExterno;
        registroDeEventos = repositorioEventos;
    }

    public CentralDeMonitoramento() {
        this(new ServicoEmergenciaSimulado(), new RegistroDeEventos());
    }

    @Override
    public void adicionarZona(Zona novaZona) {
        if (novaZona == null) {
            throw new IllegalArgumentException("Nao e permitido adicionar uma zona nula na central.");
        }
        if (!zonas.contains(novaZona)) {
            zonas.add(novaZona);
        }
    }

    @Override
    public void receberAtivacao(Sensor sensorAcionado) {
        if (sensorAcionado == null) {
            throw new IllegalArgumentException("O sensor informado na ativacao nao pode ser nulo.");
        }

        // Fluxo alternativo 2a do Caso de Uso 01: ignorar sinal caso o sensor esteja INATIVO.
        if (!sensorAcionado.estaAtivo()) {
            return;
        }

        Zona zonaOrigem = sensorAcionado.getZona();
        if (zonaOrigem == null) {
            throw new IllegalStateException("O sensor ativado deve pertencer a uma zona cadastrada.");
        }
        if (!zonas.contains(zonaOrigem)) {
            zonas.add(zonaOrigem);
        }

        // Verificar se ja existe alarme PENDENTE do mesmo tipo na mesma zona ou criar um novo alarme.
        Alarme alarmeCorrente = localizarAlarmePendente(zonaOrigem, sensorAcionado.obterTipoAlarme());
        if (alarmeCorrente == null) {
            alarmeCorrente = new Alarme(sensorAcionado.obterTipoAlarme(), zonaOrigem, sensorAcionado);
            alarmes.add(alarmeCorrente);
        } else {
            alarmeCorrente.adicionarSensorOrigem(sensorAcionado);
        }

        // Registrar imediatamente a ativacao do sensor conforme RF02 e Caso de Uso 02.
        String descricaoAtivacao = "Sensor " + sensorAcionado.getId() + " detectou "
                + sensorAcionado.obterTipoAlarme() + " (Alarme " + alarmeCorrente.getId() + ").";
        registroDeEventos.registrar(new Evento(
                TIPO_EVENTO_ATIVACAO,
                zonaOrigem.getNome(),
                sensorAcionado.getId(),
                descricaoAtivacao));

        // Delegar polimorficamente para a zona a decisao de confirmacao imediata ou por multiplos sensores.
        boolean confirmadoAgora = zonaOrigem.processarAlarme(alarmeCorrente);
        if (confirmadoAgora) {
            registrarConfirmacaoAutomatica(zonaOrigem, alarmeCorrente);
            acionarServicoEmergencia(alarmeCorrente);
        }
    }

    private Alarme localizarAlarmePendente(Zona zonaBusca, TipoAlarme tipoBusca) {
        Alarme encontrado = null;

        // Buscar um alarme ainda PENDENTE na mesma zona e para o mesmo tipo de ocorrencia.
        for (Alarme item : alarmes) {
            if (item.getZona() == zonaBusca) {
                if (item.getTipo() == tipoBusca) {
                    if (item.getStatus() == StatusAlarme.PENDENTE) {
                        encontrado = item;
                        break;
                    }
                }
            }
        }
        return encontrado;
    }

    private void registrarConfirmacaoAutomatica(Zona zonaOrigem, Alarme alarmeConfirmado) {
        String motivoConfirmacao = "Confirmado por multiplos sensores ativos na zona normal.";
        if (zonaOrigem.ehCritica()) {
            motivoConfirmacao = "Confirmacao imediata obrigatoria em ZONA CRITICA (RF03/RF04).";
        }
        registroDeEventos.registrar(new Evento(
                TIPO_EVENTO_CONFIRMACAO,
                zonaOrigem.getNome(),
                "CentralDeMonitoramento",
                "Alarme " + alarmeConfirmado.getId() + " confirmado. " + motivoConfirmacao));
    }

    @Override
    public void acionarServicoEmergencia(Alarme alarmeConfirmado) {
        if (alarmeConfirmado == null) {
            throw new IllegalArgumentException("Nao e possivel acionar emergencia para alarme nulo.");
        }

        Zona zonaAlvo = alarmeConfirmado.getZona();
        servicoEmergencia.acionar(zonaAlvo, alarmeConfirmado);

        // Registrar o acionamento do servico externo de emergencia conforme Caso de Uso 04 e RF15.
        registroDeEventos.registrar(new Evento(
                TIPO_EVENTO_EMERGENCIA,
                zonaAlvo.getNome(),
                "CentralDeMonitoramento",
                "Servico de emergencia acionado para o alarme " + alarmeConfirmado.getId()
                        + " (" + alarmeConfirmado.getTipo() + ")."));
    }

    public void transmitirAlarmeManual(Operador operadorAtuante, Alarme alarmeAlvo) {
        validarOperadorEAlarme(operadorAtuante, alarmeAlvo);

        // Pre-condicao do Caso de Uso 05: o alarme deve estar PENDENTE ou CONFIRMADO.
        StatusAlarme statusAtual = alarmeAlvo.getStatus();
        if (statusAtual == StatusAlarme.FALSO || statusAtual == StatusAlarme.ENCERRADO) {
            throw new IllegalStateException("Nao e possivel transmitir manualmente um alarme FALSO ou ENCERRADO.");
        }

        alarmeAlvo.setOperadorResponsavel(operadorAtuante);
        if (statusAtual == StatusAlarme.PENDENTE) {
            alarmeAlvo.confirmar();
        }

        registroDeEventos.registrar(new Evento(
                TIPO_EVENTO_TRANSMISSAO_OPERADOR,
                alarmeAlvo.getZona().getNome(),
                "Operador " + operadorAtuante.getNome(),
                "Operador transmitiu manualmente o alarme " + alarmeAlvo.getId() + " ao servico de emergencia."));

        acionarServicoEmergencia(alarmeAlvo);
    }

    public void registrarRespostaPessoal(Operador operadorAtuante, Alarme alarmeAlvo) {
        validarOperadorEAlarme(operadorAtuante, alarmeAlvo);
        if (alarmeAlvo.getStatus() == StatusAlarme.ENCERRADO) {
            throw new IllegalStateException("Nao e possivel responder a um alarme ja ENCERRADO.");
        }

        alarmeAlvo.setOperadorResponsavel(operadorAtuante);

        // Se o alarme estava PENDENTE em zona normal, o atendimento presencial dispensa acionamento externo (RF07/UC06).
        if (alarmeAlvo.getStatus() == StatusAlarme.PENDENTE) {
            alarmeAlvo.descartar();
        }

        registroDeEventos.registrar(new Evento(
                TIPO_EVENTO_RESPOSTA_OPERADOR,
                alarmeAlvo.getZona().getNome(),
                "Operador " + operadorAtuante.getNome(),
                "Operador respondeu pessoalmente ao alarme " + alarmeAlvo.getId()
                        + " (status resultante: " + alarmeAlvo.getStatus() + ")."));
    }

    public void descartarAlarmeManual(Operador operadorAtuante, Alarme alarmeAlvo) {
        validarOperadorEAlarme(operadorAtuante, alarmeAlvo);
        alarmeAlvo.setOperadorResponsavel(operadorAtuante);
        alarmeAlvo.descartar();

        registroDeEventos.registrar(new Evento(
                TIPO_EVENTO_DESCARTE,
                alarmeAlvo.getZona().getNome(),
                "Operador " + operadorAtuante.getNome(),
                "Alarme " + alarmeAlvo.getId() + " descartado como FALSO pelo operador."));
    }

    public void encerrarAlarme(Operador operadorAtuante, Alarme alarmeAlvo) {
        validarOperadorEAlarme(operadorAtuante, alarmeAlvo);
        alarmeAlvo.setOperadorResponsavel(operadorAtuante);
        alarmeAlvo.encerrar();

        registroDeEventos.registrar(new Evento(
                TIPO_EVENTO_ENCERRAMENTO,
                alarmeAlvo.getZona().getNome(),
                "Operador " + operadorAtuante.getNome(),
                "Ocorrencia do alarme " + alarmeAlvo.getId() + " encerrada na central."));
    }

    public int verificarTimeouts(long tempoLimiteMs, Date instanteReferencia) {
        if (tempoLimiteMs < 0) {
            throw new IllegalArgumentException("O tempo-limite nao pode ser negativo.");
        }
        if (instanteReferencia == null) {
            throw new IllegalArgumentException("O instante de referencia nao pode ser nulo.");
        }

        int totalExpirados = 0;

        // Transicao after(T) da Figura 5: alarmes pendentes sem confirmacao apos T mudam para FALSO.
        for (Alarme item : alarmes) {
            if (item.getStatus() == StatusAlarme.PENDENTE) {
                long decorrido = instanteReferencia.getTime() - item.getTimestamp().getTime();
                if (decorrido >= tempoLimiteMs) {
                    item.descartar();
                    registroDeEventos.registrar(new Evento(
                            instanteReferencia,
                            TIPO_EVENTO_TIMEOUT,
                            item.getZona().getNome(),
                            "CentralDeMonitoramento",
                            "Alarme " + item.getId() + " marcado como FALSO por tempo-limite sem confirmacao."));
                    totalExpirados++;
                }
            }
        }
        return totalExpirados;
    }

    private void validarOperadorEAlarme(Operador operadorAtuante, Alarme alarmeAlvo) {
        if (operadorAtuante == null) {
            throw new IllegalArgumentException("O operador responsavel nao pode ser nulo.");
        }
        if (alarmeAlvo == null) {
            throw new IllegalArgumentException("O alarme alvo da operacao nao pode ser nulo.");
        }
    }

    @Override
    public List<Zona> getZonas() {
        return Collections.unmodifiableList(zonas);
    }

    @Override
    public List<Alarme> getAlarmes() {
        return Collections.unmodifiableList(alarmes);
    }

    public Alarme obterUltimoAlarme() {
        if (alarmes.isEmpty()) {
            return null;
        }
        return alarmes.get(alarmes.size() - 1);
    }

    public ServicoEmergencia getServicoEmergencia() {
        return servicoEmergencia;
    }

    public IRegistro getRegistroDeEventos() {
        return registroDeEventos;
    }
}
