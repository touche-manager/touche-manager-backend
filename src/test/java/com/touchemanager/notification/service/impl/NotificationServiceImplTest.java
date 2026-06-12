package com.touchemanager.notification.service.impl;

import com.touchemanager.athlete.entity.Athlete;
import com.touchemanager.auth.entity.User;
import com.touchemanager.auth.repository.UserRepository;
import com.touchemanager.bout.entity.Bout;
import com.touchemanager.bout.repository.BoutRepository;
import com.touchemanager.notification.dto.NotificationDTO;
import com.touchemanager.notification.dto.NotifyUpcomingBoutRequest;
import com.touchemanager.notification.entity.Notification;
import com.touchemanager.notification.entity.NotificationType;
import com.touchemanager.notification.repository.NotificationRepository;
import com.touchemanager.shared.exception.BoutNotFoundException;
import com.touchemanager.tournament.entity.Tournament;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private BoutRepository boutRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private NotificationServiceImpl service;

    private Bout bout;
    private User leftUser;
    private User rightUser;

    @BeforeEach
    void setUp() {
        Tournament tournament = new Tournament();
        ReflectionTestUtils.setField(tournament, "id", 10L);
        tournament.setName("Torneo Apertura");

        leftUser = new User();
        ReflectionTestUtils.setField(leftUser, "id", 100L);
        rightUser = new User();
        ReflectionTestUtils.setField(rightUser, "id", 200L);

        Athlete left = athlete(1L, "Carlos", "Gómez", leftUser);
        Athlete right = athlete(2L, "Facundo", "García", rightUser);

        bout = new Bout();
        ReflectionTestUtils.setField(bout, "id", 5L);
        bout.setTournament(tournament);
        bout.setAthleteLeft(left);
        bout.setAthleteRight(right);
    }

    private Athlete athlete(Long id, String firstName, String lastName, User user) {
        Athlete a = new Athlete();
        ReflectionTestUtils.setField(a, "id", id);
        a.setFirstName(firstName);
        a.setLastName(lastName);
        a.setUser(user);
        return a;
    }

    private NotifyUpcomingBoutRequest request(int minutes, String piste) {
        NotifyUpcomingBoutRequest r = new NotifyUpcomingBoutRequest();
        r.setMinutesAhead(minutes);
        r.setPiste(piste);
        return r;
    }

    @Test
    @DisplayName("notifyUpcomingBout persists one notification per athlete and pushes them via WebSocket")
    void notifyUpcomingBout_notifiesBothAthletes() {
        when(boutRepository.findById(5L)).thenReturn(Optional.of(bout));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        List<NotificationDTO> sent = service.notifyUpcomingBout("org@test.com", 5L, request(5, "Pista A"));

        assertThat(sent).hasSize(2);
        assertThat(sent.get(0).type()).isEqualTo(NotificationType.UPCOMING_BOUT);
        assertThat(sent.get(0).message()).contains("5 minutos").contains("Facundo García").contains("Pista A");
        assertThat(sent.get(1).message()).contains("Carlos Gómez");

        verify(messagingTemplate).convertAndSendToUser(eq("100"), eq("/queue/notifications"), any(NotificationDTO.class));
        verify(messagingTemplate).convertAndSendToUser(eq("200"), eq("/queue/notifications"), any(NotificationDTO.class));
    }

    @Test
    @DisplayName("notifyUpcomingBout persists the piste on the bout when provided")
    void notifyUpcomingBout_assignsPiste() {
        when(boutRepository.findById(5L)).thenReturn(Optional.of(bout));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        service.notifyUpcomingBout("org@test.com", 5L, request(5, "Pista 3"));

        assertThat(bout.getPiste()).isEqualTo("Pista 3");
        verify(boutRepository).save(bout);
    }

    @Test
    @DisplayName("notifyUpcomingBout fails with BoutNotFoundException for an unknown bout")
    void notifyUpcomingBout_unknownBout() {
        when(boutRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.notifyUpcomingBout("org@test.com", 99L, request(5, null)))
                .isInstanceOf(BoutNotFoundException.class);
    }

    @Test
    @DisplayName("markAsRead rejects a notification that belongs to another user")
    void markAsRead_rejectsForeignNotification() {
        User me = new User();
        ReflectionTestUtils.setField(me, "id", 1L);
        when(userRepository.findByEmail("me@test.com")).thenReturn(Optional.of(me));

        Notification foreign = new Notification();
        foreign.setRecipientUserId(999L);
        when(notificationRepository.findById(7L)).thenReturn(Optional.of(foreign));

        assertThatThrownBy(() -> service.markAsRead("me@test.com", 7L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("markAsRead flags the notification as read")
    void markAsRead_setsReadFlag() {
        User me = new User();
        ReflectionTestUtils.setField(me, "id", 100L);
        when(userRepository.findByEmail("me@test.com")).thenReturn(Optional.of(me));

        Notification mine = new Notification();
        mine.setRecipientUserId(100L);
        when(notificationRepository.findById(7L)).thenReturn(Optional.of(mine));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        NotificationDTO result = service.markAsRead("me@test.com", 7L);

        assertThat(result.read()).isTrue();
    }
}
