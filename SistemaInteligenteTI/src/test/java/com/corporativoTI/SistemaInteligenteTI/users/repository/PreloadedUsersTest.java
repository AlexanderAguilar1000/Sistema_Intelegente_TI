package com.corporativoTI.SistemaInteligenteTI.users.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.corporativoTI.SistemaInteligenteTI.catalog.model.Area;
import com.corporativoTI.SistemaInteligenteTI.users.model.Role;
import com.corporativoTI.SistemaInteligenteTI.users.model.User;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;

/**
 * RF-1, RF-2 (T-02): al arrancar de cero la tabla {@code users} contiene supervisores y técnicos de
 * ejemplo en áreas distintas, y volver a arrancar (o relanzar la carga) no los duplica.
 */
@SpringBootTest
@Transactional
class PreloadedUsersTest {

    private static final String SEED_SCRIPT = "db/migration/V2__seed_users.sql";

    @Autowired private UserRepository userRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private Flyway flyway;

    private List<User> usersWithRole(Role role) {
        return userRepository.findAll().stream().filter(u -> u.getRole() == role).toList();
    }

    @Test
    void preloadsAtLeastTwoSupervisorsWithoutArea() {
        List<User> supervisors = usersWithRole(Role.SUPERVISOR);

        assertThat(supervisors).hasSizeGreaterThanOrEqualTo(2);
        assertThat(supervisors).allSatisfy(s -> assertThat(s.getArea()).isNull());
    }

    @Test
    void preloadsAtLeastFourTechniciansSpreadAcrossDifferentAreas() {
        List<User> technicians = usersWithRole(Role.TECHNICIAN);

        assertThat(technicians).hasSizeGreaterThanOrEqualTo(4);
        assertThat(technicians).allSatisfy(t -> assertThat(t.getArea()).isNotNull());

        List<Area> areas = technicians.stream().map(User::getArea).distinct().toList();
        assertThat(areas).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void preloadedUsersHaveUniqueUsernamesAndNames() {
        List<User> users = userRepository.findAll();

        assertThat(users).allSatisfy(u -> {
            assertThat(u.getUsername()).isNotBlank();
            assertThat(u.getFullName()).isNotBlank();
        });
        List<String> usernames = users.stream().map(User::getUsername).collect(Collectors.toList());
        assertThat(usernames).doesNotHaveDuplicates();
    }

    @Test
    void restartingTheMigrationsDoesNotDuplicateUsers() {
        long before = userRepository.count();

        flyway.migrate();

        assertThat(userRepository.count()).isEqualTo(before);
    }

    @Test
    void rerunningTheSeedScriptDoesNotDuplicateUsers() throws Exception {
        String script =
                StreamUtils.copyToString(
                        new ClassPathResource(SEED_SCRIPT).getInputStream(), StandardCharsets.UTF_8);
        long before = userRepository.count();

        jdbcTemplate.execute(script);
        jdbcTemplate.execute(script);

        assertThat(userRepository.count()).isEqualTo(before);
    }
}
