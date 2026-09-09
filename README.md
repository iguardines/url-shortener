# URL Shortener

Proyecto con Java 21, Spring Boot 3.5, Maven y Spring Data JPA. Usa H2 en memoria en local y PostgreSQL en producción. Flyway administra el esquema en ambos motores. La ejecución local no requiere Docker.

## Ejecutar

Desde esta carpeta, con Java 21 y Maven instalados. Verificá que `mvn -version` muestre Java 21.
En macOS, si tenés varias versiones instaladas:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export PATH="$JAVA_HOME/bin:$PATH"
```

Iniciar la aplicación:

```bash
mvn spring-boot:run
```

La API escucha en http://localhost:8080. Swagger UI: http://localhost:8080/swagger-ui.html.

Para compilar y ejecutar el JAR:

```bash
mvn clean package
java -jar target/url-shortener-0.0.1-SNAPSHOT.jar
```

## Separación de capas

- **web**: controlador REST, DTOs, validación de entradas y manejo de errores.
- **application**: servicio de casos de uso y contratos de persistencia.
- **domain**: modelo de enlace, reglas de URL y generador aleatorio Base62.
- **infrastructure**: adaptador JPA, entidad de base de datos y configuración.

El controlador llama al servicio; el servicio utiliza el contrato de repositorio; el adaptador JPA persiste en H2. El
dominio no depende de JPA.

## Uso

Crear un enlace:

```bash
curl -i -X POST http://localhost:8080/api/v1/urls \
  -H 'Content-Type: application/json' \
  -d '{"url":"https://example.com/ruta/muy/larga","customAlias":"ejemplo"}'
```

La respuesta incluye `shortCode`, `shortUrl`, `originalUrl`, fechas y contador de visitas. Se puede omitir `customAlias`
para generar un código aleatorio. El alias admite entre 4 y 32 letras, números, guiones y guiones bajos. El campo
opcional `expiresAt` acepta una fecha futura ISO-8601 en UTC.

| Método | Ruta                     | Resultado                        |
|--------|--------------------------|----------------------------------|
| POST   | /api/v1/urls             | Crea un enlace (201)             |
| GET    | /{shortCode}             | Redirige a la URL original (302) |
| GET    | /api/v1/urls/{shortCode} | Consulta metadatos y visitas     |
| DELETE | /api/v1/urls/{shortCode} | Elimina el enlace (204)          |

```bash
curl -i http://localhost:8080/ejemplo
curl http://localhost:8080/api/v1/urls/ejemplo
curl -i -X DELETE http://localhost:8080/api/v1/urls/ejemplo
```

Solo se permiten URLs HTTP/HTTPS con host y un máximo de 2048 caracteres. Los alias `actuator`, `swagger-ui` y `error`
están reservados. Errores: entrada inválida (400), código inexistente (404), alias ocupado (409) y enlace vencido (410).

Se puede configurar `BASE_URL` para cambiar el origen de los enlaces devueltos y `SERVER_PORT` para cambiar el puerto
del servidor.

## Pruebas

```bash
mvn verify
```

Incluye pruebas unitarias de dominio y servicio, pruebas del controlador y pruebas de integración con H2 que verifican
creación, redirección, conteo, eliminación y validaciones.

## Decisiones técnicas

- Códigos Base62 de 8 caracteres generados con `SecureRandom`.
- Restricción única en H2 como garantía final de unicidad. Cada inserción usa una transacción independiente para poder
  reintentar una colisión sin reutilizar una transacción fallida.
- Incremento de visitas mediante un `UPDATE` atómico en la base de datos.
- Redirecciones con `Cache-Control: no-store` para evitar que el navegador omita nuevas visitas al servidor.
- `Clock` inyectable para probar el vencimiento sin esperas.
- Contrato de caché con implementación vacía: todas las consultas se resuelven con H2.
- Errores JSON consistentes, incluida una petición JSON malformada.

La suite incluye pruebas de dominio, servicio, controlador e integración. Una prueba provoca una violación real de
unicidad en H2 y verifica que el siguiente intento se persiste correctamente. GitHub Actions ejecuta `mvn verify` con
Java 21 y PostgreSQL con Testcontainers en cada push y pull request.

## Alcance de la demo

Esta API está pensada para mostrar diseño por capas, validación, persistencia y testing en un portfolio. En local, H2 es volátil: reiniciar la aplicación elimina los enlaces. En producción, PostgreSQL conserva los enlaces mientras se conserve la base. No hay autenticación; cualquiera con acceso a la API puede consultar o
eliminar un enlace conociendo su código.

Comprobación de estado: `GET /actuator/health`. Documentación OpenAPI: `GET /v3/api-docs`.

## CD en Render

El repositorio incluye `Dockerfile` y `render.yaml`. GitHub Actions verifica el código y Render despliega
los nuevos commits de `master` cuando pasan los checks (`autoDeployTrigger: checksPass`). Los pull requests
se prueban sin desplegarse al servicio principal. El primer despliegue al crear el servicio se inicia durante
su configuración; comprobá que el CI esté verde antes de crearlo.

1. Subí estos archivos a GitHub y esperá que **Java CI** termine correctamente.
2. En Render elegí **New + → Blueprint**, conectá tu cuenta de GitHub y seleccioná este repositorio.
3. Elegí la rama `master` y el archivo `render.yaml`. Revisá que la instancia sea **Free** y creá el servicio.
4. En el servicio verificá **Settings → Auto-Deploy → After CI Checks Pass**.
5. Esperá el estado **Live** en Render y abrí `/swagger-ui/index.html` en la URL pública del servicio.

Si ya tenés este Web Service creado, configurá Docker, rama `master`, Dockerfile `./Dockerfile`,
health check `/actuator/health`, variable `SERVER_FORWARD_HEADERS_STRATEGY=framework` y auto-deploy
**After CI Checks Pass** en ese servicio, sin crear otro. Conectá el repositorio mediante la integración
GitHub de Render; usar solamente una URL pública de Git no habilita este flujo automático.

Render proporciona `PORT` y `RENDER_EXTERNAL_URL`: la aplicación los usa para escuchar en el puerto asignado
y devolver enlaces públicos HTTPS. `BASE_URL` permite sobrescribir el origen si agregás un dominio propio.
No hacen falta deploy hooks ni secretos de Render en GitHub. El resultado del CI se ve en GitHub Actions;
el estado real del despliegue y los logs de arranque se ven en Render, en **Events** y **Logs**.

El Dockerfile omite los tests durante el empaquetado porque se ejecutan antes en CI. Para probar la imagen local:

```bash
docker build -t url-shortener .
docker run --rm -p 8080:8080 url-shortener
```

El plan del workspace Hobby es independiente del tipo de instancia. Este Blueprint solicita explícitamente
una instancia Free. Los servicios Free se suspenden tras 15 minutos sin tráfico y pueden tardar en reactivarse.
Con el perfil prod, los enlaces permanecen en PostgreSQL después de reinicios y despliegues del Web Service. La disponibilidad y retención de la base dependen del proveedor y su plan.

Referencias: [despliegues](https://render.com/docs/deploys),
[Blueprint](https://render.com/docs/blueprint-spec), [límites Free](https://render.com/docs/free).

## Perfiles y PostgreSQL

- Sin variables adicionales se activa `local`: H2 en memoria.
- `SPRING_PROFILES_ACTIVE=prod`: PostgreSQL, sin fallback a H2. Requiere `DB_URL`, `DB_USERNAME` y `DB_PASSWORD`, sin valores de respaldo.
- Los tests H2 fijan explícitamente `local` para evitar usar una base real configurada en el entorno.
- El repositorio y el adaptador JPA son compartidos por ambos motores.

En Render → Web Service → Environment configurá:

| Variable | Valor |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `DB_URL` | `jdbc:postgresql://HOST:5432/BASE` |
| `DB_USERNAME` | Usuario de la base |
| `DB_PASSWORD` | Contraseña de la base |

Usá las opciones SSL indicadas por tu proveedor en la URL JDBC. No pegues una URL `postgresql://` directamente:
el driver requiere `jdbc:postgresql://`. Las credenciales se cargan por separado. No las subas al repositorio.
Si usás Blueprint, `render.yaml` solicita URL, usuario y contraseña mediante `sync: false`; no crea una base de datos.
Configurá las variables antes de desplegar este cambio en producción. Los enlaces existentes de H2 no se transfieren
a PostgreSQL automáticamente.

Flyway ejecuta `db/migration/V1__create_short_urls.sql` al arrancar sobre una base vacía. Hibernate usa `validate`,
no `create-drop`, y Flyway tiene `clean` deshabilitado. Las siguientes modificaciones del esquema deben agregarse
como nuevos archivos `V2__...sql`, sin editar migraciones ya aplicadas. Para una base con tablas previas, revisá su
esquema antes de migrar; no actives un baseline automático para ocultar diferencias.

Tests rápidos (sin Docker):

```bash
mvn verify
```

Suite completa con PostgreSQL temporal (requiere Docker activo):

```bash
mvn verify -Ppostgres-tests
```

El perfil Maven `postgres-tests` es distinto del perfil Spring `prod`. Failsafe ejecuta los tests `*IT` contra
un contenedor aislado con credenciales de prueba, sin conectarse a tu base de Render. GitHub Actions ejecuta esta
suite completa antes del despliegue.


## Eventos para Analytics

El servicio independiente se encuentra en la carpeta hermana `../url-shortener-analytics`. No existe dependencia Maven entre ambos repositorios ni acceso cruzado a tablas. Este productor declara sus propios records bajo `infrastructure/kafka/event`.

Publica `ShortUrlCreatedEvent` en `short-url-created.v1` después de persistir y `ShortUrlVisitedEvent` en `short-url-visited.v1` después del commit de la visita, incluso cuando hay cache hit. El redirect consulta identidad/expiración en DB también en cache hit; esta consulta adicional permite publicar el identificador correcto sin ampliar el contrato del cache.

Payload JSON: `eventId` UUID aleatorio por hecho, `shortUrlId` UUID estable, `shortCode`, `occurredAt` ISO-8601 UTC; creación agrega `originalUrl`. La key Kafka es `shortUrlId`. El UUID se deriva del Long existente con `UUID.nameUUIDFromBytes(("url-shortener:" + id).getBytes(UTF_8))`; no cambia la PK ni la API. Si se unen varias instalaciones con secuencias de IDs independientes, usar un namespace distinto por productor. No reiniciar la secuencia de IDs conservando eventos históricos.

Arranque local: levantar Kafka con el Compose del repositorio Analytics y ejecutar este Shortener normalmente. `KAFKA_EVENTS_ENABLED` vale `true` por defecto; usar `false` para operar sin Kafka. Tests existentes deshabilitan la integración externa explícitamente, y las pruebas del adaptador verifican JSON y publicación después del commit.

Variables: `KAFKA_BOOTSTRAP_SERVERS` (default local `localhost:9092`), `KAFKA_TOPIC_CREATED`, `KAFKA_TOPIC_VISITED`. En perfil `prod` configurar además `KAFKA_USERNAME`, `KAFKA_PASSWORD`, `KAFKA_CA_CERTIFICATE` (contenido PEM con saltos de línea reales). Usa SASL_SSL + SCRAM-SHA-256 y verificación de hostname. Si una credencial contiene comillas o barras invertidas, configurar `SPRING_KAFKA_PROPERTIES_SASL_JAAS_CONFIG` como secreto con escape JAAS correcto. Crear topics previamente; este productor no administra el clúster.

Publicación best effort: `acks=all` e idempotencia del productor no eliminan la ventana de pérdida entre commit SQL y envío Kafka. Los fallos se registran en logs sin revertir una operación ya confirmada. La espera por metadatos Kafka se limita a tres segundos; el envío asíncrono puede fallar después. Pendiente Transactional Outbox, replay operativo y backfill para enlaces anteriores a habilitar eventos. Solo se publican creación y visita; eliminación queda para una extensión posterior. No se considera garantía de entrega exactamente una vez ni implementación completamente production-ready.

Para probar ambos servicios, con Analytics en `8081` y Shortener en `8080`:

```bash
curl -i -X POST http://localhost:8080/api/v1/urls -H 'Content-Type: application/json' -d '{"url":"https://example.com","customAlias":"demo1234"}'
curl -i http://localhost:8080/demo1234
curl -fsS http://localhost:8081/api/analytics/urls/demo1234
```

La estadística converge de forma asíncrona. Crear un alias nuevo si `demo1234` ya existe.


### Ejecución dentro del stack de Analytics

El Compose opcional `../url-shortener-analytics/docker-compose.stack.yml` agrega este servicio y su PostgreSQL persistente. Desde Analytics: `docker compose -p url-shortener-analytics -f docker-compose.yml -f docker-compose.stack.yml up --build -d`.

El puerto del Shortener sigue siendo 8080. PostgreSQL se publica en 5434, base/usuario `shortener`, contraseña de desarrollo `shortener-local`. Las URLs previas en H2 no se migran automáticamente.

`APP_EVENTS_ID_NAMESPACE` configura el prefijo para derivar UUID de eventos. Por defecto conserva `url-shortener`; la base nueva del stack usa `url-shortener-postgres-local` para evitar colisiones con datos históricos de H2. No cambiar el namespace de una base existente ni reutilizarlo para una base nueva que reinicie sus IDs si se conservan sus eventos históricos.
