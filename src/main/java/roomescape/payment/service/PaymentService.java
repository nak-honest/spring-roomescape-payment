package roomescape.payment.service;


import org.springframework.stereotype.Service;
import roomescape.payment.domain.Payment;
import roomescape.payment.domain.PaymentRestClient;
import roomescape.payment.dto.PaymentCreateRequest;
import roomescape.payment.dto.RestClientPaymentApproveResponse;
import roomescape.payment.repository.PaymentRepository;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentRestClient restClient;

    public PaymentService(PaymentRepository paymentRepository,
                          PaymentRestClient paymentRestClient) {
        this.paymentRepository = paymentRepository;
        this.restClient = paymentRestClient;
    }

    public void approvePayment(PaymentCreateRequest paymentCreateRequest) {
        RestClientPaymentApproveResponse restClientPaymentApproveResponse =
                restClient.approvePayment(paymentCreateRequest);
        paymentRepository.save(restClientPaymentApproveResponse.createPayment(paymentCreateRequest.reservation()));
    }

    public void cancelPayment(Long reservationId) {
        Payment payment = paymentRepository.findByReservation_Id(reservationId);
        restClient.cancelPayment(payment.getPaymentKey());
        paymentRepository.deleteByReservation_Id(payment.getReservationId());
    }
}
