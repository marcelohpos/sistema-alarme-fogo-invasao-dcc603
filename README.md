# Sistema de Monitoramento de Alarmes de Fogo e Invasão

**Disciplina:** DCC603 – Engenharia de Software I (2026/2) — DCC/ICEx/UFMG  
**Professor:** Eduardo Figueiredo  
**Grupo 3:** João Vitor Castilho Lisboa e Marcelo Henrique Pereira Oliveira e Silva  

---

## 1. Visão Geral do Projeto

Este repositório reúne a documentação completa (Sprints 1, 2 e 3), a implementação parcial em **Java** e a suíte de testes automatizados em **JUnit 4** do **Sistema de Monitoramento de Alarmes de Fogo e Invasão** (Tema 3 do Trabalho Prático).

Seguindo o modelo de **Entrega Incremental** definido na Sprint 1 e o projeto arquitetural/detalhado modelado na Sprint 2, a implementação da Sprint 3 cobre integralmente o **núcleo crítico de detecção, confirmação polimórfica, escalonamento de emergência, operação manual na área central e auditoria imutável (`RF01` a `RF07` e `RF15`)**, acompanhada de um **Painel Visual da Área Central em Java Swing** e modo de simulação em console.

---

## 2. Rastreabilidade de Requisitos (Escopo da Sprint 3)

| ID | Requisito | Classes Principais | Testes Automatizados (JUnit 4) |
|---|---|---|---|
| **RF01** | Monitoramento contínuo de sensores de fogo e invasão em todas as zonas | `Sensor`, `SensorFogo`, `SensorInvasao`, `Zona`, `CentralDeMonitoramento` | `SensorEMonitoramentoTest` |
| **RF02** | Registro de cada ativação indicando zona, tipo e horário | `Evento`, `RegistroDeEventos`, `CentralDeMonitoramento` | `CentralDeMonitoramentoTest` |
| **RF03** | Identificação de zonas críticas com prioridade máxima (sem descarte) | `ZonaCritica`, `Alarme`, `CentralDeMonitoramento` | `ZonaCriticaENormalTest`, `OperadorTest` |
| **RF04** | Acionamento imediato da emergência em zonas críticas sem confirmação prévia | `ZonaCritica`, `CentralDeMonitoramento`, `ServicoEmergenciaSimulado` | `ZonaCriticaENormalTest`, `CentralDeMonitoramentoTest` |
| **RF05** | Exigência de confirmação prévia em zonas normais contra falsos positivos | `ZonaNormal`, `CentralDeMonitoramento` | `ZonaCriticaENormalTest`, `CentralDeMonitoramentoTest` |
| **RF06** | Confirmação por múltiplos sensores ativos do mesmo tipo na mesma zona | `ZonaNormal.processarAlarme()`, `ZonaNormal.contarSensoresAtivos()` | `ZonaCriticaENormalTest`, `CentralDeMonitoramentoTest` |
| **RF07** | Transmissão manual à emergência ou resposta pessoal pelo operador humano | `Operador`, `PainelOperadorSwing`, `CentralDeMonitoramento` | `OperadorTest` |
| **RF15** | Log auditável de ativações, confirmações, acionamentos e ações do operador | `Evento`, `RegistroDeEventos` | `CicloDeVidaAlarmeERegistroTest` |
| **RNF03** | Redundância entre sensores da mesma zona tolerando falha individual | `ZonaNormal`, `Sensor` | `SensorEMonitoramentoTest` |
| **RNF05** | Imutabilidade dos registros para auditoria posterior | `Evento` (cópias defensivas), `RegistroDeEventos.listar()` | `CicloDeVidaAlarmeERegistroTest` |
| **RNF06** | Escalabilidade para adição de novas zonas e sensores sem redesenho | `Zona`, `CentralDeMonitoramento.adicionarZona()` | `CentralDeMonitoramentoTest` |
| **RNF08** | Interface clara identificando zona, tipo de alarme e criticidade | `PainelOperadorSwing` | Demonstração interativa Swing |

*(Os requisitos de atuadores físicos de hardware `RF08`–`RF14` permanecem fora do incremento codificado nesta entrega parcial, exatamente conforme planejado no documento de Modelo de Processo da Sprint 1 e na Sprint 2.)*

---

## 3. Arquitetura de Componentes e Estrutura de Diretórios

Os pacotes em `src/main/java/br/ufmg/dcc603/alarme/` mapeiam **1 para 1** os 5 componentes arquiteturais e as 16 classes/interfaces/enums especificados na Sprint 2:

```text
.
├── src/
│   ├── main/java/br/ufmg/dcc603/alarme/
│   │   ├── app/
│   │   │   └── SistemaAlarmeApp.java                            # Classe isolada com main() (Idiomas #2 e #3)
│   │   ├── auditoria/                                           # Componente Auditoria/Log
│   │   │   ├── IRegistro.java
│   │   │   ├── Evento.java
│   │   │   └── RegistroDeEventos.java
│   │   ├── monitoramento/                                       # Componente Monitoramento de Sensores
│   │   │   ├── EstadoSensor.java
│   │   │   ├── IMonitoramento.java
│   │   │   ├── Sensor.java
│   │   │   ├── SensorFogo.java
│   │   │   └── SensorInvasao.java
│   │   ├── notificacao/                                         # Componente Notificação/Emergência
│   │   │   ├── INotificacao.java
│   │   │   ├── ServicoEmergencia.java
│   │   │   └── ServicoEmergenciaSimulado.java
│   │   ├── processamento/                                       # Componente Processamento de Alarmes
│   │   │   ├── TipoAlarme.java
│   │   │   ├── StatusAlarme.java
│   │   │   ├── Alarme.java
│   │   │   ├── Zona.java
│   │   │   ├── ZonaCritica.java
│   │   │   ├── ZonaNormal.java
│   │   │   ├── IProcessamento.java
│   │   │   └── CentralDeMonitoramento.java
│   │   └── operador/                                            # Componente Interface do Operador
│   │       ├── Operador.java
│   │       ├── PainelOperadorSwing.java
│   │       └── RoteiroDemonstracao.java
│   └── test/java/br/ufmg/dcc603/alarme/                         # Suíte automatizada JUnit 4 (35 testes)
│       ├── SensorEMonitoramentoTest.java
│       ├── ZonaCriticaENormalTest.java
│       ├── CentralDeMonitoramentoTest.java
│       ├── OperadorTest.java
│       └── CicloDeVidaAlarmeERegistroTest.java
├── mvnw / mvnw.cmd                                              # Maven Wrapper (dispensa instalar Maven)
├── pom.xml                                                      # Configuração Maven + JUnit 4.13.2
└── README.md                                                    # Manual de instalação e execução
```

---

## 4. Pré-requisitos e Manual de Instalação

- **Java JDK 17 ou superior** (testado com **OpenJDK 21 LTS**).
- **Não é necessário instalar o Apache Maven manualmente**: o repositório inclui o **Maven Wrapper** (`mvnw` / `mvnw.cmd`), que baixa e utiliza automaticamente a versão exata do Maven na primeira execução.

### Passo 1 — Clonar o repositório

```bash
git clone <URL_DO_REPOSITORIO_GITHUB>
cd sistema-alarme-fogo-invasao
```

### Passo 2 — Compilar e executar todos os testes automatizados (JUnit 4)

**No Windows (PowerShell ou Prompt de Comando):**
```powershell
.\mvnw.cmd clean test
```

**No Linux ou macOS:**
```bash
chmod +x mvnw
./mvnw clean test
```

### Passo 3 — Executar o Painel Visual da Área Central (Java Swing)

**No Windows:**
```powershell
.\mvnw.cmd compile exec:java
```

**No Linux ou macOS:**
```bash
./mvnw compile exec:java
```

Na interface gráfica, você pode:
- Disparar ou resetar individualmente sensores de fogo (`SensorFogo`) e de invasão (`SensorInvasao`) tanto na **Zona Crítica** (*CPD / Sala de Servidores*) quanto nas **Zonas Normais** (*Recepção Principal* e *Escritórios 2º Andar*);
- Selecionar qualquer alarme na tabela para acionar **Transmitir à Emergência**, **Responder Pessoalmente**, **Descartar Falso** ou **Encerrar Ocorrência**;
- Clicar em **Simular Roteiro Completo** para demonstrar instantaneamente todos os fluxos de negócio e inspecionar os chamados 193/190 e o `RegistroDeEventos` imutável.

### Passo 4 — Executar a demonstração guiada no terminal (modo Console)

```powershell
.\mvnw.cmd compile exec:java "-Dexec.args=--demo"
```

### Execução via IDE (Eclipse, IntelliJ IDEA ou VS Code)

1. No **Eclipse**: `File -> Import -> Maven -> Existing Maven Projects` e selecione a pasta raiz do projeto.
   - Para rodar os testes: clique com o botão direito na pasta `src/test/java` → `Run As -> JUnit Test`.
   - Para abrir o painel gráfico: clique com o botão direito em `SistemaAlarmeApp.java` → `Run As -> Java Application`.
2. No **IntelliJ IDEA / VS Code**: abra a pasta raiz do projeto; a IDE reconhecerá automaticamente o `pom.xml`. Execute `SistemaAlarmeApp.main()` ou rode a pasta `src/test/java`.

---

## 5. Uso de Inteligência Artificial

Em conformidade com o item 8 das instruções do Trabalho Prático da disciplina DCC603:
- **Sprints 1 e 2:** Utilização da ferramenta Claude para auxílio na revisão de consistência dos requisitos, diagramas UML e formatação documental.
- **Sprint 3 (Entrega Final):** Utilização de assistente de IA (Google Antigravity / Gemini) como apoio na geração da estrutura Maven, codificação das classes Java estritamente fiéis aos diagramas da Sprint 2, elaboração da interface gráfica Swing de demonstração, escrita de casos de teste JUnit 4 e diagramação dos documentos PDF finais, sob revisão, validação e responsabilidade integral dos integrantes do Grupo 3.
