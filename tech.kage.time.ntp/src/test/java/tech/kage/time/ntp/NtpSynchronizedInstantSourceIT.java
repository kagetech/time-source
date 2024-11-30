/*
 * Copyright (c) 2024, Dariusz Szpakowski
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package tech.kage.time.ntp;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.InstantSource;

import org.junit.jupiter.api.Test;

/**
 * Integration tests for {@link NtpSynchronizedInstantSource}.
 * 
 * @author Dariusz Szpakowski
 */
class NtpSynchronizedInstantSourceIT {
    static final String NTP_SERVER = "pool.ntp.org";

    @Test
    void retrievesNtpOffsetDuringSync() throws Exception {
        // Given
        var instantSource = new NtpSynchronizedInstantSource(NTP_SERVER);

        // When
        instantSource.sync();

        // Then
        var offset = readOffset(instantSource);

        assertNotNull(offset, "offset should be set");
    }

    @Test
    void addsNtpOffsetToReturnedInstant() throws Exception {
        // Given
        var instantSource = new NtpSynchronizedInstantSource(NTP_SERVER);

        var offset = 15000;

        injectOffset(instantSource, offset);

        var expectedInstant = Instant.now().plusMillis(offset);

        // When
        var instant = instantSource.instant();

        // Then
        assertEquals(expectedInstant.truncatedTo(SECONDS), instant.truncatedTo(SECONDS));
    }

    @Test
    void throwsExceptionWhenSyncNotPerformed() throws Exception {
        // Given
        var instantSource = new NtpSynchronizedInstantSource(NTP_SERVER);

        // When
        var thrown = assertThrows(IllegalStateException.class, () -> instantSource.instant());

        // Then
        assertTrue(thrown.getMessage().contains("Offset not set"), "sync() must be performed before first use");
    }

    private Long readOffset(InstantSource instantSource)
            throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        var offsetField = NtpSynchronizedInstantSource.class.getDeclaredField("offset");

        offsetField.setAccessible(true);
        return (Long) offsetField.get(instantSource);
    }

    private void injectOffset(InstantSource instantSource, long offset)
            throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        var offsetField = NtpSynchronizedInstantSource.class.getDeclaredField("offset");

        offsetField.setAccessible(true);
        offsetField.set(instantSource, offset);
    }
}
