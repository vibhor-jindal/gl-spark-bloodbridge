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
        when(emailChannel.getType()).thenReturn(Channel.EMAIL);
        when(smsChannel.getType()).thenReturn(Channel.SMS);
        when(pushChannel.getType()).thenReturn(Channel.PUSH);

        dispatchService = new NotificationDispatchService(
                List.of(pushChannel, smsChannel, emailChannel), notificationLogRepository);

        when(notificationLogRepository.save(any(NotificationLog.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("US-007 AC1: A successful email dispatch logs SENT on the EMAIL channel, no fallback needed")
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
    @DisplayName("US-007 AC2: Email failing (even after one retry) falls back to SMS")
    void testDispatch_EmailFailsTwice_FallsBackToSms() {
        when(emailChannel.send(any(), any(), any())).thenReturn(false);
        when(smsChannel.send(any(), any(), any())).thenReturn(true);

        NotificationLog result = dispatchService.dispatch(1L, RecipientType.DONOR, 100L,
                "donor@example.com", "9876543210", "subject", "message");

        assertThat(result.getChannel()).isEqualTo(Channel.SMS);
        assertThat(result.getStatus()).isEqualTo(DeliveryStatus.SENT);
        verify(emailChannel, times(2)).send(any(), any(), any());
    }

    @Test
    @DisplayName("US-007: If every channel fails, the log records FAILED on the last channel attempted")
    void testDispatch_AllChannelsFail_LogsFailed() {
        when(emailChannel.send(any(), any(), any())).thenReturn(false);
        when(smsChannel.send(any(), any(), any())).thenReturn(false);
        when(pushChannel.send(any(), any(), any())).thenReturn(false);

        NotificationLog result = dispatchService.dispatch(1L, RecipientType.DONOR, 100L,
                "donor@example.com", "9876543210", "subject", "message");

        assertThat(result.getStatus()).isEqualTo(DeliveryStatus.FAILED);
        assertThat(result.getChannel()).isEqualTo(Channel.PUSH);
    }

    @Test
    @DisplayName("US-007 AC3: Every dispatch attempt is persisted with a delivery status and timestamp")
    void testDispatch_AlwaysPersistsLogEntry() {
        when(emailChannel.send(any(), any(), any())).thenReturn(true);

        dispatchService.dispatch(1L, RecipientType.REQUESTER, 100L, "req@example.com", null, "s", "m");

        verify(notificationLogRepository, times(1)).save(any(NotificationLog.class));
    }
}
