# Implementation Plan: AWS S3 e SQS Integration

## Overview

Este plano implementa a integração com Amazon S3 e Amazon SQS no sistema Video Slicer, seguindo arquitetura hexagonal. A implementação será incremental, começando pela camada de domínio, passando pelas portas de aplicação, configuração, adapters, e finalmente integrando tudo na camada de aplicação e controller.

## Tasks

- [x] 1. Configurar dependências e estrutura base
  - Adicionar dependências AWS SDK v2 (S3 e SQS) no pom.xml
  - Adicionar dependência jqwik para property-based testing
  - Criar estrutura de pacotes: domain/video, application/ports, adapters/aws, config
  - _Requirements: 4.1, 5.1_

- [ ] 2. Implementar Domain Layer
  - [x] 2.1 Criar classe VideoMetadata
    - Implementar value object com campos: videoId, fileName, extension, sizeInBytes, uploadTimestamp
    - Implementar método getS3Key() que retorna "{uuid}.{extension}"
    - _Requirements: 1.2, 1.3, 2.1, 2.3_
  
  - [x] 2.2 Escrever testes unitários para VideoMetadata
    - Testar construção de VideoMetadata com valores válidos
    - Testar método getS3Key() retorna formato correto
    - _Requirements: 1.2, 1.3_
  
  - [x] 2.3 Criar classe VideoValidation
    - Implementar validação de extensões suportadas (mp4, avi, mov, mkv, webm)
    - Implementar validação de tamanho máximo (500MB)
    - Implementar validação de arquivo vazio
    - Implementar classe interna ValidationResult
    - _Requirements: 8.1, 8.2, 8.3_
  
  - [x] 2.4 Escrever testes unitários para VideoValidation
    - Testar validação com arquivo MP4 válido de 100MB
    - Testar rejeição de arquivo vazio
    - Testar rejeição de arquivo com extensão não suportada
    - Testar rejeição de arquivo acima de 500MB
    - Testar arquivo exatamente no limite de 500MB
    - _Requirements: 8.1, 8.2, 8.3, 8.4_

- [ ] 3. Implementar Application Ports (interfaces)
  - [x] 3.1 Criar interface S3StorageService
    - Definir método store(VideoMetadata, InputStream) que retorna String (URL)
    - Definir método retrieve(String videoId, String extension) que retorna InputStream
    - Definir classe interna StorageException
    - _Requirements: 1.1, 1.4_
  
  - [x] 3.2 Criar interface SQSPublisherService
    - Definir método publish(VideoMetadata) que retorna void
    - Definir classe interna PublishException
    - _Requirements: 3.1_

- [ ] 4. Implementar Configuration Layer
  - [x] 4.1 Criar classe AwsConfiguration
    - Configurar bean AwsCredentialsProvider (priorizar variáveis de ambiente)
    - Configurar bean S3Client com região e credenciais
    - Configurar bean SqsClient com região e credenciais
    - Configurar bean ObjectMapper para serialização JSON
    - _Requirements: 4.1, 4.2, 4.3, 5.3_
  
  - [x] 4.2 Criar classe AwsConfigurationValidator
    - Implementar validação de aws.region na inicialização
    - Implementar validação de aws.s3.bucket-name na inicialização
    - Implementar validação de aws.sqs.queue-url na inicialização
    - Lançar IllegalStateException com mensagem descritiva se configurações ausentes
    - _Requirements: 4.4, 4.5, 5.5_
  
  - [x] 4.3 Adicionar propriedades no application.properties
    - Adicionar propriedades: aws.region, aws.access-key-id, aws.secret-access-key
    - Adicionar propriedades: aws.s3.bucket-name, aws.s3.folder-prefix
    - Adicionar propriedade: aws.sqs.queue-url
    - _Requirements: 4.1, 5.1, 5.2, 5.4_
  
  - [x] 4.4 Escrever testes unitários para AwsConfigurationValidator
    - Testar falha na inicialização sem aws.region
    - Testar falha na inicialização sem aws.s3.bucket-name
    - Testar falha na inicialização sem aws.sqs.queue-url
    - Testar sucesso com todas as configurações presentes
    - _Requirements: 4.5, 5.5_

- [ ] 5. Checkpoint - Validar estrutura base
  - Ensure all tests pass, ask the user if questions arise.

- [x] 6. Implementar AwsS3StorageAdapter
  - [x] 6.1 Criar classe AwsS3StorageAdapter implementando S3StorageService
    - Injetar S3Client, bucket-name e folder-prefix via construtor
    - Implementar método store() com upload para S3
    - Implementar retry com backoff exponencial (3 tentativas)
    - Implementar método getContentType() para mapear extensões
    - Implementar método retrieve() para download do S3
    - Adicionar logging de todas as operações e tentativas
    - _Requirements: 1.1, 1.2, 1.3, 1.5, 6.1, 6.3, 6.5_
  
  - [x] 6.2 Escrever testes unitários para AwsS3StorageAdapter
    - Mockar S3Client usando Mockito
    - Testar upload bem-sucedido retorna URL no formato correto
    - Testar falha após 3 tentativas lança StorageException
    - Testar sucesso na 2ª tentativa após falha na 1ª
    - Testar retrieve de vídeo existente retorna InputStream
    - _Requirements: 1.1, 1.4, 6.1, 6.3_
  
  - [x] 6.3 Escrever property test para S3 Key Format
    - **Property 1: S3 Key Format**
    - **Validates: Requirements 1.2, 1.3, 1.5, 2.1**
    - Gerar VideoMetadata aleatórios com extensões válidas
    - Verificar que chave S3 segue formato "{prefix}/{uuid}.{extension}"
    - Executar 100 iterações
    - _Requirements: 1.2, 1.3, 1.5_
  
  - [x] 6.4 Escrever property test para S3 Retry Mechanism
    - **Property 6: S3 Retry Mechanism**
    - **Validates: Requirements 6.1**
    - Simular falhas retryable no S3Client
    - Verificar que exatamente 3 tentativas são feitas
    - Verificar backoff exponencial entre tentativas
    - Executar 100 iterações
    - _Requirements: 6.1_

- [ ] 7. Implementar AwsSqsPublisherAdapter
  - [x] 7.1 Criar classe AwsSqsPublisherAdapter implementando SQSPublisherService
    - Injetar SqsClient, queue-url e ObjectMapper via construtor
    - Implementar método publish() com envio para SQS
    - Implementar retry com backoff exponencial (3 tentativas)
    - Implementar método createMessageBody() para serializar JSON
    - Adicionar logging de todas as operações e tentativas
    - _Requirements: 3.1, 3.2, 3.3, 3.5, 6.2, 6.5_
  
  - [x] 7.2 Escrever testes unitários para AwsSqsPublisherAdapter
    - Mockar SqsClient usando Mockito
    - Testar publicação bem-sucedida não lança exceção
    - Testar falha após 3 tentativas lança PublishException
    - Testar mensagem gerada é JSON válido com campos corretos
    - _Requirements: 3.1, 3.5, 6.2_
  
  - [x] 7.3 Escrever property test para SQS Message Structure
    - **Property 3: SQS Message Structure**
    - **Validates: Requirements 3.2, 3.3, 3.5**
    - Gerar VideoMetadata aleatórios
    - Verificar que mensagem JSON contém: videoId, fileName, extension, sizeInBytes, uploadTimestamp
    - Verificar que videoId é UUID válido e uploadTimestamp é ISO-8601
    - Executar 100 iterações
    - _Requirements: 3.2, 3.3, 3.5_
  
  - [x] 7.4 Escrever property test para SQS Retry Mechanism
    - **Property 7: SQS Retry Mechanism**
    - **Validates: Requirements 6.2**
    - Simular falhas retryable no SqsClient
    - Verificar que exatamente 3 tentativas são feitas
    - Verificar backoff exponencial entre tentativas
    - Executar 100 iterações
    - _Requirements: 6.2_

- [ ] 8. Checkpoint - Validar adapters
  - Ensure all tests pass, ask the user if questions arise.

- [x] 9. Modificar Application Layer
  - [x] 9.1 Atualizar interface UploadVideoUseCase
    - Adicionar campos videoId e videoUrl na classe interna UploadResult
    - Manter campo fileName para retrocompatibilidade
    - _Requirements: 2.2, 7.2, 7.3, 9.3, 9.4_
  
  - [x] 9.2 Modificar classe UploadVideoUseCaseImpl
    - Injetar S3StorageService e SQSPublisherService via construtor
    - Implementar validação usando VideoValidation.validate()
    - Implementar geração de UUID para videoId
    - Implementar criação de VideoMetadata
    - Implementar chamada para s3StorageService.store()
    - Implementar chamada para sqsPublisherService.publish() com try-catch
    - Implementar política: falha no SQS não impede sucesso do upload
    - Adicionar logging de operações e erros
    - _Requirements: 1.1, 2.1, 2.3, 3.1, 3.4, 6.4, 6.5, 8.5_
  
  - [x] 9.3 Escrever testes unitários para UploadVideoUseCaseImpl
    - Mockar S3StorageService e SQSPublisherService
    - Testar upload de vídeo válido retorna success=true com videoId e videoUrl
    - Testar upload com arquivo vazio retorna success=false
    - Testar falha no S3 retorna success=false
    - Testar falha no SQS não impede sucesso do upload
    - _Requirements: 1.1, 3.1, 3.4, 6.4, 8.4_
  
  - [~] 9.4 Escrever property test para Video ID Consistency
    - **Property 2: Video ID Consistency**
    - **Validates: Requirements 2.4**
    - Gerar MultipartFile aleatórios válidos
    - Executar upload e capturar resultado
    - Verificar que videoId retornado corresponde ao nome base do arquivo no S3
    - Executar 100 iterações
    - _Requirements: 2.4_
  
  - [~] 9.5 Escrever property test para Upload Response Structure
    - **Property 4: Upload Response Structure**
    - **Validates: Requirements 7.1, 7.2, 7.3, 7.4, 9.3, 9.4**
    - Gerar MultipartFile aleatórios válidos
    - Executar upload e capturar UploadResult
    - Verificar estrutura: success=true, message não vazio, videoId UUID válido, videoUrl URL válida, fileName não vazio
    - Executar 100 iterações
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 9.3, 9.4_
  
  - [~] 9.6 Escrever property test para Validation Rejection
    - **Property 5: Validation Rejection**
    - **Validates: Requirements 8.1, 8.2, 8.3, 8.4**
    - Gerar arquivos inválidos: vazios, extensão não suportada, acima de 500MB
    - Executar upload e capturar resultado
    - Verificar que success=false e mensagem de erro é descritiva
    - Executar 100 iterações
    - _Requirements: 8.1, 8.2, 8.3, 8.4_

- [x] 10. Modificar Controller Layer
  - [x] 10.1 Atualizar classe VideoController
    - Modificar método upload() para retornar estrutura com content contendo fileName, videoId e videoUrl
    - Implementar mapeamento de erros de validação para HTTP 400
    - Implementar mapeamento de erros de servidor para HTTP 500
    - Manter sucesso como HTTP 201
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 9.3_
  
  - [x] 10.2 Escrever testes de integração para VideoController
    - Usar @SpringBootTest e MockMvc
    - Mockar S3StorageService e SQSPublisherService
    - Testar POST /api/video/upload com arquivo válido retorna 201
    - Testar POST com arquivo inválido retorna 400
    - Testar POST com falha no S3 retorna 500
    - Verificar estrutura da resposta JSON contém fileName, videoId e videoUrl
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_

- [x] 11. Implementar generators para property-based testing
  - [x] 11.1 Criar classe TestGenerators com Arbitraries customizados
    - Implementar generator validVideos() para VideoMetadata válidos
    - Implementar generator invalidFiles() para arquivos inválidos
    - Implementar generator validMultipartFiles() para MultipartFile válidos
    - _Requirements: Suporte para todos os property tests_

- [~] 12. Checkpoint final - Validação completa
  - Ensure all tests pass, ask the user if questions arise.

- [x] 13. Verificar retrocompatibilidade
  - [x] 13.1 Validar endpoint /api/video/slice continua funcional
    - Executar testes existentes do SliceVideoUseCase
    - Verificar que nenhuma funcionalidade existente foi quebrada
    - _Requirements: 9.1, 9.2_
  
  - [x] 13.2 Validar estrutura de resposta mantém campo fileName
    - Verificar que resposta de upload contém campo fileName
    - Verificar que campo fileName está no formato esperado
    - _Requirements: 9.3, 9.4_

## Notes

- Tarefas marcadas com `*` são opcionais e podem ser puladas para MVP mais rápido
- Cada tarefa referencia requisitos específicos para rastreabilidade
- Checkpoints garantem validação incremental
- Property tests validam propriedades universais de corretude
- Unit tests validam exemplos específicos e casos extremos
- Implementação segue ordem recomendada no design: Domain → Ports → Config → Adapters → Application → Controller
- Política de resiliência: falha no SQS não impede sucesso do upload (vídeo já está no S3)
- Todos os property tests devem executar no mínimo 100 iterações
