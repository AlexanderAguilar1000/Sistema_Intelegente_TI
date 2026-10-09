package com.corporativoTI.SistemaInteligenteTI.incidents.model;

import java.util.EnumMap;
import java.util.Map;
import org.springframework.stereotype.Component;
//ESTO ACTUALIZA EL ESTADO DEL INCIDENTE AUTOMATICAMENTE    

/**
 * Única fuente de las transiciones de estado del incidente (plan, sección 4). Solo conoce el grafo
 * de estados; las condiciones de cada caso de uso (área del técnico, longitud de la solución, rol)
 * las comprueban los servicios.
 */
@Component
public class IncidentStateMachine {

    private static final Map<IncidentStatus, Map<IncidentAction, IncidentStatus>> TRANSITIONS =
            new EnumMap<>(IncidentStatus.class);

    static {
        for (IncidentStatus status : IncidentStatus.values()) {
            TRANSITIONS.put(status, new EnumMap<>(IncidentAction.class));
        }
        TRANSITIONS.get(IncidentStatus.REGISTERED).put(IncidentAction.ASSIGN, IncidentStatus.ASSIGNED);

        TRANSITIONS.get(IncidentStatus.ASSIGNED).put(IncidentAction.START, IncidentStatus.IN_PROGRESS);

        for (IncidentStatus active : new IncidentStatus[] {IncidentStatus.ASSIGNED, IncidentStatus.IN_PROGRESS}) {
            Map<IncidentAction, IncidentStatus> actions = TRANSITIONS.get(active);
            actions.put(IncidentAction.REASSIGN, IncidentStatus.ASSIGNED);
            actions.put(IncidentAction.UNASSIGN_BY_AREA_CHANGE, IncidentStatus.REGISTERED);
            actions.put(IncidentAction.CLOSE_WITHOUT_SOLUTION, IncidentStatus.CLOSED_WITHOUT_SOLUTION);
        }

        TRANSITIONS.get(IncidentStatus.IN_PROGRESS).put(IncidentAction.RESOLVE, IncidentStatus.RESOLVED);
    }
}
    //esto permite ver si una acción es válida para un estado dado
    public boolean canApply(IncidentStatus from, IncidentAction action) {
        return TRANSITIONS.get(from).containsKey(action);
    }

    /** Devuelve el estado resultante o lanza {@link InvalidTransitionException}. */
    //si por ejemplo el sistema mira que se cambio el estado de registrado a solucionado , devueve error .No lo permite . 
    public IncidentStatus transition(IncidentStatus from, IncidentAction action) {
        IncidentStatus target = TRANSITIONS.get(from).get(action);
        if (target == null) {
            throw new InvalidTransitionException(from, action);
        }
        return target;
    }
}
