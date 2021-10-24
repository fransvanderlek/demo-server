package org.intelligentindustry;

import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.eclipse.milo.opcua.sdk.core.AccessLevel;
import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.server.Lifecycle;
import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.api.DataItem;
import org.eclipse.milo.opcua.sdk.server.api.ManagedNamespaceWithLifecycle;
import org.eclipse.milo.opcua.sdk.server.api.MonitoredItem;
import org.eclipse.milo.opcua.sdk.server.model.nodes.objects.BaseEventTypeNode;
import org.eclipse.milo.opcua.sdk.server.model.nodes.objects.ServerTypeNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaObjectNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaObjectTypeNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.sdk.server.nodes.filters.AttributeFilters;
import org.eclipse.milo.opcua.sdk.server.util.SubscriptionModel;
import org.eclipse.milo.opcua.stack.core.BuiltinReferenceType;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.ubyte;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.ushort;

public class DemoNamespace extends ManagedNamespaceWithLifecycle {

    public static final String NAMESPACE_URI = "urn:intelligentindustry:demo-server";
    private final SubscriptionModel subscriptionModel;
    private final Random random = new Random();
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private volatile Thread eventThread;
    private volatile boolean keepPostingEvents = true;

    private UaObjectNode myConveyor ;
    
    public DemoNamespace(OpcUaServer server) {
        super(server, NAMESPACE_URI);

        subscriptionModel = new SubscriptionModel(server, this);

        getLifecycleManager().addLifecycle(subscriptionModel);

        getLifecycleManager().addStartupTask(this::createAndAddNodes);

        getLifecycleManager().addLifecycle(new Lifecycle() {
            @Override
            public void startup() {
                startBogusEventNotifier();
            }

            @Override
            public void shutdown() {
                try {
                    keepPostingEvents = false;
                    eventThread.interrupt();
                    eventThread.join();
                } catch (InterruptedException ignored) {
                    // ignored
                }
            }
        });
        
    }

    private void startBogusEventNotifier() {
        // Set the EventNotifier bit on Server Node for Events.
        UaNode serverNode = getServer()
            .getAddressSpaceManager()
            .getManagedNode(Identifiers.Server)
            .orElse(null);

      
        if (serverNode instanceof ServerTypeNode) {
            ((ServerTypeNode) serverNode).setEventNotifier(ubyte(1));

            // Post a bogus Event every couple seconds
            eventThread = new Thread(() -> {
                while (keepPostingEvents) {
                    try {
                        BaseEventTypeNode eventNode = getServer().getEventFactory().createEvent(
                            myConveyor.getNodeId(),
                            Identifiers.BaseEventType
                        );


                        eventNode.setBrowseName(new QualifiedName(1, "Conveyor Event"));
                        eventNode.setDisplayName(LocalizedText.english("Conveyor Event"));
                        eventNode.setEventId(ByteString.of(new byte[]{0, 1, 2, 3}));
                        eventNode.setEventType(Identifiers.BaseEventType);
                        eventNode.setSourceNode(myConveyor.getNodeId());
                        eventNode.setSourceName(myConveyor.getDisplayName().getText());
                        eventNode.setTime(DateTime.now());
                        eventNode.setReceiveTime(DateTime.NULL_VALUE);
                        eventNode.setMessage(LocalizedText.english("Conveyor started"));
                        eventNode.setSeverity(ushort(2));

                        //noinspection UnstableApiUsage
                        getServer().getEventBus().post(eventNode);


                        eventNode.delete();
                    } catch (Throwable e) {
                        logger.error("Error creating EventNode: {}", e.getMessage(), e);
                    }

                    try {
                        //noinspection BusyWait
                        Thread.sleep(2_000);
                    } catch (InterruptedException ignored) {
                        // ignored
                    }
                }
            }, "bogus-event-poster");

            eventThread.start();
        }
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
  

        addCustomObjectTypeAndInstance(folderNode);
    }

    private void addCustomObjectTypeAndInstance(UaFolderNode rootFolder) {
        // Define a new ObjectType called "MyObjectType".
        UaObjectTypeNode conveyorTypeNode = UaObjectTypeNode.builder(getNodeContext())
            .setNodeId(newNodeId("ObjectTypes/ConveyorType"))
            .setBrowseName(newQualifiedName("ConveyorType"))
            .setDisplayName(LocalizedText.english("ConveyorType"))
            .setIsAbstract(false)
            .build();

        // "Foo" and "Bar" are members. These nodes are what are called "instance declarations" by the spec.
        UaVariableNode motorsType = UaVariableNode.builder(getNodeContext())
            .setNodeId(newNodeId("ObjectTypes/ConveyorType.Motors"))
            .setAccessLevel(AccessLevel.READ_WRITE)
            .setBrowseName(newQualifiedName("Motors"))
            .setDisplayName(LocalizedText.english("Motors"))
            .setDataType(Identifiers.Int16)
            .setTypeDefinition(Identifiers.BaseDataVariableType)
            .build();

            motorsType.addReference(new Reference(
                motorsType.getNodeId(),
            Identifiers.HasModellingRule,
            Identifiers.ModellingRule_Mandatory.expanded(),
            true
        ));



        motorsType.setValue(new DataValue(new Variant(2)));
        conveyorTypeNode.addComponent(motorsType);

        UaVariableNode runningSpeedType = UaVariableNode.builder(getNodeContext())
            .setNodeId(newNodeId("ObjectTypes/ConveyorType.RunningSpeed"))
            .setAccessLevel(AccessLevel.READ_WRITE)
            .setBrowseName(newQualifiedName("RunningSpeed"))
            .setDisplayName(LocalizedText.english("RunningSpeed"))
            .setDataType(Identifiers.Double)
            .setTypeDefinition(Identifiers.BaseDataVariableType)
            .build();

            runningSpeedType.addReference(new Reference(
                runningSpeedType.getNodeId(),
            Identifiers.HasModellingRule,
            Identifiers.ModellingRule_Mandatory.expanded(),
            true
        ));

        runningSpeedType.setValue(new DataValue(new Variant(0.0)));

        runningSpeedType.getFilterChain().addLast(
            new AttributeLoggingFilter(),
            AttributeFilters.getValue(
                ctx ->
                    new DataValue(new Variant(random.nextDouble()))
            )
        );

        conveyorTypeNode.addComponent(runningSpeedType);

        // Tell the ObjectTypeManager about our new type.
        // This let's us use NodeFactory to instantiate instances of the type.
        getServer().getObjectTypeManager().registerObjectType(
            conveyorTypeNode.getNodeId(),
            UaObjectNode.class,
            UaObjectNode::new
        );

        // Add the inverse SubtypeOf relationship.
        conveyorTypeNode.addReference(new Reference(
            conveyorTypeNode.getNodeId(),
            Identifiers.HasSubtype,
            Identifiers.BaseObjectType.expanded(),
            false
        ));

        

        // Add type definition and declarations to address space.
        getNodeManager().addNode(conveyorTypeNode);
        getNodeManager().addNode(motorsType);
        getNodeManager().addNode(runningSpeedType);

        // Use NodeFactory to create instance of MyObjectType called "MyObject".
        // NodeFactory takes care of recursively instantiating MyObject member nodes
        // as well as adding all nodes to the address space.
        try {
             myConveyor = (UaObjectNode) getNodeFactory().createNode(
                newNodeId("IntelligentIndustry/Conveyor-1"),
                conveyorTypeNode.getNodeId()
            );
            myConveyor.setBrowseName(newQualifiedName("Conveyor-1"));
            myConveyor.setDisplayName(LocalizedText.english("Conveyor-1"));

            // Add forward and inverse references from the root folder.
            rootFolder.addOrganizes(myConveyor);

            myConveyor.addReference(new Reference(
                myConveyor.getNodeId(),
                Identifiers.Organizes,
                rootFolder.getNodeId().expanded(),
                false
            ));

        } catch (UaException e) {
            logger.error("Error creating ConveyorType instance: {}", e.getMessage(), e);
        }
    }

    @Override
    public void onDataItemsCreated(List<DataItem> dataItems) {
        subscriptionModel.onDataItemsCreated(dataItems);
    }

    @Override
    public void onDataItemsModified(List<DataItem> dataItems) {
        subscriptionModel.onDataItemsModified(dataItems);
    }

    @Override
    public void onDataItemsDeleted(List<DataItem> dataItems) {
        subscriptionModel.onDataItemsDeleted(dataItems);
    }

    @Override
    public void onMonitoringModeChanged(List<MonitoredItem> monitoredItems) {
        subscriptionModel.onMonitoringModeChanged(monitoredItems);
    }
    
}
