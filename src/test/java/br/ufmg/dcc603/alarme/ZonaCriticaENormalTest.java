package br.ufmg.dcc603.alarme;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import br.ufmg.dcc603.alarme.monitoramento.SensorFogo;
import br.ufmg.dcc603.alarme.monitoramento.SensorInvasao;
import br.ufmg.dcc603.alarme.processamento.Alarme;
import br.ufmg.dcc603.alarme.processamento.StatusAlarme;
import br.ufmg.dcc603.alarme.processamento.TipoAlarme;
import br.ufmg.dcc603.alarme.processamento.ZonaCritica;
import br.ufmg.dcc603.alarme.processamento.ZonaNormal;

/**
 * Testes automatizados de polimorfismo entre ZonaCritica e ZonaNormal (RF03, RF04, RF05, RF06, UC03).
 */
public class ZonaCriticaENormalTest {

    @Before
    public void prepararAmbiente() {
        Alarme.reiniciarContadorIds();
    }

    @Test
    public void testZonaCriticaConfirmaAlarmeDeFogoImediatamenteComUmUnicoSensorRF03RF04() {
        // 1) Eu tenho uma ZonaCritica com apenas 1 sensor de fogo ativado.
        ZonaCritica laboratorio = new ZonaCritica("ZC-01", "Laboratorio Quimico");
        SensorFogo sensorUnico = new SensorFogo("SF-LAB-01", laboratorio);
        sensorUnico.ativar();
        Alarme alarmeCritico = new Alarme(TipoAlarme.FOGO, laboratorio, sensorUnico);

        // 2) E eu faco o processamento polimorfico do alarme na zona critica.
        boolean confirmado = laboratorio.processarAlarme(alarmeCritico);

        // 3) Entao eu espero confirmacao imediata sem exigir outros sensores.
        assertTrue("ZonaCritica deve retornar true imediatamente em processarAlarme.", confirmado);
        assertEquals("Status do alarme deve mudar para CONFIRMADO.",
                StatusAlarme.CONFIRMADO, alarmeCritico.getStatus());
        assertTrue("ZonaCritica deve identificar-se como critica.", laboratorio.ehCritica());
    }

    @Test
    public void testZonaCriticaConfirmaAlarmeDeInvasaoImediatamente() {
        // 1) Eu tenho uma ZonaCritica (Cofre/CPD) com 1 sensor de invasao ativado.
        ZonaCritica cofre = new ZonaCritica("ZC-02", "Sala Cofre");
        SensorInvasao sensorInvasao = new SensorInvasao("SI-COF-01", cofre);
        sensorInvasao.ativar();
        Alarme alarmeInvasao = new Alarme(TipoAlarme.INVASAO, cofre, sensorInvasao);

        // 2) E eu faco o processamento na zona critica.
        boolean confirmado = cofre.processarAlarme(alarmeInvasao);

        // 3) Entao eu espero confirmacao imediata da ocorrencia.
        assertTrue("Alarme de invasao em zona critica tambem confirma imediatamente.", confirmado);
        assertEquals("Status deve ser CONFIRMADO.", StatusAlarme.CONFIRMADO, alarmeInvasao.getStatus());
    }

    @Test
    public void testZonaNormalNaoConfirmaComApenasUmSensorAtivoQuandoExigeDoisRF05() {
        // 1) Eu tenho uma ZonaNormal com limiar de 2 sensores e somente 1 sensor ativo.
        ZonaNormal escritorio = new ZonaNormal("ZN-01", "Escritorio Contabil", 2);
        SensorFogo sensor1 = new SensorFogo("SF-ESC-01", escritorio);
        new SensorFogo("SF-ESC-02", escritorio);
        sensor1.ativar();
        Alarme alarmePendente = new Alarme(TipoAlarme.FOGO, escritorio, sensor1);

        // 2) E eu faco o processamento na zona normal.
        boolean confirmado = escritorio.processarAlarme(alarmePendente);

        // 3) Entao eu espero que retorne false e mantenha o alarme PENDENTE para evitar falso positivo.
        assertFalse("Com 1 de 2 sensores ativos, processarAlarme deve retornar false.", confirmado);
        assertEquals("O alarme deve permanecer PENDENTE.", StatusAlarme.PENDENTE, alarmePendente.getStatus());
        assertEquals("contarSensoresAtivos() deve retornar 1.", 1, escritorio.contarSensoresAtivos());
    }

    @Test
    public void testZonaNormalConfirmaAoAtingirQuantidadeNecessariaDeSensoresDoMesmoTipoRF06() {
        // 1) Eu tenho uma ZonaNormal com limiar de 2 sensores e 2 sensores de fogo ativos.
        ZonaNormal biblioteca = new ZonaNormal("ZN-02", "Biblioteca", 2);
        SensorFogo sensor1 = new SensorFogo("SF-BIB-01", biblioteca);
        SensorFogo sensor2 = new SensorFogo("SF-BIB-02", biblioteca);
        sensor1.ativar();
        sensor2.ativar();
        Alarme alarmeFogo = new Alarme(TipoAlarme.FOGO, biblioteca, sensor1);

        // 2) E eu faco o processamento na zona normal.
        boolean confirmado = biblioteca.processarAlarme(alarmeFogo);

        // 3) Entao eu espero confirmacao automatica por multiplos sensores (RF06).
        assertTrue("Com 2 sensores de fogo ativos (limiar = 2), deve confirmar.", confirmado);
        assertEquals("O status do alarme deve ser CONFIRMADO.", StatusAlarme.CONFIRMADO, alarmeFogo.getStatus());
        assertEquals("contarSensoresAtivos() deve contabilizar 2 sensores.", 2, biblioteca.contarSensoresAtivos());
    }

    @Test
    public void testZonaNormalNaoConfirmaFogoUsandoSensorDeInvasaoAtivoUC03() {
        // 1) Eu tenho uma ZonaNormal (limiar = 2) com 1 sensor de FOGO ativo e 1 sensor de INVASAO ativo.
        ZonaNormal saguo = new ZonaNormal("ZN-03", "Saguao", 2);
        SensorFogo sensorFogo = new SensorFogo("SF-SAG-01", saguo);
        SensorInvasao sensorInvasao = new SensorInvasao("SI-SAG-01", saguo);
        sensorFogo.ativar();
        sensorInvasao.ativar();
        Alarme alarmeFogo = new Alarme(TipoAlarme.FOGO, saguo, sensorFogo);

        // 2) E eu faco o processamento do alarme de fogo.
        boolean confirmado = saguo.processarAlarme(alarmeFogo);

        // 3) Entao eu espero que NAO confirme, pois o Caso de Uso 03 exige sensores do MESMO tipo.
        assertEquals("Total geral de sensores ativos na zona e 2.", 2, saguo.contarSensoresAtivos());
        assertEquals("Total de sensores ativos do tipo FOGO e apenas 1.",
                1, saguo.contarSensoresAtivosPorTipo(TipoAlarme.FOGO));
        assertFalse("Sensores de tipos distintos nao devem confirmar alarme cruzado.", confirmado);
        assertEquals("Alarme de fogo deve continuar PENDENTE.", StatusAlarme.PENDENTE, alarmeFogo.getStatus());
    }

    @Test
    public void testZonaNormalComLimiarTresExigeTresSensoresFronteira() {
        // 1) Eu tenho uma ZonaNormal configurada com limiar estrito de 3 sensores ativos.
        ZonaNormal galpao = new ZonaNormal("ZN-04", "Galpao Industrial", 3);
        SensorFogo s1 = new SensorFogo("SF-G1", galpao);
        SensorFogo s2 = new SensorFogo("SF-G2", galpao);
        SensorFogo s3 = new SensorFogo("SF-G3", galpao);
        s1.ativar();
        s2.ativar();
        Alarme alarme = new Alarme(TipoAlarme.FOGO, galpao, s1);

        // 2) E eu faco a verificacao na fronteira com 2 sensores e em seguida com 3 sensores.
        boolean resultadoComDois = galpao.processarAlarme(alarme);
        s3.ativar();
        boolean resultadoComTres = galpao.processarAlarme(alarme);

        // 3) Entao eu espero false para 2 sensores e true exatamente ao atingir 3 sensores.
        assertFalse("Com 2 de 3 sensores ativos, nao deve confirmar.", resultadoComDois);
        assertTrue("Ao ativar o 3o sensor, deve confirmar.", resultadoComTres);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testZonaNormalRejeitaLimiarMenorQueUm() {
        // 1) Eu tenho um limiar invalido igual a zero.
        // 2) E eu faco a construcao da ZonaNormal.
        // 3) Entao eu espero IllegalArgumentException.
        new ZonaNormal("ZN-INV", "Zona Invalida", 0);
    }

    @Test
    public void testZonaNormalIdentificaSeComoNaoCritica() {
        // 1) Eu tenho uma instancia de ZonaNormal.
        ZonaNormal copa = new ZonaNormal("ZN-05", "Copa");

        // 2) E eu faco a consulta de criticidade.
        boolean critica = copa.ehCritica();

        // 3) Entao eu espero false.
        assertFalse("ZonaNormal nao deve ser marcada como critica.", critica);
    }
}
