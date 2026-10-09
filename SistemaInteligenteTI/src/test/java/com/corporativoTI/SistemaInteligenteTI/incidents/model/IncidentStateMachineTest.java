package com.corporativoTI.SistemaInteligenteTI.incidents.model;

import static com.corporativoTI.SistemaInteligenteTI.incidents.model.IncidentAction.ASSIGN;
import static com.corporativoTI.SistemaInteligenteTI.incidents.model.IncidentAction.CLOSE_WITHOUT_SOLUTION;
import static com.corporativoTI.SistemaInteligenteTI.incidents.model.IncidentAction.REASSIGN;
import static com.corporativoTI.SistemaInteligenteTI.incidents.model.IncidentAction.RESOLVE;
import static com.corporativoTI.SistemaInteligenteTI.incidents.model.IncidentAction.START;
import static com.corporativoTI.SistemaInteligenteTI.incidents.model.IncidentAction.UNASSIGN_BY_AREA_CHANGE;
import static com.corporativoTI.SistemaInteligenteTI.incidents.model.IncidentStatus.ASSIGNED;
import static com.corporativoTI.SistemaInteligenteTI.incidents.model.IncidentStatus.CLOSED_WITHOUT_SOLUTION;
import static com.corporativoTI.SistemaInteligenteTI.incidents.model.IncidentStatus.IN_PROGRESS;
import static com.corporativoTI.SistemaInteligenteTI.incidents.model.IncidentStatus.REGISTERED;
import static com.corporativoTI.SistemaInteligenteTI.incidents.model.IncidentStatus.RESOLVED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * RF-3, RF-7, RF-10, RF-12, RF-14 (T-10): transiciones de la sección 4 del plan. Test de dominio
 * puro, sin Spring ni base de datos.
 */
class IncidentStateMachineTest {

    private final IncidentStateMachine stateMachine = new IncidentStateMachine();

    /** Todas las transiciones válidas de la sección 4: (estado, acción, estado resultante). */
    static Stream<Arguments> validTransitions() {
        return Stream.of(
                Arguments.of(REGISTERED, ASSIGN, ASSIGNED),
                Arguments.of(ASSIGNED, START, IN_PROGRESS),
                Arguments.of(ASSIGNED, REASSIGN, ASSIGNED),
                Arguments.of(IN_PROGRESS, REASSIGN, ASSIGNED),
                Arguments.of(ASSIGNED, UNASSIGN_BY_AREA_CHANGE, REGISTERED),
                Arguments.of(IN_PROGRESS, UNASSIGN_BY_AREA_CHANGE, REGISTERED),
                Arguments.of(IN_PROGRESS, RESOLVE, RESOLVED),
                Arguments.of(ASSIGNED, CLOSE_WITHOUT_SOLUTION, CLOSED_WITHOUT_SOLUTION),
                Arguments.of(IN_PROGRESS, CLOSE_WITHOUT_SOLUTION, CLOSED_WITHOUT_SOLUTION));
    }

    @ParameterizedTest(name = "{0} --{1}--> {2}")
    @MethodSource("validTransitions")
    void acceptsEveryValidTransition(IncidentStatus from, IncidentAction action, IncidentStatus expected) {
        assertThat(stateMachine.canApply(from, action)).isTrue();
        assertThat(stateMachine.transition(from, action)).isEqualTo(expected);
    }

    /** Todo par (estado, acción) que no está en la lista de válidas debe rechazarse. */
    static Stream<Arguments> invalidTransitions() {
        Set<String> valid = new java.util.HashSet<>();
        validTransitions().forEach(a -> valid.add(a.get()[0] + ":" + a.get()[1]));
        return EnumSet.allOf(IncidentStatus.class).stream()
                .flatMap(s -> EnumSet.allOf(IncidentAction.class).stream().map(a -> Arguments.of(s, a)))
                .filter(a -> !valid.contains(a.get()[0] + ":" + a.get()[1]));
    }

    @ParameterizedTest(name = "{0} --{1}--> rechazada")
    @MethodSource("invalidTransitions")
    void rejectsEveryInvalidTransition(IncidentStatus from, IncidentAction action) {
        assertThat(stateMachine.canApply(from, action)).isFalse();
        assertThatThrownBy(() -> stateMachine.transition(from, action))
                .isInstanceOf(InvalidTransitionException.class)
                .hasMessageContaining(from.name())
                .hasMessageContaining(action.name());
    }

    @Test
    void cannotResolveWithoutStarting() {
        assertThatThrownBy(() -> stateMachine.transition(ASSIGNED, RESOLVE))
                .isInstanceOf(InvalidTransitionException.class);
        assertThatThrownBy(() -> stateMachine.transition(REGISTERED, RESOLVE))
                .isInstanceOf(InvalidTransitionException.class);
    }

    @Test
    void finalStatesAcceptNoAction() {
        Map<IncidentStatus, String> finals =
                Map.of(RESOLVED, "RESOLVED", CLOSED_WITHOUT_SOLUTION, "CLOSED_WITHOUT_SOLUTION");
        for (IncidentStatus finalState : finals.keySet()) {
            assertThat(finalState.isFinal()).isTrue();
            for (IncidentAction action : IncidentAction.values()) {
                assertThat(stateMachine.canApply(finalState, action)).isFalse();
            }
        }
        assertThat(REGISTERED.isFinal()).isFalse();
        assertThat(ASSIGNED.isFinal()).isFalse();
        assertThat(IN_PROGRESS.isFinal()).isFalse();
    }

    @Test
    void cannotCloseWithoutSolutionAnUnassignedIncident() {
        assertThatThrownBy(() -> stateMachine.transition(REGISTERED, CLOSE_WITHOUT_SOLUTION))
                .isInstanceOf(InvalidTransitionException.class);
    }
}
