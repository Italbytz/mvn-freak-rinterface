/*
 * This file is part of RFrEAK. For licensing and copyright information
 * please see the file COPYING in the root directory of this
 * distribution or contact <robin.nunkesser@udo.edu>.
 */

package freak.rinterface.model;

import freak.core.control.Batch;
import freak.core.control.BatchList;
import freak.core.control.Schedule;
import freak.core.control.ScheduleInterface;
import freak.core.fitness.FitnessFunction;
import freak.core.graph.FreakGraphModel;
import freak.core.graph.Initialization;
import freak.core.mapper.Mapper;
import freak.core.modulesupport.*;
import freak.core.observer.Observer;
import freak.core.observer.ObserverManagerInterface;
import freak.core.populationmanager.PopulationManager;
import freak.core.searchspace.SearchSpace;
import freak.core.stoppingcriterion.StoppingCriterion;
import freak.gui.graph.OperatorGraphFile;
import freak.module.fitness.pointset.*;
import freak.module.observer.ResultObserver;
import freak.module.operator.initialization.LTSInitialization;
import freak.module.searchspace.PointSet;
import freak.module.stoppingcriterion.Duration;
import freak.module.stoppingcriterion.GenerationCount;

import java.io.File;
import java.io.FileInputStream;
import java.util.jar.JarFile;

/**
 * A class to create and partly configure Schedules via .jcalls directly from R
 * @author  Dominic,Robin
 */
public class ScheduleConfigurator {

    //	public static String[] regressionMethods={"lms","lqd","lqs","lta","lts"};
    static ScheduleInterface currentSchedule;

    static boolean editingFinished = false;

    public static ScheduleInterface getCurrentSchedule() {
        return currentSchedule;
    }

    public static void setCurrentSchedule(ScheduleInterface sched) {
        //this method is used to pass a schedule that has just been edited with the graphical ScheduleEditor
        //to the Schedule Configurator, thus it can be retrieved from R
        currentSchedule = sched;
        editingFinished = true;
        /*System.out.println("\n\nThe Schedule that has just been edited, is now available in the \nScheduleConfigurator.");
        System.out.println("To retrieve it from R you can use:");
        System.out.println("[destination object] <- getCurrentSched()\n");**/
    }

    private static void updateBatchForSchedule(ScheduleInterface schedule, int runs) {
        // Batches und Runs
        Batch batch = schedule.createBatchFromCurrentConfigurations();
        batch.setRuns(runs);
        BatchList batchList = schedule.getBatchList();
        batchList.clear();
        batchList.add(batch.copy(), true);
        schedule.setDefaultBatch(batch);
    }

    public static ScheduleInterface getLTSSchedule(RDoubleMatrix data, int h, boolean adjust, int runs, int generationCountStop) {
        return getLTSSchedule(data, h, adjust, runs, generationCountStop, 0, 0);
    }

    public static ScheduleInterface getLTSSchedule(RDoubleMatrix data, int h, boolean adjust, int runs, int generationCountStop,
            int duration, String fitnessFunction) {
        int function = 0;
        if (fitnessFunction.equals("lts")) function = 0;
        if (fitnessFunction.equals("lms")) function = 1;
        if (fitnessFunction.equals("lta")) function = 6;
        if (fitnessFunction.equals("lqs")) function = 7;
        if (fitnessFunction.equals("lqd")) function = 8;
        return getLTSSchedule(data, h, adjust, runs, generationCountStop, duration, function);
    }


    public static ScheduleInterface getLTSSchedule(RDoubleMatrix data, int h, boolean adjust, int runs, int generationCountStop,
            int duration, int fitnessFunction) {
        ScheduleInterface schedule = new Schedule();

        /*ScheduleDependencyChecker scheduleDependencyChecker = new ScheduleDependencyChecker(null);
        scheduleDependencyChecker.setSchedule(schedule);*/

        Module m;
        try {
            // Suchraum
            m = new PointSet(schedule);
            int noOfPoints = data.getDim()[0];
            int dimension = data.getDim()[1];
            PointSet.Point[] points = new PointSet.Point[noOfPoints];
            for (int i = 0; i < noOfPoints; i++) {
                points[i] = new PointSet.Point(dimension);
                for (int j = 0; j < dimension; j++) {
                    points[i].setK(j, data.getValues()[i + j * noOfPoints]);
                }
            }
            ((PointSet) m).setPoints(points);
            m.testSchedule(schedule);
            m.initialize();
            m.createEvents();
            schedule.setPhenotypeSearchSpace((SearchSpace) m);

            // Fitnessfunktion
            switch (fitnessFunction) {
                case 0:
                    m = new LtSOptimization(schedule);
                    ((LtSOptimization) m).setPropertyH(new Integer(h));
                    ((LtSOptimization) m).setPropertyAdjust(adjust);
                    break;
                case 1:
                    m = new LMSOptimization(schedule);
                    ((LMSOptimization) m).setPropertyH(new Integer(h));
                    ((LMSOptimization) m).setPropertyAdjust(adjust);
                    break;
                case 2:
                    m = new LtSParetoQuantileOptimization(schedule);
                    ((LtSParetoQuantileOptimization) m).setPropertyH(new Integer(h));
                    ((LtSParetoQuantileOptimization) m).setPropertyAdjust(adjust);
                    break;
                case 3:
                    m = new LtSParetoOptimization(schedule);
                    ((LtSParetoOptimization) m).setPropertyH(new Integer(h));
                    ((LtSParetoOptimization) m).setPropertyAdjust(adjust);
                    break;
                case 4:
                    m = new LtSParetoQuantileOptimization(schedule);
                    ((LtSParetoQuantileOptimization) m).setPropertyH(new Integer(h));
                    ((LtSParetoQuantileOptimization) m).setPropertyAdjust(adjust);
                    break;
                case 5:
                    m = new LtSParetoOptimization(schedule);
                    ((LtSParetoOptimization) m).setPropertyH(new Integer(h));
                    ((LtSParetoOptimization) m).setPropertyAdjust(adjust);
                    break;
                case 6:
                    m = new LTAOptimization(schedule);
                    ((LTAOptimization) m).setPropertyH(new Integer(h));
                    ((LTAOptimization) m).setPropertyAdjust(adjust);
                    break;
                case 7:
                    m = new LQSOptimization(schedule);
                    ((LQSOptimization) m).setPropertyH(new Integer(h));
                    //					((LQSOptimization)m).setPropertyAdjust(adjust);
                    break;
                case 8:
                    m = new LQDOptimization(schedule);
                    ((LQDOptimization) m).setPropertyH(new Integer(h));
                    //					((LQDOptimization)m).setPropertyAdjust(adjust);					
                    break;
                case 9:
                    m = new LMSOptimization(schedule);
                    ((LMSOptimization) m).setPropertyH(new Integer(h));
                    ((LMSOptimization) m).setPropertyAdjust(adjust);
                    ((LMSOptimization) m).setAdjustMethodeIndex(1);
                    break;
                case 10:
                    m = new LMSOptimization(schedule);
                    ((LMSOptimization) m).setPropertyH(new Integer(h));
                    ((LMSOptimization) m).setPropertyAdjust(adjust);
                    ((LMSOptimization) m).setAdjustMethodeIndex(2);
                    break;
                case 11:
                    m = new LMSOptimization(schedule);
                    ((LMSOptimization) m).setPropertyH(new Integer(h));
                    ((LMSOptimization) m).setPropertyAdjust(adjust);
                    ((LMSOptimization) m).setAdjustMethodeIndex(3);
                    break;
                case 12:
                    m = new LMSOptimization(schedule);
                    ((LMSOptimization) m).setPropertyH(new Integer(h));
                    ((LMSOptimization) m).setPropertyAdjust(adjust);
                    ((LMSOptimization) m).setAdjustMethodeIndex(4);
                    break;
                case 13:
                    m = new LtSOptimization(schedule);
                    ((LtSOptimization) m).setPropertyH(new Integer(h));
                    ((LtSOptimization) m).setPropertyAdjust(adjust);
                    break;
                case 14:
                    m = new LMSOptimization(schedule);
                    ((LMSOptimization) m).setPropertyH(new Integer(h));
                    ((LMSOptimization) m).setPropertyAdjust(adjust);
                    break;

            }
            m.testSchedule(schedule);
            m.initialize();
            m.createEvents();
            schedule.setFitnessFunction((FitnessFunction) m);

            // Mapper
            m = new freak.module.mapper.pointset.BitStringMapper(schedule);
            m.testSchedule(schedule);
            m.initialize();
            m.createEvents();
            schedule.setMapper((Mapper) m);

            // Stoppkriterium
            if (generationCountStop > 0) {
                GenerationCount generationCount = new GenerationCount(schedule);
                generationCount.setPropertyCount(new Integer(generationCountStop));
                generationCount.testSchedule(schedule);
                generationCount.initialize();
                generationCount.createEvents();

                StoppingCriterion[] stoppingCriteria = { generationCount };
                schedule.setStoppingCriteria(stoppingCriteria);
            }

            if (duration > 0) {
                Duration durationObject = new Duration(schedule);
                durationObject.setPropertyDuration(new Integer(duration));
                durationObject.testSchedule(schedule);
                durationObject.initialize();
                durationObject.createEvents();

                StoppingCriterion[] stoppingCriteria = { durationObject };
                schedule.setStoppingCriteria(stoppingCriteria);
            }

            // Populationmanager
            m = new freak.module.populationmanager.DefaultPopulationManager(schedule);
            m.testSchedule(schedule);
            m.initialize();
            m.createEvents();
            schedule.setPopulationManager((PopulationManager) m);

            // Operatorgraph
            OperatorGraphCollector collector = new OperatorGraphCollector(schedule);

            ModuleInfo[] graphs = null;
            if (fitnessFunction >= 13) {
                graphs = collector.getPredefinedGraphs("LTSRestart.fop");
            } else if ((fitnessFunction <= 1) || (fitnessFunction > 5)) {
                graphs = collector.getPredefinedGraphs("LTSGraph.fop");
            } else if (fitnessFunction <= 3) {
                graphs = collector.getPredefinedGraphs("LTSCrossover.fop");
            } else if (fitnessFunction <= 5) {
                graphs = collector.getPredefinedGraphs("LTSCrossMove.fop");
            }
            if (graphs != null) {
                try {
                    OperatorGraphFile ogFile = null;

                    String[] classpaths = ClassCollector.getClassPaths();

                    for (String path:classpaths) {
                        if (path.contains("freak-core-graph")) {
                            JarFile jf = new JarFile(path);
                            ogFile = OperatorGraphFile.read(jf.getInputStream(jf.getJarEntry(graphs[0].getClassName())));
                        }
                    }

                   /* String startedFrom = ClassCollector.getStartedFrom();
                    // NEW CHECK
                    if (graphs[0].getClassName().startsWith("freak") && startedFrom.toLowerCase().endsWith(".jar")) {
                        JarFile jf = new JarFile(startedFrom);
                        ogFile = OperatorGraphFile.read(jf.getInputStream(jf.getJarEntry(graphs[0].getClassName())));
                    } else {
                        ogFile = OperatorGraphFile.read(new FileInputStream(new File(graphs[0].getClassName())));
                    }*/

                    if (ogFile==null) throw new RuntimeException("Error loading graph " + graphs[0].getClassName());

                    FreakGraphModel model = ogFile.generateGraph(schedule);
                    model.getOperatorGraph().setName(graphs[0].getName());

                    schedule.setGraphModel(model);

                } catch (Exception exc) {
                    System.out.println("Error loading graph " + graphs[0].getClassName());
                    exc.printStackTrace();
                }
            }

            // Initialisierung
            m = new freak.module.operator.initialization.LTSInitialization(schedule.getOperatorGraph());
            ((LTSInitialization) m).setPropertyNoOfBitsSetted(dimension);
            m.testSchedule(schedule);
            m.initialize();
            m.createEvents();
            schedule.setInitialization((Initialization) m);
			
            // Observer, View and Postprocessor		
            ObserverManagerInterface om = schedule.getObserverManager();
            m = new ResultObserver(schedule);

            //View
            /*freak.module.view.RReturn rReturn = new freak.module.view.RReturn(schedule);
            try {
                ((Observer) m).addView(rReturn);
            } catch (ObserverViewMismatchException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }*/
            m.initialize();
            m.createEvents();
            om.addObserver((Observer) m);

        } catch (UnsupportedEnvironmentException e) {
            throw new RuntimeException("Something is wrong with the default Schedule.", e);
        }

        updateBatchForSchedule(schedule, runs);

        // Update signalisieren (hat nur Auswirkungen auf den Operatorgraph)
        schedule.modulesEdited();

        return schedule;
    }


    /**
     * @return the editingFinished
     */
    public static boolean isEditingFinished() {
        return editingFinished;
    }

    /**
     * @param editingFinished the editingFinished to set
     */
    public static void setEditingFinished(boolean editingFinished) {
        ScheduleConfigurator.editingFinished = editingFinished;
    }

}
