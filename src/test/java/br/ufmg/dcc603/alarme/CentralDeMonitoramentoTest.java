package br.ufmg.dcc603.alarme;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import br.ufmg.dcc603.alarme.auditoria.RegistroDeEventos;
import br.ufmg.dcc603.alarme.monitoramento.SensorFogo;
import br.ufmg.dcc603.alarme.monitoramento.SensorInvasao;
import br.ufmg.dcc603.alarme.notificacao.ServicoEmergenciaSimulado;
import br.ufmg.dcc603.alarme.processamento.Alarme;
import br.ufmg.dcc603.alarme.processamento.CentralDeMonitoramento;
import br.ufmg.dcc603.alarme.processamento.StatusAlarme;
import br.ufmg.dcc603.alarme.processamento.ZonaCritica;
import br.ufmg.dcc603.alarme.processamento.ZonaNormal;

/**
 * Testes de integracao do fluxo completo na CentralDeMonitoramento (Figura 4 - Sequencia, RF01-RF06, RF15, RNF06).
 */
public class CentralDeMonitoramentoTest {

    private ServicoEmergenciaSimulado servicoSimulado;
    private RegistroDeEventos registroEventos;
    private CentralDeMonitoramento central;

    @Before
    public void prepararCentral() {
        Alarme.reiniciarContadorIds();
        servicoSimulado = new ServicoEmergenciaSimulado();
        registroEventos = new RegistroDeEventos();
        central = new CentralDeMonitoramento(servicoSimulado, registroEventos);
    }

    @Test
    public void testFluxoZonaCriticaAcionaBombeirosImediatamenteERegistraTresEventosRF04() {
        // 1) Eu tenho uma ZonaCritica cadastrada na central com 1 sensor de fogo.
        ZonaCritica cpd = new ZonaCritica("ZC-CPD", "CPD Principal");
        SensorFogo sensorFogo = new SensorFogo("SF-CPD-01", cpd);
        central.adicionarZona(cpd);

        // 2) E eu faco a ativacao desse unico sensor e notifico a central.
        sensorFogo.ativar();
        central.receberAtivacao(sensorFogo);

        // 3) Entao eu espero alarme CONFIRMADO, Bombeiros acionados 1 vez e 3 eventos no log (ativacao, confirmacao, emergencia).
        Alarme alarme = central.obterUltimoAlarme();
        assertNotNull("A central deve ter criado um alarme.", alarme);
        assertEquals("Alarme em zona critica deve ficar CONFIRMADO.", StatusAlarme.CONFIRMADO, alarme.getStatus());
        assertEquals("O servico de emergencia deve ter sido acionado 1 vez.",
                1, servicoSimulado.obterQuantidadeAcionamentos());
        assertTrue("A notificacao deve referenciar o Corpo de Bombeiros.",
                servicoSimulado.listarNotificacoes().get(0).contains("Bombeiros"));
        assertEquals("Devem existir 3 eventos auditaveis registrados.", 3, registroEventos.obterTotalEventos());
    }

    @Test
    public void testFluxoSequencialZonaNormalPrimeiroSensorPendenteSegundoSensorConfirmaEAcionaRF05RF06() {
        // 1) Eu tenho uma ZonaNormal (limiar = 2) com dois sensores de fogo instalados.
        ZonaNormal recepcao = new ZonaNormal("ZN-REC", "Recepcao", 2);
        SensorFogo sf1 = new SensorFogo("SF-REC-01", recepcao);
        SensorFogo sf2 = new SensorFogo("SF-REC-02", recepcao);
        central.adicionarZona(recepcao);

        // 2) E eu faco primeiro o disparo de sf1, verifico pendencia, e depois disparo sf2.
        sf1.ativar();
        central.receberAtivacao(sf1);
        StatusAlarme statusAposPrimeiro = central.obterUltimoAlarme().getStatus();
        int chamadosAposPrimeiro = servicoSimulado.obterQuantidadeAcionamentos();

        sf2.ativar();
        central.receberAtivacao(sf2);

        // 3) Entao eu espero que o mesmo alarme tenha evoluido de PENDENTE para CONFIRMADO com 2 sensores de origem.
        assertEquals("Apos apenas 1 sensor, status deve ser PENDENTE.", StatusAlarme.PENDENTE, statusAposPrimeiro);
        assertEquals("Apos apenas 1 sensor, emergencia NAO deve ser acionada.", 0, chamadosAposPrimeiro);
        assertEquals("Nao deve duplicar o alarme na mesma zona.", 1, central.getAlarmes().size());

        Alarme alarmeFinal = central.obterUltimoAlarme();
        assertEquals("Apos o 2o sensor, o alarme deve estar CONFIRMADO.",
                StatusAlarme.CONFIRMADO, alarmeFinal.getStatus());
        assertEquals("O alarme confirmado deve listar os 2 sensores de origem.",
                2, alarmeFinal.getSensoresOrigem().size());
        assertEquals("Emergencia deve ter sido acionada exatamente 1 vez.",
                1, servicoSimulado.obterQuantidadeAcionamentos());
    }

    @Test
    public void testAlarmeDeInvasaoConfirmadoAcionaPoliciaEmVezDeBombeiros() {
        // 1) Eu tenho uma ZonaNormal com 2 sensores de invasao.
        ZonaNormal tesouraria = new ZonaNormal("ZN-TES", "Tesouraria", 2);
        SensorInvasao si1 = new SensorInvasao("SI-TES-01", tesouraria);
        SensorInvasao si2 = new SensorInvasao("SI-TES-02", tesouraria);
        central.adicionarZona(tesouraria);

        // 2) E eu faco o disparo de ambos os sensores de invasao.
        si1.ativar();
        central.receberAtivacao(si1);
        si2.ativar();
        central.receberAtivacao(si2);

        // 3) Entao eu espero que o chamado externo seja direcionado para Policia / Seguranca Patrimonial.
        assertEquals("Deve haver 1 acionamento externo.", 1, servicoSimulado.obterQuantidadeAcionamentos());
        assertTrue("Chamado de invasao deve acionar Policia (190).",
                servicoSimulado.listarNotificacoes().get(0).contains("Policia"));
    }

    @Test
    public void testEscalabilidadeAdicaoDinamicaDeMultiplasZonasESensoresRNF06() {
        // 1) Eu tenho uma central inicialmente vazia.
        assertTrue("Central inicia sem zonas.", central.getZonas().isEmpty());

        // 2) E eu faco a adicao dinamica de 5 novas zonas (normais e criticas) com sensores sem alterar codigo estrutural.
        for (int indice = 1; indice <= 4; indice++) {
            ZonaNormal novaZona = new ZonaNormal("ZN-" + indice, "Andar " + indice, 2);
            novaZona.adicionarSensor(new SensorFogo("SF-A" + indice));
            central.adicionarZona(novaZona);
        }
        ZonaCritica subestacao = new ZonaCritica("ZC-SUB", "Subestacao Eletrica");
        SensorFogo sensorSub = new SensorFogo("SF-SUB-01", subestacao);
        central.adicionarZona(subestacao);

        sensorSub.ativar();
        central.receberAtivacao(sensorSub);

        // 3) Entao eu espero todas as 5 zonas monitoradas e acionamento correto na nova zona critica.
        assertEquals("A central deve gerenciar as 5 zonas adicionadas.", 5, central.getZonas().size());
        assertTrue("O servico deve ter sido acionado para o alarme da subestacao.",
                servicoSimulado.foiAcionadoPara(central.obterUltimoAlarme()));
    }

    @Test
    public void testAlarmesSimultaneosDeFogoEInvasaoNaMesmaZonaSaoTratadosIndependentemente() {
        // 1) Eu tenho uma ZonaNormal com sensores de fogo e de invasao.
        ZonaNormal arquivo = new ZonaNormal("ZN-ARQ", "Arquivo Geral", 2);
        SensorFogo sf1 = new SensorFogo("SF-ARQ-01", arquivo);
        SensorInvasao si1 = new SensorInvasao("SI-ARQ-01", arquivo);
        central.adicionarZona(arquivo);

        // 2) E eu faco a ativacao de 1 sensor de fogo e 1 sensor de invasao na mesma zona.
        sf1.ativar();
        central.receberAtivacao(sf1);
        si1.ativar();
        central.receberAtivacao(si1);

        // 3) Entao eu espero 2 alarmes distintos na central, ambos ainda PENDENTES e sem acionamento indevido.
        assertEquals("Devem existir 2 alarmes separados (um FOGO e um INVASAO).", 2, central.getAlarmes().size());
        assertFalse("Nenhum chamado externo deve ocorrer sem confirmacao.",
                servicoSimulado.obterQuantidadeAcionamentos() > 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCentralRejeitaServicoEmergenciaNulo() {
        // 1) Eu tenho um RegistroDeEventos valido, mas ServicoEmergencia nulo.
        // 2) E eu faco a construcao da CentralDeMonitoramento.
        // 3) Entao eu espero IllegalArgumentException.
        new CentralDeMonitoramento(null, new RegistroDeEventos());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCentralRejeitaSensorNuloEmReceberAtivacao() {
        // 1) Eu tenho uma central valida.
        // 2) E eu faco receberAtivacao(null).
        // 3) Entao eu espero IllegalArgumentException.
        central.receberAtivacao(null);
    }
}
