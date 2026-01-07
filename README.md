# 📊 Weekly Feedback Report Function

Esta Cloud Function é responsável por **gerar e enviar automaticamente o relatório semanal de satisfação dos alunos** da plataforma FeedAlert.

O relatório é gerado em **PDF**, com base nos feedbacks armazenados no banco de dados, e enviado por **email** aos administradores cadastrados.  
A execução é **automatizada via Cloud Scheduler**, garantindo que o relatório seja enviado periodicamente sem intervenção manual.

---

## 🚀 Visão Geral

- 📅 Execução automática (semanal)
- 🗄️ Consulta dados no Cloud SQL (PostgreSQL)
- 🔐 Segredos protegidos via Secret Manager
- 📄 Geração de PDF com layout personalizado
- ✉️ Envio de email com anexo via SendGrid
- 🔒 Função privada (sem acesso público)

---

## 🛠️ Tecnologias Utilizadas

- **Java 17**
- **Spring Boot 2.7**
- **Spring Cloud Function**
- **Google Cloud Functions (2ª geração)**
- **Google Cloud Scheduler**
- **Google Cloud SQL (PostgreSQL)**
- **Google Secret Manager**
- **SendGrid**
- **OpenHTMLToPDF**
- **Maven**

---

## 🧩 Estrutura do Projeto

```
src/
└── main/
    ├── java/
    │   └── com/
    │       └── feedalert/
    │           └── feedbackreport/
    │               ├── FeedbackreportApplication.java
    │               ├── WeeklyReportFunction.java
    │               ├── dto/
    │               │   └── ReportData.java
    │               └── service/
    │                   └── EmailService.java
    │                   └── ReportService.java
    │                   └── UserService.java
    │
    └── resources/
        ├── application.properties
        └── report-template.html
```

### 📌 Descrição dos principais componentes

- **FeedbackreportApplication**: Classe principal responsável por inicializar o contexto Spring.

- **WeeklyReportFunction**: Implementa a Cloud Function responsável por orquestrar a geração e envio do relatório semanal.

- **EmailService**: Responsável pelo envio de emails utilizando a API do SendGrid.

- **ReportService**: Responsável pela lógica de geração do relatório:
  - Consulta os feedbacks no banco de dados
  - Processa os dados
  - Gera o PDF do relatório

- **UserService**: Responsável por buscar os administradores no banco de dados.

- **ReportData**: Objeto de transferência de dados (DTO) utilizado para transportar as informações do relatório.

- **report-template.html**: Template HTML utilizado como base para a geração do relatório em PDF.

- **application.properties**: Arquivo de configuração da aplicação, incluindo integração com Cloud SQL, Secret Manager e SendGrid.

---

## ⚙️ Funcionamento da Função

1. A função é acionada automaticamente pelo **Cloud Scheduler**.
2. Os administradores são buscados no banco de dados.
3. Os feedbacks da última semana são coletados.
4. Os dados são processados e consolidados.
5. Um **relatório em PDF** é gerado.
6. O relatório é enviado por **email** aos administradores.

---

## 🔧 Configuração do Projeto

A configuração da **function** é baseada em variáveis externas e serviços gerenciados do Google Cloud, garantindo segurança e flexibilidade.

### 🔐 Gerenciamento de Secrets

Todas as informações sensíveis são armazenadas no **Google Secret Manager**, incluindo:

- URL de conexão com o banco de dados
- Usuário e senha do banco
- Chave da API do SendGrid

Os segredos são importados automaticamente pela aplicação por meio da seguinte configuração:

```properties
spring.config.import=sm://
```

### 🗄️ Conexão com o Banco de Dados

A função se conecta a um banco PostgreSQL hospedado no Cloud SQL, utilizando o Cloud SQL Socket Factory.


--- 

## 🚀 Deploy da Cloud Function + Cloud Scheduler

Esta Cloud Function é implantada utilizando o **Google Cloud CLI (`gcloud`)**, com suporte a **Java 17** e **Cloud Functions de 2ª geração (Gen2)**.

O processo de deploy empacota a aplicação Spring Cloud Function em um **JAR executável**, que é utilizado como fonte da função.

---

### 👤 Service Accounts Dedicadas

Para garantir **segurança, isolamento de responsabilidades e princípio do menor privilégio**, foram criadas **Service Accounts dedicadas** para a execução da Cloud Function e para o Cloud Scheduler.


#### 🔹 Service Account da Cloud Function

A Cloud Function utiliza uma Service Account própria, responsável por acessar apenas os recursos necessários para a execução do relatório semanal.

##### 📛 Nome
`weekly-report-sa`

##### 🔑 Permissões atribuídas

- **Cloud SQL Client**  
  Permite conexão com o banco de dados PostgreSQL hospedado no Cloud SQL.

- **Secret Manager Secret Accessor**  
  Permite acesso aos segredos armazenados no Secret Manager (credenciais de banco e API do SendGrid).

- **Cloud Logging Writer**  
  Permite escrita de logs no Cloud Logging.


#### 🔹 Service Account do Cloud Scheduler

A Service Account do Cloud Scheduler é responsável por invocar a Cloud Function de forma segura, utilizando autenticação OIDC.

##### 📛 Nome
`scheduler-invoker`

##### 🔑 Permissões atribuídas
- **Cloud Functions Invoker**  
  Permite que o Cloud Scheduler invoque a Cloud Function de forma autenticada.

---

### 📦 Build do Projeto

Antes do deploy, é necessário gerar o artefato da aplicação utilizando o Maven:

```bash
mvn clean package
```

Ao final do processo, o JAR será gerado no diretório:
`target/deploy/`

### ☁️ Deploy da Função no Google Cloud

O deploy da função é realizado com o comando abaixo:
```powershell
gcloud functions deploy weekly-report-function `
  --gen2 `
  --runtime=java17 `
  --region=us-central1 `
  --entry-point=org.springframework.cloud.function.adapter.gcp.GcfJarLauncher `
  --trigger-http `
  --memory=512MB `
  --service-account=weekly-report-sa@<PROJECT_ID>.iam.gserviceaccount.com `
  --set-env-vars SPRING_CLOUD_FUNCTION_DEFINITION=weeklyReport `
  --source=target/deploy
```

#### 🔎 Parâmetros principais

- `--gen2`: Utiliza Cloud Functions de 2ª geração (baseadas em Cloud Run)
- `--runtime java17`: Runtime Java 17 (compatível e recomendado para GCP)
- `--region us-central1`: Região onde a função será deployada
- `--entry-point org.springframework.cloud.function.adapter.gcp.GcfJarLauncher`: Classe responsável por inicializar a Spring Cloud Function
- `--trigger-http`: Define que a função será acionada via HTTP (necessário para o Cloud Scheduler)
- `--memory 512MB`: Memória alocada para a função
- `--service-account`: Conta de serviço utilizada para acessar recursos como Cloud SQL, Secret Manager e APIs externas
- `--set-env-vars`: Variáveis de ambiente necessárias para a função
- `--source target/deploy`: Diretório onde está o JAR empacotado

**⚠️ Substitua os valores antes de executar:**
- `PROJECT_ID`: ID do projeto no GCP (ex: `feedalert-12345`)

#### 🧪 Testes

Para fins de teste, é possível adicionar o parâmetro `--allow-unauthenticated` para permitir chamadas públicas temporariamente.

> ⚠️ Para produção, deve ser feito um deploy **SEM** esse parâmetro, garantindo que a função permaneça privada.



### ⏰ Integração com Cloud Scheduler

Essa função foi pensada para usar em conjunto com o Cloud Scheduler, que vai atuar como gatilho da função, realizando chamadas HTTP autenticadas através de autenticação **OIDC**.

### Exemplo de criação do job (PowerShell)

```powershell
gcloud scheduler jobs create http weekly-report `
  --location=us-central1 `
  --schedule="0 8 * * 1" `
  --uri="https://us-central1-<PROJECT_ID>.cloudfunctions.net/weekly-report-function" `
  --http-method=POST `
  --oidc-service-account-email="scheduler-invoker@<PROJECT_ID>.iam.gserviceaccount.com" `
  --time-zone="America/Sao_Paulo"
```

> O exemplo acima executa a função **toda segunda-feira às 08:00**.

#### 🧪 Testes

Para fins de teste, o agendamento pode ser configurado com uma frequência menor, como:
`*/5 * * * *` → (envio a cada 5 minutos).

---

## 🔐 Segurança

- A função **não é pública**.
- O acesso é restrito ao **Cloud Scheduler**.
- A autenticação é feita via **OIDC**.
- Segredos sensíveis são armazenados no **Secret Manager**.
- Não há credenciais hardcoded no código.

---

## 🧾 Relatório PDF

O relatório é gerado a partir de um template HTML (`report-template.html`) e contém:

- 📅 **Período do relatório**
- ⭐ **Avaliação média**
- 📊 **Total de feedbacks**
- 🚨 **Feedbacks urgentes**
- 📈 **Feedbacks por dia**
- 💬 **Comentários dos alunos**

#### Quando não há feedbacks no período:

- A avaliação média é exibida como **“-”**
- A cor utilizada é **neutra (preto)**

---

## ✉️ Envio de Email

- **Serviço:** SendGrid

O email contém:
- Assunto personalizado
- Texto explicativo
- Relatório em PDF como anexo

### 📎 Nome do arquivo

O arquivo PDF é nomeado como: `relatorio-semanal-YYYY-MM-DD.pdf`

---

## 📌 Observações

- Emails podem ser direcionados para a caixa de spam, pois o projeto não utiliza domínio próprio.

- O projeto foi desenvolvido com foco acadêmico, priorizando boas práticas de arquitetura e segurança.

--- 

## 📄 Licença

Este projeto faz parte do Tech Challenge da Fase 4 - Cloud Computing e Serverless.
