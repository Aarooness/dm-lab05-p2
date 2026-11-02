# Marketing Fácil

Aplicación Java 21 / Spring Boot para crear campañas de productos con plantillas locales, diseños PNG y un video MP4 sin audio. No usa IA generativa, servicios de pago ni integraciones de redes sociales.

## Iniciar en Windows o Linux con Docker

1. Copia `.env.example` como `.env` y cambia `POSTGRES_PASSWORD` y `SPRING_DATASOURCE_PASSWORD` por la misma contraseña segura.
2. Desde esta carpeta ejecuta:

   ```powershell
   docker compose up --build -d
   ```

3. Abre <http://localhost:8002>. El puerto se puede cambiar con `APP_PORT`.
4. Para detener: `docker compose down`. Los volúmenes con campañas, imágenes y videos se conservan. **No** uses `docker compose down -v` si quieres conservarlos.

No necesitas instalar Java ni Maven en Windows. Docker compila con Maven y Java 21 dentro de la imagen. PostgreSQL 16 no publica ningún puerto en el host.

## Flujo y funciones implementadas

- Carga JPG/PNG de hasta 10 MB; se valida el formato real y se limitan las dimensiones a 6000 px por lado y 20 megapíxeles.
- Crea guiones informativo, emocional y promocional mediante plantillas con los datos ingresados. No inventa precios, descuentos ni atributos.
- Después de elegir un guion, genera tres composiciones PNG (cuadrada, vertical y horizontal) con Java Graphics2D, imagen proporcional y texto ajustado.
- En segundo plano, con un ejecutor limitado a un trabajo simultáneo, FFmpeg produce un MP4 sin audio de aproximadamente 9 segundos. Los tres formatos se ajustan a un lienzo 1280×720 sin deformación. El límite de espera de FFmpeg es 120 segundos.
- Permite volver a abrir campañas, previsualizar y descargar los PNG, el MP4 y un archivo de texto para publicación manual.
- Guarda datos y estados en PostgreSQL y archivos bajo `/app/uploads`, respaldados por volúmenes persistentes de Docker.
- Publicación: el estado inicial es `PENDING_CONFIGURATION`. “Simular publicación” guarda `SIMULATED` e indica expresamente que no publicó en ninguna red. No existe una URL de publicación ni se presenta una publicación real.

La simulación y la descarga **no cumplen por sí solas** un requisito de publicación automática real. Para cumplirlo habrá que integrar una API oficial (por ejemplo, Instagram Graph API o TikTok Content Posting API), registrar/configurar una aplicación, obtener los permisos y credenciales aprobados, gestionar tokens y cumplir los requisitos de revisión y publicación de cada plataforma. La publicación real permanece deshabilitada hasta entonces; no se automatizan contraseñas ni sesiones del navegador.

## Variables de entorno

| Variable | Uso | Valor de ejemplo |
|---|---|---|
| `APP_PORT` | Puerto publicado en el host | `8002` |
| `POSTGRES_DB` | Base de datos | `marketing_facil` |
| `POSTGRES_USER` | Usuario PostgreSQL | `marketing_app` |
| `POSTGRES_PASSWORD` | Contraseña de PostgreSQL | Cambiar antes del despliegue |
| `SPRING_DATASOURCE_URL` | URL JDBC interna del servicio | `jdbc:postgresql://postgres:5432/marketing_facil` |
| `SPRING_DATASOURCE_USERNAME` | Usuario de la aplicación | `marketing_app` |
| `SPRING_DATASOURCE_PASSWORD` | Contraseña de la aplicación | La misma que `POSTGRES_PASSWORD` |
| `UPLOAD_DIR` | Carpeta de archivos en el contenedor | `/app/uploads` |

Hibernate usa `ddl-auto=update`: no elimina las tablas al reiniciar. El servicio de aplicación espera a que el healthcheck de PostgreSQL esté saludable.

### Si la aplicación se reinicia y no abre el puerto

Ejecuta `docker compose ps` y `docker compose logs --tail 100 app`. Si aparece `password authentication failed`, revisa que `POSTGRES_PASSWORD` y `SPRING_DATASOURCE_PASSWORD` tengan exactamente el mismo valor en `.env`. PostgreSQL solo aplica `POSTGRES_PASSWORD` cuando inicializa un volumen vacío; cambiar `.env` no cambia automáticamente la contraseña guardada en un volumen existente. Actualiza el rol `marketing_app` dentro de PostgreSQL para que coincida con `.env` y recrea solo la aplicación con `docker compose up -d --force-recreate app`. No elimines el volumen de PostgreSQL para resolver este problema, porque contiene las campañas guardadas.

## Publicar y desplegar manualmente en AWS

La imagen esperada es `aarooness0/marketing-lab5:latest`. No se publica automáticamente desde este proyecto.

1. Construye y etiqueta la imagen: `docker compose build app`.
2. Inicia sesión manualmente en Docker Hub con `docker login`.
3. Publica solo cuando estés listo: `docker push aarooness0/marketing-lab5:latest`.
4. En una instancia EC2 con Docker Engine y el plugin Docker Compose, copia únicamente `docker-compose.yml` y un `.env` creado allí. No es necesario copiar ni montar el código fuente.
5. Configura en `.env` las credenciales fuertes de PostgreSQL y ejecuta `docker compose pull app` y `docker compose up -d`.
6. Conserva los volúmenes Docker `marketing_postgres` y `marketing_uploads`; haz copias de seguridad regulares. Protege el puerto de la aplicación con el firewall/grupos de seguridad de AWS y configura TLS mediante un proxy inverso si se expone públicamente.

Los `.env` y archivos `.pem` están excluidos de Git y de la imagen Docker. El archivo `.env.example` solo contiene valores de ejemplo; nunca lo uses como conjunto de credenciales para producción.

## Alcance no implementado

No hay cuentas de usuario, pagos, IA generativa, publicación real, alojamiento de medios externo ni gestión de credenciales de plataforma. Para publicación real y uso en producción se requiere la integración oficial y el despliegue seguro descritos arriba.
