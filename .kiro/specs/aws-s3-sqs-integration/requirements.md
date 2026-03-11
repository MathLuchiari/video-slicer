# Requirements Document

## Introduction

Esta funcionalidade adiciona integração com serviços AWS (S3 e SQS) ao sistema Video Slicer existente. O objetivo é migrar o armazenamento de vídeos do sistema de arquivos local para Amazon S3 e implementar processamento assíncrono através de mensageria com Amazon SQS. Esta mudança permitirá escalabilidade, durabilidade e processamento distribuído dos vídeos.

## Glossary

- **Video_Slicer**: Sistema Spring Boot Java responsável por upload e corte de vídeos
- **Upload_Endpoint**: Endpoint REST que recebe arquivos de vídeo via HTTP multipart
- **S3_Storage_Service**: Serviço responsável por armazenar vídeos no Amazon S3
- **SQS_Publisher**: Serviço responsável por publicar mensagens na fila Amazon SQS
- **Video_ID**: Identificador único (UUID) gerado para cada vídeo no momento do upload
- **S3_Bucket**: Container de armazenamento no Amazon S3 onde os vídeos são salvos
- **SQS_Queue**: Fila de mensagens Amazon SQS para processamento assíncrono
- **Video_Message**: Mensagem contendo o Video_ID enviada para a SQS_Queue

## Requirements

### Requirement 1: Armazenamento de Vídeos no S3

**User Story:** Como desenvolvedor do sistema, eu quero que os vídeos sejam armazenados no Amazon S3, para que o sistema seja escalável e não dependa do sistema de arquivos local.

#### Acceptance Criteria

1. WHEN um vídeo é recebido pelo Upload_Endpoint, THE S3_Storage_Service SHALL armazenar o arquivo no S3_Bucket configurado
2. THE S3_Storage_Service SHALL preservar a extensão original do arquivo de vídeo no nome do objeto S3
3. THE S3_Storage_Service SHALL utilizar um Video_ID único (UUID) como nome base do arquivo no S3
4. IF o upload para S3 falhar, THEN THE S3_Storage_Service SHALL retornar um erro descritivo
5. THE S3_Storage_Service SHALL organizar os vídeos em uma pasta específica dentro do S3_Bucket

### Requirement 2: Geração de Identificador Único

**User Story:** Como cliente da API, eu quero receber um identificador único do vídeo após o upload, para que eu possa referenciar o vídeo em operações futuras.

#### Acceptance Criteria

1. WHEN um vídeo é recebido, THE Video_Slicer SHALL gerar um Video_ID único no formato UUID v4
2. THE Upload_Endpoint SHALL retornar o Video_ID na resposta HTTP de sucesso
3. THE Video_ID SHALL ser gerado antes do armazenamento no S3
4. FOR ALL uploads bem-sucedidos, THE Video_ID retornado SHALL corresponder ao nome do arquivo armazenado no S3

### Requirement 3: Publicação de Mensagens na Fila SQS

**User Story:** Como desenvolvedor do sistema, eu quero que o ID do vídeo seja enviado para uma fila SQS após upload bem-sucedido, para que o processamento de corte possa ser feito de forma assíncrona.

#### Acceptance Criteria

1. WHEN o upload para S3 é concluído com sucesso, THE SQS_Publisher SHALL enviar uma Video_Message para a SQS_Queue configurada
2. THE Video_Message SHALL conter o Video_ID como atributo principal
3. THE Video_Message SHALL incluir metadados adicionais: timestamp do upload, extensão do arquivo, e tamanho do arquivo em bytes
4. IF o envio para SQS falhar, THEN THE SQS_Publisher SHALL registrar o erro em log mas não reverter o upload do S3
5. THE SQS_Publisher SHALL utilizar o formato JSON para a estrutura da mensagem

### Requirement 4: Configuração de Credenciais AWS

**User Story:** Como operador do sistema, eu quero configurar as credenciais AWS através de variáveis de ambiente ou arquivo de configuração, para que o sistema possa autenticar com os serviços AWS de forma segura.

#### Acceptance Criteria

1. THE Video_Slicer SHALL suportar configuração de credenciais AWS através do arquivo application.properties
2. THE Video_Slicer SHALL suportar configuração de credenciais AWS através de variáveis de ambiente
3. WHERE variáveis de ambiente estão definidas, THE Video_Slicer SHALL priorizar variáveis de ambiente sobre application.properties
4. THE Video_Slicer SHALL validar a presença de credenciais AWS obrigatórias na inicialização
5. IF credenciais AWS estão ausentes ou inválidas, THEN THE Video_Slicer SHALL falhar na inicialização com mensagem de erro clara

### Requirement 5: Configuração de Recursos AWS

**User Story:** Como operador do sistema, eu quero configurar o nome do bucket S3 e URL da fila SQS, para que o sistema possa ser implantado em diferentes ambientes (desenvolvimento, staging, produção).

#### Acceptance Criteria

1. THE Video_Slicer SHALL permitir configuração do nome do S3_Bucket através de propriedade de configuração
2. THE Video_Slicer SHALL permitir configuração da URL da SQS_Queue através de propriedade de configuração
3. THE Video_Slicer SHALL permitir configuração da região AWS através de propriedade de configuração
4. THE Video_Slicer SHALL permitir configuração do prefixo de pasta dentro do S3_Bucket através de propriedade de configuração
5. IF configurações de recursos AWS estão ausentes, THEN THE Video_Slicer SHALL falhar na inicialização com mensagem de erro clara

### Requirement 6: Tratamento de Erros e Resiliência

**User Story:** Como desenvolvedor do sistema, eu quero que falhas de comunicação com AWS sejam tratadas adequadamente, para que o sistema seja resiliente a problemas temporários de rede ou serviços.

#### Acceptance Criteria

1. WHEN uma operação S3 falha por timeout ou erro de rede, THE S3_Storage_Service SHALL tentar novamente até 3 vezes com backoff exponencial
2. WHEN uma operação SQS falha por timeout ou erro de rede, THE SQS_Publisher SHALL tentar novamente até 3 vezes com backoff exponencial
3. IF todas as tentativas de upload S3 falharem, THEN THE Upload_Endpoint SHALL retornar HTTP 500 com mensagem de erro descritiva
4. IF o envio para SQS falhar após todas as tentativas, THEN THE Video_Slicer SHALL registrar o erro em log mas retornar sucesso do upload (pois o vídeo está no S3)
5. THE Video_Slicer SHALL registrar em log todas as tentativas e falhas de comunicação com AWS para auditoria

### Requirement 7: Resposta do Endpoint de Upload

**User Story:** Como cliente da API, eu quero receber uma resposta estruturada após o upload, para que eu saiba se a operação foi bem-sucedida e possa obter o identificador do vídeo.

#### Acceptance Criteria

1. WHEN o upload é bem-sucedido, THE Upload_Endpoint SHALL retornar HTTP 201 Created
2. THE Upload_Endpoint SHALL retornar um objeto JSON contendo o Video_ID no campo "videoId"
3. THE Upload_Endpoint SHALL retornar a URL completa do vídeo no S3 no campo "videoUrl"
4. THE Upload_Endpoint SHALL retornar uma mensagem de sucesso no campo "message"
5. WHEN o upload falha, THE Upload_Endpoint SHALL retornar código HTTP apropriado (400 para erro de cliente, 500 para erro de servidor) com mensagem de erro descritiva

### Requirement 8: Validação de Arquivos de Vídeo

**User Story:** Como desenvolvedor do sistema, eu quero validar os arquivos recebidos antes de enviar para S3, para que apenas vídeos válidos sejam armazenados e processados.

#### Acceptance Criteria

1. WHEN um arquivo é recebido, THE Upload_Endpoint SHALL validar que o arquivo não está vazio
2. THE Upload_Endpoint SHALL validar que a extensão do arquivo está na lista de formatos suportados (mp4, avi, mov, mkv, webm)
3. THE Upload_Endpoint SHALL validar que o tamanho do arquivo não excede o limite configurado (padrão: 500MB)
4. IF a validação falhar, THEN THE Upload_Endpoint SHALL retornar HTTP 400 Bad Request com mensagem explicativa
5. THE Upload_Endpoint SHALL realizar todas as validações antes de iniciar o upload para S3

### Requirement 9: Compatibilidade com Sistema Existente

**User Story:** Como desenvolvedor do sistema, eu quero manter compatibilidade com o endpoint de slice existente, para que funcionalidades atuais continuem operando durante a transição.

#### Acceptance Criteria

1. THE Video_Slicer SHALL manter o endpoint /api/video/slice funcional sem modificações
2. WHERE o sistema está configurado para usar S3, THE Video_Slicer SHALL permitir que o slice endpoint baixe vídeos do S3 temporariamente para processamento local
3. THE Video_Slicer SHALL manter a estrutura de resposta atual do Upload_Endpoint, adicionando novos campos sem remover os existentes
4. THE Video_Slicer SHALL manter retrocompatibilidade com clientes que esperam o campo "fileName" na resposta de upload
