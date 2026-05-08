# OrionPay - Maquinha Simulate

Simulador de terminal de pagamentos (POS) Android com suporte a leitura de cartões via NFC (EMV) e integração com gateway de pagamentos OrionPay.

## 🏗️ Arquitetura do Projeto

O projeto utiliza uma **Arquitetura Hexagonal (Ports and Adapters)** combinada com princípios de **Clean Architecture**, organizada em camadas para garantir testabilidade e desacoplamento de hardware/rede.

### Estrutura de Camadas e Pastas:

```text
app/src/main/java/orionpay/maquinha_simulate/
├── domain/               # [CORE] Regras de negócio, Enums e Modelos de domínio
├── application/          # [PORTAS] Casos de uso e Interfaces (Ports)
├── infrastructure/       # [ADAPTADORES] Implementações de rede, auth e persistência
├── data/                 # [ADAPTADORES] Baixo nível: NFC, Protocolo EMV e TLV
├── presentation/         # [ADAPTADORES] ViewModels e Gestão de Estado (MVVM)
└── ui/                   # [ADAPTADORES] Telas e Componentes (Jetpack Compose)
```

### Detalhes das Camadas:

1.  **Domain (`domain`)**: Contém a lógica de negócio pura, modelos de dados (`TransactionDomain`) e enums. É o coração do sistema e não depende de frameworks externos.
2.  **Application (`application`)**: Implementa os casos de uso (`UseCases`), orquestrando o fluxo de dados entre o domínio e os adaptadores externos.
3.  **Infrastructure (`infrastructure`)**: Implementa os adaptadores reais para comunicação externa (Network, Auth, API Client).
4.  **Presentation (`presentation`)**: Camada que lida com a lógica de exibição, utilizando **ViewModels** para manter o estado da UI de forma reativa.
5.  **UI (`ui`)**: Implementação visual utilizando **Jetpack Compose**, organizada por fluxo (Home, Transaction, Components).
6.  **Data/NFC (`data/nfc`)**: Implementação técnica de baixo nível para interação com o hardware NFC e decodificação do protocolo EMV (Chip de cartão).

---

## 🔄 Fluxo de Venda

O aplicativo foi otimizado para um fluxo de venda rápido e intuitivo:
1.  **Menu Principal**: Seleção do tipo de produto (Crédito, Débito, Pix).
2.  **Valor**: Inserção do valor da transação.
3.  **Leitura NFC**: Aproximação do cartão para leitura dos dados EMV.
4.  **Processamento**: Envio assíncrono para o gateway com proteção de idempotência.
5.  **Resultado**: Tela de confirmação ou erro, com opção de visualização de comprovante detalhado.

*Nota: O fluxo foi simplificado para avançar diretamente do valor para a leitura, utilizando a pré-seleção do menu.*

---

## 🛠️ Implementações Técnicas

### 1. Leitura de Cartão (NFC/EMV)
O sistema implementa um leitor EMV customizado (`ReadEmvReader`) capaz de:
*   **Seleção de AID**: Descoberta automática da bandeira (Visa, Mastercard, Elo, etc.).
*   **Processamento GPO**: Negociação de opções com o chip do cartão.
*   **Dados Dinâmicos**: Captura de **ATC (Application Transaction Counter)** e **Cryptogram (9F26)** para conformidade com padrões de segurança.

### 2. Segurança e Idempotência
*   **X-Idempotency-Key**: Implementação de chaves UUID para evitar cobranças duplicadas em caso de instabilidade de rede.
*   **Mascaramento de Dados**: Utilitários que garantem que dados sensíveis (PAN, CVV) não sejam expostos em logs.

### 3. Configuração Centralizada
Toda a infraestrutura de rede é gerida pelo `ApiConfig.kt`, permitindo ajuste rápido de IPs, portas e timeouts de transação.

---

## 🚀 Como Rodar o Projeto

1.  **Configurar IP**: Ajuste o `API_HOST` no arquivo `ApiConfig.kt` para o endereço do seu gateway.
2.  **NFC**: Certifique-se de que o dispositivo possui suporte a NFC e que ele está habilitado.
3.  **Compilação**: Use o Android Studio para compilar e instalar o app.

---

## 🛠️ Tecnologias Utilizadas

*   **Linguagem**: Kotlin
*   **UI**: Jetpack Compose (Material 3)
*   **NFC**: Android NFC Adapter (IsoDep)
*   **Concorrência**: Kotlin Coroutines
*   **Arquitetura**: Hexagonal / MVVM
