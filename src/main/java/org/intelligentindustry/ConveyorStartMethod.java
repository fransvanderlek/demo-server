package org.intelligentindustry;

import java.util.UUID;

import org.eclipse.milo.opcua.sdk.core.ValueRanks;
import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.api.methods.AbstractMethodInvocationHandler;
import org.eclipse.milo.opcua.sdk.server.model.nodes.objects.BaseEventTypeNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.structured.Argument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.ushort;


public class ConveyorStartMethod extends AbstractMethodInvocationHandler {

     
    private final Logger logger = LoggerFactory.getLogger(getClass());


    public static final Argument START_RESULT = new Argument(
        "start_result",
        Identifiers.String,
        ValueRanks.Any,
        null,
        new LocalizedText("The result of the start command")
    );

    public ConveyorStartMethod(UaMethodNode node) {
        super(node);

    }

    @Override
    public Argument[] getInputArguments() {    
        return new Argument[]{};
    }

    @Override
    public Argument[] getOutputArguments() {  
        return new Argument[]{START_RESULT};
    }

    @Override
    protected Variant[] invoke(InvocationContext invocationContext, Variant[] inputValues) throws UaException {    

        logger.info(this.getNode().getNodeId().toString() + " was invoked on object "+ invocationContext.getObjectId().toString());

        OpcUaServer server = this.getNode().getNodeContext().getServer();

        BaseEventTypeNode eventNode = server.getEventFactory().createEvent(
            new NodeId(1, UUID.randomUUID()),
            Identifiers.BaseEventType
        );

        eventNode.setBrowseName(new QualifiedName(1, "Conveyor Started Event"));
        eventNode.setDisplayName(LocalizedText.english("Conveyor Started Event"));
        eventNode.setEventId(ByteString.of(new byte[]{0, 1, 2, 3}));
        eventNode.setEventType(Identifiers.BaseEventType);
        eventNode.setSourceNode(invocationContext.getObjectId());
        eventNode.setSourceName(invocationContext.getObjectId().getType().toString());
        eventNode.setTime(DateTime.now());
        eventNode.setReceiveTime(DateTime.NULL_VALUE);
        eventNode.setMessage(LocalizedText.english("Conveyor Started!"));
        eventNode.setSeverity(ushort(2));

        server.getEventBus().post(eventNode);

        eventNode.delete();

        return new Variant[]{new Variant("Start succeeded.")};
    }

}
