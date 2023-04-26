package com.urbanspork.common.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.urbanspork.common.codec.SupportedCipher;
import com.urbanspork.common.protocol.Protocols;
import com.urbanspork.common.protocol.shadowsocks.network.Network;
import com.urbanspork.common.protocol.shadowsocks.network.PacketEncoding;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class ServerConfig {

    private String host;

    private int port;

    private String password;

    private SupportedCipher cipher;

    private Protocols protocol;

    private String remark;

    @JsonProperty("network")
    @JsonSerialize(using = NetworkListSerializer.class)
    @JsonDeserialize(using = NetworkListDeserializer.class)
    private Network[] networkList;

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

    public Network[] getNetworkList() {
        return networkList;
    }

    public void setNetworkList(Network[] networkList) {
        this.networkList = networkList;
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
        if (builder.length() == 0) {
            builder.append(" XX ");
        }
        return builder.toString();
    }

    public String listItemText() {
        return remark == null || remark.isEmpty() ? host + ':' + port : remark;
    }

    public boolean udpEnabled() {
        return networkList != null && Arrays.stream(networkList).anyMatch(n -> n == Network.UDP);
    }

    private boolean isEmpty(String s) {
        return s != null && !s.isEmpty();
    }


    static class NetworkListDeserializer extends JsonDeserializer<Network[]> {
        @Override
        public Network[] deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
            Set<Network> set = new HashSet<>();
            for (String str : jsonParser.getValueAsString().split(",")) {
                if (Network.TCP.value().equals(str)) {
                    set.add(Network.TCP);
                }
                if (Network.UDP.value().equals(str)) {
                    set.add(Network.UDP);
                }
            }
            return set.toArray(new Network[0]);
        }
    }

    static class NetworkListSerializer extends JsonSerializer<Network[]> {
        @Override
        public void serialize(Network[] networkList, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
            jsonGenerator.writeString(Arrays.stream(networkList).map(Network::value).collect(Collectors.joining(",")));
        }
    }

}
