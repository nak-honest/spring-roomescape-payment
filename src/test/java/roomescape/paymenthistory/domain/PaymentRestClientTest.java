package roomescape.paymenthistory.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import roomescape.fixture.MemberFixture;
import roomescape.fixture.ThemeFixture;
import roomescape.fixture.TimeFixture;
import roomescape.paymenthistory.dto.PaymentCreateRequest;
import roomescape.paymenthistory.exception.PaymentException;
import roomescape.reservation.domain.Reservation;

@SpringBootTest
class PaymentRestClientTest {

    @Autowired
    PaymentRestClient paymentRestClient;

    @DisplayName("결제 시간이 만료된 경우 커스텀 예외를 발생한다.")
    @Test
    void approvePaymentTest_whenInvalidSecretKeyd() {
        PaymentCreateRequest paymentCreateRequest = new PaymentCreateRequest(
                "tgen_20240528211", "MC40MTMwMTk0ODU0ODU4", 1000, new Reservation(1L, MemberFixture.MEMBER_BRI,
                LocalDate.now().plusDays(1), TimeFixture.TIME_1, ThemeFixture.THEME_1));

        assertThatThrownBy(
                () -> paymentRestClient.approvePayment(paymentCreateRequest))
                .isInstanceOf(PaymentException.class)
                .hasMessage("결제 시간이 만료되어 결제 진행 데이터가 존재하지 않습니다.");
    }
}