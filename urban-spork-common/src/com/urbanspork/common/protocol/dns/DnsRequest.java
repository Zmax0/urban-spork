package com.urbanspork.common.protocol.dns;

import io.netty.handler.codec.dns.DefaultDnsQuery;
import io.netty.handler.codec.dns.DefaultDnsQuestion;
import io.netty.handler.codec.dns.DnsRecordType;
import io.netty.handler.codec.dns.DnsSection;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.QueryStringEncoder;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public record DnsRequest(InetSocketAddress address, Object msg) {
    public DnsRequest(String nameServer, String domain) {
        short tid = (short) ThreadLocalRandom.current().nextInt(); // unsigned short
        DefaultDnsQuery dnsQuery = new DefaultDnsQuery(tid);
        DefaultDnsQuestion question = new DefaultDnsQuestion(domain, DnsRecordType.A);
        dnsQuery.addRecord(DnsSection.QUESTION, question);
        domain = Base64.getUrlEncoder().withoutPadding().encodeToString(DnsQueryEncoder.encode(dnsQuery));
        dnsQuery.release();
        URI uri = URI.create(nameServer);
        Map<String, List<String>> queryParams;
        if (uri.getQuery() == null) {
            uri = URI.create(nameServer + "?dns=" + domain);
        } else if ((queryParams = QueryStringDecoder.builder().build(uri).parameters()).containsKey("dns")) {
            String base = uri.getScheme() + "://" + uri.getRawAuthority() + uri.getRawPath();
            QueryStringEncoder encoder = new QueryStringEncoder(base);
            for (Map.Entry<String, List<String>> entry : queryParams.entrySet()) {
                encoder.addParam(entry.getKey(), "dns".equals(entry.getKey()) ? domain : entry.getValue().getFirst());
            }
            uri = URI.create(encoder.toString());
        } else {
            uri = URI.create(nameServer + "&dns=" + domain);
        }
        int port = uri.getPort();
        if (port == -1) {
            port = uri.getScheme().equals("https") ? 443 : 80;
        }
        String host = uri.getHost();
        FullHttpRequest msg = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, uri.toString());
        msg.headers().set(HttpHeaderNames.ACCEPT, "application/dns-message").set(HttpHeaderNames.CONTENT_TYPE, "application/dns-message");
        this(InetSocketAddress.createUnresolved(host, port), msg);
    }
}
