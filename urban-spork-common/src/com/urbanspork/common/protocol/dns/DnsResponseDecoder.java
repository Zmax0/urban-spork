/*
 * Copyright 2019 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.urbanspork.common.protocol.dns;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.handler.codec.dns.DefaultDnsResponse;
import io.netty.handler.codec.dns.DnsOpCode;
import io.netty.handler.codec.dns.DnsRecord;
import io.netty.handler.codec.dns.DnsRecordDecoder;
import io.netty.handler.codec.dns.DnsResponse;
import io.netty.handler.codec.dns.DnsResponseCode;
import io.netty.handler.codec.dns.DnsSection;

public interface DnsResponseDecoder {
    static DnsResponse decode(ByteBuf buffer) throws Exception {
        int id = buffer.readUnsignedShort();
        int flags = buffer.readUnsignedShort();
        if (flags >> 15 == 0) {
            throw new CorruptedFrameException("not a response");
        }
        DnsResponse response = new DefaultDnsResponse(id, DnsOpCode.valueOf((byte) (flags >> 11 & 0xf)), DnsResponseCode.valueOf((byte) (flags & 0xf)));
        response.setRecursionDesired((flags >> 8 & 1) == 1);
        response.setAuthoritativeAnswer((flags >> 10 & 1) == 1);
        response.setTruncated((flags >> 9 & 1) == 1);
        response.setRecursionAvailable((flags >> 7 & 1) == 1);
        response.setZ(flags >> 4 & 0x7);

        boolean success = false;
        try {
            int questionCount = buffer.readUnsignedShort();
            int answerCount = buffer.readUnsignedShort();
            int authorityRecordCount = buffer.readUnsignedShort();
            int additionalRecordCount = buffer.readUnsignedShort();

            decodeQuestions(response, buffer, questionCount);
            if (!decodeRecords(response, DnsSection.ANSWER, buffer, answerCount)) {
                success = true;
                return response;
            }
            if (!decodeRecords(response, DnsSection.AUTHORITY, buffer, authorityRecordCount)) {
                success = true;
                return response;
            }

            decodeRecords(response, DnsSection.ADDITIONAL, buffer, additionalRecordCount);
            success = true;
            return response;
        } finally {
            if (!success) {
                response.release();
            }
        }
    }

    private static void decodeQuestions(DnsResponse response, ByteBuf buf, int questionCount) throws Exception {
        for (int i = questionCount; i > 0; i--) {
            response.addRecord(DnsSection.QUESTION, DnsRecordDecoder.DEFAULT.decodeQuestion(buf));
        }
    }

    private static boolean decodeRecords(DnsResponse response, DnsSection section, ByteBuf buf, int count) throws Exception {
        for (int i = count; i > 0; i--) {
            DnsRecord r = DnsRecordDecoder.DEFAULT.decodeRecord(buf);
            if (r == null) {
                // Truncated response
                return false;
            }
            response.addRecord(section, r);
        }
        return true;
    }
}
