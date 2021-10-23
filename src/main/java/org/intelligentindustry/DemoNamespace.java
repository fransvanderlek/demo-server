package org.intelligentindustry;

import java.util.List;
import java.util.Random;

import org.eclipse.milo.opcua.sdk.core.AccessLevel;
import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.api.DataItem;
import org.eclipse.milo.opcua.sdk.server.api.ManagedNamespaceWithLifecycle;
import org.eclipse.milo.opcua.sdk.server.api.MonitoredItem;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.sdk.server.nodes.filters.AttributeFilters;
import org.eclipse.milo.opcua.sdk.server.util.SubscriptionModel;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;

public class DemoNamespace extends ManagedNamespaceWithLifecycle {

    public static final String NAMESPACE_URI = "urn:intelligentindustry:demo-server";
    private final SubscriptionModel subscriptionModel;
    private final Random random = new Random();

    public DemoNamespace(OpcUaServer server) {
        super(server, NAMESPACE_URI);

        subscriptionModel = new SubscriptionModel(server, this);

        getLifecycleManager().addLifecycle(subscriptionModel);

        getLifecycleManager().addStartupTask(this::createAndAddNodes);
        
    }

    private void createAndAddNodes() {
        // Create a "HelloWorld" folder and add it to the node manager
        NodeId folderNodeId = newNodeId("IntelligentIndustry");

        UaFolderNode folderNode = new UaFolderNode(
            getNodeContext(),
            folderNodeId,
            newQualifiedName("IntelligentIndustry"),
            LocalizedText.english("IntelligentIndustry")
        );

        getNodeManager().addNode(folderNode);

        // Make sure our new folder shows up under the server's Objects folder.
        folderNode.addReference(new Reference(
            folderNode.getNodeId(),
            Identifiers.Organizes,
            Identifiers.ObjectsFolder.expanded(),
            false
        ));

        // Add the rest of the nodes
       // variable scalar nodes

       //create a folder for the scalartypes
       UaFolderNode dynamicFolder = new UaFolderNode(
        getNodeContext(),
        newNodeId("IntellingetIndustry/Dynamic"),
        newQualifiedName("Dynamic"),
        LocalizedText.english("Dynamic")
    );

    getNodeManager().addNode(dynamicFolder);
    folderNode.addOrganizes(dynamicFolder);
   
    // Dynamic Double
    {
        String name = "Double";
        NodeId typeId = Identifiers.Double;
        Variant variant = new Variant(0.0);

        UaVariableNode node = new UaVariableNode.UaVariableNodeBuilder(getNodeContext())
            .setNodeId(newNodeId("IntelligentIndustry/Dynamic/" + name))
            .setAccessLevel(AccessLevel.READ_WRITE)
            .setBrowseName(newQualifiedName(name))
            .setDisplayName(LocalizedText.english(name))
            .setDataType(typeId)
            .setTypeDefinition(Identifiers.BaseDataVariableType)
            .build();

        node.setValue(new DataValue(variant));

        node.getFilterChain().addLast(
            new AttributeLoggingFilter(),
            AttributeFilters.getValue(
                ctx ->
                    new DataValue(new Variant(random.nextDouble()))
            )
        );

        getNodeManager().addNode(node);
        dynamicFolder.addOrganizes(node);
    }

       // addSqrtMethod(folderNode);
  

//        addCustomObjectTypeAndInstance(folderNode);
    }

   

    @Override
    public void onDataItemsCreated(List<DataItem> dataItems) {
        
        
    }

    @Override
    public void onDataItemsModified(List<DataItem> dataItems) {
        
        
    }

    @Override
    public void onDataItemsDeleted(List<DataItem> dataItems) {
        
    }

    @Override
    public void onMonitoringModeChanged(List<MonitoredItem> monitoredItems) {
        
    }
    
}
