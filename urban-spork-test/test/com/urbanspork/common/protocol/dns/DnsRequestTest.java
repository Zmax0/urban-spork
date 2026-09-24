package com.urbanspork.common.protocol.dns;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.QueryStringDecoder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DnsRequestTest {
    @Test
    void testNameServerWithoutQuery() {
        DnsRequest request = new DnsRequest("https://8.8.8.8/dns-query", "example.com");
        Assertions.assertEquals("8.8.8.8", request.address().getHostString());
        Assertions.assertEquals(443, request.address().getPort());
        FullHttpRequest msg = (FullHttpRequest) request.msg();
        Assertions.assertEquals(HttpMethod.GET, msg.method());
        Assertions.assertEquals("application/dns-message", msg.headers().get(HttpHeaderNames.ACCEPT));
        Assertions.assertEquals("application/dns-message", msg.headers().get(HttpHeaderNames.CONTENT_TYPE));
        Assertions.assertTrue(msg.uri().startsWith("https://8.8.8.8/dns-query?dns="));
        String dns = msg.uri().substring(msg.uri().indexOf("?dns=") + 5);
        Assertions.assertFalse(dns.isEmpty());
        Assertions.assertFalse(dns.contains("="));
    }

    @Test
    void testNameServerWithDnsQueryParam() {
        DnsRequest request = new DnsRequest("https://8.8.8.8/dns-query?dns=old&ct=application%2Fdns-message", "example.com");
        FullHttpRequest msg = (FullHttpRequest) request.msg();
        String uri = msg.uri();
        Assertions.assertEquals(1, uri.chars().filter(c -> c == '?').count());
        Assertions.assertFalse(uri.contains("dns=old"));
        QueryStringDecoder decoder = QueryStringDecoder.builder().build(uri);
        Assertions.assertEquals(1, decoder.parameters().get("dns").size());
        Assertions.assertNotEquals("old", decoder.parameters().get("dns").getFirst());
        Assertions.assertEquals("application/dns-message", decoder.parameters().get("ct").getFirst());
        Assertions.assertEquals("8.8.8.8", request.address().getHostString());
        Assertions.assertEquals(443, request.address().getPort());
    }

    @Test
    void testNameServerWithOtherQueryParam() {
        DnsRequest request = new DnsRequest("http://1.1.1.1/dns-query?ct=application/dns-message", "example.com");
        FullHttpRequest msg = (FullHttpRequest) request.msg();
        Assertions.assertTrue(msg.uri().contains("?ct=application/dns-message&dns="));
        Assertions.assertEquals("1.1.1.1", request.address().getHostString());
        Assertions.assertEquals(80, request.address().getPort());
    }

    @Test
    void testNameServerWithExplicitPort() {
        DnsRequest request = new DnsRequest("http://1.1.1.1:5353/dns-query", "example.com");
        Assertions.assertEquals(5353, request.address().getPort());
    }

    @Test
    void testNameServerWithDnsQueryParamWithoutPath() {
        DnsRequest request = new DnsRequest("https://8.8.8.8?dns=old", "example.com");
        FullHttpRequest msg = (FullHttpRequest) request.msg();
        Assertions.assertEquals(1, msg.uri().chars().filter(c -> c == '?').count());
        Assertions.assertFalse(msg.uri().contains("dns=old"));
        QueryStringDecoder decoder = QueryStringDecoder.builder().build(msg.uri());
        Assertions.assertEquals(1, decoder.parameters().get("dns").size());
        Assertions.assertEquals("8.8.8.8", request.address().getHostString());
    }
}
