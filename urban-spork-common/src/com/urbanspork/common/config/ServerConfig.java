package com.urbanspork.common.config;

import com.urbanspork.common.codec.SupportedCipher;
import com.urbanspork.common.protocol.Protocols;
import com.urbanspork.common.protocol.shadowsocks.network.Network;
import com.urbanspork.common.protocol.shadowsocks.network.PacketEncoding;

import java.util.Arrays;

public class ServerConfig {

    private String host;

    private int port;

    private String password;

    private SupportedCipher cipher;

    private Protocols protocol;

    private String remark;

    private Network[] networks;

    private PacketEncoding packetEncoding;

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

    public SupportedCipher getCipher() {
        return cipher;
    }

    public void setCipher(SupportedCipher cipher) {
        this.cipher = cipher;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Protocols getProtocol() {
        return protocol;
    }

    public void setProtocol(Protocols protocol) {
        this.protocol = protocol;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Network[] getNetworks() {
        return networks;
    }

    public void setNetworks(Network[] networks) {
        this.networks = networks;
    }

    public PacketEncoding getPacketEncoding() {
        return packetEncoding;
    }

    public void setPacketEncoding(PacketEncoding packetEncoding) {
        this.packetEncoding = packetEncoding;
    }

    public boolean check() {
        return isEmpty(host) && isEmpty(password) && cipher != null;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(listItemText());
        if (protocol != null) {
            builder.append('|').append(protocol).append('|').append(cipher.toString());
        }
        return builder.toString();
    }

    public String listItemText() {
        return remark == null || remark.isEmpty() ? host + ':' + port : remark;
    }

    public boolean udpEnabled() {
        return networks != null && Arrays.stream(networks).anyMatch(n -> n == Network.UDP);
    }

    private boolean isEmpty(String s) {
        return s != null && !s.isEmpty();
    }

}
