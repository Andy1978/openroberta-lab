package de.fhg.iais.roberta.visitor.validate;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.ClassToInstanceMap;

import de.fhg.iais.roberta.bean.IProjectBean;
import de.fhg.iais.roberta.components.ConfigurationAst;
import de.fhg.iais.roberta.components.UsedActor;
import de.fhg.iais.roberta.components.UsedSensor;
import de.fhg.iais.roberta.syntax.Phrase;
import de.fhg.iais.roberta.syntax.action.Action;
import de.fhg.iais.roberta.syntax.action.communication.BluetoothCheckConnectAction;
import de.fhg.iais.roberta.syntax.action.communication.BluetoothReceiveAction;
import de.fhg.iais.roberta.syntax.action.communication.BluetoothSendAction;
import de.fhg.iais.roberta.syntax.action.display.ClearDisplayAction;
import de.fhg.iais.roberta.syntax.action.display.ShowTextAction;
import de.fhg.iais.roberta.syntax.action.light.LedAction;
import de.fhg.iais.roberta.syntax.action.motor.differential.CurveAction;
import de.fhg.iais.roberta.syntax.action.motor.differential.DriveAction;
import de.fhg.iais.roberta.syntax.action.motor.differential.MotorDriveStopAction;
import de.fhg.iais.roberta.syntax.action.motor.differential.TurnAction;
import de.fhg.iais.roberta.syntax.action.sound.GetVolumeAction;
import de.fhg.iais.roberta.syntax.action.sound.PlayFileAction;
import de.fhg.iais.roberta.syntax.action.sound.PlayNoteAction;
import de.fhg.iais.roberta.syntax.action.sound.SetVolumeAction;
import de.fhg.iais.roberta.syntax.action.sound.ToneAction;
import de.fhg.iais.roberta.syntax.configuration.ConfigurationComponent;
import de.fhg.iais.roberta.syntax.lang.expr.ConnectConst;
import de.fhg.iais.roberta.syntax.lang.expr.Expr;
import de.fhg.iais.roberta.syntax.lang.expr.NumConst;
import de.fhg.iais.roberta.syntax.lang.functions.MathOnListFunct;
import de.fhg.iais.roberta.syntax.lang.functions.MathSingleFunct;
import de.fhg.iais.roberta.syntax.sensor.ExternalSensor;
import de.fhg.iais.roberta.syntax.sensor.generic.ColorSensor;
import de.fhg.iais.roberta.syntax.sensor.generic.EncoderReset;
import de.fhg.iais.roberta.syntax.sensor.generic.EncoderSensor;
import de.fhg.iais.roberta.syntax.sensor.generic.HTColorSensor;
import de.fhg.iais.roberta.syntax.sensor.generic.KeysSensor;
import de.fhg.iais.roberta.syntax.sensor.generic.LightSensor;
import de.fhg.iais.roberta.syntax.sensor.generic.SoundSensor;
import de.fhg.iais.roberta.syntax.sensor.generic.TimerReset;
import de.fhg.iais.roberta.syntax.sensor.generic.TimerSensor;
import de.fhg.iais.roberta.syntax.sensor.generic.TouchSensor;
import de.fhg.iais.roberta.syntax.sensor.generic.UltrasonicSensor;
import de.fhg.iais.roberta.util.syntax.FunctionNames;
import de.fhg.iais.roberta.util.syntax.SC;
import de.fhg.iais.roberta.visitor.INxtVisitor;
import de.fhg.iais.roberta.visitor.NxtMethods;

public class NxtValidatorAndCollectorVisitor extends CommonNepoAndMotorValidatorAndCollectorVisitor implements INxtVisitor<Void> {

    private static final Map<String, String> SENSOR_COMPONENT_TYPE_MAP = Collections.unmodifiableMap(new HashMap<String, String>() {{
        put("COLOR_SENSING", SC.COLOR);
        put("HTCOLOR_SENSING", SC.HT_COLOR);
        put("LIGHT_SENSING", SC.LIGHT);
        put("SOUND_SENSING", SC.SOUND);
        put("TOUCH_SENSING", SC.TOUCH);
        put("ULTRASONIC_SENSING", SC.ULTRASONIC);
    }});

    private final boolean isSim;

    public NxtValidatorAndCollectorVisitor(ConfigurationAst brickConfiguration, ClassToInstanceMap<IProjectBean.IBuilder> beanBuilders, boolean isSim) {
        super(brickConfiguration, beanBuilders);
        this.isSim = isSim;
    }

    @Override
    public Void visitBluetoothCheckConnectAction(BluetoothCheckConnectAction bluetoothCheckConnectAction) {
        addToPhraseIfUnsupportedInSim(bluetoothCheckConnectAction, false, isSim);
        requiredComponentVisited(bluetoothCheckConnectAction, bluetoothCheckConnectAction.connection);
        return null;
    }

    @Override
    public Void visitBluetoothReceiveAction(BluetoothReceiveAction bluetoothReceiveAction) {
        addToPhraseIfUnsupportedInSim(bluetoothReceiveAction, true, isSim);
        requiredComponentVisited(bluetoothReceiveAction, bluetoothReceiveAction.connection);
        return null;
    }

    @Override
    public Void visitBluetoothSendAction(BluetoothSendAction bluetoothSendAction) {
        addToPhraseIfUnsupportedInSim(bluetoothSendAction, false, isSim);
        requiredComponentVisited(bluetoothSendAction, bluetoothSendAction.connection, bluetoothSendAction.msg);
        return null;
    }

    @Override
    public Void visitColorSensor(ColorSensor colorSensor) {
        if ( isSim && colorSensor.getMode().equals("AMBIENTLIGHT") ) {
            addErrorToPhrase(colorSensor, "SIM_BLOCK_NOT_SUPPORTED");
        }
        checkSensorPort(colorSensor);
        usedHardwareBuilder.addUsedSensor(new UsedSensor(colorSensor.getUserDefinedPort(), SC.COLOR, colorSensor.getMode()));
        return null;
    }

    @Override
    public Void visitConnectConst(ConnectConst connectConst) {
        addToPhraseIfUnsupportedInSim(connectConst, true, isSim);
        return null;
    }

    @Override
    public Void visitClearDisplayAction(ClearDisplayAction clearDisplayAction) {
        return null;
    }

    @Override
    public Void visitEncoderSensor(EncoderSensor encoderSensor) {
        ConfigurationComponent configurationComponent = robotConfiguration.optConfigurationComponent(encoderSensor.getUserDefinedPort());
        if ( configurationComponent == null ) {
            addErrorToPhrase(encoderSensor, "CONFIGURATION_ERROR_MOTOR_MISSING");
        } else {
            usedHardwareBuilder.addUsedActor(new UsedActor(encoderSensor.getUserDefinedPort(), configurationComponent.componentType));
        }
        return null;
    }

    @Override
    public Void visitEncoderReset(EncoderReset encoderReset) {
        ConfigurationComponent configurationComponent = robotConfiguration.optConfigurationComponent(encoderReset.sensorPort);
        if ( configurationComponent == null ) {
            addErrorToPhrase(encoderReset, "CONFIGURATION_ERROR_MOTOR_MISSING");
        } else {
            usedHardwareBuilder.addUsedActor(new UsedActor(encoderReset.sensorPort, configurationComponent.componentType));
        }
        return null;
    }

    @Override
    public Void visitHTColorSensor(HTColorSensor htColorSensor) {
        addToPhraseIfUnsupportedInSim(htColorSensor, true, isSim);
        checkSensorPort(htColorSensor);
        usedHardwareBuilder.addUsedSensor(new UsedSensor(htColorSensor.getUserDefinedPort(), SC.HT_COLOR, htColorSensor.getMode()));
        return null;
    }

    @Override
    public Void visitKeysSensor(KeysSensor keysSensor) {
        return null;
    }

    @Override
    public Void visitLightAction(LedAction lightAction) {
        ConfigurationComponent configurationComponent = robotConfiguration.optConfigurationComponent(lightAction.port);
        if ( configurationComponent == null ) {
            addErrorToPhrase(lightAction, "CONFIGURATION_ERROR_SENSOR_MISSING");
        } else if ( !configurationComponent.componentType.equals(SC.COLOR) ) {
            addErrorToPhrase(lightAction, "CONFIGURATION_ERROR_SENSOR_WRONG");
        } else {
            usedHardwareBuilder.addUsedSensor(new UsedSensor(lightAction.port, configurationComponent.componentType, SC.COLOR));
        }
        return null;
    }

    @Override
    public Void visitLightSensor(LightSensor lightSensor) {
        if ( isSim && lightSensor.getMode().equals("AMBIENTLIGHT") ) {
            addErrorToPhrase(lightSensor, "SIM_BLOCK_NOT_SUPPORTED");
        }
        checkSensorPort(lightSensor);
        usedHardwareBuilder.addUsedSensor(new UsedSensor(lightSensor.getUserDefinedPort(), SC.LIGHT, lightSensor.getMode()));
        return null;
    }

    @Override
    public Void visitMathOnListFunct(MathOnListFunct mathOnListFunct) {
        super.visitMathOnListFunct(mathOnListFunct);
        if ( mathOnListFunct.functName == FunctionNames.STD_DEV ) {
            usedMethodBuilder.addUsedMethod(FunctionNames.POWER);
            usedMethodBuilder.addUsedMethod(FunctionNames.STD_DEV);
        }
        return null;
    }

    @Override
    public Void visitMathSingleFunct(MathSingleFunct mathSingleFunct) {
        super.visitMathSingleFunct(mathSingleFunct);
        switch ( mathSingleFunct.functName ) {
            case ACOS:
                usedMethodBuilder.addUsedMethod(FunctionNames.ASIN);
                usedMethodBuilder.addUsedMethod(FunctionNames.POWER);
                usedMethodBuilder.addUsedMethod(NxtMethods.FACTORIAL);
                break;
            case ASIN:
            case COS:
            case SIN:
                usedMethodBuilder.addUsedMethod(FunctionNames.POWER);
                usedMethodBuilder.addUsedMethod(NxtMethods.FACTORIAL);
                break;
            case ATAN:
            case EXP:
            case LN:
                usedMethodBuilder.addUsedMethod(FunctionNames.POWER);
                break;
            case LOG10:
                usedMethodBuilder.addUsedMethod(FunctionNames.LN);
                usedMethodBuilder.addUsedMethod(FunctionNames.POWER);
                break;
            case ROUND:
            case ROUNDUP:
                usedMethodBuilder.addUsedMethod(FunctionNames.ROUNDDOWN);
                break;
            case TAN:
                usedMethodBuilder.addUsedMethod(FunctionNames.SIN);
                usedMethodBuilder.addUsedMethod(FunctionNames.COS);
                usedMethodBuilder.addUsedMethod(FunctionNames.POWER);
                usedMethodBuilder.addUsedMethod(NxtMethods.FACTORIAL);
                break;
            default:
                break; // no action necessary
        }
        return null;
    }

    @Override
    public Void visitPlayFileAction(PlayFileAction playFileAction) {
        return null;
    }

    @Override
    public Void visitPlayNoteAction(PlayNoteAction playNoteAction) {
        usedHardwareBuilder.addUsedActor(new UsedActor("", SC.SOUND));
        return null;
    }

    @Override
    public Void visitShowTextAction(ShowTextAction showTextAction) {
        requiredComponentVisited(showTextAction, showTextAction.msg, showTextAction.x, showTextAction.y);
        return null;
    }

    @Override
    public Void visitSoundSensor(SoundSensor soundSensor) {
        checkSensorPort(soundSensor);
        usedHardwareBuilder.addUsedSensor(new UsedSensor(soundSensor.getUserDefinedPort(), SC.SOUND, soundSensor.getMode()));
        return null;
    }

    @Override
    public Void visitTimerSensor(TimerSensor timerSensor) {
        usedHardwareBuilder.addUsedSensor(new UsedSensor(timerSensor.getUserDefinedPort(), SC.TIMER, timerSensor.getMode()));
        return null;
    }

    @Override
    public Void visitTimerReset(TimerReset timerReset) {
        usedHardwareBuilder.addUsedSensor(new UsedSensor(timerReset.sensorPort, SC.TIMER, SC.DEFAULT));
        return null;
    }

    @Override
    public Void visitToneAction(ToneAction toneAction) {
        requiredComponentVisited(toneAction, toneAction.duration, toneAction.frequency);
        return null;
    }

    @Override
    public Void visitTouchSensor(TouchSensor touchSensor) {
        checkSensorPort(touchSensor);
        usedHardwareBuilder.addUsedSensor(new UsedSensor(touchSensor.getUserDefinedPort(), SC.TOUCH, touchSensor.getMode()));
        return null;
    }

    @Override
    public Void visitUltrasonicSensor(UltrasonicSensor ultrasonicSensor) {
        checkSensorPort(ultrasonicSensor);
        usedHardwareBuilder.addUsedSensor(new UsedSensor(ultrasonicSensor.getUserDefinedPort(), SC.ULTRASONIC, ultrasonicSensor.getMode()));
        return null;
    }

    @Override
    public Void visitGetVolumeAction(GetVolumeAction getVolumeAction) {

        usedHardwareBuilder.addUsedActor(new UsedActor("", SC.SOUND));
        return null;
    }

    @Override
    public Void visitSetVolumeAction(SetVolumeAction setVolumeAction) {
        requiredComponentVisited(setVolumeAction, setVolumeAction.volume);
        usedHardwareBuilder.addUsedActor(new UsedActor("", SC.SOUND));
        return null;
    }

    @Override
    public Void visitCurveAction(CurveAction curveAction) {
        requiredComponentVisited(curveAction, curveAction.paramLeft.getSpeed(), curveAction.paramRight.getSpeed());
        Optional.ofNullable(curveAction.paramLeft.getDuration())
            .ifPresent(duration -> requiredComponentVisited(curveAction, duration.getValue()));
        Optional.ofNullable(curveAction.paramRight.getDuration())
            .ifPresent(duration -> requiredComponentVisited(curveAction, duration.getValue()));
        checkForZeroSpeedInCurve(curveAction.paramLeft.getSpeed(), curveAction.paramRight.getSpeed(), curveAction);
        checkLeftRightMotorPort(curveAction);
        addLeftAndRightMotorToUsedActors();
        return null;
    }

    @Override
    public Void visitTurnAction(TurnAction turnAction) {
        checkAndVisitMotionParam(turnAction, turnAction.param);
        checkLeftRightMotorPort(turnAction);
        addLeftAndRightMotorToUsedActors();
        return null;
    }

    @Override
    public Void visitDriveAction(DriveAction driveAction) {
        checkAndVisitMotionParam(driveAction, driveAction.param);
        checkLeftRightMotorPort(driveAction);
        addLeftAndRightMotorToUsedActors();
        return null;
    }

    @Override
    public Void visitMotorDriveStopAction(MotorDriveStopAction stopAction) {
        checkLeftRightMotorPort(stopAction);
        addLeftAndRightMotorToUsedActors();
        return null;
    }

    private void checkLeftRightMotorPort(Phrase driveAction) {
        if ( hasTooManyMotors(driveAction) ) {
            return;
        }

        ConfigurationComponent leftMotor = this.robotConfiguration.getFirstMotor(SC.LEFT);
        ConfigurationComponent rightMotor = this.robotConfiguration.getFirstMotor(SC.RIGHT);
        checkLeftMotorPresenceAndRegulation(driveAction, leftMotor);
        checkRightMotorPresenceAndRegulation(driveAction, rightMotor);
        checkMotorRotationDirection(driveAction, leftMotor, rightMotor);
    }

    protected boolean hasTooManyMotors(Phrase driveAction) {
        if ( this.robotConfiguration.getMotors(SC.RIGHT).size() > 1 ) {
            addErrorToPhrase(driveAction, "CONFIGURATION_ERROR_MULTIPLE_RIGHT_MOTORS");
            return true;
        }
        if ( this.robotConfiguration.getMotors(SC.LEFT).size() > 1 ) {
            addErrorToPhrase(driveAction, "CONFIGURATION_ERROR_MULTIPLE_LEFT_MOTORS");
            return true;
        }
        return false;
    }

    private void checkRightMotorPresenceAndRegulation(Phrase driveAction, ConfigurationComponent rightMotor) {
        if ( rightMotor == null ) {
            addErrorToPhrase(driveAction, "CONFIGURATION_ERROR_MOTOR_RIGHT_MISSING");
        } else {
            checkIfMotorRegulated(driveAction, rightMotor, "CONFIGURATION_ERROR_MOTOR_RIGHT_UNREGULATED");
        }
    }

    private void checkLeftMotorPresenceAndRegulation(Phrase driveAction, ConfigurationComponent leftMotor) {
        if ( leftMotor == null ) {
            addErrorToPhrase(driveAction, "CONFIGURATION_ERROR_MOTOR_LEFT_MISSING");
        } else {
            checkIfMotorRegulated(driveAction, leftMotor, "CONFIGURATION_ERROR_MOTOR_LEFT_UNREGULATED");
        }
    }

    private void checkIfMotorRegulated(Phrase driveAction, ConfigurationComponent motor, String errorMsg) {
        if ( !motor.getProperty(SC.MOTOR_REGULATION).equals(SC.TRUE) ) {
            addErrorToPhrase(driveAction, errorMsg);
        }
    }

    private void checkMotorRotationDirection(Phrase driveAction, ConfigurationComponent leftMotor, ConfigurationComponent rightMotor) {
        if ( (leftMotor == null) || (rightMotor == null) ) {
            return;
        }

        boolean rotationDirectionsEqual = leftMotor.getProperty(SC.MOTOR_REVERSE).equals(rightMotor.getProperty(SC.MOTOR_REVERSE));
        if ( !rotationDirectionsEqual ) {
            addErrorToPhrase(driveAction, "CONFIGURATION_ERROR_MOTORS_ROTATION_DIRECTION");
        }
    }

    private void checkForZeroSpeedInCurve(Expr speedLeft, Expr speedRight, Action action) {
        if ( speedLeft.getKind().hasName("NUM_CONST") && speedRight.getKind().hasName("NUM_CONST") ) {
            double speedLeftNumConst = Double.parseDouble(((NumConst) speedLeft).value);
            double speedRightNumConst = Double.parseDouble(((NumConst) speedRight).value);

            boolean bothMotorsHaveZeroSpeed = (Math.abs(speedLeftNumConst) < DOUBLE_EPS) && (Math.abs(speedRightNumConst) < DOUBLE_EPS);
            if ( bothMotorsHaveZeroSpeed ) {
                addWarningToPhrase(action, "MOTOR_SPEED_0");
            }
        }
    }

    private void addLeftAndRightMotorToUsedActors() {
        Optional<String> optionalLeftPort = Optional.ofNullable(robotConfiguration.getFirstMotor(SC.LEFT))
            .map(configurationComponent1 -> configurationComponent1.userDefinedPortName);

        Optional<String> optionalRightPort = Optional.ofNullable(robotConfiguration.getFirstMotor(SC.RIGHT))
            .map(configurationComponent -> configurationComponent.userDefinedPortName);

        if ( optionalLeftPort.isPresent() && optionalRightPort.isPresent() ) {
            usedHardwareBuilder.addUsedActor(new UsedActor(optionalLeftPort.get(), SC.LARGE));
            usedHardwareBuilder.addUsedActor(new UsedActor(optionalRightPort.get(), SC.LARGE));
        }
    }

    private void checkSensorPort(ExternalSensor sensor) {
        ConfigurationComponent configurationComponent = robotConfiguration.optConfigurationComponent(sensor.getUserDefinedPort());
        if ( configurationComponent == null ) {
            addErrorToPhrase(sensor, "CONFIGURATION_ERROR_SENSOR_MISSING");
            return;
        }
        String expectedComponentType = SENSOR_COMPONENT_TYPE_MAP.get(sensor.getKind().getName());
        if ( expectedComponentType != null && !expectedComponentType.equalsIgnoreCase(configurationComponent.componentType) ) {
            addErrorToPhrase(sensor, "CONFIGURATION_ERROR_SENSOR_WRONG");
        }
    }
}
