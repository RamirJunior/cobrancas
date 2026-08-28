# Cobranças API

Microserviço de pagamento e cobrança com suporte a PIX e cartão.

## Tecnologias
- Java 17
- Spring Boot 3
- Spring Data JPA
- H2 Database
- Maven

## Endpoints

### POST /api/v1/cobrancas
Cria uma cobrança.

Exemplo:
json {   "valor": 25.50,   "tipo": "RECARGA",   "metodo": "PIX" }

### GET /api/v1/cobrancas/{id}
Busca uma cobrança por ID.

### POST /api/v1/cobrancas/webhook/pix
Recebe a notificação de pagamento PIX.

### POST /api/v1/cobrancas/{transactionId}/validate
Valida checkout de cartão.

## Regras de negócio
- Lock distribuído por usuário com TTL de 5 segundos.
- Mensagem de lock: "Geracao de cobranca em andamento."
- Timezone oficial: America/Sao_Paulo.
- Erro de criação: "Erro ao criar cobranca."

## Como rodar
bash ./mvnw spring-boot:run

## Como testar
```bash
./mvnw test