# Política de privacidad – Lucky Prospector

**Última actualización:** 29 de septiembre de 2026

Esta información se facilita conforme al art. 13 del Reglamento (UE) 2016/679 («RGPD»). Explica qué datos trata la aplicación Android Lucky Prospector (`com.luckyprospector.app`), por qué y durante cuánto tiempo. El texto de referencia es la versión italiana; en caso de duda, prevalece esta.

## 1. Responsable del tratamiento

Daniele Bulgarelli, Reggio Emilia, Italia. Contacto de privacidad: intervistaconleai@gmail.com.

## 2. En resumen

- Puedes buscar y guardar tréboles sin cuenta y sin conexión.
- Las fotos, los deseos y tu posición precisa se quedan en tu teléfono.
- Solo se publica en línea lo que decides compartir en el mapa compartido: el tipo de hallazgo, una zona redondeada a al menos 100 m y, si quieres, una foto recortada sin datos GPS.
- La aplicación no contiene publicidad ni herramientas de análisis o seguimiento.

## 3. Datos que se quedan en tu dispositivo

El responsable no recibe estos datos y no puede leerlos.

- **Imágenes de la cámara.** Durante la búsqueda, el modelo de reconocimiento (TensorFlow Lite/LiteRT) analiza las imágenes de la cámara en el teléfono. Las imágenes no se guardan ni se envían. Tus fotos no se usan para entrenar el modelo.
- **Hallazgos.** Para cada hallazgo guardado, la aplicación conserva en una base de datos local: fecha y hora, tipo (trébol de cuatro hojas o grupo de tréboles), tu deseo opcional, la posición precisa y su exactitud (solo si has permitido la ubicación), el resultado del reconocimiento (clase, confianza, versión del modelo), una posible corrección de la clase y el estado de compartición.
- **Fotos.** Cada foto se guarda en el almacenamiento privado de la aplicación y **se copia en la galería del teléfono** («Pictures/Lucky Prospector»). La copia de la galería es visible para otras aplicaciones con acceso a las fotos y para los servicios de copia de seguridad que hayas activado. Puede quedarse en la galería tras desinstalar la aplicación.
- **Sesión y caché.** Si inicias sesión, el SDK de Firebase conserva en el teléfono la sesión y una caché de los datos del mapa compartido.

La copia de seguridad automática de Android está desactivada para la aplicación, así que estos datos no se copian en la copia de seguridad en la nube del sistema. Puedes eliminar un hallazgo en la aplicación: se borran el registro, la foto privada y la copia de la galería. Si el hallazgo estaba compartido, también se retira el pin público.

## 4. Datos tratados en línea

### 4.1 Inicio de sesión con Google (opcional)

Iniciar sesión solo es necesario para publicar en el mapa compartido, verlo y enviar sugerencias. Se hace a través de Google (Credential Manager) y Firebase Authentication. Google recibe la solicitud de inicio de sesión y nos transmite un identificador de tu cuenta. Firebase conserva un identificador de usuario (UID), tu dirección de correo, el nombre y la foto de tu perfil de Google, datos técnicos de acceso (fechas de creación y del último acceso) y tokens de sesión. La aplicación muestra tu nombre y tu foto solo a ti, en la pantalla Perfil.

### 4.2 Mapa compartido (solo si decides compartir)

Un hallazgo se publica solo cuando tocas «Compartir» y confirmas tras ver una vista previa. El **documento público** es visible para todos los usuarios con sesión iniciada y contiene únicamente:

- el tipo, decidido por el modelo de reconocimiento en el teléfono;
- la posición **redondeada al centro de una celda de al menos 100 m** (150 m por defecto), y un geohash calculado a partir de ese punto redondeado;
- la fecha de publicación y la de la última confirmación;
- contadores anónimos: confirmaciones, denuncias, «ya no está»;
- si lleva una foto.

El documento público no contiene identidad, deseo ni posición precisa.

**Foto opcional:** un recorte cuadrado alrededor de los tréboles reconocidos, de 384 px como máximo, recodificado a partir de los píxeles. No lleva metadatos EXIF: ni GPS, ni fecha, ni modelo de teléfono. El recorte reduce el fondo pero no lo elimina, por eso la aplicación te pide que revises la imagen antes de enviarla.

**Documentos privados vinculados a tu cuenta.** Se conservan bajo tu UID:

- una «reivindicación» por cada pin publicado, con su fecha de creación, que te permite retirar el pin;
- los recibos de tus confirmaciones, tus denuncias (con el motivo) y tus «ya no está»;
- contadores diarios que limitan la publicación a 20 pines al día y las sugerencias a 10 mensajes al día.

Los demás usuarios no pueden ver estos documentos. **El responsable puede vincular tu cuenta a los pines que has publicado y a tus denuncias a través de la consola de Firebase.** A efectos del RGPD, compartir es anónimo para los demás usuarios pero seudónimo para el responsable.

**Retirada.** Puedes retirar en cualquier momento un pin que hayas publicado: se eliminan el pin, su foto y tu reivindicación. Los pines no caducan por sí solos. Tras 3 denuncias por abuso, o cuando 2 personas distintas indican «ya no está», el pin y su foto se ocultan para todos. El documento se conserva durante el tiempo indicado en la sección 7.

### 4.3 Sugerencias

Desde la pantalla Perfil puedes enviar un mensaje con: categoría, texto (hasta 2000 caracteres), un contacto opcional, versión de la aplicación, idioma y fecha. El mensaje no contiene tu UID ni tu nombre. Sin embargo, un contador diario de tu área privada se actualiza con la hora del último envío. **Comparando esa hora con la fecha del mensaje, el responsable podría vincular el mensaje a tu cuenta.** No incluyas datos personales innecesarios en el texto.

### 4.4 Seguridad (Firebase App Check)

Para comprobar que las solicitudes proceden de la aplicación auténtica en un dispositivo auténtico, la aplicación usa Firebase App Check con Google Play Integrity. Google evalúa la integridad del dispositivo y de la aplicación y emite un token. El responsable no recibe datos del dispositivo.

## 5. Mapas (OpenFreeMap y MapLibre)

Los mapas los dibuja MapLibre en el teléfono, con estilo y teselas descargados de OpenFreeMap (`tiles.openfreemap.org`). El proveedor recibe tu dirección IP y las teselas solicitadas, por lo que puede saber **qué zona estás mirando**, también en tu mapa privado. No recibe tus hallazgos ni los marcadores. Según la información que publica, OpenFreeMap no registra las IP en los registros ordinarios, puede conservarlas hasta 30 días durante incidencias técnicas y puede usar Cloudflare. Para los detalles se aplica su propia política. Sin conexión sigue disponible un mapa local simplificado.

## 6. Finalidades y bases jurídicas

| Tratamiento | Base jurídica |
|---|---|
| Inicio de sesión y gestión de la cuenta | Ejecución del servicio que solicitas (art. 6.1.b) |
| Publicación en el mapa compartido, foto incluida | Consentimiento, dado al confirmar la compartición y retirado al retirar el pin (art. 6.1.a) |
| Reivindicaciones, recibos, límites diarios, umbrales de retirada, App Check | Interés legítimo en prevenir abusos y spam y en proteger el servicio (art. 6.1.f) |
| Sugerencias y posible respuesta | Interés legítimo en mejorar la aplicación; el contacto es opcional (art. 6.1.f) |
| Mapas | Interés legítimo en mostrar el mapa que abres (art. 6.1.f) |
| Ubicación del teléfono | Permiso opcional de Android; el dato se queda en el dispositivo |

No hay decisiones automatizadas con efectos jurídicos. El modelo de reconocimiento solo decide cómo se muestra un pin (trébol de cuatro hojas o grupo) y si puede compartirse.

## 7. Conservación

- **Datos locales:** hasta que elimines el hallazgo o desinstales la aplicación. La copia de la galería se queda hasta que la borres.
- **Cuenta y documentos privados:** mientras exista la cuenta. Se eliminan con la cuenta, salvo los contadores técnicos descritos en la sección 8.
- **Pines públicos y fotos:** hasta que los retires o elimines tu cuenta. Pines y fotos ocultados tras denuncias: se eliminan en un plazo de 120 días desde que se ocultan.
- **Sugerencias:** hasta 24 meses desde su envío; después se eliminan.
- **Registros técnicos de Google/Firebase:** según las políticas de Google.

## 8. Eliminar la cuenta

En la pantalla Perfil, en «Información y privacidad», «Eliminar cuenta y datos» borra: los pines que has publicado con sus fotos y reivindicaciones; las confirmaciones, denuncias y «ya no está» que has dado a otros pines, que se retiran (el contador público baja en uno) junto con sus recibos; y por último tu cuenta de Firebase. Solo quedan dos contadores técnicos diarios (número de envíos y hora del último), sin contenido y que ya no pueden vincularse contigo tras eliminar la cuenta: si pudieran borrarse, cualquiera podría poner a cero los límites antispam. Un pin ya ocultado por la comunidad sigue oculto aunque se retire tu voto. Si ya no tienes la aplicación, puedes solicitar la eliminación en https://emergentoremergency.github.io/lucky-prospector/delete-account-es.html o escribiendo a intervistaconleai@gmail.com. Los datos locales se borran eliminando los hallazgos o desinstalando la aplicación. Las sugerencias no están vinculadas a la cuenta: podemos borrarlas si lo pides y nos ayudas a encontrarlas.

## 9. Destinatarios y transferencias

- **Google Ireland Ltd / Google LLC** (Firebase Authentication, Cloud Firestore, App Check, Play Integrity), como encargado del tratamiento según las condiciones de Firebase. La base de datos Firestore está en la región **`eur3` (Unión Europea)**. La autenticación, App Check y algunos servicios de apoyo también pueden tratar datos fuera de la UE, sobre todo en Estados Unidos. En esos casos Google se basa en las cláusulas contractuales tipo de la Comisión Europea y/o en el EU-U.S. Data Privacy Framework.
- **OpenFreeMap** (y su posible CDN), para las teselas del mapa (sección 5).
- Los **demás usuarios** solo ven los documentos públicos descritos en la sección 4.2.

Tus datos no se venden ni se usan para publicidad.

## 10. Tus derechos

Dentro de los límites de los arts. 15 a 22 del RGPD puedes solicitar el acceso, la rectificación, la supresión, la limitación y la portabilidad de tus datos, y oponerte a los tratamientos basados en el interés legítimo. Puedes retirar tu consentimiento en cualquier momento retirando tus pines compartidos, sin que afecte a los tratamientos anteriores. Escribe a intervistaconleai@gmail.com. También tienes derecho a presentar una reclamación ante el **Garante per la protezione dei dati personali** (www.garanteprivacy.it) o ante la autoridad de tu país de residencia (en España, la AEPD: www.aepd.es).

## 11. Menores

La aplicación no está dirigida a personas menores de 14 años, la edad a partir de la cual en Italia se puede consentir por uno mismo a los servicios en línea (art. 2-quinquies del Código italiano de privacidad). Si la ley de tu país fija una edad mayor, se aplica esa. Por debajo de esa edad no se debe iniciar sesión ni compartir sin el consentimiento de quien ejerza la patria potestad.

## 12. Permisos de Android

- **Cámara** (obligatorio): para la búsqueda y las fotos.
- **Ubicación precisa/aproximada** (opcional): para situar un hallazgo en tu mapa.
- **Internet y estado de la red:** para el inicio de sesión, el mapa compartido, las teselas del mapa y las sugerencias.
- **Escritura en el almacenamiento** (solo Android 9 y anteriores): para copiar las fotos en la galería.

## 13. Cambios

Publicaremos aquí cualquier cambio con la fecha actualizada. Si el cambio es importante, también te avisaremos en la aplicación.
