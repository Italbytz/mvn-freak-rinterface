/*
 * This file is part of RFrEAK. For licensing and copyright information
 * please see the file COPYING in the root directory of this
 * distribution or contact <robin.nunkesser@udo.edu>.
 */

package freak.rinterface.control;

import freak.core.control.*;
import freak.core.control.Actions.Action;
import freak.core.fitness.FitnessFunction;
import freak.core.graph.FreakGraphModel;
import freak.core.graph.Initialization;
import freak.core.modulesupport.Module;
import freak.core.modulesupport.UnsupportedEnvironmentException;
import freak.core.observer.Observer;
import freak.core.observer.ObserverManagerInterface;
import freak.core.observer.ObserverViewMismatchException;
import freak.core.populationmanager.PopulationManager;
import freak.core.searchspace.SearchSpace;
import freak.core.stoppingcriterion.StoppingCriterion;
import freak.module.observer.ResultObserver;
import freak.module.postprocessor.booleanfunction.Count;
import freak.module.stoppingcriterion.GenerationCount;
import freak.module.support.OperatorGraphFile;
import freak.rinterface.model.RReturns;

import java.io.*;

/**
 * @author  Heiko, Stefan, Robin
 */
public class LogRegInterface implements StateListener {

    //flag that tells the program to not execute System.exit(...) when terminated(...) is called
    //(because otherwise R would crash)
    //this flag should only be set to "true", when the program is run from R
    //consequently the default value is "false".
    //the R Interface will automatically set it to "true", when needed. 
    public static boolean launchedFromR = true;

    //flag that tells the program, that no schedule file sould be read,
    //because the schedule will be set from R
    public static boolean scheduleWillBeSetByR = false;

    private static ScheduleInterface rSchedule;

    private RunControl runControl;

    private File loadFile;
    private File saveFile;
    private File[] tempFiles;
    private int nextTempFile;

    private long lastSave;
    private long saveInterval;

    private volatile boolean terminated = false;

    public LogRegInterface(File loadFile, File saveFile, File[] tempFiles, long saveInterval) {
        this.loadFile = loadFile;
        this.saveFile = saveFile;
        this.tempFiles = tempFiles;
        this.saveInterval = saveInterval;
    }

    public void run() throws FileNotFoundException, IOException, ClassNotFoundException {
        RReturns.clear();
        if (scheduleWillBeSetByR) { //Schedule that was set by R is used
            runControl = new RunControl(this);
            runControl.setNewSchedule(rSchedule);
            runControl.request(new Actions.StartAction());

        } else { // schedule is created as usual

            runControl = new RunControl(this);
            /*			RunControl runControlFromScratch = new RunControl(this);
            			RunControl runControlFromCopying = new RunControl(this);*/

            FreakFile file = FreakFile.read(new FileInputStream(loadFile));

            runControl.fromFile(file);
            /*			runControlFromScratch.setNewSchedule(this.createSchedule());
            			runControlFromCopying.setNewSchedule(this
            					.testScheduleCopying(runControl.getSchedule()));*/
            //this.testScheduleEditing(runControl.getSchedule());

            /*Iterator it = runControl.getSchedule().getAllModules().iterator();
            while (it.hasNext()) {
            	System.out.println(it.next().toString());
            }
            System.out.println("---------------------------------");
            it = runControl.getSchedule().getBatchList().iterator();
            while (it.hasNext()) {
            	System.out.println(((Batch) it.next()).getRuns());
            }
            System.out.println("---------------------------------");*/

            lastSave = System.currentTimeMillis();
            // runControl.setNewSchedule(this.testScheduleEditing(runControl.getSchedule()));
            runControl.request(new Actions.StartAction());
            //			runControlFromScratch.request(new Actions.StartAction());
            // runControlFromCopying.request(new Actions.StartAction());
        }
        while ((launchedFromR) && (!terminated)) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                // inentionally do nothing
            }
        }
    }

    public Schedule testScheduleCopying(Schedule aSchedule) {
        Schedule schedule = new Schedule();
        Module m;
        try {
            // Suchraum
            schedule.setPhenotypeSearchSpace(aSchedule.getPhenotypeSearchSpace());

            // Fitnessfunktion
            schedule.setFitnessFunction(aSchedule.getFitnessFunction());

            // Stoppkriterium
            GenerationCount generationCount = new GenerationCount(schedule);
            generationCount.setPropertyCount(new Integer(40));
            generationCount.testSchedule(schedule);
            generationCount.initialize();
            generationCount.createEvents();

            StoppingCriterion[] stoppingCriteria = { generationCount };
            schedule.setStoppingCriteria(stoppingCriteria);

            // Populationmanager
            schedule.setPopulationManager(aSchedule.getPopulationManager());

            // Operatorgraph
            schedule.setGraphModel(aSchedule.getFreakGraphModel());

            // Initialisierung
            schedule.setInitialization(aSchedule.getInitialization());

        } catch (UnsupportedEnvironmentException e) {
            throw new RuntimeException("Something is wrong with the default Schedule.", e);
        }
        schedule.modulesEdited();
        return schedule;
    }

    public Schedule createSchedule() {
        Schedule schedule = new Schedule();

        Module m;
        try {
            // Suchraum
            m = new freak.module.searchspace.BooleanFunction(schedule);
            m.testSchedule(schedule);
            m.initialize();
            m.createEvents();
            schedule.setPhenotypeSearchSpace((SearchSpace) m);

            // Fitnessfunktion
            m = new freak.module.fitness.booleanfunction.GenericPareto(schedule);
            m.testSchedule(schedule);
            m.initialize();
            m.createEvents();
            schedule.setFitnessFunction((FitnessFunction) m);

            // Stoppkriterium
            GenerationCount generationCount = new GenerationCount(schedule);
            generationCount.setPropertyCount(new Integer(40));
            generationCount.testSchedule(schedule);
            generationCount.initialize();
            generationCount.createEvents();

            StoppingCriterion[] stoppingCriteria = { generationCount };
            schedule.setStoppingCriteria(stoppingCriteria);

            // Populationmanager
            m = new freak.module.populationmanager.DefaultPopulationManager(schedule);
            m.testSchedule(schedule);
            m.initialize();
            m.createEvents();
            schedule.setPopulationManager((PopulationManager) m);

            // Operatorgraph
            File f = new File("./export/resource/freak/module/graph/booleanfunction/ZuFaulZumKlicken.fop"); ///Users/nunkesser/sfb475a5/FrEAK
            try {
                OperatorGraphFile ogFile = OperatorGraphFile.read(new FileInputStream(f));
                FreakGraphModel model = ogFile.generateGraph(schedule);
                schedule.setGraphModel(model);

            } catch (Exception exc) {
                exc.printStackTrace();
            }

            // Initialisierung
            m = new freak.module.operator.initialization.RandomInitialization(schedule.getOperatorGraph());
            m.testSchedule(schedule);
            m.initialize();
            m.createEvents();
            schedule.setInitialization((Initialization) m);
        } catch (UnsupportedEnvironmentException e) {
            throw new RuntimeException("Something is wrong with the default Schedule.", e);
        }

        // Observer, View and Postprocessor		
        ObserverManagerInterface om = schedule.getObserverManager();
        m = new ResultObserver(schedule);
        //View
/*        freak.module.view.FileWriter output = new freak.module.view.FileWriter(schedule);
        try {
            ((Observer) m).addView(output);
        } catch (ObserverViewMismatchException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }*/
        //Postprocessor
        ((ResultObserver) m).setPostprocessor(-1);
        ((ResultObserver) m).setSelectPostprocessorFromGui(false);
        Count postprocessor = new Count(schedule);
        ((ResultObserver) m).setSelectedProcessor(postprocessor);
        m.initialize();
        m.createEvents();
        om.addObserver((Observer) m);

        // Batches und Runs
        Batch batch = schedule.createBatchFromCurrentConfigurations();
        batch.setRuns(1);
        BatchList batchList = schedule.getBatchList();
        batchList.clear();
        batchList.add(batch.copy(), true);

        // Update signalisieren
        schedule.modulesEdited();
        return schedule;
    }

    public Schedule testScheduleCreation() {
        Schedule schedule = new Schedule();

        Module m;
        try {
            // Suchraum
            m = new freak.module.searchspace.BitString(schedule);
            m.testSchedule(schedule);
            m.initialize();
            m.createEvents();
            schedule.setPhenotypeSearchSpace((SearchSpace) m);

            // Fitnessfunktion
            m = new freak.module.fitness.bitstring.OneMax(schedule);
            m.testSchedule(schedule);
            m.initialize();
            m.createEvents();
            schedule.setFitnessFunction((FitnessFunction) m);

            // Stoppkriterium
            GenerationCount generationCount = new GenerationCount(schedule);
            generationCount.setPropertyCount(new Integer(40));
            generationCount.testSchedule(schedule);
            generationCount.initialize();
            generationCount.createEvents();

            StoppingCriterion[] stoppingCriteria = { generationCount };
            schedule.setStoppingCriteria(stoppingCriteria);

            // Populationmanager
            m = new freak.module.populationmanager.DefaultPopulationManager(schedule);
            m.testSchedule(schedule);
            m.initialize();
            m.createEvents();
            schedule.setPopulationManager((PopulationManager) m);

            // Operatorgraph
            File f = new File("./export/resource/freak/module/graph/common/(1+1)EA.fop"); ///Users/nunkesser/sfb475a5/FrEAK
            try {
                OperatorGraphFile ogFile = OperatorGraphFile.read(new FileInputStream(f));
                FreakGraphModel model = ogFile.generateGraph(schedule);
                /*				model.getOperatorGraph().testSchedule(schedule);
                				model.getOperatorGraph().initialize();
                				model.getOperatorGraph().createEvents();*/
                schedule.setGraphModel(model);

            } catch (Exception exc) {
                exc.printStackTrace();
            }

            // Initialisierung
            m = new freak.module.operator.initialization.RandomInitialization(schedule.getOperatorGraph());
            m.testSchedule(schedule);
            m.initialize();
            m.createEvents();
            schedule.setInitialization((Initialization) m);
        } catch (UnsupportedEnvironmentException e) {
            throw new RuntimeException("Something is wrong with the default Schedule.", e);
        }
        createDefaultBatch(schedule);
        if (schedule.getBatchList().size() == 0) {
            createDefaultBatchList(schedule);
        }
        schedule.modulesEdited();
        return schedule;
    }

    public void testScheduleEditing(Schedule schedule) {
        try {
            // Set Stopping Criterion
            GenerationCount generationCount = new GenerationCount(schedule);
            generationCount.setPropertyCount(new Integer(40));
            generationCount.testSchedule(schedule);
            generationCount.initialize();
            generationCount.createEvents();

            StoppingCriterion[] stoppingCriteria = { generationCount };
            schedule.setStoppingCriteria(stoppingCriteria);
            //	BooleanFunction searchspace=(BooleanFunction)schedule.getGenotypeSearchSpace();
            //	searchspace.setPropertyInputPath("genica.csv");
        } catch (UnsupportedEnvironmentException e) {
            throw new RuntimeException("Something is wrong with the default Schedule.", e);
        }
        schedule.modulesEdited();
    }

    public void asynchroneousFeedback(ScheduleInterface schedule, Replay replay) {
        if (tempFiles == null) return;

        try {
            long now = System.currentTimeMillis();
            if (now - lastSave > saveInterval) {
                FreakFile currentState = new FreakFile(replay, schedule);
                File tempFile = tempFiles[nextTempFile];
                currentState.write(new FileOutputStream(tempFile));

                nextTempFile = (nextTempFile + 1) % tempFiles.length;
                lastSave = now;
            }
        } catch (IOException exc) {
            throw new RuntimeException(exc);
        }
    }

    public void synchroneousFeedback(ScheduleInterface activeSchedule, Replay replay) {}

    public void simulationCompleted(Action lastProcessed) {

        try {
            if (saveFile != null) {
                FreakFile save = runControl.toFile();
                save.write(new FileOutputStream(saveFile));
            }
        } catch (IOException exc) {
            exc.printStackTrace();
        } finally {
            // Cleanly terminate run control
            runControl.request(new Actions.TerminateAction());
        }
    }

    public void simulationException(Exception exc) {
        saveFile = null; // results may be invalid
        exc.printStackTrace();
    }

    public void terminated(Action lastProcessedBeforeTermination) {
        terminated = true;
        // Forcibly stop event queue
        if (launchedFromR) {
            runControl = null;
            //System.exit(0);
            //do nothing, ie. do not call System.exit(0)
        } else { //terminate JVM
            System.exit(0);
        }
    }

    public static void setRMode() {
        launchedFromR = true;
    }

    public static void setNormalMode() {
        launchedFromR = false;
    }

    //sets a schedule from R that will be used, when run() is called
    public static void rSetSchedule(ScheduleInterface sched) {
        scheduleWillBeSetByR = true;
        rSchedule = sched;
    }

    /**
     * @param scheduleWillBeSetByR the scheduleWillBeSetByR to set
     */
    public static void setScheduleWillBeSetByR(boolean scheduleWillBeSetByR) {
        LogRegInterface.scheduleWillBeSetByR = scheduleWillBeSetByR;
    }

    /**
     * Creates a new default <code>Batch</code>.
     */
    public void createDefaultBatch(ScheduleInterface schedule) {
        // create new default batch inside the schedule
        schedule.setDefaultBatch(schedule.createBatchFromCurrentConfigurations());
    }

    /**
     * Creates a whole new <code>BatchList</code> with a copy of the
     * default <code>Batch</code>.
     */
    public void createDefaultBatchList(ScheduleInterface schedule) {
        BatchList batchList = schedule.getBatchList();
        batchList.clear();

        // add a copy of the default batch to the batch list
        batchList.add(schedule.getDefaultBatch().copy(), true);
    }

}
