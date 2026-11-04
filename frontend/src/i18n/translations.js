export const DEFAULT_LANGUAGE = "es";
export const LANGUAGES = ["es", "en"];
export const LANGUAGE_STORAGE_KEY = "crud-initializr-language";
export const LANGUAGE_URL_KEYS = ["lang", "language", "locale", "lng"];

export const normalizeLanguage = (value) => {
  if (value === null || value === undefined) {
    return null;
  }
  const normalized = String(value).trim().toLowerCase();
  if (normalized.startsWith("en") || normalized === "english") {
    return "en";
  }
  if (normalized.startsWith("es") || normalized === "spanish" || normalized === "español") {
    return "es";
  }
  return null;
};

/**
 * Reads the language from the URL so links coming from the portfolio can
 * preselect it. Supported forms:
 *   https://app.example.com/?lang=en
 *   https://app.example.com/?language=es
 *   https://app.example.com/en
 *   https://app.example.com/es
 *   https://app.example.com/#lang=en
 */
export const readLanguageFromUrl = (href) => {
  if (typeof window === "undefined" && !href) {
    return null;
  }

  try {
    const url = new URL(href || window.location.href);

    const firstSegment = url.pathname.split("/").filter(Boolean)[0];
    const pathLanguage = normalizeLanguage(firstSegment);
    if (pathLanguage) {
      return pathLanguage;
    }

    for (const key of LANGUAGE_URL_KEYS) {
      const language = normalizeLanguage(url.searchParams.get(key));
      if (language) {
        return language;
      }
    }

    if (url.hash) {
      const hashParams = new URLSearchParams(url.hash.replace(/^#/, ""));
      for (const key of LANGUAGE_URL_KEYS) {
        const language = normalizeLanguage(hashParams.get(key));
        if (language) {
          return language;
        }
      }
    }
  } catch {
    // Ignore malformed URLs and fall back to the stored/browser language.
  }

  return null;
};

const plural = (count, singular, pluralForm) =>
  count === 1 ? singular : pluralForm;

export const translations = {
  es: {
    "language.label": "Idioma",
    "language.es": "Español",
    "language.en": "English",
    "language.switchTo": "Cambiar a {language}",

    "common.cancel": "Cancelar",
    "common.delete": "Eliminar",
    "common.save": "Guardar",

    "home.subtitle": "Proyecto personal de Lorenzo Lopez",
    "home.savedProjectsTooltip": "Proyectos guardados",
    "home.openSavedProjects": "Abrir proyectos guardados",
    "home.projectName": "Nombre del proyecto",
    "home.createTooltip":
      "Descarga un ZIP con un proyecto Spring Boot listo para ejecutar, incluyendo tus entidades, relaciones y composiciones.",
    "home.creating": "Creando proyecto...",
    "home.create": "Crear proyecto",
    "home.saveTooltip": "Guardar el proyecto en este navegador",
    "home.save": "Guardar",
    "home.editorEyebrow": "EDITOR DE ENTIDAD",
    "home.unnamedEntity": "Entidad sin nombre",
    "home.noEntitySelected": "Ninguna entidad seleccionada",
    "home.closeEditor": "Cerrar editor",
    "home.closeEditorAria": "Cerrar editor de entidad",
    "home.emptyEditor":
      "Seleccioná una entidad en el mapa para editarla o creá una nueva.",
    "home.addEntity": "Agregar entidad",

    "home.downloaded.mongoGraphql":
      "Proyecto GraphQL generado con MongoDB. Incluye GraphiQL en /graphiql; los tests usan Testcontainers y requieren Docker.",
    "home.downloaded.mongo":
      "Proyecto MongoDB generado. Los tests usan Testcontainers y requieren Docker.",
    "home.downloaded.graphql": ({ database }) =>
      `Proyecto GraphQL generado con ${database}. Incluye el esquema y GraphiQL en /graphiql. Los tests usan H2 en memoria.`,
    "home.downloaded.sql": ({ database }) =>
      `Proyecto generado con ${database}. Los tests usan H2 en memoria.`,

    "home.mongoRelationsTitle": "Relaciones SQL detectadas",
    "home.mongoRelationsWarning": ({ count }) =>
      `Hay ${count} ${plural(count, "relación", "relaciones")} SQL en el mapa. MongoDB no las soporta: cambialas a composiciones o volvé a una base SQL.`,
    "home.sqlCompositionsTitle": "Composiciones de MongoDB detectadas",
    "home.sqlCompositionsWarning": ({ compositions, embedded }) =>
      `Hay ${compositions} ${plural(compositions, "composición", "composiciones")} y ${embedded} ${plural(embedded, "entidad embebida", "entidades embebidas")} de MongoDB. Las bases SQL no las soportan: cambialas o volvé a MongoDB.`,

    "home.warning.eyebrow": "Advertencia de compatibilidad",
    "home.mongoDialog.title": "MongoDB no soporta relaciones SQL",
    "home.mongoDialog.intro": ({ count }) =>
      `Detectamos ${count} ${plural(count, "relación", "relaciones")} SQL en el mapa. MongoDB usa composiciones con documentos embebidos, así que esas relaciones no serán válidas para este motor.`,
    "home.mongoDialog.note":
      "No se eliminarán automáticamente. Podés convertirlas en composiciones o volver a una base SQL antes de crear el proyecto.",
    "home.mongoDialog.confirm": "Usar MongoDB igual",

    "home.sqlDialog.title": "Las bases SQL no soportan composiciones",
    "home.sqlDialog.intro": ({ compositions, embedded }) =>
      `Detectamos ${compositions} ${plural(compositions, "composición", "composiciones")} y ${embedded} ${plural(embedded, "entidad embebida", "entidades embebidas")} de MongoDB.`,
    "home.sqlDialog.note":
      "Al continuar, las entidades embebidas volverán a ser entidades raíz. Las composiciones no se eliminarán automáticamente y deberás convertirlas a relaciones SQL o borrarlas.",
    "home.sqlDialog.confirm": "Usar base SQL igual",

    "home.help.tooltip": "Ver qué incluye el proyecto generado",
    "home.help.aria": "Ver detalle del proyecto que se genera",
    "home.help.eyebrow": "QUÉ VAS A DESCARGAR",
    "home.help.title": "Detalle del proyecto generado",
    "home.help.intro":
      "Al presionar **Crear proyecto** se descarga un archivo ZIP con un proyecto Maven de Spring Boot 4.1.2, generado a partir del nombre del proyecto, la base de datos elegida, el tipo de API (REST o GraphQL) y las entidades definidas en el mapa.",
    "home.help.base.title": "Proyecto base",
    "home.help.base.pom":
      "**pom.xml** con las dependencias de Spring Web, Spring Data JPA o MongoDB, validaciones y el driver de la base elegida.",
    "home.help.base.mainGraphql":
      "Clase principal de Spring Boot, manejador global de errores para GraphQL, configuración de la aplicación, README y .gitignore.",
    "home.help.base.mainRest":
      "Clase principal de Spring Boot, manejador global de errores para REST, configuración de la aplicación, README y .gitignore.",
    "home.help.base.compose":
      "**docker-compose.yml** cuando la base necesita servidor (PostgreSQL, MariaDB, MySQL o MongoDB).",
    "home.help.api.title": "API {api}",
    "home.help.graphql.controller":
      "Un **controller GraphQL** por entidad raíz con queries `findAll...` / `find...ById` y mutations `create...`, `update...` y `delete...`.",
    "home.help.graphql.schema":
      "Esquema en `src/main/resources/graphql/schema.graphqls` con los tipos, inputs, `Query` y `Mutation`.",
    "home.help.graphql.endpoint": "Endpoint GraphQL en `/graphql`.",
    "home.help.graphql.graphiql":
      "**GraphiQL** habilitado en `/graphiql`: es una interfaz web para explorar el esquema y probar queries y mutations con autocompletado, sin necesidad de escribir las peticiones HTTP a mano. Es una herramienta de desarrollo, no forma parte de la API, y podés deshabilitarla con `spring.graphql.graphiql.enabled=false`.",
    "home.help.graphql.scalars":
      "Los tipos `Date`, `DateTime`, `Time`, `BigDecimal` y `UUID` se exponen como **scalars GraphQL**.",
    "home.help.rest.controller":
      "Un **controller REST** por entidad con endpoints bajo `/api/nombreEntidad`.",
    "home.help.rest.swagger":
      "**Swagger UI** en `/swagger-ui.html` y el contrato OpenAPI en `/v3/api-docs`.",
    "home.help.rest.errors":
      "Manejador global de errores para devolver HTTP 400 con el detalle de la validación.",
    "home.help.relations":
      "En SQL las relaciones se envían por ID: `categoriaId` para N:1 y 1:1, y `etiquetasIds` para listas (1:N y N:M). En MongoDB las composiciones se envían como objetos anidados.",
    "home.help.entity.title": "Por cada entidad",
    "home.help.entity.domain":
      "**Domain:** clase JPA o documento MongoDB con su id, fechaCreacion, atributos y, en SQL, relaciones. Soporta String, Integer, Long, Boolean, Date, DateTime, Time, Decimal, Double y UUID. Las entidades embebidas de MongoDB no llevan id ni fechaCreacion.",
    "home.help.entity.repository":
      "**Repository** y **Service** con las validaciones configuradas y, en SQL, guardado en cascada de entidades relacionadas.",
    "home.help.entity.controllerGraphql":
      "**Controller GraphQL** con queries y mutations que delegan en el service de la entidad.",
    "home.help.entity.controllerRest":
      "**Controller REST** con endpoints bajo /api/nombreEntidad.",
    "home.help.entity.tests":
      "**Tests y builders** para el service de cada entidad.",
    "home.help.database.title": "Base de datos y esquema",
    "home.help.database.config":
      "Configuración de conexión y driver para PostgreSQL, MariaDB, MySQL, SQLite, H2 o MongoDB.",
    "home.help.database.schema":
      "**schema.sql** con tablas, claves primarias y foreign keys para las bases SQL. MongoDB no usa schema: las colecciones se crean al insertar.",
    "home.help.database.h2":
      "Con H2 se habilita la consola en `/h2-console` y la base se crea automáticamente.",
    "home.help.database.types":
      "Los tipos Date, DateTime, Time, Decimal, Double y UUID se traducen al tipo nativo de cada motor SQL y se persisten como fecha, decimal, número o UUID en MongoDB.",
    "home.help.links.title": "Relaciones y composiciones",
    "home.help.links.sql":
      "En las bases SQL, las relaciones **CUSTOM** se generan como `@ManyToOne`, las **CUSTOM_LIST** como `@OneToMany`, las **CUSTOM_ONE_TO_ONE** como `@OneToOne` con foreign key única y las **CUSTOM_MANY_TO_MANY** como `@ManyToMany` con tabla intermedia.",
    "home.help.links.mongo":
      "En MongoDB, las composiciones **COMPOSITION** y **COMPOSITION_LIST** generan documentos embebidos. La entidad embebida no tiene colección, repository, service ni controller propio.",
    "home.help.note":
      "Los campos **id** y **fechaCreacion** son reservados y se agregan automáticamente en las entidades raíz. En SQL los tests usan H2 en memoria; en MongoDB usan Testcontainers y requieren Docker. Los builders genéricos inicializan las relaciones y composiciones (incluidas las referencias inversas) para que los tests funcionen aunque haya validaciones sobre esos atributos.",
    "home.help.accept": "Entendido",

    "home.feedback.saved": ({ name }) => `Proyecto «${name}» guardado`,
    "home.feedback.saveError":
      "No se pudo guardar el proyecto en este navegador",
    "home.feedback.deleted": "Proyecto eliminado",
    "home.feedback.deleteError": "No se pudo eliminar el proyecto",
    "home.feedback.exported": "Proyectos exportados",
    "home.feedback.exportError": "No se pudieron exportar los proyectos",
    "home.feedback.importedOne": "Se importó 1 proyecto",
    "home.feedback.importedMany": ({ count }) =>
      `Se importaron ${count} proyectos`,
    "home.feedback.importError": "No se pudieron importar los proyectos",

    "validation.projectNameEmpty": "El nombre del proyecto no puede estar vacío",
    "validation.projectNameTooLong":
      "El nombre del proyecto no puede tener más de 30 caracteres",
    "validation.projectNameInvalid":
      "El nombre del proyecto debe contener al menos una letra o número",
    "validation.redisPrefixEmpty":
      "El prefijo de claves de Redis no puede estar vacío",
    "validation.redisPrefixTooLong":
      "El prefijo de claves de Redis no puede tener más de 50 caracteres",
    "validation.redisPrefixChars":
      "El prefijo de claves de Redis solo puede contener letras, números, ':', '_' y '-'",
    "validation.redisTtl":
      "El TTL de Redis debe ser un número entero mayor que cero",
    "validation.noEntities": "El proyecto debe tener al menos una entidad",
    "validation.entityNoName": ({ index }) =>
      `La entidad #${index} no tiene nombre`,
    "validation.entityNameInvalid": ({ name }) =>
      `El nombre de la entidad "${name}" no es válido`,
    "validation.entityNameTooLong": ({ name }) =>
      `El nombre de la entidad "${name}" no puede tener más de 30 caracteres`,
    "validation.entityDuplicate": ({ name }) =>
      `La entidad "${name}" está repetida`,
    "validation.attributeNoName": ({ entity }) =>
      `La entidad "${entity}" tiene un atributo sin nombre`,
    "validation.attributeNameInvalid": ({ name }) =>
      `El nombre del atributo "${name}" no es válido`,
    "validation.attributeNameTooLong": ({ name }) =>
      `El nombre del atributo "${name}" no puede tener más de 30 caracteres`,
    "validation.attributeDuplicate": ({ attribute, entity }) =>
      `El atributo "${attribute}" está repetido en "${entity}"`,
    "validation.attributeNeedsTarget": ({ name }) =>
      `El atributo "${name}" necesita una entidad destino seleccionada`,
    "validation.attributeNeedsMin": ({ name }) =>
      `El atributo "${name}" necesita un valor mínimo`,
    "validation.attributeNeedsMax": ({ name }) =>
      `El atributo "${name}" necesita un valor máximo`,
    "validation.attributeMinGreaterThanMax": ({ name }) =>
      `El valor mínimo de "${name}" no puede ser mayor que el máximo`,
    "validation.attributeMissingTarget": ({ attribute, entity, target }) =>
      `El atributo "${attribute}" de "${entity}" referencia a una entidad inexistente: "${target}"`,
    "validation.attributeSelfReference": ({ attribute, entity }) =>
      `El atributo "${attribute}" de "${entity}" no puede referenciarse a sí mismo`,
    "validation.mongoNoRelations": ({ attribute, entity }) =>
      `MongoDB no soporta relaciones SQL en esta versión. El atributo "${attribute}" de "${entity}" es una relación. Cambialo a una composición o elegí una base SQL.`,
    "validation.embeddedNoComposition": ({ entity }) =>
      `La entidad embebida "${entity}" no puede contener composiciones en esta versión.`,
    "validation.compositionNeedsEmbedded": ({ attribute, entity }) =>
      `La composición "${attribute}" de "${entity}" debe apuntar a un documento embebido.`,
    "validation.embeddedOnlyMongo": ({ entity }) =>
      `La entidad "${entity}" está marcada como documento embebido y solo es válida para MongoDB.`,
    "validation.compositionOnlyMongo": ({ attribute, entity }) =>
      `La composición "${attribute}" de "${entity}" solo es válida para MongoDB.`,
    "validation.cacheNoEntities":
      "Seleccioná al menos una entidad para cachear o deshabilitá el cache Redis",

    "database.title": "Base de datos",
    "database.subtitle":
      "El proyecto se genera con la configuración y la capa de persistencia de este motor.",
    "database.aria": "Base de datos",
    "database.postgres.hint": "Recomendada para producción",
    "database.postgres.note":
      "Recomendada para producción. El proyecto incluye Docker Compose y los tests usan H2 en memoria.",
    "database.mariadb.hint": "Compatible con MySQL",
    "database.mariadb.note":
      "Compatible con MySQL. El proyecto incluye Docker Compose y los tests usan H2 en memoria.",
    "database.mysql.hint": "Servidor · Docker",
    "database.mysql.note":
      "La opción clásica. El proyecto incluye Docker Compose y los tests usan H2 en memoria.",
    "database.sqlite.hint": "Archivo local, sin servidor",
    "database.sqlite.note":
      "Archivo local, sin servidor. La conexión habilita foreign_keys y los tests usan H2 en memoria.",
    "database.h2.hint": "Embebida, sin servidor",
    "database.h2.note":
      "Embebida: ideal para probar sin instalar nada. Incluye consola H2 y los tests usan H2 en memoria.",
    "database.mongodb.hint": "Documental con composiciones",
    "database.mongodb.note":
      "Documental: entidades raíz y composiciones con documentos embebidos. Incluye Docker Compose y tests con Testcontainers.",

    "api.title": "Tipo de API",
    "api.subtitle": "Elegí cómo se exponen las operaciones CRUD de las entidades.",
    "api.aria": "Tipo de API",
    "api.rest.hint": "Controllers REST por entidad",
    "api.rest.note":
      "Genera un controller REST por entidad con endpoints bajo /api/nombreEntidad y un manejador global de errores.",
    "api.graphql.hint": "Esquema y controllers GraphQL",
    "api.graphql.note":
      "Genera un esquema GraphQL y un controller por entidad. Incluye GraphiQL en /graphiql para explorar el esquema y probar queries y mutations.",

    "cache.title": "Cache Redis",
    "cache.subtitle":
      "Opcional. Agrega una capa de cache para las lecturas del proyecto generado.",
    "cache.enable": "Habilitar cache Redis",
    "cache.prefix": "Prefijo de claves",
    "cache.prefixExample": "Ejemplo: crud",
    "cache.ttl": "TTL (segundos)",
    "cache.cacheLists": "Cachear listados",
    "cache.entitiesTitle": "Entidades a cachear",
    "cache.entitiesCount": ({ cached, total }) => `${cached} de ${total}`,
    "cache.empty": "Agregá al menos una entidad para poder cachearla.",
    "cache.note":
      "findById se cachea siempre en las entidades seleccionadas. Las escrituras actualizan o invalidan el cache. Los tests generados usan cache deshabilitado.",
    "cache.disabledNote":
      "El proyecto se genera sin Redis. Podés habilitarlo después.",

    "entity.nameLabel": "Nombre de la entidad de dominio",
    "entity.deleteAria": ({ name }) => `Eliminar entidad ${name}`,
    "entity.embedded": "Documento embebido",
    "entity.embeddedTrue":
      "Se guarda dentro de otra entidad: no genera colección ni API propia.",
    "entity.embeddedFalse":
      "Entidad raíz: genera colección, repository, service y controller.",
    "entity.cacheLabel": "Cachear esta entidad",
    "entity.cacheTrue":
      "Sus endpoints usan Redis para las lecturas y las escrituras invalidan el cache.",
    "entity.cacheFalse":
      "Sus endpoints no usan cache: siempre consultan la base de datos.",
    "entity.attributes": "Atributos de la entidad:",
    "entity.addAttribute": "Agregar nuevo atributo",

    "attribute.type.composition": "Composición 1:1",
    "attribute.type.compositionList": "Composición 1:N",
    "attribute.unnamed": "Atributo sin nombre",
    "attribute.noTarget": "sin entidad",
    "attribute.validationsCount": ({ count }) =>
      `${count} ${plural(count, "validación", "validaciones")}`,
    "attribute.deleteAria": ({ name }) => `Eliminar atributo ${name}`,
    "attribute.nameLabel": "Nombre del atributo",
    "attribute.typeLabel": "Tipo del atributo",
    "attribute.type": "Tipo",
    "attribute.typeOption.relationManyToOne": "Relación N:1 · Muchos a uno",
    "attribute.typeOption.relationOneToMany": "Relación 1:N · Uno a muchos",
    "attribute.typeOption.relationOneToOne": "Relación 1:1 · Uno a uno",
    "attribute.typeOption.relationManyToMany": "Relación N:M · Muchos a muchos",
    "attribute.typeOption.composition":
      "Composición 1:1 · Documento embebido",
    "attribute.typeOption.compositionList":
      "Composición 1:N · Lista de documentos",
    "attribute.reason.mongoUnavailable": "no disponible en MongoDB",
    "attribute.reason.embeddedUnavailable": "no disponible en embebidas",
    "attribute.reason.mongoOnly": "solo MongoDB",
    "attribute.embeddedSimple":
      "Los documentos embebidos solo admiten campos simples en esta versión.",
    "attribute.embeddedNoLinks":
      "Una entidad embebida no puede tener relaciones ni composiciones. Cambiá este atributo a un tipo simple.",
    "attribute.mongoNoRelations":
      "MongoDB no soporta relaciones SQL. Cambiá este atributo a una composición.",
    "attribute.sqlNoCompositions":
      "Las composiciones solo son válidas para MongoDB. Cambiá este atributo a una relación SQL.",
    "attribute.targetMustBeEmbedded":
      "La entidad destino debe estar marcada como documento embebido.",
    "attribute.targetLabel": "Entidad destino",
    "attribute.selectEntity": "Seleccionar entidad",
    "attribute.markAsEmbedded": " · marcá la entidad como embebida",
    "attribute.validations": "Validaciones",
    "attribute.selectValidation": "Seleccionar validación",
    "attribute.min": "Valor mínimo",
    "attribute.max": "Valor máximo",
    "attribute.selectedValidations": "Validaciones seleccionadas:",
    "attribute.minChip": ({ value }) => `Mínimo: ${value}`,
    "attribute.maxChip": ({ value }) => `Máximo: ${value}`,

    "validationLabel.NOT_NULL": "Not null",
    "validationLabel.NOT_BLANK": "String no vacío",
    "validationLabel.NOT_EMPTY": "Lista no vacía",
    "validationLabel.PAST": "Fecha pasada",
    "validationLabel.PAST_OR_PRESENT": "Fecha pasada o presente",
    "validationLabel.FUTURE": "Fecha futura",
    "validationLabel.FUTURE_OR_PRESENT": "Fecha futura o presente",
    "validationLabel.POSITIVE": "Positivo",
    "validationLabel.NEGATIVE": "Negativo",
    "validationLabel.POSITIVE_OR_ZERO": "Positivo o cero",
    "validationLabel.NEGATIVE_OR_ZERO": "Negativo o cero",
    "validationLabel.MIN": "Mínimo",
    "validationLabel.MAX": "Máximo",

    "graph.word.composition": ({ count }) =>
      plural(count, "composición", "composiciones"),
    "graph.word.link": ({ count }) => plural(count, "vínculo", "vínculos"),
    "graph.word.relation": ({ count }) => plural(count, "relación", "relaciones"),
    "graph.node.clickToEdit": "click para editar",
    "graph.node.dropLink": "Soltá un vínculo acá",
    "graph.node.noAttributes": "Sin atributos",
    "graph.node.moreAttributes": ({ count }) =>
      `+${count} ${plural(count, "atributo", "atributos")} más`,
    "graph.node.missingTarget": ({ count }) => `${count} sin destino`,
    "graph.node.embeddedCannotCompose":
      "Los documentos embebidos no pueden originar composiciones",
    "graph.node.dragToCreate": "Arrastrá para crear un vínculo",
    "graph.node.missingEntity": "SIN ENTIDAD",
    "graph.edge.composes": ({ cardinality }) => `compone · ${cardinality}`,
    "graph.empty": "Todavía no hay entidades para mostrar.",
    "graph.attribute.unnamed": "sin nombre",
    "graph.attribute.noTarget": "sin destino",
    "graph.actions.addEntity": "Agregar entidad",
    "graph.actions.relayout": "Reordenar",
    "graph.actions.center": "Centrar",
    "graph.actions.exit": "Salir",
    "graph.actions.fullscreen": "Pantalla completa",
    "graph.legend.manyToOne": "Muchos a uno",
    "graph.legend.oneToMany": "Uno a muchos",
    "graph.legend.oneToOne": "Uno a uno",
    "graph.legend.manyToMany": "Muchos a muchos",
    "graph.legend.compositionOneToOne": "Composición uno a uno",
    "graph.legend.compositionOneToMany": "Composición uno a muchos",
    "graph.legend.hintSql":
      "Click en una entidad para editarla en el panel · Arrastra desde el + para crear una relación · Click en una arista para editarla.",
    "graph.legend.hintMongo":
      "Click en una entidad para editarla · Arrastra desde el + de una entidad raíz hacia un documento embebido · Click en una arista para editarla.",
    "graph.fullscreen.eyebrowRelations": "MAPA DE RELACIONES",
    "graph.fullscreen.eyebrowCompositions": "MAPA DE COMPOSICIONES",
    "graph.fullscreen.title": "Entidades y sus vínculos",
    "graph.fullscreen.closeTooltip": "Cerrar (Esc)",
    "graph.fullscreen.closeAria": "Cerrar mapa de relaciones",
    "graph.panel.titleRelations": "Mapa de relaciones",
    "graph.panel.titleCompositions": "Mapa de composiciones",
    "graph.panel.summary": ({ entities, links, mongo }) =>
      `${entities} ${plural(entities, "entidad", "entidades")} · ${links} ${
        mongo
          ? plural(links, "composición", "composiciones")
          : plural(links, "relación", "relaciones")
      }`,
    "graph.linkWord.relation": "Relación",
    "graph.linkWord.composition": "Composición",
    "graph.notice.selfRelation":
      "Una entidad no puede relacionarse consigo misma.",
    "graph.notice.embeddedSource":
      "Un documento embebido no puede originar composiciones. Marcá como raíz la entidad origen.",
    "graph.notice.targetNeedsName":
      "La entidad destino necesita un nombre antes de vincularla.",
    "graph.notice.markEmbedded": ({ name }) =>
      `Marcá "${name}" como documento embebido para usarla en una composición.`,
    "graph.notice.targetMissing": "La entidad destino no existe.",
    "graph.notice.selfLink": "Una entidad no puede vincularse consigo misma.",
    "graph.notice.embeddedCannotComposeShort":
      "Un documento embebido no puede originar composiciones.",
    "graph.notice.markEmbeddedMove": ({ name }) =>
      `Marcá "${name}" como documento embebido para mover la composición.`,
    "graph.notice.embeddedNotSql": ({ name }) =>
      `La entidad "${name}" está marcada como embebida y no es válida para SQL.`,
    "graph.notice.moved": ({ link, source, target }) =>
      `${link} movida: ${source} → ${target}. Click en la arista para renombrarla.`,
    "graph.notice.created": ({ link, source, target }) =>
      `${link} creada: ${source} → ${target}`,
    "graph.notice.updated": ({ link }) => `${link} actualizada`,
    "graph.notice.deleted": "Vínculo eliminado",

    "relation.eyebrowEdit": "EDITAR RELACIÓN",
    "relation.eyebrowCreate": "NUEVA RELACIÓN",
    "relation.deleteWarning": ({ name, source }) =>
      `Se va a eliminar el atributo «${name}» de ${source}. Esta acción no se puede deshacer.`,
    "relation.nameLabel": "Nombre del atributo",
    "relation.helperCreate":
      "Se agrega como atributo de la entidad origen.",
    "relation.helperEdit": "Nombre del atributo en la entidad origen.",
    "relation.typeComposition": "Tipo de composición",
    "relation.typeRelation": "Tipo de relación",
    "relation.targetInvalid": ({ name }) =>
      `La entidad «${name}» debe estar marcada como documento embebido para usarla en una composición.`,
    "relation.targetMissing": ({ name }) =>
      `La entidad «${name}» no existe. La relación va a quedar sin destino hasta que crees esa entidad.`,
    "relation.hintCreate": ({ source }) =>
      `También podés ajustar esta relación desde el formulario de ${source}.`,
    "relation.hintEdit":
      "También podés arrastrar el extremo de la relación en el mapa para cambiar la entidad destino.",
    "relation.back": "Volver",
    "relation.deleteRelation": "Eliminar relación",
    "relation.createComposition": "Crear composición",
    "relation.createRelation": "Crear relación",
    "relation.error.targetEmbedded":
      "La entidad destino debe ser un documento embebido",
    "relation.error.nameEmpty": "El nombre no puede estar vacío",
    "relation.error.nameStartsWithLetter": "El nombre debe empezar con una letra",
    "relation.error.nameTooLong":
      "El nombre no puede tener más de 30 caracteres",
    "relation.error.nameDuplicate": ({ source }) =>
      `Ya existe un atributo con ese nombre en ${source}`,
    "relation.type.mongoEmbedded": "1:1 · Embebido",
    "relation.type.mongoList": "1:N · Lista",
    "relation.type.manyToOne": "N:1 · Muchos a uno",
    "relation.type.oneToMany": "1:N · Uno a muchos",
    "relation.type.oneToOne": "1:1 · Uno a uno",
    "relation.type.manyToMany": "N:M · Muchos a muchos",

    "projects.eyebrow": "TUS PROYECTOS",
    "projects.title": "Proyectos guardados",
    "projects.storageNote": "Se guardan en este navegador.",
    "projects.closeAria": "Cerrar proyectos guardados",
    "projects.emptyTitle": "Todavía no hay proyectos guardados",
    "projects.emptyHint":
      "Usá el botón Guardar del encabezado para guardar el trabajo actual.",
    "projects.current": "En edición",
    "projects.deleteTooltip": "Eliminar proyecto",
    "projects.deleteAria": ({ name }) => `Eliminar proyecto ${name}`,
    "projects.unnamedEntity": "Sin nombre",
    "projects.new": "Nuevo proyecto",
    "projects.import": "Importar",
    "projects.export": "Exportar",
    "projects.importAria": "Importar proyectos desde un archivo JSON",
    "projects.footerNote":
      "Los proyectos quedan guardados solo en este navegador. Usá Importar/Exportar para moverlos entre navegadores.",
    "projects.deleteEyebrow": "ELIMINAR PROYECTO",
    "projects.deleteHeading": ({ name }) => `¿Eliminar «${name}»?`,
    "projects.deleteIntro":
      "Se borrará de este navegador. Esta acción no se puede deshacer.",
    "projects.meta": ({ date, entities }) =>
      `${date} · ${entities} ${plural(entities, "entidad", "entidades")}`,

    "saveDialog.eyebrow": "GUARDAR PROYECTO",
    "saveDialog.title": "Nombrá tu proyecto",
    "saveDialog.descriptionDefault":
      "Guardá el trabajo actual para poder retomarlo después.",
    "saveDialog.descriptionPending":
      "Para abrir el proyecto elegido primero guardá el trabajo actual con un nombre.",
    "saveDialog.nameLabel": "Nombre del proyecto",
    "saveDialog.nameHelper": "También se usa como nombre del ZIP.",
    "saveDialog.errorEmpty": "El proyecto necesita un nombre para guardarse",
    "saveDialog.errorTooLong":
      "El nombre no puede tener más de 30 caracteres",

    "openDialog.eyebrow": "REEMPLAZAR CONFIGURACIÓN",
    "openDialog.newTitle": "Empezar un proyecto nuevo",
    "openDialog.openTitle": ({ name }) => `Abrir «${name}»`,
    "openDialog.newIntro":
      "Se vaciará la configuración actual para empezar de cero: nombre, entidades, base de datos, API y cache.",
    "openDialog.openIntro":
      "Se reemplazará la configuración actual con la del proyecto guardado: nombre, entidades, base de datos, API y cache.",
    "openDialog.note": "Los cambios sin guardar se pierden.",
    "openDialog.noteUnnamed":
      "El trabajo actual no tiene nombre: podés descartarlo o escribir uno para guardarlo.",
    "openDialog.noteNamed":
      "Podés guardar el trabajo actual antes de continuar.",
    "openDialog.discard": "Descartar cambios",
    "openDialog.saveAndContinue": "Guardar y continuar",

    "service.createError": "No se pudo crear el proyecto",
    "service.connectionError": "No se pudo conectar con el servidor",
    "import.invalidJson": "El archivo no es un JSON válido",
    "import.noList": "El archivo no contiene una lista de proyectos",
    "import.noValidProjects":
      "No se encontraron proyectos válidos en el archivo",
  },

  en: {
    "language.label": "Language",
    "language.es": "Español",
    "language.en": "English",
    "language.switchTo": "Switch to {language}",

    "common.cancel": "Cancel",
    "common.delete": "Delete",
    "common.save": "Save",

    "home.subtitle": "Personal project by Lorenzo Lopez",
    "home.savedProjectsTooltip": "Saved projects",
    "home.openSavedProjects": "Open saved projects",
    "home.projectName": "Project name",
    "home.createTooltip":
      "Download a ZIP with a ready-to-run Spring Boot project, including your entities, relations and compositions.",
    "home.creating": "Creating project...",
    "home.create": "Create project",
    "home.saveTooltip": "Save the project in this browser",
    "home.save": "Save",
    "home.editorEyebrow": "ENTITY EDITOR",
    "home.unnamedEntity": "Unnamed entity",
    "home.noEntitySelected": "No entity selected",
    "home.closeEditor": "Close editor",
    "home.closeEditorAria": "Close entity editor",
    "home.emptyEditor":
      "Select an entity on the map to edit it, or create a new one.",
    "home.addEntity": "Add entity",

    "home.downloaded.mongoGraphql":
      "GraphQL project generated with MongoDB. It includes GraphiQL at /graphiql; tests use Testcontainers and require Docker.",
    "home.downloaded.mongo":
      "MongoDB project generated. Tests use Testcontainers and require Docker.",
    "home.downloaded.graphql": ({ database }) =>
      `GraphQL project generated with ${database}. It includes the schema and GraphiQL at /graphiql. Tests use in-memory H2.`,
    "home.downloaded.sql": ({ database }) =>
      `Project generated with ${database}. Tests use in-memory H2.`,

    "home.mongoRelationsTitle": "SQL relations detected",
    "home.mongoRelationsWarning": ({ count }) =>
      `There ${count === 1 ? "is" : "are"} ${count} SQL ${plural(count, "relation", "relations")} on the map. MongoDB does not support ${count === 1 ? "it" : "them"}: convert ${count === 1 ? "it" : "them"} to compositions or switch back to a SQL database.`,
    "home.sqlCompositionsTitle": "MongoDB compositions detected",
    "home.sqlCompositionsWarning": ({ compositions, embedded }) =>
      `There ${compositions === 1 ? "is" : "are"} ${compositions} ${plural(compositions, "composition", "compositions")} and ${embedded} ${plural(embedded, "embedded entity", "embedded entities")} from MongoDB. SQL databases do not support them: convert them or switch back to MongoDB.`,

    "home.warning.eyebrow": "Compatibility warning",
    "home.mongoDialog.title": "MongoDB does not support SQL relations",
    "home.mongoDialog.intro": ({ count }) =>
      `We detected ${count} SQL ${plural(count, "relation", "relations")} on the map. MongoDB uses compositions with embedded documents, so those relations will not be valid for this engine.`,
    "home.mongoDialog.note":
      "They will not be deleted automatically. You can convert them to compositions or switch back to a SQL database before creating the project.",
    "home.mongoDialog.confirm": "Use MongoDB anyway",

    "home.sqlDialog.title": "SQL databases do not support compositions",
    "home.sqlDialog.intro": ({ compositions, embedded }) =>
      `We detected ${compositions} ${plural(compositions, "composition", "compositions")} and ${embedded} ${plural(embedded, "embedded entity", "embedded entities")} from MongoDB.`,
    "home.sqlDialog.note":
      "If you continue, embedded entities will become root entities again. Compositions will not be deleted automatically, and you will need to convert them to SQL relations or remove them.",
    "home.sqlDialog.confirm": "Use SQL database anyway",

    "home.help.tooltip": "See what the generated project includes",
    "home.help.aria": "See details of the generated project",
    "home.help.eyebrow": "WHAT YOU WILL DOWNLOAD",
    "home.help.title": "Generated project details",
    "home.help.intro":
      "Clicking **Create project** downloads a ZIP file with a Spring Boot 4.1.2 Maven project, generated from the project name, the selected database, the API type (REST or GraphQL) and the entities defined on the map.",
    "home.help.base.title": "Base project",
    "home.help.base.pom":
      "**pom.xml** with the Spring Web, Spring Data JPA or MongoDB dependencies, validations and the driver for the selected database.",
    "home.help.base.mainGraphql":
      "Spring Boot main class, global error handler for GraphQL, application configuration, README and .gitignore.",
    "home.help.base.mainRest":
      "Spring Boot main class, global error handler for REST, application configuration, README and .gitignore.",
    "home.help.base.compose":
      "**docker-compose.yml** when the database needs a server (PostgreSQL, MariaDB, MySQL or MongoDB).",
    "home.help.api.title": "API {api}",
    "home.help.graphql.controller":
      "One **GraphQL controller** per root entity with `findAll...` / `find...ById` queries and `create...`, `update...` and `delete...` mutations.",
    "home.help.graphql.schema":
      "Schema at `src/main/resources/graphql/schema.graphqls` with the types, inputs, `Query` and `Mutation`.",
    "home.help.graphql.endpoint": "GraphQL endpoint at `/graphql`.",
    "home.help.graphql.graphiql":
      "**GraphiQL** enabled at `/graphiql`: a web interface to explore the schema and test queries and mutations with autocompletion, without writing HTTP requests by hand. It is a development tool, not part of the API, and you can disable it with `spring.graphql.graphiql.enabled=false`.",
    "home.help.graphql.scalars":
      "The `Date`, `DateTime`, `Time`, `BigDecimal` and `UUID` types are exposed as **GraphQL scalars**.",
    "home.help.rest.controller":
      "One **REST controller** per entity with endpoints under `/api/entityName`.",
    "home.help.rest.swagger":
      "**Swagger UI** at `/swagger-ui.html` and the OpenAPI contract at `/v3/api-docs`.",
    "home.help.rest.errors":
      "Global error handler that returns HTTP 400 with the validation details.",
    "home.help.relations":
      "In SQL, relations are sent by ID: `categoryId` for N:1 and 1:1, and `tagIds` for lists (1:N and N:M). In MongoDB, compositions are sent as nested objects.",
    "home.help.entity.title": "Per entity",
    "home.help.entity.domain":
      "**Domain:** JPA class or MongoDB document with its id, fechaCreacion, attributes and, in SQL, relations. Supports String, Integer, Long, Boolean, Date, DateTime, Time, Decimal, Double and UUID. MongoDB embedded entities have no id or fechaCreacion.",
    "home.help.entity.repository":
      "**Repository** and **Service** with the configured validations and, in SQL, cascading saves of related entities.",
    "home.help.entity.controllerGraphql":
      "**GraphQL controller** with queries and mutations that delegate to the entity service.",
    "home.help.entity.controllerRest":
      "**REST controller** with endpoints under /api/entityName.",
    "home.help.entity.tests":
      "**Tests and builders** for each entity service.",
    "home.help.database.title": "Database and schema",
    "home.help.database.config":
      "Connection configuration and driver for PostgreSQL, MariaDB, MySQL, SQLite, H2 or MongoDB.",
    "home.help.database.schema":
      "**schema.sql** with tables, primary keys and foreign keys for SQL databases. MongoDB does not use a schema: collections are created on insert.",
    "home.help.database.h2":
      "With H2, the console is enabled at `/h2-console` and the database is created automatically.",
    "home.help.database.types":
      "The Date, DateTime, Time, Decimal, Double and UUID types are translated to each SQL engine's native type and persisted as date, decimal, number or UUID in MongoDB.",
    "home.help.links.title": "Relations and compositions",
    "home.help.links.sql":
      "In SQL databases, **CUSTOM** relations are generated as `@ManyToOne`, **CUSTOM_LIST** as `@OneToMany`, **CUSTOM_ONE_TO_ONE** as `@OneToOne` with a unique foreign key, and **CUSTOM_MANY_TO_MANY** as `@ManyToMany` with a join table.",
    "home.help.links.mongo":
      "In MongoDB, **COMPOSITION** and **COMPOSITION_LIST** compositions generate embedded documents. Embedded entities have no collection, repository, service or controller of their own.",
    "home.help.note":
      "The **id** and **fechaCreacion** fields are reserved and added automatically to root entities. In SQL, tests use in-memory H2; in MongoDB they use Testcontainers and require Docker. Generic builders initialize relations and compositions (including back references) so tests work even when those attributes have validations.",
    "home.help.accept": "Got it",

    "home.feedback.saved": ({ name }) => `Project "${name}" saved`,
    "home.feedback.saveError":
      "The project could not be saved in this browser",
    "home.feedback.deleted": "Project deleted",
    "home.feedback.deleteError": "The project could not be deleted",
    "home.feedback.exported": "Projects exported",
    "home.feedback.exportError": "The projects could not be exported",
    "home.feedback.importedOne": "1 project imported",
    "home.feedback.importedMany": ({ count }) =>
      `${count} projects imported`,
    "home.feedback.importError": "The projects could not be imported",

    "validation.projectNameEmpty": "The project name cannot be empty",
    "validation.projectNameTooLong":
      "The project name cannot be longer than 30 characters",
    "validation.projectNameInvalid":
      "The project name must contain at least one letter or number",
    "validation.redisPrefixEmpty": "The Redis key prefix cannot be empty",
    "validation.redisPrefixTooLong":
      "The Redis key prefix cannot be longer than 50 characters",
    "validation.redisPrefixChars":
      "The Redis key prefix can only contain letters, numbers, ':', '_' and '-'",
    "validation.redisTtl":
      "The Redis TTL must be an integer greater than zero",
    "validation.noEntities": "The project must have at least one entity",
    "validation.entityNoName": ({ index }) =>
      `Entity #${index} has no name`,
    "validation.entityNameInvalid": ({ name }) =>
      `The entity name "${name}" is not valid`,
    "validation.entityNameTooLong": ({ name }) =>
      `The entity name "${name}" cannot be longer than 30 characters`,
    "validation.entityDuplicate": ({ name }) =>
      `The entity "${name}" is duplicated`,
    "validation.attributeNoName": ({ entity }) =>
      `The entity "${entity}" has an attribute with no name`,
    "validation.attributeNameInvalid": ({ name }) =>
      `The attribute name "${name}" is not valid`,
    "validation.attributeNameTooLong": ({ name }) =>
      `The attribute name "${name}" cannot be longer than 30 characters`,
    "validation.attributeDuplicate": ({ attribute, entity }) =>
      `The attribute "${attribute}" is duplicated in "${entity}"`,
    "validation.attributeNeedsTarget": ({ name }) =>
      `The attribute "${name}" needs a target entity`,
    "validation.attributeNeedsMin": ({ name }) =>
      `The attribute "${name}" needs a minimum value`,
    "validation.attributeNeedsMax": ({ name }) =>
      `The attribute "${name}" needs a maximum value`,
    "validation.attributeMinGreaterThanMax": ({ name }) =>
      `The minimum value of "${name}" cannot be greater than the maximum`,
    "validation.attributeMissingTarget": ({ attribute, entity, target }) =>
      `The attribute "${attribute}" of "${entity}" references a missing entity: "${target}"`,
    "validation.attributeSelfReference": ({ attribute, entity }) =>
      `The attribute "${attribute}" of "${entity}" cannot reference itself`,
    "validation.mongoNoRelations": ({ attribute, entity }) =>
      `MongoDB does not support SQL relations in this version. The attribute "${attribute}" of "${entity}" is a relation. Change it to a composition or pick a SQL database.`,
    "validation.embeddedNoComposition": ({ entity }) =>
      `The embedded entity "${entity}" cannot contain compositions in this version.`,
    "validation.compositionNeedsEmbedded": ({ attribute, entity }) =>
      `The composition "${attribute}" of "${entity}" must point to an embedded document.`,
    "validation.embeddedOnlyMongo": ({ entity }) =>
      `The entity "${entity}" is marked as an embedded document and is only valid for MongoDB.`,
    "validation.compositionOnlyMongo": ({ attribute, entity }) =>
      `The composition "${attribute}" of "${entity}" is only valid for MongoDB.`,
    "validation.cacheNoEntities":
      "Select at least one entity to cache or disable the Redis cache",

    "database.title": "Database",
    "database.subtitle":
      "The project is generated with this engine's configuration and persistence layer.",
    "database.aria": "Database",
    "database.postgres.hint": "Recommended for production",
    "database.postgres.note":
      "Recommended for production. The project includes Docker Compose and tests use in-memory H2.",
    "database.mariadb.hint": "MySQL compatible",
    "database.mariadb.note":
      "MySQL compatible. The project includes Docker Compose and tests use in-memory H2.",
    "database.mysql.hint": "Server · Docker",
    "database.mysql.note":
      "The classic choice. The project includes Docker Compose and tests use in-memory H2.",
    "database.sqlite.hint": "Local file, no server",
    "database.sqlite.note":
      "Local file, no server. The connection enables foreign_keys and tests use in-memory H2.",
    "database.h2.hint": "Embedded, no server",
    "database.h2.note":
      "Embedded: ideal for trying things out without installing anything. Includes the H2 console and tests use in-memory H2.",
    "database.mongodb.hint": "Document store with compositions",
    "database.mongodb.note":
      "Document store: root entities and compositions with embedded documents. Includes Docker Compose and Testcontainers-based tests.",

    "api.title": "API type",
    "api.subtitle": "Choose how the entities' CRUD operations are exposed.",
    "api.aria": "API type",
    "api.rest.hint": "REST controllers per entity",
    "api.rest.note":
      "Generates one REST controller per entity with endpoints under /api/entityName and a global error handler.",
    "api.graphql.hint": "GraphQL schema and controllers",
    "api.graphql.note":
      "Generates a GraphQL schema and one controller per entity. Includes GraphiQL at /graphiql to explore the schema and test queries and mutations.",

    "cache.title": "Redis cache",
    "cache.subtitle":
      "Optional. Adds a cache layer for reads in the generated project.",
    "cache.enable": "Enable Redis cache",
    "cache.prefix": "Key prefix",
    "cache.prefixExample": "Example: crud",
    "cache.ttl": "TTL (seconds)",
    "cache.cacheLists": "Cache list endpoints",
    "cache.entitiesTitle": "Entities to cache",
    "cache.entitiesCount": ({ cached, total }) => `${cached} of ${total}`,
    "cache.empty": "Add at least one entity to be able to cache it.",
    "cache.note":
      "findById is always cached on the selected entities. Writes update or invalidate the cache. Generated tests use caching disabled.",
    "cache.disabledNote":
      "The project is generated without Redis. You can enable it later.",

    "entity.nameLabel": "Domain entity name",
    "entity.deleteAria": ({ name }) => `Delete entity ${name}`,
    "entity.embedded": "Embedded document",
    "entity.embeddedTrue":
      "Stored inside another entity: it does not generate its own collection or API.",
    "entity.embeddedFalse":
      "Root entity: generates a collection, repository, service and controller.",
    "entity.cacheLabel": "Cache this entity",
    "entity.cacheTrue":
      "Its endpoints use Redis for reads and writes invalidate the cache.",
    "entity.cacheFalse":
      "Its endpoints do not use the cache: they always query the database.",
    "entity.attributes": "Entity attributes:",
    "entity.addAttribute": "Add new attribute",

    "attribute.type.composition": "1:1 composition",
    "attribute.type.compositionList": "1:N composition",
    "attribute.unnamed": "Unnamed attribute",
    "attribute.noTarget": "no entity",
    "attribute.validationsCount": ({ count }) =>
      `${count} ${plural(count, "validation", "validations")}`,
    "attribute.deleteAria": ({ name }) => `Delete attribute ${name}`,
    "attribute.nameLabel": "Attribute name",
    "attribute.typeLabel": "Attribute type",
    "attribute.type": "Type",
    "attribute.typeOption.relationManyToOne": "N:1 relation · Many to one",
    "attribute.typeOption.relationOneToMany": "1:N relation · One to many",
    "attribute.typeOption.relationOneToOne": "1:1 relation · One to one",
    "attribute.typeOption.relationManyToMany": "N:M relation · Many to many",
    "attribute.typeOption.composition":
      "1:1 composition · Embedded document",
    "attribute.typeOption.compositionList":
      "1:N composition · List of documents",
    "attribute.reason.mongoUnavailable": "not available for MongoDB",
    "attribute.reason.embeddedUnavailable": "not available for embedded entities",
    "attribute.reason.mongoOnly": "MongoDB only",
    "attribute.embeddedSimple":
      "Embedded documents only support simple fields in this version.",
    "attribute.embeddedNoLinks":
      "An embedded entity cannot have relations or compositions. Change this attribute to a simple type.",
    "attribute.mongoNoRelations":
      "MongoDB does not support SQL relations. Change this attribute to a composition.",
    "attribute.sqlNoCompositions":
      "Compositions are only valid for MongoDB. Change this attribute to a SQL relation.",
    "attribute.targetMustBeEmbedded":
      "The target entity must be marked as an embedded document.",
    "attribute.targetLabel": "Target entity",
    "attribute.selectEntity": "Select entity",
    "attribute.markAsEmbedded": " · mark the entity as embedded",
    "attribute.validations": "Validations",
    "attribute.selectValidation": "Select validation",
    "attribute.min": "Minimum value",
    "attribute.max": "Maximum value",
    "attribute.selectedValidations": "Selected validations:",
    "attribute.minChip": ({ value }) => `Minimum: ${value}`,
    "attribute.maxChip": ({ value }) => `Maximum: ${value}`,

    "validationLabel.NOT_NULL": "Not null",
    "validationLabel.NOT_BLANK": "Non-blank string",
    "validationLabel.NOT_EMPTY": "Non-empty list",
    "validationLabel.PAST": "Past date",
    "validationLabel.PAST_OR_PRESENT": "Past or present date",
    "validationLabel.FUTURE": "Future date",
    "validationLabel.FUTURE_OR_PRESENT": "Future or present date",
    "validationLabel.POSITIVE": "Positive",
    "validationLabel.NEGATIVE": "Negative",
    "validationLabel.POSITIVE_OR_ZERO": "Positive or zero",
    "validationLabel.NEGATIVE_OR_ZERO": "Negative or zero",
    "validationLabel.MIN": "Minimum",
    "validationLabel.MAX": "Maximum",

    "graph.word.composition": ({ count }) =>
      plural(count, "composition", "compositions"),
    "graph.word.link": ({ count }) => plural(count, "link", "links"),
    "graph.word.relation": ({ count }) => plural(count, "relation", "relations"),
    "graph.node.clickToEdit": "click to edit",
    "graph.node.dropLink": "Drop a link here",
    "graph.node.noAttributes": "No attributes",
    "graph.node.moreAttributes": ({ count }) =>
      `+${count} more ${plural(count, "attribute", "attributes")}`,
    "graph.node.missingTarget": ({ count }) => `${count} without target`,
    "graph.node.embeddedCannotCompose":
      "Embedded documents cannot originate compositions",
    "graph.node.dragToCreate": "Drag to create a link",
    "graph.node.missingEntity": "NO ENTITY",
    "graph.edge.composes": ({ cardinality }) => `composes · ${cardinality}`,
    "graph.empty": "There are no entities to show yet.",
    "graph.attribute.unnamed": "unnamed",
    "graph.attribute.noTarget": "no target",
    "graph.actions.addEntity": "Add entity",
    "graph.actions.relayout": "Re-layout",
    "graph.actions.center": "Center",
    "graph.actions.exit": "Exit",
    "graph.actions.fullscreen": "Full screen",
    "graph.legend.manyToOne": "Many to one",
    "graph.legend.oneToMany": "One to many",
    "graph.legend.oneToOne": "One to one",
    "graph.legend.manyToMany": "Many to many",
    "graph.legend.compositionOneToOne": "One-to-one composition",
    "graph.legend.compositionOneToMany": "One-to-many composition",
    "graph.legend.hintSql":
      "Click an entity to edit it in the panel · Drag from + to create a relation · Click an edge to edit it.",
    "graph.legend.hintMongo":
      "Click an entity to edit it · Drag from a root entity's + to an embedded document · Click an edge to edit it.",
    "graph.fullscreen.eyebrowRelations": "RELATION MAP",
    "graph.fullscreen.eyebrowCompositions": "COMPOSITION MAP",
    "graph.fullscreen.title": "Entities and their links",
    "graph.fullscreen.closeTooltip": "Close (Esc)",
    "graph.fullscreen.closeAria": "Close relation map",
    "graph.panel.titleRelations": "Relation map",
    "graph.panel.titleCompositions": "Composition map",
    "graph.panel.summary": ({ entities, links, mongo }) =>
      `${entities} ${plural(entities, "entity", "entities")} · ${links} ${
        mongo
          ? plural(links, "composition", "compositions")
          : plural(links, "relation", "relations")
      }`,
    "graph.linkWord.relation": "Relation",
    "graph.linkWord.composition": "Composition",
    "graph.notice.selfRelation": "An entity cannot relate to itself.",
    "graph.notice.embeddedSource":
      "An embedded document cannot originate compositions. Mark the source entity as a root entity.",
    "graph.notice.targetNeedsName":
      "The target entity needs a name before linking it.",
    "graph.notice.markEmbedded": ({ name }) =>
      `Mark "${name}" as an embedded document to use it in a composition.`,
    "graph.notice.targetMissing": "The target entity does not exist.",
    "graph.notice.selfLink": "An entity cannot link to itself.",
    "graph.notice.embeddedCannotComposeShort":
      "An embedded document cannot originate compositions.",
    "graph.notice.markEmbeddedMove": ({ name }) =>
      `Mark "${name}" as an embedded document to move the composition.`,
    "graph.notice.embeddedNotSql": ({ name }) =>
      `The entity "${name}" is marked as embedded and is not valid for SQL.`,
    "graph.notice.moved": ({ link, source, target }) =>
      `${link} moved: ${source} → ${target}. Click the edge to rename it.`,
    "graph.notice.created": ({ link, source, target }) =>
      `${link} created: ${source} → ${target}`,
    "graph.notice.updated": ({ link }) => `${link} updated`,
    "graph.notice.deleted": "Link deleted",

    "relation.eyebrowEdit": "EDIT RELATION",
    "relation.eyebrowCreate": "NEW RELATION",
    "relation.deleteWarning": ({ name, source }) =>
      `The attribute "${name}" will be removed from ${source}. This action cannot be undone.`,
    "relation.nameLabel": "Attribute name",
    "relation.helperCreate": "It is added as an attribute of the source entity.",
    "relation.helperEdit": "Name of the attribute on the source entity.",
    "relation.typeComposition": "Composition type",
    "relation.typeRelation": "Relation type",
    "relation.targetInvalid": ({ name }) =>
      `The entity "${name}" must be marked as an embedded document to use it in a composition.`,
    "relation.targetMissing": ({ name }) =>
      `The entity "${name}" does not exist. The relation will have no target until you create that entity.`,
    "relation.hintCreate": ({ source }) =>
      `You can also adjust this relation from the ${source} form.`,
    "relation.hintEdit":
      "You can also drag the relation's endpoint on the map to change the target entity.",
    "relation.back": "Back",
    "relation.deleteRelation": "Delete relation",
    "relation.createComposition": "Create composition",
    "relation.createRelation": "Create relation",
    "relation.error.targetEmbedded":
      "The target entity must be an embedded document",
    "relation.error.nameEmpty": "The name cannot be empty",
    "relation.error.nameStartsWithLetter": "The name must start with a letter",
    "relation.error.nameTooLong":
      "The name cannot be longer than 30 characters",
    "relation.error.nameDuplicate": ({ source }) =>
      `An attribute with that name already exists in ${source}`,
    "relation.type.mongoEmbedded": "1:1 · Embedded",
    "relation.type.mongoList": "1:N · List",
    "relation.type.manyToOne": "N:1 · Many to one",
    "relation.type.oneToMany": "1:N · One to many",
    "relation.type.oneToOne": "1:1 · One to one",
    "relation.type.manyToMany": "N:M · Many to many",

    "projects.eyebrow": "YOUR PROJECTS",
    "projects.title": "Saved projects",
    "projects.storageNote": "They are stored in this browser.",
    "projects.closeAria": "Close saved projects",
    "projects.emptyTitle": "There are no saved projects yet",
    "projects.emptyHint":
      "Use the Save button in the header to save the current work.",
    "projects.current": "Editing",
    "projects.deleteTooltip": "Delete project",
    "projects.deleteAria": ({ name }) => `Delete project ${name}`,
    "projects.unnamedEntity": "No name",
    "projects.new": "New project",
    "projects.import": "Import",
    "projects.export": "Export",
    "projects.importAria": "Import projects from a JSON file",
    "projects.footerNote":
      "Projects are stored only in this browser. Use Import/Export to move them between browsers.",
    "projects.deleteEyebrow": "DELETE PROJECT",
    "projects.deleteHeading": ({ name }) => `Delete "${name}"?`,
    "projects.deleteIntro":
      "It will be removed from this browser. This action cannot be undone.",
    "projects.meta": ({ date, entities }) =>
      `${date} · ${entities} ${plural(entities, "entity", "entities")}`,

    "saveDialog.eyebrow": "SAVE PROJECT",
    "saveDialog.title": "Name your project",
    "saveDialog.descriptionDefault":
      "Save the current work so you can resume it later.",
    "saveDialog.descriptionPending":
      "To open the selected project, first save the current work with a name.",
    "saveDialog.nameLabel": "Project name",
    "saveDialog.nameHelper": "It is also used as the ZIP file name.",
    "saveDialog.errorEmpty": "The project needs a name to be saved",
    "saveDialog.errorTooLong":
      "The name cannot be longer than 30 characters",

    "openDialog.eyebrow": "REPLACE CONFIGURATION",
    "openDialog.newTitle": "Start a new project",
    "openDialog.openTitle": ({ name }) => `Open "${name}"`,
    "openDialog.newIntro":
      "The current configuration will be cleared to start from scratch: name, entities, database, API and cache.",
    "openDialog.openIntro":
      "The current configuration will be replaced with the saved project's: name, entities, database, API and cache.",
    "openDialog.note": "Unsaved changes will be lost.",
    "openDialog.noteUnnamed":
      "The current work has no name: you can discard it or enter a name to save it.",
    "openDialog.noteNamed":
      "You can save the current work before continuing.",
    "openDialog.discard": "Discard changes",
    "openDialog.saveAndContinue": "Save and continue",

    "service.createError": "The project could not be created",
    "service.connectionError": "Could not connect to the server",
    "import.invalidJson": "The file is not valid JSON",
    "import.noList": "The file does not contain a list of projects",
    "import.noValidProjects": "No valid projects were found in the file",
  },
};

export const detectLanguage = () => {
  const fromUrl = readLanguageFromUrl();
  if (fromUrl) {
    return fromUrl;
  }

  try {
    const stored = window.localStorage.getItem(LANGUAGE_STORAGE_KEY);
    if (LANGUAGES.includes(stored)) {
      return stored;
    }
  } catch {
    // localStorage can be unavailable (private mode, disabled cookies).
  }

  const browserLanguage =
    typeof navigator !== "undefined" ? navigator.language || "" : "";
  return browserLanguage.toLowerCase().startsWith("en") ? "en" : DEFAULT_LANGUAGE;
};

export const translate = (language, key, params = {}) => {
  const normalized = LANGUAGES.includes(language) ? language : DEFAULT_LANGUAGE;
  const dictionary = translations[normalized] || translations[DEFAULT_LANGUAGE];
  const value = dictionary[key] ?? translations[DEFAULT_LANGUAGE][key] ?? key;

  if (typeof value === "function") {
    return value(params);
  }

  return String(value).replace(/\{(\w+)\}/g, (match, name) =>
    params[name] === undefined ? match : String(params[name]),
  );
};
