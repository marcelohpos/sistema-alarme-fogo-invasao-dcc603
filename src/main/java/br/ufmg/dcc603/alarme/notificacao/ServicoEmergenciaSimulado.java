package br.ufmg.dcc603.alarme.notificacao;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import br.ufmg.dcc603.alarme.processamento.Alarme;
import br.ufmg.dcc603.alarme.processamento.TipoAlarme;
import br.ufmg.dcc603.alarme.processamento.Zona;

/**
 * Implementacao simulada do servico externo de emergencia para validacao e testes JUnit (Figura 3 - Sprint 2).
 */
public class ServicoEmergenciaSimulado implements ServicoEmergencia {

    public static final String UNIDADE_BOMBEIROS = "Corpo de Bombeiros (193)";
    public static final String UNIDADE_POLICIA = "Policia / Seguranca Patrimonial (190)";
    public static final String FORMATO_HORARIO = "HH:mm:ss";

    private final List<String> notificacoesEnviadas;
    private final List<Alarme> alarmesAcionados;

    public ServicoEmergenciaSimulado() {
        notificacoesEnviadas = new ArrayList<String>();
        alarmesAcionados = new ArrayList<Alarme>();
    }

    @Override
    public void acionar(Zona zonaAlvo, Alarme alarmeAcionado) {
        // Validar argumentos obrigatorios antes de expedir o chamado externo.
        if (zonaAlvo == null) {
            throw new IllegalArgumentException("A zona informada ao servico de emergencia nao pode ser nula.");
        }
        if (alarmeAcionado == null) {
            throw new IllegalArgumentException("O alarme informado ao servico de emergencia nao pode ser nulo.");
        }

        String unidadeDestino = selecionarUnidadeEmergencia(alarmeAcionado.getTipo());
        SimpleDateFormat formatador = new SimpleDateFormat(FORMATO_HORARIO);
        String horarioChamado = formatador.format(new Date());
        String protocolo = "[" + horarioChamado + "] Chamado enviado para " + unidadeDestino
                + " | Alarme " + alarmeAcionado.getId()
                + " (" + alarmeAcionado.getTipo() + ") na zona '"
                + zonaAlvo.getNome() + "' (" + zonaAlvo.getId() + ")";

        notificacoesEnviadas.add(protocolo);
        alarmesAcionados.add(alarmeAcionado);
    }

    private String selecionarUnidadeEmergencia(TipoAlarme tipoAlarme) {
        String unidade = "";

        // Selecionar a corporacao correspondente ao tipo do alarme.
        switch (tipoAlarme) {
            case FOGO:
                unidade = UNIDADE_BOMBEIROS;
                break;
            case INVASAO:
                unidade = UNIDADE_POLICIA;
                break;
            default:
                throw new IllegalStateException("Tipo de alarme nao suportado: " + tipoAlarme);
        }
        return unidade;
    }

    public List<String> listarNotificacoes() {
        return Collections.unmodifiableList(notificacoesEnviadas);
    }

    public List<Alarme> listarAlarmesAcionados() {
        return Collections.unmodifiableList(alarmesAcionados);
    }

    public int obterQuantidadeAcionamentos() {
        return alarmesAcionados.size();
    }

    public boolean foiAcionadoPara(Alarme alarmeConsulta) {
        return alarmesAcionados.contains(alarmeConsulta);
    }
}
