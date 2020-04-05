package frc.robot;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;


/**
 * A list of constants used by the rest of the robot code. This include physics
 * constants as well as constants determined through calibrations.
 */
public class Constants
{
    // singleton class
    private static Constants instance = null;

    public static Constants getInstance()
    {
        if (instance == null)
        {
            instance = new Constants();
        }
        return instance;
    }

    //=========================================================================
    // Port/Channel Numbers
    //
    // Do not change anything in this section unless you rewire the robot and
    // update the spreadsheet!
    //=========================================================================

    // Motor Controllers
    public static int kLeftMotorMasterDeviceId =   1;
    public static int kLeftMotorSlave1DeviceId =   2;
    public static int kLeftMotorSlave2DeviceId =   3;
    public static int kRightMotorMasterDeviceId =  4;
    public static int kRightMotorSlave1DeviceId =  5;
    public static int kRightMotorSlave2DeviceId =  6;


    //=========================================================================
    // Generic Constants
    //=========================================================================

    public static double kLoopDt = 0.01;
    public static double kDriveWatchdogTimerThreshold = 0.500;
    public static final int kCANTimeoutMs = 10;         // ms, use for on the fly updates
    public static final int kLongCANTimeoutMs = 100;    // ms, use for constructors
    public static double kTalonFramePeriod = 0.1;       // sec
    public static int kTalonTimeoutMs = 5; // ms


    public static int kTalonPidIdx = 0; // 0 for primary closed-loop PIDs, 1 for auxiliary closed-loop PIDs
    public static int kTalonFXPidIdx = 1; // 0 for primary closed-loop PIDs, 1 for auxiliary closed-loop PIDs

    public static double kNominalBatteryVoltage = 12.0;


    //=========================================================================
    // Robot Geometry Constants
    //=========================================================================

    // Bumpers
    public static double kCenterToFrontBumper = 18.0; // position of front bumper with respect to robot center of rotation
    public static double kCenterToExtendedIntake = 18.0; // position of intake sweetspot when extended with respect to robot center of rotation
    public static double kCenterToRearBumper = 18.0; // position of rear bumper with respect to robot center of rotation
    public static double kCenterToSideBumper = 18.0; // position of side bumper with respect to robot center of rotation
    public static double kCenterToCornerBumper = Math.sqrt(kCenterToRearBumper * kCenterToRearBumper + kCenterToSideBumper * kCenterToSideBumper);

    // Camera
    public static double kCameraPoseX = 0.0; // camera location with respect to robot center of rotation, +X axis is in direction of travel
    public static double kCameraPoseY = 0.0; // camera location with respect to robot center of rotation, +Y axis is positive to the left
    public static double kCameraPoseTheta = 0.0; // camera angle with respect to robot heading

    //=========================================================================
    // Robot Drive Constants
    //=========================================================================

     // Motors inversions
     public static boolean kLeftMotorInverted = false;
     public static boolean kRightMotorInverted = true;
     public static boolean kLeftMotorSensorPhase = false;
     public static boolean kRightMotorSensorPhase = false;
 
     public static int kDriveTrainCurrentLimit = 25;

    // Gyro selection 
    public enum GyroSelectionEnum { BNO055, NAVX, PIGEON; }
    public static GyroSelectionEnum GyroSelection = GyroSelectionEnum.PIGEON;


    //=========================================================================
    // Vision/Targeting Constants
    //=========================================================================

    public static double kVisionMaxVel = 20.0; // inches/sec
    public static double kVisionMaxAccel = 20.0; // inches/sec^2
    public static double kTargetWidthInches = 10.25;
    public static double kVisionCompletionTolerance = 1.0;
    public static double kVisionMaxDistanceInches = 240; // ignore targets greater than this distance
    public static double kVisionLookaheadDist = 24.0; // inches
    public static double kCameraFOVDegrees = 42.5; // Camera Field of View (degrees)
    public static double kCameraHalfFOVRadians = kCameraFOVDegrees / 2.0 * Math.PI / 180.0; // Half of Camera Field of
                                                                                            // View (radians)
    public static double kTangentCameraHalfFOV = Math.tan(kCameraHalfFOVRadians);
    public static double kCameraLatencySeconds = 0.240; // Camera image capturing latency
    public static double kTargetLocationFilterConstant = (30.0 * kLoopDt); // 30 time constants in 1 second

    public static double kTargetDistanceFromCameraInches = kCenterToFrontBumper - kCameraPoseX + 12.0;


    /**
     * @return the MAC address of the robot
     */
    public static String getMACAddress() {
        try {
            Enumeration<NetworkInterface> nwInterface = NetworkInterface.getNetworkInterfaces();
            StringBuilder ret = new StringBuilder();
            while (nwInterface.hasMoreElements()) {
                NetworkInterface nis = nwInterface.nextElement();
                if (nis != null) {
                    byte[] mac = nis.getHardwareAddress();
                    if (mac != null) {
                        for (int i = 0; i < mac.length; i++) {
                            ret.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
                        }
                        return ret.toString();
                    } else {
                        System.out.println("Address doesn't exist or is not accessible");
                    }
                } else {
                    System.out.println("Network Interface for the specified address is not found.");
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        return "";
    }    
};
