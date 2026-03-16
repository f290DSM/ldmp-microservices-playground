# Edge Server

Muitas vezes nós estruturamos micro serviços que centralizem o acesso externo, protegendo serviços externos; por exemplo, um e-commerce possui uma infra-estrutura distribuiída e escalável, porém o ponto acesso às operações é único, protegendo seu variados micro-serviços que o constituem, este é o Design Pattern **Edge Server**.

```mermaid
flowchart TD
    cli(Client)
    subgraph MicroService Context
        es(Edge Server)
        a(Microservice A)
        b(Microservice B)
        c(Microservice C)
        d(Microservice D)
        e(Microservice E)
        f(Microservice F)
    end

        cli --> es
        es --> b
        b --> a
        b --> c
        b --> e
        a --> d
        e --> f
```

> O Edge Server atua como um proxy reverso e é integrado ao **Servvice Discovery** para prover o balanceamento de carga.

# Criando o Edge Server

1. Dentro do diretorio `spring-cloud` crie um projeto Spring com a dependencias: * `Actuator` e `Reative Gateway`;
2. Renomeie o arquivo `main\resources\application.properties` para `application.yml`;
3. No arquivo `application.yml` inclua as configurações.

```yml
server:
  port: 8765

spring:
  application:
    name: edge-server
  cloud:
    gateway:
      server:
        webflux:
          discovery:
            locator:
              enabled: true
              lower-case-service-id: true

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka
    fetch-registry: true
    register-with-eureka: true

```

> Acima configuramos a porta padrão do servidor e ativamos a busca e registro para com o Service Discovery

## Criando o roteamento e balanceamento de carga.

Agora que nossos microserviços estão devidamente registrados ao **Eureka Server**, vamos configurar o balanceamento de carga para nossos mricro-serviços.

### Configurando o Load Balancer

> Nesta seção, configuramos a captura das requisições ao micro serviço **clientms** para com o end-point **/clients/**, assim todas as requisições para este end-point serão balanceadas para as instancias dos micro serviços  disponíveis. Como temos 3 instancias, cada instancia receberá uma parte da carga das requisições.

O arquivo final ficará desta forma.

```yml
server:
  port: 8765

spring:
  application:
    name: edge-server
  cloud:
    gateway:
      server:
        webflux:
          discovery:
            locator:
              enabled: true
              lower-case-service-id: true

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka
    fetch-registry: true
    register-with-eureka: true
```

#### Configurando o roteamento via Java
Agora iremos realizar as configuracoes de roteamento via `.YML`.

> Inclua no arquivo de configurações as configuraões abaixo.

```yml
server:
  port: 8765

spring:
  application:
    name: edgeserver
  cloud:
    gateway:
      server:
        webflux:
          discovery:
            locator:
              enabled: true
              lower-case-service-id: true
          routes:
            - id: product-service
              uri: lb://product-service
              predicates:
                - Path=/products/**
eureka:
  client:
    register-with-eureka: true
    fetch-registry: true
    service-url:
      defaultZone: http://localhost:8761/eureka
management:
  endpoints:
    web:
      exposure:
        include: "*"
  endpoint:
    health:
      show-details: always
```

Observe que no trecho abaixo, estamos configurando o balanceamento de carga para as instancias do micro serviço de clientes.
```yml
          routes:
            - id: product-service # Identificador de micro serviço
              uri: lb://product-service # Balanceamento de carga com base no id do micrp serviço
              predicates:
                - Path=/products/** # Configuração dos paths com os identificadores dos resources da API
``` 

#### Inicie o Edge Server e confirme seu status no Eureka Service Discovery.

### Alterando o product-service

Precisamos ajustar os ids e as portas dos micro serviços dos clients para serem roteados e balanceados.

1. Para todas instancias de **product-service**;
2. Remova o **id** e o número de porta **0** do `application.yml`, conform o exemplo abaixo:

```yml
spring:
  application:
    name: product-service

eureka:
  client:
    serviceUrl:
      defaultZone: http://localhost:8761/eureka/
    initialInstanceInfoReplicationIntervalSeconds: 5
    registryFetchIntervalSeconds: 5
  instance:
    leaseRenewalIntervalInSeconds: 5
    leaseExpirationDurationInSeconds: 5  
    # instance-id: ${spring.application.name}:${spring.application.instance_id:${random.value}}
    
# server:
  # port: 0
```

> Voce pode comentar ou remover, em outro momento iremos fixar as portas quando até entrarmos em detalhes sobre o Docker.

# Testando o balanceamento de carga

Neste ponto temos o seguinte cenário:
* Eureka ativo;
* Edge Server ativo;

Precisamos subir 3 instancias do micros-erviço, realizar várias requisições e validar se elas foram balanceadas para os 3 micro serviços.

## Subindo as instancias

Você precisará de 6 terminais em abertos para realizar os testes, então siga os steps abaixo atentamente.

1. Pare todas os projetos ativos;
2. Inicialize o **Service Discovery**, no primeiro terminal, a partir da raiz do `eureka-server`;
```yml
.\gradlew bootRun
```
3. Inicialize o **Edge Server**, no segundo terminal, a partir da raiz do `edge-server`;
```yml
.\gradlew bootRun
```
4. Inicialize a **Primeira Instancia de Product Service**, no terceiro terminal, a partir da raiz do `clientms`;
```yml
.\gradlew bootRun --args='--server.port=8001'
```
5. Inicialize a **Segunda Instancia de Product Service**, no quarto terminal, a partir da raiz do `clientms`;
```yml
.\gradlew bootRun --args='--server.port=8002'
```
6. Inicialize a **Terceira Instancia de Product Service**, no quinto terminal, a partir da raiz do `clientms`;
```yml
.\gradlew bootRun --args='--server.port=8003'
```
7. Visualize as 3 instancias do `product-service` e `edge-server` no `eureka-sever`.

## Validando o balanceamento

Em um outro terminal, iremos disparar as requisições; então será necessário um pouco de destreza para enviar pelo menos umas 20 requisições o mais rápido que puder e verificar o **IP** retornado por cada uma das instancias.

### Resultado esperado

Como resultado, é esperado que enviemos todas as requisições para a url `http://localhost:8765/products` e que as 3 instancias retornem o processamento das requisições, você poderá conferir pelo número da porta que a requisição retornou.

```shell
curl "http://localhost:8765/products"
{"id":1,"name":"Product 1","description":"Product description id:1"}

curl "http://localhost:8765/products"
{"id":1,"name":"Product 1","description":"Product description id:2"}

curl "http://localhost:8765/products"
{"id":1,"name":"Product 1","description":"Product description id:3"}

curl "http://localhost:8765/products"
{"id":1,"name":"Product 1","description":"Product description id:4"}

curl "http://localhost:8765/products"
{"id":1,"name":"Product 1","description":"Product description id:5"}

curl "http://localhost:8765/products"
{"id":1,"name":"Product 1","description":"Product description id:6"}
```

# Parabéns, vocâ acabou de implementar mais um Design Pattern para Micro Serviços, o Edge Server.

O Edge Server irá esconder as 3 instancias dos micro serviços de clientes atrás do IP do gateway.

Todas as chamadas serão realizadas ao Edge Server na porta 8765.

Cada recurso será acessado com o prefixo do micro serviço registrado no Eureka Server, então para acessar qualquer uma das 3 instancias devemos acessar o end-point.

```shell
http://localhost:8765/product-service/products
```

> O Edge Server se encarregará do balanceamento e redirecionamento das requisições para os devidos micro serviços do ecossistema.

# Configurando o MonoRepo

> Agora chega de criar terminais isolados, vamos configurar o nosso projeto para que possamos realizar quaisquer testes e configurações à partir de uma estrutura centralizada e controlada pelo Gradle.`

Na raiz da pasta `ldmp_microservices` voce precisará incluir alguns arquivos gradle para poder tratar as dependencias do MonoRepo.

1. Copie os arquivos listados abaixo do projeto `product-service`:
* Diretório gradle;
* Arquivp gradlew;
* Arquivo gradlew.bat;
* Arquivo settings.gradle;

2. Edite o arquivo settings.gradle e adicione os projetos abaixo:

```gradle
rootProject.name = 'ldmp_microservices'
include 'spring-cloud:eureka-server'
include 'spring-cloud:edge-server'
include 'microservices:product-service'
```
3. Clique no logo do Gradle e selecione a opção `Refresh`;
4. Compile os projetos a partir da raiz ldmp_microservices:
```gradle
./gradlew build
```
 
 5. Abra a Guia de Services e execute as seguintes ações;
 * Edite a configuração ProductServiceApplication;
 * Renomeie-a para ProductServiceApplication1;
 * Clique em `Modify options` e adicione a opção `Add VM Options`;
 * Adicione a opção `-Dserver.port=8001`;

 6. Crie um cópia de ProductServiceApplication1 e renomeie-a para ProductServiceApplication2;
 * Altere a opção `-Dserver.port=8002`;

 7. Crie um cópia de ProductServiceApplication2 e renomeie-a para ProductServiceApplication3;
 * Altere a opção `-Dserver.port=8003`;
 
 ## Inciando os projetos pela IDE.

 Agora voce pode inicializar os projetos pela IDE ao invés dos terminais.

