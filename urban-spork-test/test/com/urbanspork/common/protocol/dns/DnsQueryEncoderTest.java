package com.urbanspork.common.protocol.dns;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.EncoderException;
import io.netty.handler.codec.dns.DefaultDnsQuery;
import io.netty.handler.codec.dns.DefaultDnsRawRecord;
import io.netty.handler.codec.dns.DnsQuestion;
import io.netty.handler.codec.dns.DnsRawRecord;
import io.netty.handler.codec.dns.DnsRecordType;
import io.netty.handler.codec.dns.DnsSection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DnsQueryEncoderTest {
    @Test
    void encodeUnsupportedQuestion() {
        DefaultDnsQuery query = new DefaultDnsQuery(1);
        query.setRecursionDesired(true);
        query.addRecord(DnsSection.QUESTION, new TestDnsQuestion());
        Assertions.assertThrows(EncoderException.class, () -> DnsQueryEncoder.encode(query));
    }

    @Test
    void encodeUnsupportedAdditional() {
        DefaultDnsQuery query = new DefaultDnsQuery(1);
        query.setRecursionDesired(true);
        query.addRecord(DnsSection.ADDITIONAL, new DefaultDnsRawRecord("www.example.com", DnsRecordType.A, 1, Unpooled.EMPTY_BUFFER));
        query.addRecord(DnsSection.ADDITIONAL, new TestDnsRecord());
        Assertions.assertThrows(EncoderException.class, () -> DnsQueryEncoder.encode(query));
    }

    private static class TestDnsQuestion implements DnsQuestion {

        @Override
        public String name() {
            return "test";
        }

        @Override
        public DnsRecordType type() {
            return null;
        }

        @Override
        public int dnsClass() {
            return 0;
        }

        @Override
        public long timeToLive() {
            return 0;
        }
    }

    private static class TestDnsRecord implements DnsRawRecord {
        @Override
        public String name() {
            return "";
        }

        @Override
        public DnsRecordType type() {
            return null;
        }

        @Override
        public int dnsClass() {
            return 0;
        }

        @Override
        public long timeToLive() {
            return 0;
        }

        @Override
        public ByteBuf content() {
            return null;
        }

        @Override
        public DnsRawRecord copy() {
            return null;
        }

        @Override
        public DnsRawRecord duplicate() {
            return null;
        }

        @Override
        public DnsRawRecord retainedDuplicate() {
            return null;
        }

        @Override
        public DnsRawRecord replace(ByteBuf content) {
            return null;
        }

        @Override
        public int refCnt() {
            return 0;
        }

        @Override
        public DnsRawRecord retain() {
            return null;
        }

        @Override
        public DnsRawRecord retain(int increment) {
            return null;
        }

        @Override
        public DnsRawRecord touch() {
            return null;
        }

        @Override
        public DnsRawRecord touch(Object hint) {
            return null;
        }

        @Override
        public boolean release() {
            return false;
        }

        @Override
        public boolean release(int decrement) {
            return false;
        }
    }
}
