package roomescape.reservation.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import roomescape.member.domain.Member;
import roomescape.member.repository.MemberRepository;
import roomescape.paymenthistory.service.PaymentHistoryService;
import roomescape.reservation.domain.Reservation;
import roomescape.reservation.dto.AdminReservationCreateRequest;
import roomescape.reservation.dto.MyReservationWaitingResponse;
import roomescape.reservation.dto.ReservationCreateRequest;
import roomescape.reservation.dto.ReservationResponse;
import roomescape.reservation.dto.ReservationSearchRequest;
import roomescape.reservation.repository.ReservationRepository;
import roomescape.theme.domain.Theme;
import roomescape.theme.repository.ThemeRepository;
import roomescape.time.domain.ReservationTime;
import roomescape.time.repository.TimeRepository;
import roomescape.waiting.domain.Waiting;
import roomescape.waiting.repository.WaitingRepository;

@Service
public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final MemberRepository memberRepository;
    private final TimeRepository timeRepository;
    private final ThemeRepository themeRepository;
    private final WaitingRepository waitingRepository;
    private final PaymentHistoryService paymentHistoryService;

    public ReservationService(ReservationRepository reservationRepository, MemberRepository memberRepository,
                              TimeRepository timeRepository, ThemeRepository themeRepository,
                              WaitingRepository waitingRepository, PaymentHistoryService paymentHistoryService) {
        this.reservationRepository = reservationRepository;
        this.memberRepository = memberRepository;
        this.timeRepository = timeRepository;
        this.themeRepository = themeRepository;
        this.waitingRepository = waitingRepository;
        this.paymentHistoryService = paymentHistoryService;
    }

    public List<ReservationResponse> findReservations() {
        return reservationRepository.findAll()
                .stream()
                .map(ReservationResponse::from)
                .toList();
    }

    public List<ReservationResponse> findReservations(ReservationSearchRequest request) {
        return reservationRepository.findAllByCondition(request.memberId(), request.themeId(), request.startDate(),
                        request.endDate())
                .stream()
                .map(ReservationResponse::from)
                .toList();
    }

    public List<MyReservationWaitingResponse> findMyReservations(Long memberId) {
        return reservationRepository.findByMember_id(memberId)
                .stream()
                .map(MyReservationWaitingResponse::from)
                .toList();
    }

    public ReservationResponse createAdminReservation(AdminReservationCreateRequest request) {
        Member member = findMemberByMemberId(request.memberId());
        ReservationTime time = findTimeByTimeId(request.timeId());
        Theme theme = findThemeByThemeId(request.themeId());
        Reservation reservation = request.createReservation(member, time, theme);
        validateCreate(reservation);

        return createReservation(reservation);
    }

    @Transactional
    public ReservationResponse createReservation(ReservationCreateRequest request, Long memberId) {
        Member member = findMemberByMemberId(memberId);
        ReservationTime time = findTimeByTimeId(request.timeId());
        Theme theme = findThemeByThemeId(request.themeId());
        Reservation reservation = request.createReservation(member, time, theme);
        validateCreate(reservation);

        ReservationResponse reservationResponse = createReservation(reservation);
        paymentHistoryService.approvePayment(request.createPaymentRequest(reservation));

        return reservationResponse;
    }

    private void validateCreate(Reservation reservation) {
        validateIsAvailable(reservation);
        validateExists(reservation);
    }

    private void validateIsAvailable(Reservation reservation) {
        if (reservation.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("예약은 현재 시간 이후여야 합니다.");
        }
    }

    private void validateExists(Reservation reservation) {
        if (reservationRepository.existsByDateAndTime_idAndTheme_id(
                reservation.getDate(), reservation.getTimeId(), reservation.getThemeId())) {
            throw new IllegalArgumentException("해당 날짜와 시간에 이미 예약된 테마입니다.");
        }
    }

    private ReservationResponse createReservation(Reservation reservation) {
        Reservation createdReservation = reservationRepository.save(reservation);
        return ReservationResponse.from(createdReservation);
    }

    private Member findMemberByMemberId(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("해당 멤버가 존재하지 않습니다."));
    }

    private ReservationTime findTimeByTimeId(Long timeId) {
        return timeRepository.findById(timeId)
                .orElseThrow(() -> new IllegalArgumentException("해당 예약 시간이 존재하지 않습니다."));
    }

    private Theme findThemeByThemeId(Long themeId) {
        return themeRepository.findById(themeId)
                .orElseThrow(() -> new IllegalArgumentException("해당 테마가 존재하지 않습니다."));
    }

    @Transactional
    public void deleteReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 예약은 존재하지 않습니다."));
        validateDelete(reservation);

        waitingRepository.findFirstByReservation_idOrderByCreatedAtAsc(id)
                .ifPresentOrElse(this::promoteWaiting, () -> cancelReservation(reservation));

    }

    private void validateDelete(Reservation reservation) {
        validatePastReservation(reservation);
        validateEqualsDateReservation(reservation);
    }

    private void validatePastReservation(Reservation reservation) {
        if (reservation.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("과거 예약에 대한 취소는 불가능합니다.");
        }
    }

    private void validateEqualsDateReservation(Reservation reservation) {
        if (reservation.isEqualsDate(LocalDate.now())) {
            throw new IllegalArgumentException("당일 예약 취소는 불가능합니다.");
        }
    }

    private void promoteWaiting(Waiting waiting) {
        paymentHistoryService.cancelPayment(waiting.getReservation().getId());

        Reservation promotedReservation = waiting.promoteToReservation();
        reservationRepository.save(promotedReservation);
        waitingRepository.deleteById(waiting.getId());
    }

    private void cancelReservation(Reservation reservation) {
        if (reservation.isNotPaidReservation()) {
            reservationRepository.deleteById(reservation.getId());
            return;
        }

        paymentHistoryService.cancelPayment(reservation.getId());
        reservationRepository.deleteById(reservation.getId());
    }
}
