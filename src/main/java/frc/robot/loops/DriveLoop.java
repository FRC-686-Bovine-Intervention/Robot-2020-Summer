package frc.robot.loops;

import java.util.ArrayList;
import java.util.List;

import com.ctre.phoenix.motorcontrol.ControlMode;
import com.ctre.phoenix.motorcontrol.FeedbackDevice;
import com.ctre.phoenix.motorcontrol.NeutralMode;
import com.ctre.phoenix.motorcontrol.SensorCollection;
import com.ctre.phoenix.motorcontrol.StatusFrameEnhanced;
import com.ctre.phoenix.motorcontrol.can.BaseMotorController;
import com.ctre.phoenix.motorcontrol.can.TalonSRX;
import com.ctre.phoenix.motorcontrol.can.VictorSPX;

import edu.wpi.first.wpilibj.Timer;

import frc.robot.Constants;
import frc.robot.command_status.DriveCommand;
import frc.robot.command_status.DriveState;
import frc.robot.lib.sensors.BNO055;
import frc.robot.lib.sensors.GyroBase;
import frc.robot.lib.sensors.NavX;
import frc.robot.subsystems.Drive;

/*
 * DriveLoop is the interface between Drive.java and the actual hardware.
 * It runs periodically, taking the commands sent by Drive and sending them to the hardware.
 * In this way, Drive.java does not access the hardware directly.  The benefits of this partition are: 
 * 1) Changes to drive hardware only requires changes to DriveLoop, not Drive
 * 2) DriveLoop can be easily replaced for simulation purposes.
 */

public class DriveLoop implements Loop
{
	// singleton class
	private static DriveLoop instance = null;

	public static DriveLoop getInstance()
	{
		if (instance == null)
		{
			instance = new DriveLoop();
		}
		return instance;
	}

	private static Drive drive;
	private static GyroBase gyro;
	private DriveState driveState;

	public final TalonSRX lMotorMaster;
	public final TalonSRX rMotorMaster;
	public final List<BaseMotorController> lMotorSlaves;
	public final List<BaseMotorController> rMotorSlaves;

	private static final int kVelocityControlSlot = 0;
	private static final int kBaseLockControlSlot = 1;

	// Wheels
	public static double kDriveWheelCircumInches = 18.800;
	public static double kDriveWheelDiameterInches = 11.500;
	public static double kTrackLengthInches = 21.500;
	public static double kTrackWidthInches = kDriveWheelCircumInches / Math.PI;
	public static double kTrackEffectiveDiameter = (kTrackWidthInches * kTrackWidthInches + kTrackLengthInches * kTrackLengthInches) / kTrackWidthInches;;
	public static double kTrackScrubFactor = 0.5;

	// Wheel Encoder
	public static double kQuadEncoderGain = 1.0; 	// number of drive shaft rotations per encoder shaft rotation
													// 1.0 if encoder is directly coupled to the drive shaft
	public static int kQuadEncoderCodesPerRev = 64;
	public static int kQuadEncoderUnitsPerRev = (int) (4 * kQuadEncoderCodesPerRev / kQuadEncoderGain);;
	public static double kQuadEncoderStatusFramePeriod = 0.100; // 100 ms

	// CONTROL LOOP GAINS
	public static double kDriveSecondsFromNeutralToFull = 0.375; // decrease acceleration (reduces current, robot tipping)
	public static double kNominalEncoderPulsePer100ms = 85; // velocity at a nominal throttle (measured using NI web interface)
	public static double kNominalPercentOutput = 0.4447; // percent output of motor at above throttle (using NI web interface)

	// PID gains for drive velocity loop (sent to Talon)
	// Units: error is 4*256 counts/rev. Max output is +/- 1023 units.
	public static double kDriveVelocityKp = 20.0;
	public static double kDriveVelocityKi = 0.01;
	public static double kDriveVelocityKd = 70.0;
	public static double kDriveVelocityKf = kNominalPercentOutput * 1023.0 / kNominalEncoderPulsePer100ms;
	public static int kDriveVelocityIZone = 0;
	public static double kDriveVelocityRampRate = 0.375;
	public static int kDriveVelocityAllowableError = 0;

	// PID gains for drive base lock loop
	// Units: error is 4*256 counts/rev. Max output is +/- 1023 units.
	public static double kDriveBaseLockKp = 0.5;
	public static double kDriveBaseLockKi = 0;
	public static double kDriveBaseLockKd = 0;
	public static double kDriveBaseLockKf = 0;
	public static int kDriveBaseLockIZone = 0;
	public static double kDriveBaseLockRampRate = 0;
	public static int kDriveBaseLockAllowableError = 10;

	// PID gains for constant heading velocity control
	// Units: Error is degrees. Output is inches/second difference to
	// left/right.
	public static double kDriveHeadingVelocityKp = 4.0;
	public static double kDriveHeadingVelocityKi = 0.0;
	public static double kDriveHeadingVelocityKd = 50.0;

	// Point Turn constants
	public static double kPointTurnKp = 0.05;
	public static double kPointTurnKd = 0.50;
	public static double kPointTurnKi = 0.00;
	public static double kPointTurnKf = 0.00;
	public static double kPointTurnCompletionToleranceDeg = 3.0;
	public static double kPointTurnMaxOutput = 0.7;

	// Path following constants
	public static double kPathFollowingMaxVel = 72.0; // inches/sec
	public static double kPathFollowingAccelTime = 0.5; // sec to reach max velocity
	public static double kPathFollowingMaxAccel = kPathFollowingMaxVel / kPathFollowingAccelTime; // inches/sec^2
	public static double kPathFollowingLookahead = 24.0; // inches
	public static double kPathFollowingCompletionTolerance = 4.0;

	private DriveLoop()
	{
		drive = Drive.getInstance();
		driveState = DriveState.getInstance();

		/*****************************************************************
		 * Configure Master Motor Controllers
		 *****************************************************************/
		lMotorMaster = new TalonSRX(Constants.kLeftMotorMasterDeviceId);
		rMotorMaster = new TalonSRX(Constants.kRightMotorMasterDeviceId);

		// Get status at 100Hz (faster than default 50 Hz)
		lMotorMaster.setStatusFramePeriod(StatusFrameEnhanced.Status_2_Feedback0, 10, Constants.kCANTimeoutMs);
		rMotorMaster.setStatusFramePeriod(StatusFrameEnhanced.Status_2_Feedback0, 10, Constants.kCANTimeoutMs);

		lMotorMaster.set(ControlMode.PercentOutput, 0.0);
		rMotorMaster.set(ControlMode.PercentOutput, 0.0);
		lMotorMaster.setNeutralMode(NeutralMode.Coast);
		rMotorMaster.setNeutralMode(NeutralMode.Coast);

		lMotorMaster.configOpenloopRamp(kDriveSecondsFromNeutralToFull, Constants.kCANTimeoutMs);
		rMotorMaster.configOpenloopRamp(kDriveSecondsFromNeutralToFull, Constants.kCANTimeoutMs);

		// Set up the encoders
		lMotorMaster.configSelectedFeedbackSensor(FeedbackDevice.QuadEncoder, Constants.kTalonPidIdx, Constants.kCANTimeoutMs); // configure for closed-loop PID
		rMotorMaster.configSelectedFeedbackSensor(FeedbackDevice.QuadEncoder, Constants.kTalonPidIdx, Constants.kCANTimeoutMs);
		lMotorMaster.setSensorPhase(Constants.kLeftMotorSensorPhase);
		rMotorMaster.setSensorPhase(Constants.kRightMotorSensorPhase);
		lMotorMaster.setInverted(Constants.kLeftMotorInverted);
		rMotorMaster.setInverted(Constants.kRightMotorInverted);

		// Load velocity control gains
		lMotorMaster.config_kF(kVelocityControlSlot, kDriveVelocityKf, Constants.kCANTimeoutMs);
		lMotorMaster.config_kP(kVelocityControlSlot, kDriveVelocityKp, Constants.kCANTimeoutMs);
		lMotorMaster.config_kI(kVelocityControlSlot, kDriveVelocityKi, Constants.kCANTimeoutMs);
		lMotorMaster.config_kD(kVelocityControlSlot, kDriveVelocityKd, Constants.kCANTimeoutMs);
		lMotorMaster.config_IntegralZone(kVelocityControlSlot, kDriveVelocityIZone, Constants.kCANTimeoutMs);

		rMotorMaster.config_kF(kVelocityControlSlot, kDriveVelocityKf, Constants.kCANTimeoutMs);
		rMotorMaster.config_kP(kVelocityControlSlot, kDriveVelocityKp, Constants.kCANTimeoutMs);
		rMotorMaster.config_kI(kVelocityControlSlot, kDriveVelocityKi, Constants.kCANTimeoutMs);
		rMotorMaster.config_kD(kVelocityControlSlot, kDriveVelocityKd, Constants.kCANTimeoutMs);
		rMotorMaster.config_IntegralZone(kVelocityControlSlot, kDriveVelocityIZone, Constants.kCANTimeoutMs);

		lMotorMaster.configAllowableClosedloopError(kVelocityControlSlot, kDriveVelocityAllowableError,	Constants.kCANTimeoutMs);
		rMotorMaster.configAllowableClosedloopError(kVelocityControlSlot, kDriveVelocityAllowableError,	Constants.kCANTimeoutMs);

		// Load base lock control gains
		lMotorMaster.config_kF(kBaseLockControlSlot, kDriveBaseLockKf, Constants.kCANTimeoutMs);
		lMotorMaster.config_kP(kBaseLockControlSlot, kDriveBaseLockKp, Constants.kCANTimeoutMs);
		lMotorMaster.config_kI(kBaseLockControlSlot, kDriveBaseLockKi, Constants.kCANTimeoutMs);
		lMotorMaster.config_kD(kBaseLockControlSlot, kDriveBaseLockKd, Constants.kCANTimeoutMs);
		lMotorMaster.config_IntegralZone(kBaseLockControlSlot, kDriveBaseLockIZone, Constants.kCANTimeoutMs);

		rMotorMaster.config_kF(kBaseLockControlSlot, kDriveBaseLockKf, Constants.kCANTimeoutMs);
		rMotorMaster.config_kP(kBaseLockControlSlot, kDriveBaseLockKp, Constants.kCANTimeoutMs);
		rMotorMaster.config_kI(kBaseLockControlSlot, kDriveBaseLockKi, Constants.kCANTimeoutMs);
		rMotorMaster.config_kD(kBaseLockControlSlot, kDriveBaseLockKd, Constants.kCANTimeoutMs);
		rMotorMaster.config_IntegralZone(kBaseLockControlSlot, kDriveBaseLockIZone, Constants.kCANTimeoutMs);

		lMotorMaster.configAllowableClosedloopError(kBaseLockControlSlot, kDriveBaseLockAllowableError,	Constants.kCANTimeoutMs);
		rMotorMaster.configAllowableClosedloopError(kBaseLockControlSlot, kDriveBaseLockAllowableError,	Constants.kCANTimeoutMs);

		lMotorMaster.configOpenloopRamp(kDriveVelocityRampRate, 0);
		rMotorMaster.configOpenloopRamp(kDriveVelocityRampRate, 0);

		/*****************************************************************
		 * Configure Slave Motor Controllers
		 *****************************************************************/
		lMotorSlaves = new ArrayList<BaseMotorController>();
		rMotorSlaves = new ArrayList<BaseMotorController>();
		System.out.println("Competition Bot: 1 VictorSPX slave per side");
		lMotorSlaves.add(new VictorSPX(Constants.kLeftMotorSlave1DeviceId));
		rMotorSlaves.add(new VictorSPX(Constants.kRightMotorSlave1DeviceId));

		for (BaseMotorController lMotorSlave : lMotorSlaves)
		{
			lMotorSlave.follow(lMotorMaster); // give slave the DeviceId of it's master
			lMotorSlave.setNeutralMode(NeutralMode.Coast);
			lMotorSlave.setInverted(Constants.kLeftMotorInverted);
		}
		for (BaseMotorController rMotorSlave : rMotorSlaves)
		{
			rMotorSlave.follow(rMotorMaster); // give slave the DeviceId of it's master
			rMotorSlave.setNeutralMode(NeutralMode.Coast);
			rMotorSlave.setInverted(Constants.kRightMotorInverted);
		}

		/*****************************************************************
		 * Set Initial Motor Settings
		 *****************************************************************/
		DriveCommand neutralCmd = DriveCommand.COAST();
		setControlMode(neutralCmd);
		setMotors(neutralCmd);
		setNeutralMode(neutralCmd);
		resetEncoders(neutralCmd);

		/*****************************************************************
		 * Select which Gyro is installed
		 *****************************************************************/
		switch (Constants.GyroSelection)
		{
		case BNO055:
			System.out.println("Selected gyro = BNO055");
			gyro = BNO055.getInstance();
			break;
		case NAVX:
		default:
			System.out.println("Selected gyro = NavX");
			gyro = NavX.getInstance();
			break;
		}

	}

	@Override
	public void onStart(double timestamp)
	{
		// nothing
	}

	@Override
	public void onLoop(double timestamp)
	{
		// nothing
	}

	@Override
	public void onStop(double timestamp)
	{
		stopMotors();
	}

	private void stopMotors()
	{
		drive.setCommand(DriveCommand.COAST()); // override any incoming commands
		sendCommands();
	}

	private void getStatus()
	{
		synchronized (driveState) 	// lock DriveState until we update it, so that objects reading DriveState don't
									// get partial updates
		{
			// get Talon control & brake modes (assume right motor is configured identically)
			driveState.setTalonControlMode(lMotorMaster.getControlMode());
			driveState.setNeutralMode(DriveCommand.getNeutralMode());

			// get encoder values from hardware, set in Drive
			driveState.setLeftDistanceInches(encoderUnitsToInches(lMotorMaster.getSelectedSensorPosition(Constants.kTalonPidIdx)));
			driveState.setRightDistanceInches(encoderUnitsToInches(rMotorMaster.getSelectedSensorPosition(Constants.kTalonPidIdx)));

			driveState.setLeftSpeedInchesPerSec(encoderUnitsPerFrameToInchesPerSecond(lMotorMaster.getSelectedSensorVelocity(Constants.kTalonPidIdx)));
			driveState.setRightSpeedInchesPerSec(encoderUnitsPerFrameToInchesPerSecond(rMotorMaster.getSelectedSensorVelocity(Constants.kTalonPidIdx)));

			/*
			 * measured angle decreases with clockwise rotation it should increase with
			 * clockwise rotation (according to documentation, and standard right hand rule
			 * convention negate it here to correct
			 */
			driveState.setHeadingDeg(gyro.getHeadingDeg());

			driveState.setMotorCurrent(lMotorMaster.getOutputCurrent(), rMotorMaster.getOutputCurrent());
			driveState.setMotorPIDError(lMotorMaster.getClosedLoopError(Constants.kTalonPidIdx),rMotorMaster.getClosedLoopError(Constants.kTalonPidIdx));

			switch (driveState.getTalonControlMode())
			{
			case PercentOutput:
				driveState.setMotorStatus(lMotorMaster.getMotorOutputPercent(), rMotorMaster.getMotorOutputPercent());
				break;

			case Position:
				driveState.setMotorStatus(lMotorMaster.getSelectedSensorPosition(Constants.kTalonPidIdx),
										  rMotorMaster.getSelectedSensorPosition(Constants.kTalonPidIdx));
				break;

			case Velocity:
				driveState.setMotorStatus(lMotorMaster.getSelectedSensorVelocity(Constants.kTalonPidIdx),
										  rMotorMaster.getSelectedSensorVelocity(Constants.kTalonPidIdx));
				break;

			case Disabled:
			default:
				driveState.setMotorStatus(lMotorMaster.getMotorOutputPercent(), rMotorMaster.getMotorOutputPercent());
				break;
			}
		}
	}

	private void sendCommands()
	{
		DriveCommand newCmd = drive.getCommand();

		// Watchdog timer
		double currentTime = Timer.getFPGATimestamp();
		if (currentTime - newCmd.getCommandTime() > Constants.kDriveWatchdogTimerThreshold)
		{
			// Halt robot if new command hasn't been sent in a while
			stopMotors();
			return;
		}

		synchronized (newCmd) // lock DriveCommand so no one changes it under us while we are sending the commands
		{
			setControlMode(newCmd);
			setMotors(newCmd);
			setNeutralMode(newCmd);
			resetEncoders(newCmd);
		}
	}

	private void setControlMode(DriveCommand newCmd)
	{
		ControlMode newMode = newCmd.getTalonControlMode();

		if (newMode != driveState.getTalonControlMode())
		{
			switch (newMode)
			{
			case PercentOutput:
				break;

			case Position:
				lMotorMaster.selectProfileSlot(kBaseLockControlSlot, Constants.kTalonPidIdx);
				rMotorMaster.selectProfileSlot(kBaseLockControlSlot, Constants.kTalonPidIdx);

				lMotorMaster.set(ControlMode.Position, (double) (lMotorMaster.getSelectedSensorPosition(Constants.kTalonPidIdx)));
				rMotorMaster.set(ControlMode.Position, (double) (rMotorMaster.getSelectedSensorPosition(Constants.kTalonPidIdx)));
				break;

			case Velocity:
				lMotorMaster.selectProfileSlot(kVelocityControlSlot, Constants.kTalonPidIdx);
				rMotorMaster.selectProfileSlot(kVelocityControlSlot, Constants.kTalonPidIdx);
				break;

			case Disabled:
			default:
				break;
			}
		}
	}

	private void setNeutralMode(DriveCommand newCmd)
	{
		NeutralMode newNeutral = DriveCommand.getNeutralMode();
		setNeutralMode(newNeutral);
	}

	private void setNeutralMode(NeutralMode newNeutral)
	{
		if (newNeutral != driveState.getNeutralMode())
		{
			lMotorMaster.setNeutralMode(newNeutral);
			rMotorMaster.setNeutralMode(newNeutral);
			for (BaseMotorController lMotorSlave : lMotorSlaves)
				lMotorSlave.setNeutralMode(newNeutral);
			for (BaseMotorController rMotorSlave : rMotorSlaves)
				rMotorSlave.setNeutralMode(newNeutral);
		}
	}

	private void setMotors(DriveCommand newCmd)
	{
		double lMotorCtrl = newCmd.getLeftMotor();
		double rMotorCtrl = newCmd.getRightMotor();

		switch (newCmd.getTalonControlMode()) // assuming new mode is already configured
		{
		case PercentOutput:
			// DriveCommand given in range +/-1, with 1 representing full throttle
			lMotorMaster.set(ControlMode.PercentOutput, lMotorCtrl);
			rMotorMaster.set(ControlMode.PercentOutput, rMotorCtrl);
			break;

		case Position:
			// no changes to position set in setControlMode()
			break;

		case Velocity:
			// DriveCommand given in inches/sec
			// Talon SRX needs RPM in closed-loop mode.
			// convert inches/sec to encoder edges per 100ms
			lMotorMaster.set(ControlMode.Velocity, inchesPerSecondToEncoderUnitsPerFrame(lMotorCtrl));
			rMotorMaster.set(ControlMode.Velocity, inchesPerSecondToEncoderUnitsPerFrame(rMotorCtrl));
			break;

		case Disabled:
		default:
			lMotorMaster.set(ControlMode.Disabled, 0);
			rMotorMaster.set(ControlMode.Disabled, 0);
			break;
		}
	}

	// Talon SRX reports position in rotations while in closed-loop Position mode
	private static double encoderUnitsToInches(int _encoderPosition)
	{
		return (double) _encoderPosition / (double) kQuadEncoderUnitsPerRev * kDriveWheelCircumInches;
	}

	private static int inchesToEncoderUnits(double _inches)
	{
		return (int) (_inches / kDriveWheelCircumInches * kQuadEncoderUnitsPerRev);
	}

	// Talon SRX reports speed in RPM while in closed-loop Speed mode
	private static double encoderUnitsPerFrameToInchesPerSecond(int _encoderEdgesPerFrame)
	{
		return encoderUnitsToInches(_encoderEdgesPerFrame) / kQuadEncoderStatusFramePeriod;
	}

	private static int inchesPerSecondToEncoderUnitsPerFrame(double _inchesPerSecond)
	{
		return (int) (inchesToEncoderUnits(_inchesPerSecond) * kQuadEncoderStatusFramePeriod);
	}

	private void resetEncoders(DriveCommand newCmd)
	{
		if (newCmd.getResetEncoders())
		{
			SensorCollection collection = lMotorMaster.getSensorCollection();
			collection.setQuadraturePosition(0, Constants.kCANTimeoutMs);

			collection = rMotorMaster.getSensorCollection();
			collection.setQuadraturePosition(0, Constants.kCANTimeoutMs);

			// cannot reset gyro heading in hardware.
			// calibration to desired initial pose is done in RobotState.reset() called from
			// Robot.autonomousInit()
		}
	}

};
