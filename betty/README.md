# Betty Barbearia

Agendamento online (Spring Boot + Maven, Java 21).

## Como abrir (IntelliJ)
1. File > Open > escolha a pasta `betty` (a que tem o `pom.xml`).
2. Project SDK = Java 21. Em Settings > Build Tools > Maven > Runner, "JRE" = Java 21.
3. Se aparecer a notificação do Maven, clique em "Load Maven Project" e espere terminar (ou use o botão de recarregar na aba Maven).
4. Abra `BettyApplication.java` e clique no play verde ao lado do `main`.
5. Navegador: http://localhost:8080 (cliente) e http://localhost:8080/painel (equipe).

## Logins de teste (TROQUE antes de publicar)
| Usuário | Senha | Acesso |
|---|---|---|
| dono | dono123 | agenda de todos, equipe, serviços |
| carlos / rafael / joao | carlos123 / rafael123 / joao123 | só a própria agenda |

Os dados ficam em memória: ao reiniciar o app, voltam ao padrão.

## Pelo terminal (precisa de Maven 3.9+ e JDK 21)
```
mvn spring-boot:run     # executa o app
mvn test                # roda os testes
mvn clean package       # gera target/betty-0.0.1-SNAPSHOT.jar
java -jar target/betty-0.0.1-SNAPSHOT.jar
```
