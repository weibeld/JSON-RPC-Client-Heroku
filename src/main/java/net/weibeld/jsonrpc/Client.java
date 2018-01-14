package net.weibeld.jsonrpc;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import com.rabbitmq.tools.jsonrpc.JsonRpcClient;

class Client {

    private static final String QUEUE = "json-rpc-queue";

    public static void main(String[] args) throws Exception {

        // Establish connection to RabbitMQ server
        String uri = System.getenv("CLOUDAMQP_URL");
        if (uri == null) uri = "amqp://guest:guest@localhost";
        ConnectionFactory factory = new ConnectionFactory();
        factory.setUri(uri);
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        // Create JSON-RPC client
        JsonRpcClient client = new JsonRpcClient(channel, "", QUEUE);

        // Call one of the remote methods provided by the JSON-RPC server
        String method = "add";
        Integer[] arguments = {3, 4};
        Integer result = (Integer) client.call(method, arguments);

        System.out.println("Getting result: " + result);

        // Note:
        // Call client.getServiceDescription() to get a ServiceDescription that
        // describes all the remote methods that the JSON-RPC server provides.

        client.close();
        channel.close();
        connection.close();
    }
}
