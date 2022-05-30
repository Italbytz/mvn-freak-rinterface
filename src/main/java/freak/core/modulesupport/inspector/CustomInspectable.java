package freak.core.modulesupport.inspector;

public interface CustomInspectable {
    /**
     * Returns an inspector for editing properties of the Object.
     *
     * @return the inspector.
     */
    public Inspector getInspector();

}
