package com.urbanspork.client;

import com.urbanspork.common.codec.address.MaybeResolved;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.protocol.dns.DnsRequest;
import com.urbanspork.common.protocol.dns.IpResponse;
import com.urbanspork.common.transport.udp.DatagramPacketWrapper;
import com.urbanspork.common.util.FutureListeners;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoop;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;

import java.net.InetSocketAddress;
import java.util.List;

public class ClientUdpDnsDecoder extends MessageToMessageDecoder<DatagramPacketWrapper> {
    private final ClientChannelContext context;

    public ClientUdpDnsDecoder(ClientChannelContext context) {
        this.context = context;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, DatagramPacketWrapper msg, List<Object> out) throws Exception {
        ServerConfig config = context.config();
        msg.retain();
        EventLoop group = ctx.channel().eventLoop();
        ClientRelayHandler.resolveServerHost(group, context).addListener(FutureListeners.success(serverHost -> {
            InetSocketAddress server = new InetSocketAddress(serverHost, config.getPort());
            InetSocketAddress target = msg.server();
            new ClientDnsRelayHandler() {
                @Override
                public String label() {
                    return "udp";
                }

                @Override
                public Future<? extends Channel> newChannel(DnsRequest dnsRequest, Promise<IpResponse> ipPromise) {
                    Promise<Channel> channelPromise = group.next().newPromise();
                    if (config.quicEnabled()) {
                        newQuicChannel(group, server, dnsRequest, context, channelPromise);
                    } else {
                        newTcpChannel(group, server, dnsRequest, context, channelPromise, ipPromise);
                    }
                    channelPromise.addListener(FutureListeners.failure(ctx::fireExceptionCaught));
                    return channelPromise;
                }

                @Override
                public boolean isReadyOnceConnected() {
                    return config.quicEnabled() || config.getWs() == null;
                }

                @Override
                public void callback1(MaybeResolved maybeResolved) {
                    InetSocketAddress address = maybeResolved.address();
                    ctx.fireChannelRead(new DatagramPacketWrapper(msg.packet(), new InetSocketAddress(address.getHostString(), address.getPort())));
                }

                @Override
                public void callback0(Throwable t) {}
            }.resolve(group, context, target);
        }));
    }
}
