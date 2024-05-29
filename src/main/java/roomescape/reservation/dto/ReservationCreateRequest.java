package roomescape.reservation.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import roomescape.member.domain.Member;
import roomescape.paymenthistory.PaymentType;
import roomescape.paymenthistory.dto.PaymentCreateRequest;
import roomescape.reservation.domain.Reservation;
import roomescape.theme.domain.Theme;
import roomescape.time.domain.ReservationTime;

// TODO: 내부 필드들 record로 감싸보기
public record ReservationCreateRequest(
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate date,
        Long timeId,
        Long themeId,
        String paymentKey,
        String orderId,
        int amount,
        PaymentType paymentType
) {
    public Reservation createReservation(Member member, ReservationTime time, Theme theme) {
        return new Reservation(member, date, time, theme);
    }

    public PaymentCreateRequest createPaymentRequest(Reservation reservation) {
        return new PaymentCreateRequest(paymentKey, orderId, amount, reservation);
    }
}