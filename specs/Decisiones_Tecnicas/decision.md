# Decisiones Técnicas
- Si la IA tarda en clasificar el incidente más de 10 segundos , se queda como registrado , pero la clasificación se pone en estado pendiente , dejando todos los campos de tipo de incidente , prioridad, etc vacios . 
- El técnico se le aparece una lista de pasos que le recomendara groq para solucionar el incidente y abajo aparecera una **seccion de casos similares** ahi que aparesca por orden el que más se acerca al caso  y que cuando apriete en esa misma pestaña aparesca  el detalle de ese incidente , como lo soluciono ese incidente , esto ayuda a verificar que la IA esta recomendando correctamente la solución

## Diseño del sistema SUPERVISOR
- El supervisor se encarga de añadir técnico a la plataforma
- El supervisor crea un incidente 
- El supervisor se encarga de **asignar un técnico a un incidente** 
- El supervisor tendra acceso a la **lista global de incidentes** podrá **Editar y visualizar**    incidente.
- El supervisor si puede **cambiar el técnico asignado a un incidente** . Si el incidente esta en **estado asignado o en progreso** se puede cambiar el técnico , pero si no es el caso . El sistema bloquea cualquier intento de reasignación  y el sistema muestra una lista de técnicos que se pueden reasignar , pero tiene que petenecer al mismo área. 

- El supervisor no puede cambiar el estado del incidente **El sistema lo hace**. Ejemplo de estados 
progreso, resuelto, etc . 

## Diseño del sistema TÉCNICO 

- El técnico puede cerrar un incidente .Se le aparecera un modal para que seleccione 
**NOT_REPRODUCIBLE** Este es cuando el técnico realiza una **prueba en el sistema** ,  para validar que realmente esta ocurriendo ese incidente  y se da cuenta que el sistema o servicio funciona correctamente y que el incidente no existe . 
El técnico puede apretar **DISCARDED** cuando se da cuenta que el incidente **fue registrado por equivocación no es necesario realizar pruebas** , se sabe que es un error .
El técnico puede poner **DUPLICATE** cuando detecta que hay un incidente duplicado . 

- El técnico es el único que puede **modificar el estado del incidente** , como lo mencione arriba. A cualquiera de esas opciones. 
- El técnico puede visualizar un incidente. 