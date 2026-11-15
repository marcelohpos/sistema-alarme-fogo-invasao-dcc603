package br.ufmg.dcc603.alarme;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import br.ufmg.dcc603.alarme.auditoria.Evento;
import br.ufmg.dcc603.alarme.auditoria.RegistroDeEventos;
import br.ufmg.dcc603.alarme.monitoramento.SensorFogo;
import br.ufmg.dcc603.alarme.monitoramento.SensorInvasao;
import br.ufmg.dcc603.alarme.notificacao.ServicoEmergenciaSimulado;
import br.ufmg.dcc603.alarme.operador.Operador;
import br.ufmg.dcc603.alarme.processamento.Alarme;
import br.ufmg.dcc603.alarme.processamento.CentralDeMonitoramento;
import br.ufmg.dcc603.alarme.processamento.StatusAlarme;
import br.ufmg.dcc603.alarme.processamento.ZonaCritica;
import br.ufmg.dcc603.alarme.processamento.ZonaNormal;

/**
 * Testes automatizados para as acoes manuais do Operador na Area Central (RF07, UC05, UC06).
 */
public class OperadorTest {

    private ServicoEmergenciaSimulado servicoSimulado;
    private RegistroDeEventos registroEventos;
    private CentralDeMonitoramento central;
    private Operador operador;

    @Before
    public void prepararAmbiente() {
        Alarme.reiniciarContadorIds();
        servicoSimulado = new ServicoEmergenciaSimulado();
        registroEventos = new RegistroDeEventos();
        central = new CentralDeMonitoramento(servicoSimulado, registroEventos);
        operador = new Operador("OP-01", "Marcelo Henrique", central);
    }

    @Test
    public void testOperadorTransmiteManualmenteAlarmePendenteAoServicoDeEmergenciaRF07UC05() {
        // 1) Eu tenho um alarme PENDENTE gerado por apenas 1 sensor em uma ZonaNormal.
        ZonaNormal estacionamento = new ZonaNormal("ZN-EST", "Estacionamento", 2);
        SensorInvasao sensor = new SensorInvasao("SI-EST-01", estacionamento);
        sensor.ativar();
        central.receberAtivacao(sensor);
        Alarme alarmePendente = central.obterUltimoAlarme();

        // 2) E eu faco a transmissao manual pelo operador humano.
        operador.transmitirAlarme(alarmePendente);

        // 3) Entao eu espero que o alarme passe a CONFIRMADO, vincule o operador e acione a Policia.
        assertEquals("Alarme transmitido pelo operador deve ficar CONFIRMADO.",
                StatusAlarme.CONFIRMADO, alarmePendente.getStatus());
        assertSame("O operador deve ficar registrado como responsavel pelo alarme.",
                operador, alarmePendente.getOperadorResponsavel());
        assertEquals("O servico externo deve ter sido acionado 1 vez.",
                1, servicoSimulado.obterQuantidadeAcionamentos());
    }

    @Test
    public void testOperadorRespondePessoalmenteSemAcionarServicoExternoRF07() {
        // 1) Eu tenho um alarme PENDENTE em zona normal verificado no local pelo operador.
        ZonaNormal copa = new ZonaNormal("ZN-COPA", "Copa 1o Andar", 2);
        SensorFogo sensorFumacaLeve = new SensorFogo("SF-COP-01", copa);
        sensorFumacaLeve.ativar();
        central.receberAtivacao(sensorFumacaLeve);
        Alarme alarmeLocal = central.obterUltimoAlarme();

        // 2) E eu faco o registro de resposta pessoal pelo operador.
        operador.responderAlarme(alarmeLocal);

        // 3) Entao eu espero status FALSO (sem escalonamento externo), 0 chamados de emergencia e evento auditado.
        assertEquals("Atendimento local dispensa emergencia externa mudando PENDENTE para FALSO.",
                StatusAlarme.FALSO, alarmeLocal.getStatus());
        assertSame("Operador deve constar como responsavel.", operador, alarmeLocal.getOperadorResponsavel());
        assertEquals("Nenhum chamado externo deve ser enviado.", 0, servicoSimulado.obterQuantidadeAcionamentos());

        Evento ultimoEvento = registroEventos.listar().get(registroEventos.obterTotalEventos() - 1);
        assertEquals("Tipo do evento deve registrar a resposta pessoal do operador.",
                CentralDeMonitoramento.TIPO_EVENTO_RESPOSTA_OPERADOR, ultimoEvento.getTipo());
    }

    @Test
    public void testOperadorDescartaAlarmeFalsoPositivoUC06() {
        // 1) Eu tenho um alarme PENDENTE identificado como falso positivo no painel.
        ZonaNormal corredor = new ZonaNormal("ZN-COR", "Corredor Sul", 2);
        SensorInvasao sensor = new SensorInvasao("SI-COR-01", corredor);
        sensor.ativar();
        central.receberAtivacao(sensor);
        Alarme alarmeFalso = central.obterUltimoAlarme();

        // 2) E eu faco o descarte manual pelo operador.
        operador.descartarAlarme(alarmeFalso);

        // 3) Entao eu espero status FALSO, zero acionamentos de emergencia e registro de auditoria.
        assertEquals("Alarme descartado deve assumir status FALSO.", StatusAlarme.FALSO, alarmeFalso.getStatus());
        assertEquals("Emergencia nao pode ser acionada em alarme descartado.",
                0, servicoSimulado.obterQuantidadeAcionamentos());
        assertTrue("Log deve conter evento DESCARTE_ALARME_FALSO.",
                registroEventos.listar().get(1).getTipo().equals(CentralDeMonitoramento.TIPO_EVENTO_DESCARTE));
    }

    @Test(expected = IllegalStateException.class)
    public void testOperadorNaoPodeDescartarAlarmeDeZonaCriticaJaConfirmadoRF03() {
        // 1) Eu tenho um alarme em ZonaCritica (que ja nasce CONFIRMADO e nao pode ser ignorado segundo RF03).
        ZonaCritica gerador = new ZonaCritica("ZC-GER", "Sala do Gerador");
        SensorFogo sensorCritico = new SensorFogo("SF-GER-01", gerador);
        sensorCritico.ativar();
        central.receberAtivacao(sensorCritico);
        Alarme alarmeCritico = central.obterUltimoAlarme();

        // 2) E eu faco a tentativa de descartar esse alarme critico como falso.
        // 3) Entao eu espero IllegalStateException bloqueando o descarte de alarme confirmado.
        operador.descartarAlarme(alarmeCritico);
    }

    @Test(expected = IllegalStateException.class)
    public void testOperadorNaoPodeTransmitirAlarmeJaDescartadoComoFalso() {
        // 1) Eu tenho um alarme que ja foi descartado como FALSO pelo operador.
        ZonaNormal recepcao = new ZonaNormal("ZN-REC", "Recepcao", 2);
        SensorFogo sensor = new SensorFogo("SF-REC-01", recepcao);
        sensor.ativar();
        central.receberAtivacao(sensor);
        Alarme alarme = central.obterUltimoAlarme();
        operador.descartarAlarme(alarme);

        // 2) E eu faco a tentativa de transmiti-lo manualmente depois de descartado.
        // 3) Entao eu espero IllegalStateException (viola pre-condicao de UC05).
        operador.transmitirAlarme(alarme);
    }

    @Test
    public void testOperadorEncerraOcorrenciaAposAtendimentoConfirmadoFigura5() {
        // 1) Eu tenho um alarme CONFIRMADO cujo servico de emergencia ja foi acionado.
        ZonaCritica servidor = new ZonaCritica("ZC-SRV", "Rack Central");
        SensorFogo sensor = new SensorFogo("SF-SRV-01", servidor);
        sensor.ativar();
        central.receberAtivacao(sensor);
        Alarme alarmeAtendido = central.obterUltimoAlarme();

        // 2) E eu faco o encerramento formal da ocorrencia pelo operador.
        operador.encerrarAlarme(alarmeAtendido);

        // 3) Entao eu espero que o alarme atinja o estado final ENCERRADO do Diagrama de Estados.
        assertEquals("O alarme deve transitar para ENCERRADO.", StatusAlarme.ENCERRADO, alarmeAtendido.getStatus());
    }
}
