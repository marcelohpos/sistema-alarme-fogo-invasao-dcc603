package br.ufmg.dcc603.alarme;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import br.ufmg.dcc603.alarme.monitoramento.EstadoSensor;
import br.ufmg.dcc603.alarme.monitoramento.SensorFogo;
import br.ufmg.dcc603.alarme.monitoramento.SensorInvasao;
import br.ufmg.dcc603.alarme.processamento.Alarme;
import br.ufmg.dcc603.alarme.processamento.CentralDeMonitoramento;
import br.ufmg.dcc603.alarme.processamento.StatusAlarme;
import br.ufmg.dcc603.alarme.processamento.TipoAlarme;
import br.ufmg.dcc603.alarme.processamento.ZonaNormal;

/**
 * Testes automatizados para os sensores de fogo/invasao, associacao com zonas e redundancia (RF01, UC01, RNF03).
 */
public class SensorEMonitoramentoTest {

    @Before
    public void prepararAmbiente() {
        Alarme.reiniciarContadorIds();
    }

    @Test
    public void testSensorIniciaInativoEMudaParaAtivoAoAtivar() {
        // 1) Eu tenho um sensor de fogo recem-instalado em uma zona normal.
        ZonaNormal recepcao = new ZonaNormal("ZN-01", "Recepcao");
        SensorFogo sensorFogo = new SensorFogo("SF-01", recepcao);

        // 2) E eu faco a ativacao do sensor apos verificar seu estado inicial.
        EstadoSensor estadoInicial = sensorFogo.getEstado();
        sensorFogo.ativar();

        // 3) Entao eu espero que ele tenha iniciado INATIVO e mudado para ATIVO.
        assertEquals("O sensor recem-criado deve iniciar INATIVO.", EstadoSensor.INATIVO, estadoInicial);
        assertTrue("O metodo estaAtivo() deve retornar true apos ativar().", sensorFogo.estaAtivo());
        assertEquals("O sensor de fogo deve reportar TipoAlarme.FOGO.", TipoAlarme.FOGO, sensorFogo.obterTipoAlarme());
    }

    @Test
    public void testSensorInvasaoReportaTipoCorretoEPermiteDesativar() {
        // 1) Eu tenho um sensor de invasao previamente ativado.
        ZonaNormal almoxarifado = new ZonaNormal("ZN-02", "Almoxarifado");
        SensorInvasao sensorInvasao = new SensorInvasao("SI-01", almoxarifado);
        sensorInvasao.ativar();

        // 2) E eu faco a desativacao (reset) do sensor.
        sensorInvasao.desativar();

        // 3) Entao eu espero que o tipo seja INVASAO e o sensor volte a ficar INATIVO.
        assertEquals("SensorInvasao deve retornar TipoAlarme.INVASAO.",
                TipoAlarme.INVASAO, sensorInvasao.obterTipoAlarme());
        assertFalse("O sensor deve voltar a ficar inativo apos desativar().", sensorInvasao.estaAtivo());
    }

    @Test
    public void testAssociacaoBidirecionalEntreZonaESensor() {
        // 1) Eu tenho uma zona normal e um sensor criado sem zona inicial.
        ZonaNormal corredor = new ZonaNormal("ZN-03", "Corredor Principal");
        SensorFogo sensorCorredor = new SensorFogo("SF-COR-01");

        // 2) E eu faco o cadastro do sensor na zona.
        corredor.adicionarSensor(sensorCorredor);

        // 3) Entao eu espero que a zona possua o sensor e o sensor referencie a mesma zona.
        assertSame("getZona() no sensor deve apontar para a zona onde foi cadastrado.",
                corredor, sensorCorredor.getZona());
        assertEquals("A zona deve conter exatamente 1 sensor cadastrado.", 1, corredor.getSensores().size());
    }

    @Test
    public void testSinalDeSensorInativoEhIgnoradoPelaCentralFluxoAlternativoUC01() {
        // 1) Eu tenho uma central de monitoramento e um sensor que permanece com estado INATIVO.
        CentralDeMonitoramento central = new CentralDeMonitoramento();
        ZonaNormal salaReuniao = new ZonaNormal("ZN-04", "Sala de Reuniao");
        SensorFogo sensorInativo = new SensorFogo("SF-INAT-01", salaReuniao);
        central.adicionarZona(salaReuniao);

        // 2) E eu faco o envio de ativacao para a central sem o sensor estar ativo (Fluxo Alternativo 2a - UC01).
        central.receberAtivacao(sensorInativo);

        // 3) Entao eu espero que nenhum alarme seja criado e nenhum evento seja registrado.
        assertTrue("Nenhum alarme deve ser iniciado por sensor INATIVO.", central.getAlarmes().isEmpty());
        assertNull("O ultimo alarme da central deve permanecer nulo.", central.obterUltimoAlarme());
        assertEquals("Nenhum evento deve ser gravado para sensor inativo.",
                0, central.getRegistroDeEventos().listar().size());
    }

    @Test
    public void testRedundanciaEntreSensoresMesmaZonaQuandoUmFalhaRNF03() {
        // 1) Eu tenho uma zona com 3 sensores de fogo (limiar = 2), em que o sensor 1 falha (fica INATIVO).
        CentralDeMonitoramento central = new CentralDeMonitoramento();
        ZonaNormal garagem = new ZonaNormal("ZN-05", "Garagem Subsolo", 2);
        SensorFogo sensorFalho = new SensorFogo("SF-GAR-01", garagem);
        SensorFogo sensorOperanteA = new SensorFogo("SF-GAR-02", garagem);
        SensorFogo sensorOperanteB = new SensorFogo("SF-GAR-03", garagem);
        central.adicionarZona(garagem);

        // 2) E eu faco a ativacao dos outros dois sensores redundantes (mantendo o primeiro inativo).
        central.receberAtivacao(sensorFalho);
        sensorOperanteA.ativar();
        central.receberAtivacao(sensorOperanteA);
        sensorOperanteB.ativar();
        central.receberAtivacao(sensorOperanteB);

        // 3) Entao eu espero que a falha individual de SF-GAR-01 nao impeca a confirmacao do incendio real (RNF03).
        Alarme alarmeGerado = central.obterUltimoAlarme();
        assertEquals("O alarme deve ser CONFIRMADO pelos 2 sensores operantes.",
                StatusAlarme.CONFIRMADO, alarmeGerado.getStatus());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRejeitaSensorComIdentificadorVazio() {
        // 1) Eu tenho um identificador em branco invalido.
        // 2) E eu faco a tentativa de instanciar um SensorFogo vazio.
        // 3) Entao eu espero IllegalArgumentException.
        new SensorFogo("   ");
    }

    @Test(expected = IllegalStateException.class)
    public void testRejeitaAtivacaoDeSensorSemZonaAssociada() {
        // 1) Eu tenho um sensor ativo que nao foi vinculado a nenhuma zona do edificio.
        CentralDeMonitoramento central = new CentralDeMonitoramento();
        SensorFogo sensorOrfao = new SensorFogo("SF-ORFAO");
        sensorOrfao.ativar();

        // 2) E eu faco o envio da ativacao para a central.
        // 3) Entao eu espero IllegalStateException exigindo zona cadastrada.
        central.receberAtivacao(sensorOrfao);
    }
}
