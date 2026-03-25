# OrionPay - Maquinha Simulate

Simulador de terminal de pagamentos (POS) Android com suporte a leitura de cartões via NFC (EMV) e integração com gateway de pagamentos.

## 🏗️ Arquitetura do Projeto

O projeto utiliza uma **Arquitetura Hexagonal (Ports and Adapters)** combinada com princípios de **Clean Architecture**, organizada em camadas para garantir testabilidade, manutenibilidade e desacoplamento de hardware/rede.

### Camadas:

1.  **Domain (`domain`)**: Contém a lógica de negócio pura, modelos de dados (`TransactionDomain`) e interfaces de saída (`Ports`). É o coração do sistema e não depende de frameworks.
2.  **Application (`application`)**: Implementa os casos de uso (`UseCases`), orquestrando o fluxo de dados entre o domínio e os adaptadores.
3.  **Infrastructure (`infrastructure`)**: Implementa os adaptadores reais para comunicação externa:
    *   `network`: Comunicação HTTP com a API de pagamentos.
    *   `auth`: Gerenciamento de tokens e autenticação do terminal.
4.  **Presentation (`presentation`)**: Camada de interface com o usuário usando **Jetpack Compose** e **ViewModel** para gestão de estado reativo.
5.  **Data/NFC (`data/nfc`)**: Implementação de baixo nível para interação com o hardware NFC e decodificação do protocolo EMV (Chip de cartão).

---

## 🛠️ Implementações Técnicas

### 1. Leitura de Cartão (NFC/EMV)
O sistema implementa um leitor EMV customizado (`ReadEmvReader`) capaz de:
*   **Seleção de AID**: Descoberta automática da bandeira do cartão (Visa, Mastercard, Elo, etc.).
*   **Processamento GPO**: Negociação de opções de processamento com o chip.
*   **Leitura de Records**: Extração de PAN (número do cartão), nome do titular e validade.
*   **Dados Dinâmicos**: Execução dos comandos `GENERATE AC` e `GET DATA` para capturar o **ATC (Application Transaction Counter)** e o **Cryptogram (9F26)** reais, garantindo a conformidade com padrões de segurança bancária.

### 2. Fluxo de Pagamento
*   **Idempotência**: Implementação de chaves de idempotência (UUID) para evitar duplicidade de cobranças em caso de instabilidade de rede.
*   **Venda Manual vs. Presencial**: Lógica condicional que diferencia vendas digitadas (exigindo CVV) de vendas via chip (onde o CVV é substituído por dados do chip).
*   **Segurança de Log**: Utilitários de mascaramento que ocultam dados sensíveis (PAN, CVV, Cryptogram) nos logs do Android, prevenindo exposição de dados em ambiente de desenvolvimento.

### 3. Configuração Centralizada
Toda a infraestrutura de rede é gerida pelo `ApiConfig.kt`, permitindo a alteração rápida de:
*   IP do servidor e portas.
*   Timeouts de conexão e leitura.
*   Credenciais de autenticação automática do terminal.

---

## 🚀 Como Rodar o Projeto

1.  **Configurar Backend**: Certifique-se de que a API do gateway está rodando e acessível.
2.  **Configurar IP**: Ajuste o `API_HOST` no arquivo `ApiConfig.kt` para o IP da sua máquina ou servidor.
3.  **Compilar**: Use o Android Studio para compilar o projeto. O app exige permissão de NFC ativa no dispositivo.
4.  **Diagnóstico**: Utilize a tela de "Diagnóstico de API" (acessível em modo DEBUG) para testar a conectividade e os payloads de transação sem necessidade de um cartão físico.

---

## 🛠️ Tecnologias Utilizadas

*   **Linguagem**: Kotlin
*   **UI**: Jetpack Compose
*   **NFC**: Android NFC Adapter (IsoDep)
*   **Rede**: HttpURLConnection / Coroutines (Dispatchers.IO)
*   **Arquitetura**: Hexagonal / MVVM
