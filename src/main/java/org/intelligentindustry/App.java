package org.intelligentindustry;

import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.api.config.OpcUaServerConfig;
import org.eclipse.milo.opcua.stack.core.types.structured.BuildInfo;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.eclipse.milo.opcua.sdk.server.util.HostnameUtil;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.transport.TransportProfile;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MessageSecurityMode;
import org.eclipse.milo.opcua.stack.server.EndpointConfiguration;


import static com.google.common.collect.Lists.newArrayList;
import static org.eclipse.milo.opcua.sdk.server.api.config.OpcUaServerConfig.USER_TOKEN_POLICY_ANONYMOUS;

/**
 * Hello world!
 *
 *  TODO: add custom object events
 */
public class App 
{

    private static final int TCP_BIND_PORT = 12686;

    public static void main( String[] args ) throws InterruptedException, ExecutionException
    {
        System.out.println( "Hello World!" );

        OpcUaServer server;

        OpcUaServerConfig serverConfig = OpcUaServerConfig.builder()
            .setApplicationName(LocalizedText.english("Intelligent Industry Demo Server"))
            .setEndpoints(createEndpointConfigurations())
            .setBuildInfo(
                new BuildInfo(
                    "urn:intelligent-industry:demo-server",
                    "intelligentindustry",
                    "eclipse milo example server",
                    OpcUaServer.SDK_VERSION,
                    "", DateTime.now()))
            .setProductUri("urn:intelligent-industry:demo-server")
            .setApplicationUri("urn:intelligent-industry:demo-server")
            .build();

        server = new OpcUaServer(serverConfig);

       DemoNamespace ns = new DemoNamespace(server);

        ns.startup();

        server.startup().get();

        final CompletableFuture<Void> future = new CompletableFuture<>();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> future.complete(null)));

        future.get();

    }

    static private Set<EndpointConfiguration> createEndpointConfigurations() {
        Set<EndpointConfiguration> endpointConfigurations = new LinkedHashSet<>();

        List<String> bindAddresses = newArrayList();
        bindAddresses.add("0.0.0.0");
        bindAddresses.add("141.144.205.242");

        Set<String> hostnames = new LinkedHashSet<>();
        hostnames.add(HostnameUtil.getHostname());
        hostnames.addAll(HostnameUtil.getHostnames("0.0.0.0"));

        for (String bindAddress : bindAddresses) {
            for (String hostname : hostnames) {
                EndpointConfiguration.Builder builder = EndpointConfiguration.newBuilder()
                    .setBindAddress(bindAddress)
                    .setHostname(hostname)
                    .setPath("/milo")
                    .addTokenPolicies(
                        USER_TOKEN_POLICY_ANONYMOUS);


                EndpointConfiguration.Builder noSecurityBuilder = builder.copy()
                    .setSecurityPolicy(SecurityPolicy.None)
                    .setSecurityMode(MessageSecurityMode.None);

                endpointConfigurations.add(buildTcpEndpoint(noSecurityBuilder));




                /*
                 * It's good practice to provide a discovery-specific endpoint with no security.
                 * It's required practice if all regular endpoints have security configured.
                 *
                 * Usage of the  "/discovery" suffix is defined by OPC UA Part 6:
                 *
                 * Each OPC UA Server Application implements the Discovery Service Set. If the OPC UA Server requires a
                 * different address for this Endpoint it shall create the address by appending the path "/discovery" to
                 * its base address.
                 */

                EndpointConfiguration.Builder discoveryBuilder = builder.copy()
                    .setPath("/milo/discovery")
                    .setSecurityPolicy(SecurityPolicy.None)
                    .setSecurityMode(MessageSecurityMode.None);

                endpointConfigurations.add(buildTcpEndpoint(discoveryBuilder));
            }
        }

        return endpointConfigurations;
    }

    private static EndpointConfiguration buildTcpEndpoint(EndpointConfiguration.Builder base) {
        return base.copy()
            .setTransportProfile(TransportProfile.TCP_UASC_UABINARY)
            .setBindPort(TCP_BIND_PORT)
            .build();
    }

}
