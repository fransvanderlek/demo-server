package org.intelligentindustry;

public class ConveyorModel {

    public enum States { STARTED, IDLE};
    private ConveyorObserver observer;
    private int motors;
    private double runningSpeed;
    private ConveyorModel.States currentState;

    private void setState( States targetState ){
        this.currentState = targetState;
        this.observer.notifyState(this.currentState);

    }

    private void setMotors(int targetValue){
        this.motors = targetValue;
        this.observer.notifyMotors(this.motors);
    }

    private void setRunningSpeed(double targetValue){
        this.runningSpeed = targetValue;
        this.observer.notifyRunningSpeed(this.runningSpeed);
    }

    public ConveyorModel(ConveyorObserver observer){
        this.observer = observer;

        this.setMotors(4);
        this.setRunningSpeed(0.0);
        this.setState(ConveyorModel.States.IDLE);

    }

    public void start(){
        this.setState(ConveyorModel.States.STARTED);
    

        this.setRunningSpeed(10);
        
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        this.setRunningSpeed(0);
        this.setState(ConveyorModel.States.IDLE);

    }
    
}
