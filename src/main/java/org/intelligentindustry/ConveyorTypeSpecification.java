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
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
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

public class ConveyorTypeSpecification {

    public static final String NAMESPACE_URI = "urn:intelligentindustry:demo-server";
     
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private NodeId rootFolderId;
    private UaFolderNode folderNode ;

    private static final String TYPE_QNAME = "Conveyor";
    private static final String TYPE_NODE_ID = "ObjectTypes/"+TYPE_QNAME;
    private static final String TYPE_DISPLAY_NAME_EN = "ConveyorType";

    private static final String MEMBER_RUNNING_SPEED = "RunningSpeed"
    private static final String MEMBER_MOTORS = "Motors"   

    UaObjectTypeNode conveyorTypeNode;

    public ConveyorTypeSpecification(OpcUaServer server) {
               
    }

    private void specifyType(){

        addCustomObjectTypeAndInstance(folderNode);
        addConveyorStartMethod(folderNode);

         UaObjectTypeNode conveyorTypeNode = UaObjectTypeNode.builder(getNodeContext())
            .setNodeId(newNodeId(TYPE_NODE_ID))
            .setBrowseName(newQualifiedName(TYPE_QNAME))
            .setDisplayName(LocalizedText.english(TYPE_DISPLAY_NAME_EN))
            .setIsAbstract(false)
            .build();

        // Add the inverse SubtypeOf relationship.
        conveyorTypeNode.addReference(new Reference(
            conveyorTypeNode.getNodeId(),
            Identifiers.HasSubtype,
            Identifiers.BaseObjectType.expanded(),
            false
        ));

        // Add type definition and declarations to address space.
        getNodeManager().addNode(conveyorTypeNode);

        // Tell the ObjectTypeManager about our new type.
        // This let's us use NodeFactory to instantiate instances of the type.
        getServer().getObjectTypeManager().registerObjectType(
            conveyorTypeNode.getNodeId(),
            UaObjectNode.class,
            UaObjectNode::new
        );

        conveyorTypeNode.addComponent( specifyMemberMotorsType() );
        conveyorTypeNode.addComponent( specifyMemberRunningSpeedType() );

    }

    private UaVariableNode specifyMemberMotorsType(){

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
        getNodeManager().addNode(motorsType);

        return motorsType;

    }
       
    private void specifyMemberRunningSpeedType() {
      
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
            true));
        getNodeManager().addNode(runningSpeedType);

    }

    public  UaObjectNode instance(String instanceName, String folderPath){


        if ( conveyorTypeNode == null ){
            this.specifyType();
        }

        UaObjectNode myConveyor;

        try {
             myConveyor = (UaObjectNode) getNodeFactory().createNode(
                newNodeId(folderPath+"/"+instanceName),
                conveyorTypeNode.getNodeId()
            );

            myConveyor.setBrowseName(newQualifiedName(instanceName));
            myConveyor.setDisplayName(LocalizedText.english(instanceName));

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

        addConveyorStartMethod(myConveyor);

        return myConveyor;
    }


    private void addConveyorStartMethod( UaObjectNode myConveyor) {
        UaMethodNode methodNode = UaMethodNode.builder(getNodeContext())
            .setNodeId(newNodeId("IntelligentIndustry/conveyor_start()"))
            .setBrowseName(newQualifiedName("conveyor_start()"))
            .setDisplayName(new LocalizedText(null, "conveyor_start()"))
            .setDescription(
                LocalizedText.english("Starts the conveyor"))
            .build();

        ConveyorStartMethod conveyorStartMethod = new ConveyorStartMethod(methodNode);
        methodNode.setInputArguments(conveyorStartMethod.getInputArguments());
        methodNode.setOutputArguments(conveyorStartMethod.getOutputArguments());
        methodNode.setInvocationHandler(conveyorStartMethod);

        getNodeManager().addNode(methodNode);

        methodNode.addReference(new Reference(
            methodNode.getNodeId(),
            Identifiers.HasComponent,
            myConveyor.getNodeId().expanded(),
            false
        ));
    }
    
    
}
