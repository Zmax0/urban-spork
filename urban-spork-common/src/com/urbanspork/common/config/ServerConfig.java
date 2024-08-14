package com.urbanspork.common.config;

import com.urbanspork.common.codec.CipherKind;
import com.urbanspork.common.protocol.Protocol;
import com.urbanspork.common.transport.Transport;
import com.urbanspork.common.transport.udp.PacketEncoding;

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

    private SslSetting ssl;

    private WebSocketSetting ws;

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
        return Transport.reverseToMap(transport);
    }

    public void setTransport(Transport[] transports) {
        this.transport = Transport.toMap(transports);
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

    public SslSetting getSsl() {
        return ssl;
    }

    public void setSsl(SslSetting ssl) {
        this.ssl = ssl;
    }

    public WebSocketSetting getWs() {
        return ws;
    }

    public void setWs(WebSocketSetting ws) {
        this.ws = ws;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(listItemText());
        if (protocol != null) {
            builder.append('|').append(protocol);
            if (Protocol.vmess == protocol) {
                builder.append('|').append("negotiated");
            } else {
                builder.append('|').append(cipher);
            }
        }
        return builder.toString();
    }

    public String clientText() {
        return listItemText() + '|' + protocol + '|' + cipher;
    }

    public String listItemText() {
        return remark == null || remark.isEmpty() ? host + ':' + port : remark;
    }

    public boolean udpEnabled() {
        return transport != null && transport.length != 0 && transport[Transport.UDP.index()] != null;
    }

    public boolean wsEnabled() {
        return Protocol.trojan != protocol && transport != null && transport.length != 0 && transport[Transport.TCP.index()] == Transport.WS;
    }
}
