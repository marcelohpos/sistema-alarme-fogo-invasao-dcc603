package br.ufmg.dcc603.alarme.operador;

import java.util.List;

import br.ufmg.dcc603.alarme.auditoria.Evento;
import br.ufmg.dcc603.alarme.auditoria.RegistroDeEventos;
import br.ufmg.dcc603.alarme.monitoramento.Sensor;
import br.ufmg.dcc603.alarme.monitoramento.SensorFogo;
import br.ufmg.dcc603.alarme.monitoramento.SensorInvasao;
import br.ufmg.dcc603.alarme.notificacao.ServicoEmergenciaSimulado;
import br.ufmg.dcc603.alarme.processamento.Alarme;
import br.ufmg.dcc603.alarme.processamento.CentralDeMonitoramento;
import br.ufmg.dcc603.alarme.processamento.Zona;
import br.ufmg.dcc603.alarme.processamento.ZonaCritica;
import br.ufmg.dcc603.alarme.processamento.ZonaNormal;

/**
 * Constroi o edificio padrao de demonstracao e executa cenarios guiados cobrindo RF01-RF07 e RF15.
 */
public class RoteiroDemonstracao {

    private final ServicoEmergenciaSimulado servicoSimulado;
    private final RegistroDeEventos registroEventos;
    private final CentralDeMonitoramento central;
    private final Operador operadorPadrao;

    public RoteiroDemonstracao() {
        servicoSimulado = new ServicoEmergenciaSimulado();
        registroEventos = new RegistroDeEventos();
        central = new CentralDeMonitoramento(servicoSimulado, registroEventos);
        operadorPadrao = new Operador("OP-01", "Joao Vitor / Marcelo", central);
        configurarEdificioPadrao();
    }

    private void configurarEdificioPadrao() {
        // Zona critica: CPD / Sala de Servidores (acionamento imediato sem exigir 2o sensor).
        ZonaCritica zonaCpd = new ZonaCritica("Z-CRIT-01", "CPD / Sala de Servidores");
        zonaCpd.adicionarSensor(new SensorFogo("SF-CPD-01"));
        zonaCpd.adicionarSensor(new SensorInvasao("SI-CPD-01"));
        central.adicionarZona(zonaCpd);

        // Zona normal 1: Recepcao Principal (exige 2 sensores ativos do mesmo tipo).
        ZonaNormal zonaRecepcao = new ZonaNormal("Z-NORM-01", "Recepcao Principal", 2);
        zonaRecepcao.adicionarSensor(new SensorFogo("SF-REC-01"));
        zonaRecepcao.adicionarSensor(new SensorFogo("SF-REC-02"));
        zonaRecepcao.adicionarSensor(new SensorInvasao("SI-REC-01"));
        central.adicionarZona(zonaRecepcao);

        // Zona normal 2: Escritorios 2o Andar (exige 2 sensores ativos do mesmo tipo).
        ZonaNormal zonaEscritorios = new ZonaNormal("Z-NORM-02", "Escritorios 2o Andar", 2);
        zonaEscritorios.adicionarSensor(new SensorFogo("SF-ESC-01"));
        zonaEscritorios.adicionarSensor(new SensorFogo("SF-ESC-02"));
        zonaEscritorios.adicionarSensor(new SensorInvasao("SI-ESC-01"));
        zonaEscritorios.adicionarSensor(new SensorInvasao("SI-ESC-02"));
        central.adicionarZona(zonaEscritorios);
    }

    public void executarCenarioGuiado() {
        List<Zona> zonas = central.getZonas();
        Zona zonaCpd = zonas.get(0);
        Zona zonaRecepcao = zonas.get(1);
        Zona zonaEscritorios = zonas.get(2);

        // Cenario 1 (RF03/RF04): Zona critica dispara emergencia imediatamente com 1 unico sensor.
        Sensor sensorCpdFogo = zonaCpd.getSensores().get(0);
        sensorCpdFogo.ativar();
        central.receberAtivacao(sensorCpdFogo);

        // Cenario 2 (RF05/RF06): Zona normal fica PENDENTE com 1 sensor e confirma ao ativar o 2o sensor.
        Sensor sensorRecFogo1 = zonaRecepcao.getSensores().get(0);
        Sensor sensorRecFogo2 = zonaRecepcao.getSensores().get(1);
        sensorRecFogo1.ativar();
        central.receberAtivacao(sensorRecFogo1);
        sensorRecFogo2.ativar();
        central.receberAtivacao(sensorRecFogo2);

        // Cenario 3 (RF07/UC06): Sensor isolado em zona normal avaliado e respondido pessoalmente pelo operador.
        Sensor sensorEscFogo1 = zonaEscritorios.getSensores().get(0);
        sensorEscFogo1.ativar();
        central.receberAtivacao(sensorEscFogo1);
        Alarme alarmeFalsoPositivo = central.obterUltimoAlarme();
        operadorPadrao.responderAlarme(alarmeFalsoPositivo);

        // Cenario 4 (RF07/UC05): Invasao detectada por 1 sensor e transmitida manualmente pelo operador a Policia.
        Sensor sensorEscInv1 = zonaEscritorios.getSensores().get(2);
        sensorEscInv1.ativar();
        central.receberAtivacao(sensorEscInv1);
        Alarme alarmeInvasao = central.obterUltimoAlarme();
        operadorPadrao.transmitirAlarme(alarmeInvasao);
    }

    public void imprimirRelatorioConsole() {
        System.out.println("==========================================================================");
        System.out.println(" SISTEMA DE MONITORAMENTO DE ALARMES DE FOGO E INVASAO - GRUPO 3 (DCC603)");
        System.out.println("==========================================================================");
        System.out.println();

        System.out.println("--- ALARMES REGISTRADOS NA CENTRAL ---");
        for (Alarme item : central.getAlarmes()) {
            String responsavel = "Automatico";
            if (item.getOperadorResponsavel() != null) {
                responsavel = item.getOperadorResponsavel().getNome();
            }
            System.out.println(" * " + item + " | Sensores: " + item.getSensoresOrigem().size()
                    + " | Responsavel: " + responsavel);
        }
        System.out.println();

        System.out.println("--- CHAMADOS ENVIADOS AO SERVICO DE EMERGENCIA ("
                + servicoSimulado.obterQuantidadeAcionamentos() + ") ---");
        for (String protocolo : servicoSimulado.listarNotificacoes()) {
            System.out.println(" * " + protocolo);
        }
        System.out.println();

        System.out.println("--- LOG AUDITAVEL IMUTAVEL DE EVENTOS (RF15 / RNF05) ---");
        for (Evento registro : registroEventos.listar()) {
            System.out.println(" * " + registro);
        }
        System.out.println("==========================================================================");
    }

    public ServicoEmergenciaSimulado getServicoSimulado() {
        return servicoSimulado;
    }

    public RegistroDeEventos getRegistroEventos() {
        return registroEventos;
    }

    public CentralDeMonitoramento getCentral() {
        return central;
    }

    public Operador getOperadorPadrao() {
        return operadorPadrao;
    }
}
