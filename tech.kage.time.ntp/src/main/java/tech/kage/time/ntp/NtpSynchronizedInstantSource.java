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

import java.io.IOException;
import java.net.InetAddress;
import java.time.Duration;
import java.time.Instant;
import java.time.InstantSource;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;

/**
 * An implementation of {@link java.time.InstantSource} independent of the local
 * clock and synchronized with an NTP server. Implemented by computing the
 * origin time and measuring elapsed time since the origin. The origin time is
 * the local clock time with an offset retrieved from an NTP server.
 * 
 * @author Dariusz Szpakowski
 */
public class NtpSynchronizedInstantSource implements InstantSource {
    /**
     * NTP server to synchronize with.
     */
    private final String ntpServer;

    /**
     * Origin time in milliseconds as specified by
     * {@code System.currentTimeMillis()}.
     */
    private final long originMillis;

    /**
     * Origin value of the running Java Virtual Machine's high-resolution time
     * source, in nanoseconds as specified by {@code System.nanoTime()}.
     * 
     * {@code originNano} points to the same instant as {@code originMillis}.
     */
    private final long originNano;

    /**
     * Offset retrieved from the configured NTP server.
     */
    private Long offset;

    /**
     * Constructs a new {@link NtpSynchronizedInstantSource} instance.
     *
     * @param ntpServer NTP server to synchronize with
     */
    public NtpSynchronizedInstantSource(String ntpServer) {
        this.ntpServer = ntpServer;

        long nowMillis1;
        long nowNano;
        long nowMillis2;

        /*
         * Keep reading nowNano until nowMillis1 and nowMillis2 are the same time with
         * millisecond precision. That means nowNano was read at the instant of
         * nowMillis1 and nowMillis2. This gives us the connection between relative time
         * in System.nanoTime() and absolute time in System.currentTimeMillis().
         */
        do {
            nowMillis1 = System.currentTimeMillis();
            nowNano = System.nanoTime();
            nowMillis2 = System.currentTimeMillis();
        } while (nowMillis1 != nowMillis2);

        originMillis = nowMillis1;
        originNano = nowNano;
    }

    /**
     * Synchronizes this time source with the configured NTP server.
     * 
     * @throws IOException if an error occurs while retrieving the time
     */
    public void sync() throws IOException {
        var timeInfo = getNtpTime(ntpServer);

        timeInfo.computeDetails();

        offset = timeInfo.getOffset();
    }

    @Override
    public Instant instant() {
        if (offset == null) {
            throw new IllegalStateException("Offset not set, did you run sync()?");
        }

        var nowMillis = Math.addExact(originMillis, (System.nanoTime() - originNano) / 1000_000);

        var nowWithOffsetMillis = Math.addExact(nowMillis, offset);

        return Instant.ofEpochMilli(nowWithOffsetMillis);
    }

    /**
     * Retrieves {@link TimeInfo} from the given NTP server.
     * 
     * @param ntpServer address of an NTP server
     * 
     * @return {@link TimeInfo} containing the offset and other time related
     *         information
     * 
     * @throws IOException if an error occurs while retrieving the time
     */
    private TimeInfo getNtpTime(String ntpServer) throws IOException {
        var hostAddr = InetAddress.getByName(ntpServer);

        try (var ntpClient = new NTPUDPClient()) {
            ntpClient.setDefaultTimeout(Duration.ofSeconds(1));

            return ntpClient.getTime(hostAddr);
        }
    }
}
