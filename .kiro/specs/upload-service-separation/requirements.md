# Documento de Requisitos

## Introdução

Este documento especifica os requisitos para refatorar o repositório video-slicer em um serviço dedicado de upload de vídeos. O sistema atual combina funcionalidades de upload e slicing de vídeos. O objetivo é separar essas responsabilidades, mantendo apenas a funcionalidade de upload e removendo ou isolando a funcionalidade de slicing.

O serviço de upload resultante será responsável por:
- Receber uploads de arquivos de vídeo via API REST
- Validar formato e tamanho dos vídeos
- Armazenar vídeos no AWS S3
- Publicar eventos de upload na fila AWS SQS para processamento posterior
- Fornecer URLs de acesso aos vídeos armazenados

## Glossário

- **Upload_Service**: O serviço de upload de vídeos que será o resultado desta refatoração
- **Video_File**: Arquivo de vídeo enviado pelo usuário através da API
- **S3_Storage**: Serviço de armazenamento AWS S3 onde os vídeos são persistidos
- **SQS_Queue**: Fila AWS SQS onde eventos de upload são publicados
- **Video_Metadata**: Informações sobre o vídeo (ID, nome, extensão, tamanho, timestamp)
- **Slicing_Feature**: Funcionalidade de corte de vídeos que será removida do repositório
- **API_Endpoint**: Endpoint REST para upload de vídeos
- **Validation_Rules**: Regras de validação de formato e tamanho de vídeo
- **Retry_Mechanism**: Mecanismo de tentativas com backoff exponencial para operações AWS
- **Content_Type**: Tipo MIME do arquivo de vídeo baseado na extensão

## Requisitos

### Requisito 1: Remover Funcionalidade de Slicing

**User Story:** Como desenvolvedor, eu quero remover toda a funcionalidade de slicing do repositório, para que o serviço tenha uma responsabilidade única de upload.

#### Acceptance Criteria

1. THE Upload_Service SHALL NOT contain the SliceVideoUseCase interface
2. THE Upload_Service SHALL NOT contain the SliceVideoUseCaseImpl implementation
3. THE Upload_Service SHALL NOT contain the /api/video/slice endpoint in VideoController
4. THE Upload_Service SHALL NOT include the javacv-platform dependency in pom.xml
5. THE Upload_Service SHALL NOT contain any references to video slicing in configuration files

### Requisito 2: Renomear Pacotes e Artefatos

**User Story:** Como desenvolvedor, eu quero renomear os pacotes e artefatos do projeto para refletir sua nova responsabilidade de upload, para que o código seja mais claro e manutenível.

#### Acceptance Criteria

1. THE Upload_Service SHALL use the package name "com.videoupload" instead of "com.videoslicer"
2. THE Upload_Service SHALL use the artifact ID "video-upload-service" in pom.xml
3. THE Upload_Service SHALL use the application name "video-upload-service" in application.properties
4. THE Upload_Service SHALL rename the main class to VideoUploadServiceApplication
5. THE Upload_Service SHALL update all import statements to reflect the new package structure

### Requisito 3: Manter Funcionalidade de Upload

**User Story:** Como usuário da API, eu quero fazer upload de vídeos, para que eles sejam armazenados e processados posteriormente.

#### Acceptance Criteria

1. WHEN a Video_File is uploaded via POST /api/video/upload, THE Upload_Service SHALL validate the file format
2. WHEN a Video_File is uploaded via POST /api/video/upload, THE Upload_Service SHALL validate the file size
3. WHEN validation succeeds, THE Upload_Service SHALL store the Video_File in S3_Storage
4. WHEN storage succeeds, THE Upload_Service SHALL publish Video_Metadata to SQS_Queue
5. WHEN upload completes successfully, THE Upload_Service SHALL return HTTP 201 with videoId, fileName, and videoUrl
6. IF validation fails, THEN THE Upload_Service SHALL return HTTP 400 with error message
7. IF storage fails, THEN THE Upload_Service SHALL return HTTP 500 with error message

### Requisito 4: Preservar Validação de Vídeos

**User Story:** Como sistema, eu quero validar vídeos antes do upload, para que apenas arquivos válidos sejam processados.

#### Acceptance Criteria

1. THE Upload_Service SHALL validate that Video_File is not empty
2. THE Upload_Service SHALL validate that Video_File has a supported extension (mp4, avi, mov, mkv, webm)
3. THE Upload_Service SHALL validate that Video_File size does not exceed the configured maximum
4. WHEN validation fails, THE Upload_Service SHALL return a descriptive error message
5. THE Upload_Service SHALL use the VideoValidation domain class for all validation logic

### Requisito 5: Manter Integração com AWS S3

**User Story:** Como sistema, eu quero armazenar vídeos no S3, para que eles estejam disponíveis para acesso posterior.

#### Acceptance Criteria

1. WHEN storing a Video_File, THE Upload_Service SHALL use the configured bucket name from application.properties
2. WHEN storing a Video_File, THE Upload_Service SHALL use the configured folder prefix from application.properties
3. WHEN storing a Video_File, THE Upload_Service SHALL set the appropriate Content_Type based on file extension
4. WHEN storing a Video_File, THE Upload_Service SHALL generate a unique S3 key using videoId and extension
5. WHEN S3 operation fails, THE Upload_Service SHALL retry up to 3 times with exponential backoff
6. WHEN all retries fail, THE Upload_Service SHALL throw StorageException with descriptive message
7. WHEN storage succeeds, THE Upload_Service SHALL return the S3 URL of the stored video

### Requisito 6: Manter Integração com AWS SQS

**User Story:** Como sistema, eu quero publicar eventos de upload na fila SQS, para que outros serviços possam processar os vídeos.

#### Acceptance Criteria

1. WHEN a Video_File is successfully stored, THE Upload_Service SHALL publish Video_Metadata to SQS_Queue
2. THE Upload_Service SHALL serialize Video_Metadata to JSON format before publishing
3. THE Upload_Service SHALL include videoId, fileName, extension, sizeInBytes, and uploadTimestamp in the message
4. WHEN SQS operation fails, THE Upload_Service SHALL retry up to 3 times with exponential backoff
5. IF SQS publishing fails after all retries, THE Upload_Service SHALL log the error but NOT fail the upload operation
6. THE Upload_Service SHALL use the configured queue URL from application.properties

### Requisito 7: Preservar Arquitetura Hexagonal

**User Story:** Como desenvolvedor, eu quero manter a arquitetura hexagonal, para que o código permaneça testável e desacoplado.

#### Acceptance Criteria

1. THE Upload_Service SHALL maintain the separation between domain, application, and adapters layers
2. THE Upload_Service SHALL keep S3StorageService as a port interface in application.ports
3. THE Upload_Service SHALL keep SQSPublisherService as a port interface in application.ports
4. THE Upload_Service SHALL keep AwsS3StorageAdapter as an implementation in adapters.aws
5. THE Upload_Service SHALL keep AwsSqsPublisherAdapter as an implementation in adapters.aws
6. THE Upload_Service SHALL keep UploadVideoUseCase as an interface in application.usecases
7. THE Upload_Service SHALL keep UploadVideoUseCaseImpl as an implementation in application.usecases.impl

### Requisito 8: Remover Código Não Utilizado

**User Story:** Como desenvolvedor, eu quero remover código não utilizado relacionado ao slicing, para que o repositório seja mais limpo e manutenível.

#### Acceptance Criteria

1. THE Upload_Service SHALL NOT contain the Video domain class if it is only used for slicing
2. THE Upload_Service SHALL NOT contain the User domain classes (User, Email, Password) if they are not used
3. THE Upload_Service SHALL NOT contain the retrieve method in S3StorageService if it is only used for slicing
4. THE Upload_Service SHALL NOT contain empty .gitkeep files in package directories
5. THE Upload_Service SHALL remove any unused imports and dependencies

### Requisito 9: Atualizar Documentação

**User Story:** Como desenvolvedor, eu quero documentação atualizada, para que eu entenda o propósito e uso do serviço.

#### Acceptance Criteria

1. THE Upload_Service SHALL update README.md to describe the upload service purpose
2. THE Upload_Service SHALL document the API endpoint POST /api/video/upload in README.md
3. THE Upload_Service SHALL document required AWS configurations in README.md
4. THE Upload_Service SHALL document supported video formats in README.md
5. THE Upload_Service SHALL document the integration with SQS for downstream processing

### Requisito 10: Manter Configurações AWS

**User Story:** Como operador, eu quero configurar credenciais e recursos AWS, para que o serviço possa se conectar aos serviços necessários.

#### Acceptance Criteria

1. THE Upload_Service SHALL read AWS region from application.properties
2. THE Upload_Service SHALL read AWS credentials from application.properties
3. THE Upload_Service SHALL read S3 bucket name from application.properties
4. THE Upload_Service SHALL read S3 folder prefix from application.properties with default "videos/"
5. THE Upload_Service SHALL read SQS queue URL from application.properties
6. THE Upload_Service SHALL validate all required AWS configurations at startup using AwsConfigurationValidator

### Requisito 11: Preservar Tratamento de Erros

**User Story:** Como sistema, eu quero tratar erros adequadamente, para que falhas sejam registradas e comunicadas corretamente.

#### Acceptance Criteria

1. WHEN an exception occurs during upload, THE Upload_Service SHALL log the error with appropriate level
2. WHEN validation fails, THE Upload_Service SHALL return HTTP 400 with descriptive error message
3. WHEN S3 storage fails after retries, THE Upload_Service SHALL return HTTP 500 with error message
4. WHEN SQS publishing fails, THE Upload_Service SHALL log the error but continue with successful upload response
5. THE Upload_Service SHALL use SLF4J logger for all logging operations

### Requisito 12: Manter Limites de Upload

**User Story:** Como operador, eu quero configurar limites de upload, para que o sistema não aceite arquivos excessivamente grandes.

#### Acceptance Criteria

1. THE Upload_Service SHALL configure spring.servlet.multipart.max-file-size in application.properties
2. THE Upload_Service SHALL configure spring.servlet.multipart.max-request-size in application.properties
3. WHEN a Video_File exceeds the configured limit, THE Upload_Service SHALL return HTTP 400 with error message
4. THE Upload_Service SHALL use a default maximum file size of 500MB

