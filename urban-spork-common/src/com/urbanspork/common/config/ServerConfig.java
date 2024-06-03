package com.urbanspork.common.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.urbanspork.common.codec.CipherKind;
import com.urbanspork.common.protocol.Protocol;
import com.urbanspork.common.transport.Transport;
import com.urbanspork.common.transport.udp.PacketEncoding;
import io.netty.handler.traffic.AbstractTrafficShapingHandler;

import java.util.Arrays;
import java.util.List;

public class ServerConfig {

    private String host;

    private int port;

    private String password;

    private CipherKind cipher;

    private Protocol protocol;

    private String remark;

    private Transport[] transport;

    private PacketEncoding packetEncoding;

    private List<ServerUserConfig> user;

    @JsonIgnore
    private AbstractTrafficShapingHandler trafficShapingHandler;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public CipherKind getCipher() {
        return cipher;
    }

    public void setCipher(CipherKind cipher) {
        this.cipher = cipher;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Protocol getProtocol() {
        return protocol;
    }

    public void setProtocol(Protocol protocol) {
        this.protocol = protocol;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Transport[] getTransport() {
        return transport;
    }

    public void setTransport(Transport[] transports) {
        this.transport = transports;
    }

    public PacketEncoding getPacketEncoding() {
        return packetEncoding;
    }

    public void setPacketEncoding(PacketEncoding packetEncoding) {
        this.packetEncoding = packetEncoding;
    }

    public List<ServerUserConfig> getUser() {
        return user;
    }

    public void setUser(List<ServerUserConfig> serverUserConfig) {
        this.user = serverUserConfig;
    }

    public AbstractTrafficShapingHandler getTrafficShapingHandler() {
        return trafficShapingHandler;
    }

    public void setTrafficShapingHandler(AbstractTrafficShapingHandler trafficShapingHandler) {
        this.trafficShapingHandler = trafficShapingHandler;
    }

    public boolean check() {
        return isEmpty(host) && isEmpty(password) && cipher != null;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(listItemText());
        if (protocol != null) {
            builder.append('|').append(protocol);
            if (Protocol.vmess == protocol) {
                builder.append('|').append("negotiated");
            } else {
                builder.append('|').append(cipher.toString());
            }
        }
        return builder.toString();
    }

    public String clientText() {
        return listItemText() + '|' + protocol + '|' + cipher.toString();
    }

    public String listItemText() {
        return remark == null || remark.isEmpty() ? host + ':' + port : remark;
    }

    public boolean udpEnabled() {
        return transport != null && Arrays.stream(transport).anyMatch(n -> n == Transport.UDP);
    }

    private boolean isEmpty(String s) {
        return s != null && !s.isEmpty();
    }

}
