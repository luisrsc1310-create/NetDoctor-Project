# NetDiag Pro - Ferramenta Completa de Suporte TI, Diagnóstico & Gerenciamento de Rede

Software Desktop profissional desenvolvido 100% em **Java 21**, utilizando **JavaFX** para a interface gráfica moderna e banco de dados local **SQLite** (zero instalação de servidores). Projetado para diagnósticos rápidos de computadores e impressoras, varredura de sub-redes locais, gerenciamento de IP manual e funcionamento autônomo (**Online e Offline**).

Este projeto foi desenvolvido com o objetivo de auxiliar a empresa na qual trabalho, oferecendo uma ferramenta prática e portátil para o dia a dia do suporte técnico interno — eliminando a dependência de softwares pagos ou de difícil instalação nos ambientes atendidos.

---

## Funcionalidades Completas

### 1. Diagnóstico do Computador & Hardware
* **Hardware & Sistema:**
  * Nome do Hostname e Usuário logado no Windows.
  * Versão exata do Sistema Operacional e arquitetura.
  * Quantidade de núcleos lógicos do processador (CPU).
  * Uso de memória RAM em tempo real (Total, Em uso, Livre e barra percentual).
  * Lista de todos os discos e partições com espaço livre/total.
* **Placas de Rede:**
  * Detecção das interfaces de rede ativas (Ethernet, Wi-Fi), exibindo endereço IPv4, MAC Address e status.
* **Teste Rápido de Ping:**
  * Disparo de ping ICMP para qualquer IP ou domínio com tempo de resposta em milissegundos.

### 2. Diagnóstico de Impressoras
* **Impressoras Instaladas no Windows:**
  * Lista todas as impressoras locais (USB) e de rede mapeadas no sistema.
  * Identificação da impressora padrão e status atual.
* **Diagnóstico Avançado de Impressora de Rede (via IP):**
  * Teste de conectividade ICMP (Ping).
  * Teste da **Porta RAW 9100** (padrão JetDirect da maioria das impressoras HP, Brother, Epson, etc.).
  * Teste da interface de configuração Web (Portas **80 / 443** HTTP/HTTPS).
  * Teste da porta **515 LPD**.

### 3. Varredura de Rede (IP Scanner Multithread)
* Varredura ultrarrápida de sub-rede local (ex: de `192.168.1.1` até `192.168.1.254`) em paralelo utilizando 40 threads simultâneas.
* Identificação inteligente de tipo de dispositivo:
  * **Impressora de Rede** (ao detectar a porta 9100 aberta).
  * **Computador Windows** (ao detectar a porta 445 SMB aberta).
  * **Interface Web / Dispositivo de Rede** (ao detectar porta 80 HTTP).
* Resolução automática de Hostname DNS reverso e tempo de resposta.

### 4. Configuração Manual de IP (Estático ou DHCP)
* **Atribuição Estática:**
  * Escolha a placa de rede desejada.
  * Defina manualmente: Endereço IP, Máscara de sub-rede, Gateway padrão e servidores DNS (Primário e Secundário).
  * Aplicação direta nas configurações de rede do Windows via comando `netsh`.
* **Restauração para DHCP:**
  * Botão de 1 clique para voltar a placa para obter IP e DNS automaticamente.
* **Perfis de Rede Salvos (SQLite):**
  * Salve diferentes perfis (ex: *"Bancada Manutenção"*, *"Rede Escritório"*, *"Rede Fábrica"*) para alternar configurações em segundos.

### 5. Ferramentas de Suporte TI (Reparos Rápidos)
* **Spooler de Impressão:**
  * *Reiniciar Spooler:* Para e reinicia o serviço do Windows quando a impressora para de responder.
  * *Limpar Fila Travada:* Interrompe o serviço, deleta documentos presos em `C:\Windows\System32\spool\PRINTERS` e reinicia o spooler.
* **Rede do Windows:**
  * *Flush DNS:* Limpa o cache DNS do sistema operacional (`ipconfig /flushdns`).
  * *Renew IP:* Renova a concessão de endereço IP via DHCP (`ipconfig /renew`).

### 6. Exportação de Relatórios Técnicos (HTML)
* Botão no cabeçalho para gerar um **Relatório Técnico em HTML** completo e estilizado, contendo dados de hardware da máquina, interfaces de rede, impressoras e histórico de diagnósticos, pronto para visualização no navegador ou exportação em PDF.

### 7. Banco de Dados Local (SQLite) & Modo Offline/Online
* Banco embutido autônomo (`netdiag.db`), criado na própria pasta da aplicação.
* Indicador visual dinâmico informando se a máquina tem saída real para a **Internet** ou se está em **Modo Local / Intranet**.
* Histórico persistido com data/hora de todos os testes e alterações executados.

---

## Tecnologias Utilizadas

* **Linguagem:** Java 21+
* **Interface Gráfica:** JavaFX (FXML declarativo + estilos em CSS)
* **Banco de Dados Local:** SQLite (via JDBC embutido)
* **Gerenciador de Build:** Maven

---

## Estrutura do Código-Fonte

```text
net-diagnostico/
├── pom.xml                                  <- Configuração Maven com JavaFX, SQLite e Shade Plugin
├── build-pendrive.bat                       <- Gera o pacote portável para pendrive
├── run.bat                                  <- Inicializador rápido (desenvolvimento local)
├── .gitignore                               <- Ignora target/, .idea/, *.db, pendrive-dist/
├── src/
│   └── main/
│       ├── java/com/netdiag/
│       │   ├── Launcher.java                <- Ponto de entrada padrão
│       │   ├── MainApp.java                 <- Classe Application do JavaFX
│       │   ├── controller/
│       │   │   └── MainController.java      <- Gerenciador das telas, threads assíncronas e UI
│       │   ├── model/
│       │   │   ├── DiagnosticLog.java       <- Modelo para logs de auditoria no SQLite
│       │   │   ├── DiscoveredDevice.java    <- Dispositivos identificados no IP Scanner
│       │   │   ├── NetworkProfile.java      <- Perfis de IP salvos no SQLite
│       │   │   └── PrinterInfo.java         <- Dados de impressoras do Windows
│       │   └── service/
│       │       ├── DatabaseService.java     <- Conexão SQLite e queries SQL
│       │       ├── NetworkService.java      <- Ping, sockets, IP Scanner e comandos netsh
│       │       ├── PrinterService.java      <- Spooler, limpeza de fila e portas de impressão
│       │       ├── ReportService.java       <- Gerador de relatório técnico em HTML
│       │       └── SystemInfoService.java   <- Leitura de hardware, RAM, CPU e discos
│       └── resources/com/netdiag/view/
│           ├── main.fxml                    <- Telas e abas em FXML
│           └── styles.css                   <- Estilização CSS profissional
```

---

## Como Executar

### No IntelliJ IDEA
1. Abra o **IntelliJ IDEA**.
2. Vá em **File > Open...** e aponte para `C:\Users\PC\IdeaProjects\net-diagnostico`.
3. Abra o arquivo [Launcher.java](src/main/java/com/netdiag/Launcher.java) e clique em **Run**.

### Via Prompt / Arquivo BAT (desenvolvimento local)
* Dê duplo clique em `run.bat` na raiz do projeto.
* > [!IMPORTANT]
  > Para usar as funções de alterar IP do Windows ou limpar a fila do Spooler, execute o arquivo clicando com o botão direito e selecionando **"Executar como Administrador"**.

---

## Uso via Pendrive (sem Java instalado)

O NetDiag Pro pode rodar em **qualquer computador Windows** diretamente do pendrive como um **`.exe` standalone** — sem instalar Java, sem instalar nada.

### Como funciona

O script `build-pendrive.bat` usa o **jpackage** (ferramenta oficial do JDK) para empacotar a aplicação + um JRE mínimo dentro de um `.exe` autossuficiente. Quem receber o pendrive só precisa clicar duas vezes.

### Passo 1 — Gerar o EXE (feito uma vez, no seu computador)

> Pré-requisito: JDK 21+ instalado (ou gerenciado pelo IntelliJ em `C:\Users\PC\.jdks\`).

1. Abra o Prompt de Comando na pasta do projeto:
   ```
   cd C:\Users\PC\IdeaProjects\net-diagnostico
   ```
2. Execute o script de build:
   ```
   build-pendrive.bat
   ```
3. Aguarde (~1-2 min). O script irá:
   - Detectar o JDK automaticamente
   - Compilar o projeto com Maven (fat-jar)
   - Gerar o `.exe` com JRE embutido via `jpackage`
   - Montar a pasta `pendrive-dist\NetDiag Pro\` pronta para copiar

### Passo 2 — Copiar para o pendrive

Copie a pasta `pendrive-dist\NetDiag Pro\` para o pendrive:

```text
pendrive-dist\
  NetDiag Pro\               <- copie esta pasta para o pendrive
    NetDiag Pro.exe          <- clique duplo para abrir, sem instalar nada
    runtime\                 <- JRE embutido (invisível ao usuário)
    app\
      netdiag-pro.jar
```

### Passo 3 — Executar no computador destino

1. Conecte o pendrive no computador.
2. Abra a pasta `NetDiag Pro` no pendrive.
3. Clique com o botão direito em **`NetDiag Pro.exe`** e selecione **"Executar como administrador"**.
   - Necessário para: configurar IP estático, limpar fila do spooler.
   - Para apenas diagnóstico e varredura de rede, duplo clique normal funciona.

> [!NOTE]
> O banco de dados `netdiag.db` é criado automaticamente na pasta `app\` do pendrive na primeira execução. Os perfis de rede e o histórico ficam salvos no pendrive entre os usos.
