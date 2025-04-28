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
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.EncoderException;
import io.netty.handler.codec.dns.DnsQuery;
import io.netty.handler.codec.dns.DnsRecordEncoder;
import io.netty.handler.codec.dns.DnsSection;

public interface DnsQueryEncoder {
    static byte[] encode(DnsQuery query) {
        ByteBuf out = Unpooled.buffer();
        encodeHeader(query, out);
        encodeQuestions(query, out);
        encodeRecords(query, out);
        return ByteBufUtil.getBytes(out);
    }

    private static void encodeHeader(DnsQuery query, ByteBuf buf) {
        buf.writeShort(query.id());
        int flags = 0;
        flags |= (query.opCode().byteValue() & 0xFF) << 14;
        if (query.isRecursionDesired()) {
            flags |= 1 << 8;
        }
        buf.writeShort(flags);
        buf.writeShort(query.count(DnsSection.QUESTION));
        buf.writeShort(0); // answerCount
        buf.writeShort(0); // authorityResourceCount
        buf.writeShort(query.count(DnsSection.ADDITIONAL));
    }

    private static void encodeQuestions(DnsQuery query, ByteBuf buf) {
        int count = query.count(DnsSection.QUESTION);
        for (int i = 0; i < count; i++) {
            try {
                DnsRecordEncoder.DEFAULT.encodeQuestion(query.recordAt(DnsSection.QUESTION, i), buf);
            } catch (Exception e) {
                throw new EncoderException(e);
            }
        }
    }

    private static void encodeRecords(DnsQuery query, ByteBuf buf) {
        final int count = query.count(DnsSection.ADDITIONAL);
        for (int i = 0; i < count; i++) {
            try {
                DnsRecordEncoder.DEFAULT.encodeRecord(query.recordAt(DnsSection.ADDITIONAL, i), buf);
            } catch (Exception e) {
                throw new EncoderException(e);
            }
        }
    }
}