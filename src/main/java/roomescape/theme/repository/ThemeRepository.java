package roomescape.theme.repository;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;
import roomescape.theme.domain.Theme;
import roomescape.theme.domain.ThemeName;

@Repository
public interface ThemeRepository extends ListCrudRepository<Theme, Long> {

    @Query("""
            SELECT t FROM Reservation AS r
            JOIN r.theme AS t
            WHERE r.date >= :startDate AND r.date <= :endDate
            GROUP BY t.id
            ORDER BY count(r.id) DESC
            LIMIT :count
             """)
    List<Theme> findThemesSortedByCountOfReservation(LocalDate startDate, LocalDate endDate, int count);

    boolean existsByName(ThemeName name);
}