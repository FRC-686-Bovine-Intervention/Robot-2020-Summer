package frc.robot.subsystems;

import java.util.ArrayList;
import java.util.List;

import frc.robot.loops.ILooper;
import frc.robot.loops.Loop;
import frc.robot.loops.LoopController;

/**
 * Used to reset, start, stop, and update all subsystems at once
 */
public class SubsystemManager implements ILooper
{
    private final List<Subsystem> mAllSubsystems;
    private List<Loop> mLoops = new ArrayList<>();
    
    public SubsystemManager(List<Subsystem> allSubsystems)
    {
        mAllSubsystems = allSubsystems;
    }
    
    public void outputToSmartDashboard()
    {
        mAllSubsystems.forEach((s) -> s.outputTelemetry());
    }

    public void writeToLog()
    {
        mAllSubsystems.forEach((s) -> s.writeToLog());
    }

    public void stop()
    {
        mAllSubsystems.forEach((s) -> s.stop());
    }




    private class EnabledLoop implements Loop
    {

        @Override
        public void onStart(double timestamp)
        {
            for (Loop l : mLoops)
            {
                l.onStart(timestamp);
            }
        }

        @Override
        public void onLoop(double timestamp)
        {
            for (Subsystem s : mAllSubsystems)
            {
                s.getStatus();
            }
            for (Loop l : mLoops)
            {
                l.onLoop(timestamp);
            }
            for (Subsystem s : mAllSubsystems)
            {
                s.sendCommands();
            }
        }

        @Override
        public void onStop(double timestamp)
        {
            for (Loop l : mLoops)
            {
                l.onStop(timestamp);
            }
        }
    }




    private class DisabledLoop implements Loop
    {

        @Override
        public void onStart(double timestamp)
        {

        }

        @Override
        public void onLoop(double timestamp)
        {
            for (Subsystem s : mAllSubsystems)
            {
                s.getStatus();
            }
            // unlike EnabledLoop, not running onLoop()
            for (Subsystem s : mAllSubsystems)
            {
                s.sendCommands();
            }
        }

        @Override
        public void onStop(double timestamp)
        {

        }
    }

    public void registerEnabledLoops(LoopController enabledLooper)
    {
        // register all subsystems with the enabledLooper
        mAllSubsystems.forEach((s) -> s.registerEnabledLoops(this));
        enabledLooper.register(new EnabledLoop());
    }

    public void registerDisabledLoops(LoopController disabledLooper)
    {
        // no subsystems are registered with the enabledLooper
        disabledLooper.register(new DisabledLoop());
    }

    @Override
    public void register(Loop loop)
    {
        mLoops.add(loop);
    }
}
