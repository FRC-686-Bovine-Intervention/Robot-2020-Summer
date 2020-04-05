package frc.robot.lib.sensors;

import com.kauailabs.navx.frc.AHRS;

//import edu.wpi.first.wpilibj.SPI;
import edu.wpi.first.wpilibj.I2C;
import frc.robot.Constants;

public class NavX extends GyroBase
{
	// singleton class
	private static NavX instance = null;

	public static NavX getInstance()
	{
		if (instance == null)
		{
			instance = new NavX();
		}
		return instance;
	}

	AHRS ahrs;

	// The SPI port the NavX is connected to
	// (see
	// https://www.pdocs.kauailabs.com/navx-mxp/guidance/selecting-an-interface/)
	// public static final SPI.Port NAVX_PORT = SPI.Port.kMXP; // the SPI port has
	// low latency (<0.1 ms)
	public static final I2C.Port NAVX_PORT = I2C.Port.kMXP; // the SPI port has low latency (<0.1 ms)
	public static byte NAVX_UPDATE_RATE = (byte) (1.0 / Constants.kLoopDt); // the SPI port supports update rates from
																			// 4-200 Hz

	// constructors
	public NavX()
	{
		ahrs = new AHRS(NAVX_PORT, NAVX_UPDATE_RATE);
	}

	/**
	 * Returns heading for the GyroBase class.
	 *
	 */
	public double getHeadingDeg()
	{
		return -ahrs.getAngle(); // sign correction so that heading increases as robot turns to the left
	}

	public double getWorldLinearAccelerationX()
	{
		return ahrs.getWorldLinearAccelX();
	}

	public double getWorldLinearAccelerationY()
	{
		return ahrs.getWorldLinearAccelY();
	}
}
