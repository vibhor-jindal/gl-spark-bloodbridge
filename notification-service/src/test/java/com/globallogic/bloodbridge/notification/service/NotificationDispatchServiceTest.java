package com.globallogic.bloodbridge.notification.service;

import com.globallogic.bloodbridge.notification.channel.NotificationChannel;
import com.globallogic.bloodbridge.notification.model.Channel;
import com.globallogic.bloodbridge.notification.model.DeliveryStatus;
import com.globallogic.bloodbridge.notification.model.NotificationLog;
import com.globallogic.bloodbridge.notification.model.RecipientType;
import com.globallogic.bloodbridge.notification.repository.NotificationLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationDispatchServiceTest {

    @Mock
    private NotificationChannel emailChannel;

    @Mock
    private NotificationChannel smsChannel;

    @Mock
    private NotificationChannel pushChannel;

    @Mock
    private NotificationLogRepository notificationLogRepository;

    private NotificationDispatchService dispatchService;

    @BeforeEach
    void setUp() {
        lenient().when(emailChannel.getType()).thenReturn(Channel.EMAIL);
        lenient().when(smsChannel.getType()).thenReturn(Channel.SMS);
        lenient().when(pushChannel.getType()).thenReturn(Channel.PUSH);

        // SMS is present in the list but must be ignored by the dispatcher.
        dispatchService = new NotificationDispatchService(
                List.of(pushChannel, smsChannel, emailChannel), notificationLogRepository);

        when(notificationLogRepository.save(any(NotificationLog.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("Email success logs SENT on EMAIL and never uses SMS or PUSH")
    void testDispatch_EmailSucceeds_NoFallback() {
        when(emailChannel.send(any(), any(), any())).thenReturn(true);

        NotificationLog result = dispatchService.dispatch(1L, RecipientType.DONOR, 100L,
                "donor@example.com", "9876543210", "subject", "message");

        assertThat(result.getChannel()).isEqualTo(Channel.EMAIL);
        assertThat(result.getStatus()).isEqualTo(DeliveryStatus.SENT);
        verify(smsChannel, never()).send(any(), any(), any());
        verify(pushChannel, never()).send(any(), any(), any());
    }

    @Test
    @DisplayName("When a real email is available, EMAIL failure marks FAILED — never fake PUSH success")
    void testDispatch_EmailFails_DoesNotFallBackToPush() {
        when(emailChannel.send(any(), any(), any())).thenReturn(false);

        NotificationLog result = dispatchService.dispatch(1L, RecipientType.DONOR, 100L,
                "donor@example.com", "9876543210", "subject", "message");

        assertThat(result.getChannel()).isEqualTo(Channel.EMAIL);
        assertThat(result.getStatus()).isEqualTo(DeliveryStatus.FAILED);
        verify(emailChannel, times(2)).send(any(), any(), any());
        verify(pushChannel, never()).send(any(), any(), any());
        verify(smsChannel, never()).send(any(), any(), any());
    }

    @Test
    @DisplayName("With no email, PUSH may be used as last-resort simulated channel")
    void testDispatch_NoEmail_UsesPush() {
        when(pushChannel.send(any(), any(), any())).thenReturn(true);

        NotificationLog result = dispatchService.dispatch(1L, RecipientType.DONOR, 100L,
                null, "9876543210", "subject", "message");

        assertThat(result.getChannel()).isEqualTo(Channel.PUSH);
        assertThat(result.getStatus()).isEqualTo(DeliveryStatus.SENT);
        verify(emailChannel, never()).send(any(), any(), any());
        verify(smsChannel, never()).send(any(), any(), any());
    }

    @Test
    @DisplayName("If email and push are unavailable, log FAILED")
    void testDispatch_NoChannels_LogsFailed() {
        NotificationLog result = dispatchService.dispatch(1L, RecipientType.DONOR, 100L,
                "", null, "subject", "message");

        assertThat(result.getStatus()).isEqualTo(DeliveryStatus.FAILED);
        verify(smsChannel, never()).send(any(), any(), any());
        verify(pushChannel, never()).send(any(), any(), any());
    }

    @Test
    @DisplayName("Every dispatch attempt is persisted")
    void testDispatch_AlwaysPersistsLogEntry() {
        when(emailChannel.send(any(), any(), any())).thenReturn(true);

        dispatchService.dispatch(1L, RecipientType.REQUESTER, 100L, "req@example.com", null, "s", "m");

        verify(notificationLogRepository, times(1)).save(any(NotificationLog.class));
    }
}
