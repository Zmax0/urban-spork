package com.urbanspork.client;

import com.urbanspork.common.codec.address.MaybeResolved;
import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.protocol.dns.DnsRequest;
import com.urbanspork.common.protocol.dns.IpResponse;
import com.urbanspork.common.transport.udp.DatagramPacketWrapper;
import com.urbanspork.common.util.FutureListeners;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;

import java.net.InetSocketAddress;
import java.util.List;

public class ClientUdpDnsDecoder extends MessageToMessageDecoder<DatagramPacketWrapper> {
    private final ClientChannelContext context;
    private final EventLoopGroup group;

    public ClientUdpDnsDecoder(ClientChannelContext context, EventLoopGroup group) {
        this.context = context;
        this.group = group;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, DatagramPacketWrapper msg, List<Object> out) throws Exception {
        ServerConfig config = context.config();
        msg.retain();
        String serverHost = ClientRelayHandler.tryResolveServerHost(group, config);
        InetSocketAddress server = new InetSocketAddress(serverHost, config.getPort());
        InetSocketAddress target = msg.server();
        new ClientDnsRelayHandler() {
            @Override
            public String label() {
                return "udp";
            }

            @Override
            public Future<? extends Channel> newChannel(DnsRequest dohRequest, Promise<IpResponse> ipPromise) {
                Promise<Channel> channelPromise = group.next().newPromise();
                if (config.quicEnabled()) {
                    newQuicChannel(group, server, dohRequest, context, channelPromise);
                } else {
                    newTcpChannel(group, server, dohRequest, context, channelPromise, ipPromise);
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
        }.resolve(ctx.channel().eventLoop(), config.getDns(), target);
    }
}
