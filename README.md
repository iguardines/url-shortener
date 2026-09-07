# URL Shortener

Proyecto independiente con Java 21, Spring Boot 3.5, Maven, Spring Data JPA y H2 en memoria. No requiere Docker,
PostgreSQL ni Redis. Los datos se pierden al apagar la aplicación.

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

La suite incluye 21 pruebas de dominio, servicio, controlador e integración. Una prueba provoca una violación real de
unicidad en H2 y verifica que el siguiente intento se persiste correctamente. GitHub Actions ejecuta `mvn verify` con
Java 21 en cada push y pull request.

## Alcance de la demo

Esta API está pensada para mostrar diseño por capas, validación, persistencia y testing en un portfolio. H2 es volátil:
reiniciar la aplicación elimina los enlaces. No hay autenticación; cualquiera con acceso a la API puede consultar o
eliminar un enlace conociendo su código.

Comprobación de estado: `GET /actuator/health`. Documentación OpenAPI: `GET /v3/api-docs`.
