## Documentar importante 
- Tenemos una clase WebConfig : Dentro de ella identificamos un resolvedor de parametros
  . Este se conecta a ActiveUserArgumentResolver el cual permite resolover  el parametro @Activeuser.
- Entonces cada endpoind que tenga este parametro @ActiveUser el sistema automaticamente buca resolver este parametro con ActiveUserArgumentResolver, ve si tiene este formato "X-User-Id" y mira si el "id" del usuario existe. 

- Ahora si te das cuenta aqui "public ActiveUserArgumentResolver(@Lazy UserRepository userRepository) "  lo estoy poniendo como @Lazy para que  solo pueda resolverse cuando se ha necesario . 

- Además, ese "public ActiveUserArgumentResolver(@Lazy UserRepository userRepository) " el principal motivo por el que se puso @Lazy es porque , evito errores en las pruebas. 
Si te das cuenta el CatalogController no tiene @ActiveUser entonces Spring ni debería preocuparse por ActiveUserArgumentResolver.""Pero spring al momento de correr las pruebas , crea el contexto  y lee ActiveUserArgumentResolver y entonces buca resolver el parametro UserRepository y como antes no tenia Lazy siempre se cargaba y al cargarse se conecta con userRepository para identificar el "id" del usuario"" . Entonces como no era @lazy el userRepository  , eso hacia que siempre buscara conectarse al JPA , entonces al correr las test del catalogo . Salia error , porque queria conectarse al JPA del catalogo , pero catalogo no tiene JPA y ahi daba error . Entonces "con ese lazy se soluciono".