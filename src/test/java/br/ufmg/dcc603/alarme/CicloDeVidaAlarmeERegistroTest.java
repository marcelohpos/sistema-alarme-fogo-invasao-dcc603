package br.ufmg.dcc603.alarme;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import br.ufmg.dcc603.alarme.auditoria.Evento;
import br.ufmg.dcc603.alarme.auditoria.RegistroDeEventos;
import br.ufmg.dcc603.alarme.monitoramento.SensorFogo;
import br.ufmg.dcc603.alarme.operador.RoteiroDemonstracao;
import br.ufmg.dcc603.alarme.processamento.Alarme;
import br.ufmg.dcc603.alarme.processamento.CentralDeMonitoramento;
import br.ufmg.dcc603.alarme.processamento.StatusAlarme;
import br.ufmg.dcc603.alarme.processamento.TipoAlarme;
import br.ufmg.dcc603.alarme.processamento.ZonaNormal;

/**
 * Testes automatizados para o Diagrama de Estados do Alarme (Figura 5) e imutabilidade da auditoria (RF15, RNF05).
 */
public class CicloDeVidaAlarmeERegistroTest {

    @Before
    public void prepararAmbiente() {
        Alarme.reiniciarContadorIds();
    }

    @Test(expected = IllegalStateException.class)
    public void testAlarmePendenteNaoPodeIrDiretamenteParaEncerradoFigura5() {
        // 1) Eu tenho um alarme recem-criado em estado PENDENTE.
        ZonaNormal sala = new ZonaNormal("ZN-01", "Sala 101");
        SensorFogo sensor = new SensorFogo("SF-101", sala);
        Alarme alarme = new Alarme(TipoAlarme.FOGO, sala, sensor);

        // 2) E eu faco a tentativa de encerra-lo diretamente sem confirmar nem descartar.
        // 3) Entao eu espero IllegalStateException preservando o ciclo de vida da Figura 5.
        alarme.encerrar();
    }

    @Test
    public void testTransicaoPorTempoLimiteMarcaAlarmePendenteComoFalsoFigura5() {
        // 1) Eu tenho um alarme PENDENTE criado no instante T0 sem confirmacao posterior.
        CentralDeMonitoramento central = new CentralDeMonitoramento();
        ZonaNormal deposito = new ZonaNormal("ZN-DEP", "Deposito", 2);
        SensorFogo sensor = new SensorFogo("SF-DEP-01", deposito);
        sensor.ativar();
        central.receberAtivacao(sensor);
        Alarme alarmePendente = central.obterUltimoAlarme();

        // 2) E eu faco a verificacao de timeouts 60 segundos apos a criacao (limite configurado = 30s).
        Date futuro = new Date(alarmePendente.getTimestamp().getTime() + 60_000L);
        int expirados = central.verificarTimeouts(30_000L, futuro);

        // 3) Entao eu espero que 1 alarme expire para FALSO e gere registro de auditoria EXPIRACAO_TEMPO_LIMITE.
        assertEquals("Exatamente 1 alarme pendente deve expirar.", 1, expirados);
        assertEquals("O alarme expirado deve mudar para FALSO conforme Figura 5.",
                StatusAlarme.FALSO, alarmePendente.getStatus());
    }

    @Test
    public void testAlarmeFalsoPodeSerEncerradoFigura5() {
        // 1) Eu tenho um alarme descartado como FALSO.
        ZonaNormal hall = new ZonaNormal("ZN-HALL", "Hall Social");
        SensorFogo sensor = new SensorFogo("SF-H1", hall);
        Alarme alarme = new Alarme(TipoAlarme.FOGO, hall, sensor);
        alarme.descartar();

        // 2) E eu faco o encerramento do alarme falso.
        alarme.encerrar();

        // 3) Entao eu espero que o estado final seja ENCERRADO.
        assertEquals("Alarme FALSO deve transitar para ENCERRADO.", StatusAlarme.ENCERRADO, alarme.getStatus());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testListaDeEventosRetornadaPeloRegistroEhImutavelRNF05() {
        // 1) Eu tenho um RegistroDeEventos contendo um evento auditavel gravado.
        RegistroDeEventos registro = new RegistroDeEventos();
        registro.registrar(new Evento("ATIVACAO_SENSOR", "CPD", "SF-01", "Disparo inicial"));

        // 2) E eu faco uma tentativa externa de apagar/adulterar a lista retornada por listar().
        List<Evento> visaoAuditoria = registro.listar();
        visaoAuditoria.clear();

        // 3) Entao eu espero UnsupportedOperationException garantindo imutabilidade (RNF05).
    }

    @Test
    public void testCopiaDefensivaImpedeAdulteracaoDeTimestampEmEventoRNF05() {
        // 1) Eu tenho um objeto Date externo usado para criar um Evento.
        Date dataOriginal = new Date(1_700_000_000_000L);
        Evento evento = new Evento(dataOriginal, "TESTE", "Zona A", "Sensor 1", "Verificacao de imutabilidade");

        // 2) E eu faco a mutacao do objeto Date externo e tambem do Date retornado por getTimestamp().
        dataOriginal.setTime(0L);
        evento.getTimestamp().setTime(0L);

        // 3) Entao eu espero que o timestamp interno do Evento permaneca intacto.
        assertEquals("O timestamp do evento auditavel nao pode sofrer mutacao externa.",
                1_700_000_000_000L, evento.getTimestamp().getTime());
    }

    @Test
    public void testFiltroDeEventosPorZonaRetornaApenasZonaSolicitada() {
        // 1) Eu tenho eventos registrados em duas zonas distintas.
        RegistroDeEventos registro = new RegistroDeEventos();
        registro.registrar(new Evento("ATIVACAO", "Recepcao", "SF-01", "Evento 1"));
        registro.registrar(new Evento("ATIVACAO", "CPD", "SF-02", "Evento 2"));
        registro.registrar(new Evento("CONFIRMACAO", "Recepcao", "Central", "Evento 3"));

        // 2) E eu faco a filtragem pela zona 'Recepcao'.
        List<Evento> eventosRecepcao = registro.filtrarPorZona("Recepcao");

        // 3) Entao eu espero receber exatamente os 2 eventos da Recepcao.
        assertEquals("Filtro deve retornar apenas os 2 eventos da Recepcao.", 2, eventosRecepcao.size());
    }

    @Test
    public void testRoteiroDemonstracaoExecutaTodosOsQuatroCenariosComSucesso() {
        // 1) Eu tenho uma instancia limpa do RoteiroDemonstracao padrao.
        RoteiroDemonstracao roteiro = new RoteiroDemonstracao();

        // 2) E eu faco a execucao completa do cenario guiado.
        roteiro.executarCenarioGuiado();

        // 3) Entao eu espero 4 alarmes demonstrados, 3 chamados de emergencia (1 critico, 1 duplo sensor, 1 manual) e log preenchido.
        assertEquals("O roteiro guiado deve produzir 4 alarmes representativos.",
                4, roteiro.getCentral().getAlarmes().size());
        assertEquals("Devem ocorrer 3 chamados ao servico de emergencia.",
                3, roteiro.getServicoSimulado().obterQuantidadeAcionamentos());
        assertTrue("O log auditavel deve registrar pelo menos 10 eventos detalhados.",
                roteiro.getRegistroEventos().obterTotalEventos() >= 10);
    }
}
