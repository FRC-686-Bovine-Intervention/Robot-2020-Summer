package frc.robot;

import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.shuffleboard.Shuffleboard;
import frc.robot.auto.AutoModeExecuter;
import frc.robot.lib.joystick.SelectedDriverControls;
import frc.robot.loops.DriveLoop;
import frc.robot.loops.LoopController;
import frc.robot.subsystems.Drive;

public class Robot extends TimedRobot {
  SmartDashboardInteractions smartDashboardInteractions = SmartDashboardInteractions.getInstance();
  SelectedDriverControls selectedDriverControls = SelectedDriverControls.getInstance();
  private AutoModeExecuter autoModeExecuter = null;
  private LoopController loopController;

  Drive drive = Drive.getInstance();
  
  @Override
  public void robotInit() {
    loopController = new LoopController();
    loopController.register(DriveLoop.getInstance());


    selectedDriverControls.setDriverControls( smartDashboardInteractions.getDriverControlsSelection() );
  }

  @Override
  public void robotPeriodic() {
  }

  @Override
  public void autonomousInit() {
		Shuffleboard.startRecording();

    selectedDriverControls.setDriverControls(smartDashboardInteractions.getDriverControlsSelection());

    if (autoModeExecuter != null)
    {
      autoModeExecuter.stop();
    }
      autoModeExecuter = null;
      autoModeExecuter = new AutoModeExecuter();
      autoModeExecuter.setAutoMode(smartDashboardInteractions.getAutoModeSelection());
      autoModeExecuter.start();
  }

  @Override
  public void autonomousPeriodic() {
    //Do nothing, auto is ran on its own thread
  }

  @Override
  public void teleopInit() {
    autoModeExecuter.stop();
    loopController.start();
  }

  @Override
  public void teleopPeriodic() {
    drive.setOpenLoop(selectedDriverControls.getDriveCommand());
  }

  @Override
  public void disabledInit() {
    loopController.stop(); //Making sure loops are closed
    autoModeExecuter.stop(); //Again making sure that we don't have runaway threads
  }
  
  @Override
  public void disabledPeriodic() {
  }

  @Override
  public void testInit() {
  }

  @Override
  public void testPeriodic() {
  }
}
