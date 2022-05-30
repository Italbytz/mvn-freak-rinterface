package freak.rinterface.control;

import freak.core.control.BatchProcessor;
import freak.core.modulesupport.PersistenceManager;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.Locale;

public class RFreak {

    public static boolean isStartable() {
        return (!(System.getProperty("java.vm.name").equalsIgnoreCase("Java HotSpot(TM) Server VM")));
    }

    public static void rMain() throws Exception {
        // processArguments
        File loadFile = null;
        File saveFile = null;
        File[] tempFiles = null;

        // Launch FrEAK
        Locale.setDefault(Locale.US);

        // The method init is called in order to build up a list of all
        // available persistence handlers.
        PersistenceManager.init();
        long interval = 1000 *  3600;
        LogRegInterface batchProcessor = new LogRegInterface(loadFile, saveFile, tempFiles, interval);
        batchProcessor.run();
    }
}
