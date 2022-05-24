package freak.rinterface.control;

public class RFreak {

    public static boolean isStartable() {
        return (!(System.getProperty("java.vm.name").equalsIgnoreCase("Java HotSpot(TM) Server VM")));
    }

}
