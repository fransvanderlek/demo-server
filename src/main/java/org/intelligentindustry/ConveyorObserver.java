package org.intelligentindustry;

import org.intelligentindustry.ConveyorModel.States;

public interface ConveyorObserver {

    public void notifyRunningSpeed( double value);

    public void notifyMotors(int i);

    public void notifyState(States currentState);

}
