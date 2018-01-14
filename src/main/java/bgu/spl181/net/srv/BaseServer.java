package bgu.spl181.net.srv;

import bgu.spl181.net.api.MessageEncoderDecoder;
import bgu.spl181.net.api.bidi.BidiMessagingProtocol;
import bgu.spl181.net.api.bidi.Connections;
import bgu.spl181.net.api.bidi.ConnectionsImpl;
import bgu.spl181.net.api.bidi.MovieMessagingProtocol;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Instant;
import java.util.function.Supplier;

public abstract class BaseServer<T> implements Server<T> {

    private final int port;
    private final Supplier<BidiMessagingProtocol<T>> protocolFactory;
    private final Supplier<MessageEncoderDecoder<T>> encdecFactory;
    private ServerSocket sock;
    ConnectionsImpl connections;

    public BaseServer(
            int port,
            Supplier<BidiMessagingProtocol<T>> protocolFactory,
            Supplier<MessageEncoderDecoder<T>> encdecFactory) {

        this.port = port;
        this.protocolFactory = protocolFactory;
        this.encdecFactory = encdecFactory;
		this.sock = null;

    }

    @Override
    public void serve() {

        try (ServerSocket serverSock = new ServerSocket(port)) {
			System.out.println("Server started");
//            System.out.println(Instant.now()+" | IP: "+ InetAddress.getLocalHost().getHostAddress()+" | Port: "+port);

            this.sock = serverSock; //just to be able to close

            while (!Thread.currentThread().isInterrupted()) {

                Socket clientSock = serverSock.accept();

                MovieMessagingProtocol mmp = new MovieMessagingProtocol<>(((MovieMessagingProtocol)protocolFactory).getUsersList(),
                        ((MovieMessagingProtocol)protocolFactory).getMoviesList());

                BlockingConnectionHandler<T> handler = new BlockingConnectionHandler<>(
                        clientSock,
                        encdecFactory.get(),
                        mmp);


                connections = ((MovieMessagingProtocol)protocolFactory).getConnections();
                Integer connectionId = connections.getNewConnectionId();
                connections.add(connectionId, handler);
                mmp.start(connectionId,connections); //TODO: think positive

//               System.out.println(Instant.now()+" | Client connected | connectionId: "+connectionId);//

                execute(handler);
            }
        } catch (IOException ex) {
        }

//        System.out.println(Instant.now()+" | Server closed");
    }

    @Override
    public void close() throws IOException {
		if (sock != null)
			sock.close();
    }

    protected abstract void execute(BlockingConnectionHandler<T>  handler);

}
