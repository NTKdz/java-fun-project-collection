import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.software.os.OperatingSystem;

public class FullSystemInfo {
    public static void main(String[] args) {
        SystemInfo si = new SystemInfo();
        OperatingSystem os = si.getOperatingSystem();
        CentralProcessor cpu = si.getHardware().getProcessor();
        GlobalMemory memory = si.getHardware().getMemory();

        System.out.println("OS: " + os);
        System.out.println("CPU: " + cpu.getProcessorIdentifier().getName());
        System.out.println("Cores: " + cpu.getLogicalProcessorCount());
        System.out.println("Total Memory: " + memory.getTotal() / (1024 * 1024) + " MB");
        System.out.println("Free Memory: " + memory.getAvailable() / (1024 * 1024) + " MB");
    }
}
