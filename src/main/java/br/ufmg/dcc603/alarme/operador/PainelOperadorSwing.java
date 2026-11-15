package br.ufmg.dcc603.alarme.operador;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;

import br.ufmg.dcc603.alarme.auditoria.Evento;
import br.ufmg.dcc603.alarme.monitoramento.Sensor;
import br.ufmg.dcc603.alarme.notificacao.ServicoEmergenciaSimulado;
import br.ufmg.dcc603.alarme.processamento.Alarme;
import br.ufmg.dcc603.alarme.processamento.CentralDeMonitoramento;
import br.ufmg.dcc603.alarme.processamento.StatusAlarme;
import br.ufmg.dcc603.alarme.processamento.Zona;
import br.ufmg.dcc603.alarme.processamento.ZonaNormal;

/**
 * Painel visual da Area Central do Operador em Java Swing para monitoramento e resposta em tempo real (RNF08).
 */
public class PainelOperadorSwing {

    private static final Color COR_TOPO = new Color(20, 33, 61);
    private static final Color COR_FUNDO = new Color(244, 246, 250);
    private static final Color COR_CRITICA = new Color(185, 28, 28);
    private static final Color COR_CRITICA_FUNDO = new Color(254, 242, 242);
    private static final Color COR_NORMAL = new Color(29, 78, 216);
    private static final Color COR_NORMAL_FUNDO = new Color(239, 246, 255);
    private static final Color COR_PENDENTE = new Color(180, 83, 9);
    private static final Color COR_VERDE = new Color(21, 128, 61);

    private final RoteiroDemonstracao contexto;
    private final JFrame janela;
    private final JLabel rotuloResumoPendentes;
    private final JLabel rotuloResumoConfirmados;
    private final JLabel rotuloResumoFalsos;
    private final JLabel rotuloResumoEmergencias;
    private final JPanel containerZonas;
    private final DefaultTableModel modeloTabelaAlarmes;
    private final JTable tabelaAlarmes;
    private final DefaultListModel<String> modeloListaEmergencias;
    private final DefaultTableModel modeloTabelaEventos;
    private boolean modoSilencioso;

    public PainelOperadorSwing(RoteiroDemonstracao contextoInicial) {
        if (contextoInicial == null) {
            throw new IllegalArgumentException("O contexto de demonstracao nao pode ser nulo.");
        }
        contexto = contextoInicial;
        modoSilencioso = false;

        janela = new JFrame("Painel Central de Alarmes de Fogo e Invasao - DCC603 Grupo 3");
        rotuloResumoPendentes = criarCardIndicador("Pendentes: 0", COR_PENDENTE);
        rotuloResumoConfirmados = criarCardIndicador("Confirmados: 0", COR_CRITICA);
        rotuloResumoFalsos = criarCardIndicador("Falsos/Resp.: 0", new Color(71, 85, 105));
        rotuloResumoEmergencias = criarCardIndicador("Chamados 193/190: 0", COR_VERDE);
        containerZonas = new JPanel();

        String[] colunasAlarmes = {
            "ID", "Horario", "Zona", "Criticidade", "Tipo", "Sensores", "Status", "Operador"
        };
        modeloTabelaAlarmes = new DefaultTableModel(colunasAlarmes, 0);
        tabelaAlarmes = new JTable(modeloTabelaAlarmes);

        modeloListaEmergencias = new DefaultListModel<String>();

        String[] colunasEventos = {
            "Horario", "Tipo de Evento", "Zona", "Origem", "Descricao Auditavel"
        };
        modeloTabelaEventos = new DefaultTableModel(colunasEventos, 0);

        montarInterface();
        atualizarTelaCompleta();
    }

    private void montarInterface() {
        janela.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        janela.setSize(1420, 840);
        janela.setLocationRelativeTo(null);
        janela.getContentPane().setBackground(COR_FUNDO);
        janela.setLayout(new BorderLayout(10, 10));

        janela.add(criarCabecalho(), BorderLayout.NORTH);

        JSplitPane divisaoSuperior = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                criarPainelZonas(),
                criarPainelAlarmesEOperador());
        divisaoSuperior.setResizeWeight(0.40);
        divisaoSuperior.setDividerLocation(550);

        JSplitPane divisaoPrincipal = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                divisaoSuperior,
                criarPainelInferiorAuditoria());
        divisaoPrincipal.setResizeWeight(0.60);
        divisaoPrincipal.setDividerLocation(480);

        janela.add(divisaoPrincipal, BorderLayout.CENTER);
    }

    private JPanel criarCabecalho() {
        JPanel cabecalho = new JPanel(new BorderLayout(16, 8));
        cabecalho.setBackground(COR_TOPO);
        cabecalho.setBorder(new EmptyBorder(12, 18, 12, 18));

        JPanel blocoTitulo = new JPanel(new GridLayout(2, 1));
        blocoTitulo.setOpaque(false);

        JLabel titulo = new JLabel("AREA CENTRAL DE CONTROLE - MONITORAMENTO DE FOGO E INVASAO");
        titulo.setForeground(Color.WHITE);
        titulo.setFont(new Font("SansSerif", Font.BOLD, 17));

        Operador op = contexto.getOperadorPadrao();
        JLabel subtitulo = new JLabel("Operador Autenticado: " + op.getNome()
                + " (" + op.getId() + ")  |  DCC603 Engenharia de Software - UFMG");
        subtitulo.setForeground(new Color(203, 213, 225));
        subtitulo.setFont(new Font("SansSerif", Font.PLAIN, 12));

        blocoTitulo.add(titulo);
        blocoTitulo.add(subtitulo);

        JPanel painelIndicadores = new JPanel(new GridLayout(1, 4, 8, 0));
        painelIndicadores.setOpaque(false);
        painelIndicadores.add(rotuloResumoPendentes);
        painelIndicadores.add(rotuloResumoConfirmados);
        painelIndicadores.add(rotuloResumoFalsos);
        painelIndicadores.add(rotuloResumoEmergencias);

        cabecalho.add(blocoTitulo, BorderLayout.WEST);
        cabecalho.add(painelIndicadores, BorderLayout.EAST);
        return cabecalho;
    }

    private JLabel criarCardIndicador(String textoInicial, Color corDestaque) {
        JLabel rotulo = new JLabel(textoInicial, SwingConstants.CENTER);
        rotulo.setOpaque(true);
        rotulo.setBackground(corDestaque);
        rotulo.setForeground(Color.WHITE);
        rotulo.setFont(new Font("SansSerif", Font.BOLD, 12));
        rotulo.setBorder(new EmptyBorder(8, 12, 8, 12));
        return rotulo;
    }

    private JScrollPane criarPainelZonas() {
        containerZonas.setLayout(new BoxLayout(containerZonas, BoxLayout.Y_AXIS));
        containerZonas.setBackground(COR_FUNDO);
        containerZonas.setBorder(new EmptyBorder(8, 8, 8, 8));

        JScrollPane rolagem = new JScrollPane(containerZonas);
        TitledBorder borda = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(148, 163, 184)),
                "Zonas do Edificio e Sensores Instalados (RF01 / RF03)");
        borda.setTitleFont(new Font("SansSerif", Font.BOLD, 13));
        rolagem.setBorder(borda);
        return rolagem;
    }

    private JPanel criarPainelAlarmesEOperador() {
        JPanel painel = new JPanel(new BorderLayout(8, 8));
        painel.setBackground(Color.WHITE);
        TitledBorder borda = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(148, 163, 184)),
                "Fila de Alarmes na Central e Decisoes do Operador (RF04 - RF07)");
        borda.setTitleFont(new Font("SansSerif", Font.BOLD, 13));
        painel.setBorder(borda);

        tabelaAlarmes.setDefaultEditor(Object.class, null);
        tabelaAlarmes.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabelaAlarmes.setRowHeight(26);
        tabelaAlarmes.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        tabelaAlarmes.setFont(new Font("SansSerif", Font.PLAIN, 12));

        painel.add(new JScrollPane(tabelaAlarmes), BorderLayout.CENTER);
        painel.add(criarBarraBotoesOperador(), BorderLayout.SOUTH);
        return painel;
    }

    private JPanel criarBarraBotoesOperador() {
        JPanel barra = new JPanel(new GridLayout(1, 5, 6, 0));
        barra.setBackground(new Color(248, 250, 252));
        barra.setBorder(new EmptyBorder(6, 6, 6, 6));

        Font fonteBotao = new Font("SansSerif", Font.BOLD, 11);

        JButton btnTransmitir = new JButton("Transmitir 193/190");
        btnTransmitir.setFont(fonteBotao);
        btnTransmitir.setBackground(COR_CRITICA);
        btnTransmitir.setForeground(Color.WHITE);
        btnTransmitir.setFocusPainted(false);
        btnTransmitir.addActionListener(evento -> executarTransmissaoManual());

        JButton btnResponder = new JButton("Responder Local");
        btnResponder.setFont(fonteBotao);
        btnResponder.setBackground(COR_NORMAL);
        btnResponder.setForeground(Color.WHITE);
        btnResponder.setFocusPainted(false);
        btnResponder.addActionListener(evento -> executarRespostaPessoal());

        JButton btnDescartar = new JButton("Descartar Falso");
        btnDescartar.setFont(fonteBotao);
        btnDescartar.setFocusPainted(false);
        btnDescartar.addActionListener(evento -> executarDescarteManual());

        JButton btnEncerrar = new JButton("Encerrar Alarme");
        btnEncerrar.setFont(fonteBotao);
        btnEncerrar.setFocusPainted(false);
        btnEncerrar.addActionListener(evento -> executarEncerramento());

        JButton btnRoteiro = new JButton("Simular Roteiro");
        btnRoteiro.setFont(fonteBotao);
        btnRoteiro.setBackground(COR_VERDE);
        btnRoteiro.setForeground(Color.WHITE);
        btnRoteiro.setFocusPainted(false);
        btnRoteiro.addActionListener(evento -> {
            contexto.executarCenarioGuiado();
            atualizarTelaCompleta();
        });

        barra.add(btnTransmitir);
        barra.add(btnResponder);
        barra.add(btnDescartar);
        barra.add(btnEncerrar);
        barra.add(btnRoteiro);
        return barra;
    }

    private JSplitPane criarPainelInferiorAuditoria() {
        JPanel painelEmergencia = new JPanel(new BorderLayout(4, 4));
        TitledBorder bordaEmergencia = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(148, 163, 184)),
                "Chamados Expedidos ao Servico de Emergencia (Bombeiros 193 / Policia 190)");
        bordaEmergencia.setTitleFont(new Font("SansSerif", Font.BOLD, 12));
        painelEmergencia.setBorder(bordaEmergencia);

        JList<String> listaVisualEmergencias = new JList<String>(modeloListaEmergencias);
        listaVisualEmergencias.setFont(new Font("Monospaced", Font.PLAIN, 12));
        painelEmergencia.add(new JScrollPane(listaVisualEmergencias), BorderLayout.CENTER);

        JPanel painelAuditoria = new JPanel(new BorderLayout(4, 4));
        TitledBorder bordaAuditoria = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(148, 163, 184)),
                "Log Auditavel Imutavel de Eventos - RegistroDeEventos (RF02 / RF15 / RNF05)");
        bordaAuditoria.setTitleFont(new Font("SansSerif", Font.BOLD, 12));
        painelAuditoria.setBorder(bordaAuditoria);

        JTable tabelaEventos = new JTable(modeloTabelaEventos);
        tabelaEventos.setDefaultEditor(Object.class, null);
        tabelaEventos.setRowHeight(22);
        tabelaEventos.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 11));
        tabelaEventos.setFont(new Font("SansSerif", Font.PLAIN, 11));
        tabelaEventos.getColumnModel().getColumn(0).setPreferredWidth(125);
        tabelaEventos.getColumnModel().getColumn(1).setPreferredWidth(175);
        tabelaEventos.getColumnModel().getColumn(2).setPreferredWidth(145);
        tabelaEventos.getColumnModel().getColumn(3).setPreferredWidth(135);
        tabelaEventos.getColumnModel().getColumn(4).setPreferredWidth(360);
        painelAuditoria.add(new JScrollPane(tabelaEventos), BorderLayout.CENTER);

        JSplitPane divisaoInferior = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                painelEmergencia,
                painelAuditoria);
        divisaoInferior.setResizeWeight(0.40);
        divisaoInferior.setDividerLocation(550);
        return divisaoInferior;
    }

    public void atualizarTelaCompleta() {
        reconstruirCartoesZonas();
        atualizarTabelaAlarmes();
        atualizarChamadosEmergencia();
        atualizarTabelaEventos();
        atualizarIndicadoresCabecalho();
        janela.revalidate();
        janela.repaint();
    }

    private void reconstruirCartoesZonas() {
        containerZonas.removeAll();
        CentralDeMonitoramento central = contexto.getCentral();

        // Renderizar um cartao visual distinto para cada zona destacando zonas criticas em vermelho (RNF08).
        for (Zona zonaAtual : central.getZonas()) {
            containerZonas.add(criarCartaoZona(zonaAtual));
            containerZonas.add(Box.createRigidArea(new Dimension(0, 8)));
        }
    }

    private JPanel criarCartaoZona(Zona zonaAtual) {
        Color corBorda = COR_NORMAL;
        Color corFundo = COR_NORMAL_FUNDO;
        String textoBadge = "ZONA NORMAL";
        if (zonaAtual.ehCritica()) {
            corBorda = COR_CRITICA;
            corFundo = COR_CRITICA_FUNDO;
            textoBadge = "CRITICA - Prioridade Maxima";
        } else if (zonaAtual instanceof ZonaNormal) {
            ZonaNormal normal = (ZonaNormal) zonaAtual;
            textoBadge = "NORMAL - Limiar: " + normal.getSensoresNecessarios() + " sensores";
        }

        JPanel cartao = new JPanel(new BorderLayout(6, 6));
        cartao.setBackground(corFundo);
        cartao.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(corBorda, 2),
                new EmptyBorder(8, 10, 8, 10)));

        JLabel cabecalhoZona = new JLabel(zonaAtual.getNome() + " [" + zonaAtual.getId() + "]  -  " + textoBadge);
        cabecalhoZona.setForeground(corBorda);
        cabecalhoZona.setFont(new Font("SansSerif", Font.BOLD, 12));
        cartao.add(cabecalhoZona, BorderLayout.NORTH);

        JPanel gradeSensores = new JPanel(new GridLayout(0, 1, 4, 4));
        gradeSensores.setOpaque(false);

        for (Sensor sensorItem : zonaAtual.getSensores()) {
            gradeSensores.add(criarLinhaSensor(sensorItem));
        }
        cartao.add(gradeSensores, BorderLayout.CENTER);
        return cartao;
    }

    private JPanel criarLinhaSensor(Sensor sensorItem) {
        JPanel linha = new JPanel(new BorderLayout(6, 0));
        linha.setOpaque(false);

        String estadoTexto = sensorItem.getEstado().name();
        JLabel descricao = new JLabel("Sensor " + sensorItem.getId()
                + " (" + sensorItem.obterTipoAlarme() + ") - Estado: " + estadoTexto);
        descricao.setFont(new Font("SansSerif", Font.PLAIN, 12));
        if (sensorItem.estaAtivo()) {
            descricao.setForeground(COR_CRITICA);
            descricao.setFont(new Font("SansSerif", Font.BOLD, 12));
        }

        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        botoes.setOpaque(false);

        JButton btnDisparar = new JButton("Disparar");
        btnDisparar.setFont(new Font("SansSerif", Font.BOLD, 11));
        btnDisparar.addActionListener(evento -> {
            sensorItem.ativar();
            contexto.getCentral().receberAtivacao(sensorItem);
            atualizarTelaCompleta();
        });

        JButton btnResetar = new JButton("Resetar");
        btnResetar.setFont(new Font("SansSerif", Font.PLAIN, 11));
        btnResetar.addActionListener(evento -> {
            sensorItem.desativar();
            atualizarTelaCompleta();
        });

        botoes.add(btnDisparar);
        botoes.add(btnResetar);

        linha.add(descricao, BorderLayout.CENTER);
        linha.add(botoes, BorderLayout.EAST);
        return linha;
    }

    private void atualizarTabelaAlarmes() {
        int linhaSelecionada = tabelaAlarmes.getSelectedRow();
        modeloTabelaAlarmes.setRowCount(0);
        SimpleDateFormat formatadorHora = new SimpleDateFormat("HH:mm:ss");

        for (Alarme item : contexto.getCentral().getAlarmes()) {
            String criticidade = "NORMAL";
            if (item.getZona().ehCritica()) {
                criticidade = "CRITICA";
            }
            String nomeOperador = "-";
            if (item.getOperadorResponsavel() != null) {
                nomeOperador = item.getOperadorResponsavel().getNome();
            }

            Object[] linha = {
                item.getId(),
                formatadorHora.format(item.getTimestamp()),
                item.getZona().getNome(),
                criticidade,
                item.getTipo().name(),
                montarResumoSensores(item.getSensoresOrigem()),
                item.getStatus().name(),
                nomeOperador
            };
            modeloTabelaAlarmes.addRow(linha);
        }

        if (linhaSelecionada >= 0 && linhaSelecionada < modeloTabelaAlarmes.getRowCount()) {
            tabelaAlarmes.setRowSelectionInterval(linhaSelecionada, linhaSelecionada);
        } else if (modeloTabelaAlarmes.getRowCount() > 0) {
            int ultima = modeloTabelaAlarmes.getRowCount() - 1;
            tabelaAlarmes.setRowSelectionInterval(ultima, ultima);
        }
    }

    private String montarResumoSensores(List<Sensor> listaSensores) {
        StringBuilder construtor = new StringBuilder();
        for (int indice = 0; indice < listaSensores.size(); indice++) {
            if (indice > 0) {
                construtor.append(", ");
            }
            construtor.append(listaSensores.get(indice).getId());
        }
        return construtor.toString();
    }

    private void atualizarChamadosEmergencia() {
        modeloListaEmergencias.clear();
        ServicoEmergenciaSimulado servico = contexto.getServicoSimulado();
        for (String chamado : servico.listarNotificacoes()) {
            modeloListaEmergencias.addElement(chamado);
        }
    }

    private void atualizarTabelaEventos() {
        modeloTabelaEventos.setRowCount(0);
        for (Evento item : contexto.getRegistroEventos().listar()) {
            Object[] linha = {
                item.formatarHorario(),
                item.getTipo(),
                item.getZona(),
                item.getOrigem(),
                item.getDescricao()
            };
            modeloTabelaEventos.addRow(linha);
        }
    }

    private void atualizarIndicadoresCabecalho() {
        int pendentes = 0;
        int confirmados = 0;
        int falsos = 0;

        for (Alarme item : contexto.getCentral().getAlarmes()) {
            StatusAlarme st = item.getStatus();
            if (st == StatusAlarme.PENDENTE) {
                pendentes++;
            } else if (st == StatusAlarme.CONFIRMADO) {
                confirmados++;
            } else if (st == StatusAlarme.FALSO) {
                falsos++;
            }
        }

        rotuloResumoPendentes.setText("Pendentes: " + pendentes);
        rotuloResumoConfirmados.setText("Confirmados: " + confirmados);
        rotuloResumoFalsos.setText("Falsos/Resp.: " + falsos);
        rotuloResumoEmergencias.setText("Chamados 193/190: "
                + contexto.getServicoSimulado().obterQuantidadeAcionamentos());
    }

    private Alarme obterAlarmeSelecionado() {
        int indice = tabelaAlarmes.getSelectedRow();
        List<Alarme> lista = contexto.getCentral().getAlarmes();
        if (indice < 0 || indice >= lista.size()) {
            exibirAviso("Selecione primeiro um alarme na tabela.");
            return null;
        }
        return lista.get(indice);
    }

    private void executarTransmissaoManual() {
        Alarme selecionado = obterAlarmeSelecionado();
        if (selecionado == null) {
            return;
        }
        try {
            contexto.getOperadorPadrao().transmitirAlarme(selecionado);
            atualizarTelaCompleta();
        } catch (RuntimeException erro) {
            exibirAviso(erro.getMessage());
        }
    }

    private void executarRespostaPessoal() {
        Alarme selecionado = obterAlarmeSelecionado();
        if (selecionado == null) {
            return;
        }
        try {
            contexto.getOperadorPadrao().responderAlarme(selecionado);
            atualizarTelaCompleta();
        } catch (RuntimeException erro) {
            exibirAviso(erro.getMessage());
        }
    }

    private void executarDescarteManual() {
        Alarme selecionado = obterAlarmeSelecionado();
        if (selecionado == null) {
            return;
        }
        try {
            contexto.getOperadorPadrao().descartarAlarme(selecionado);
            atualizarTelaCompleta();
        } catch (RuntimeException erro) {
            exibirAviso(erro.getMessage());
        }
    }

    private void executarEncerramento() {
        Alarme selecionado = obterAlarmeSelecionado();
        if (selecionado == null) {
            return;
        }
        try {
            contexto.getOperadorPadrao().encerrarAlarme(selecionado);
            atualizarTelaCompleta();
        } catch (RuntimeException erro) {
            exibirAviso(erro.getMessage());
        }
    }

    private void exibirAviso(String mensagem) {
        if (!modoSilencioso) {
            JOptionPane.showMessageDialog(janela, mensagem, "Operacao na Central", JOptionPane.WARNING_MESSAGE);
        }
    }

    public void exibir() {
        janela.setVisible(true);
    }

    public void fechar() {
        janela.dispose();
    }

    public void salvarCapturaDeTela(File destinoPng) throws IOException {
        modoSilencioso = true;
        janela.setVisible(true);
        janela.validate();
        janela.getContentPane().doLayout();
        atualizarTelaCompleta();

        int largura = janela.getContentPane().getWidth();
        int altura = janela.getContentPane().getHeight();
        BufferedImage imagem = new BufferedImage(largura, altura, BufferedImage.TYPE_INT_RGB);
        Graphics2D contextoGrafico = imagem.createGraphics();
        janela.getContentPane().paintAll(contextoGrafico);
        contextoGrafico.dispose();

        if (destinoPng.getParentFile() != null) {
            destinoPng.getParentFile().mkdirs();
        }
        ImageIO.write(imagem, "png", destinoPng);
    }

    public static void exportarCapturaDemonstracao(RoteiroDemonstracao roteiroDemo, File destinoPng) {
        roteiroDemo.executarCenarioGuiado();
        PainelOperadorSwing painel = new PainelOperadorSwing(roteiroDemo);
        try {
            painel.salvarCapturaDeTela(destinoPng);
        } catch (IOException erro) {
            throw new RuntimeException("Falha ao salvar captura do painel: " + erro.getMessage(), erro);
        } finally {
            painel.fechar();
        }
    }
}
