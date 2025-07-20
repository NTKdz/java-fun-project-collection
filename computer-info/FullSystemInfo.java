import oshi.SystemInfo;
import oshi.hardware.*;
import oshi.software.os.OperatingSystem;

public class FullSystemInfo {
    public static void main(String[] args) {
        // Get hardware and OS
        SystemInfo systemInfo = new SystemInfo();
        HardwareAbstractionLayer hal = systemInfo.getHardware();
        OperatingSystem os = systemInfo.getOperatingSystem();

        // Get Power Sources
        System.out.println("Power Sources: " + hal.getPowerSources());

        // Get Memory Info
        GlobalMemory memory = hal.getMemory();
        System.out.println("Total Memory: " + memory.getTotal()/1024 + " bytes");

        // 🔥 Get Temperature, Fan Speed, and Voltage
        Sensors sensors = hal.getSensors();
        double cpuTemp = sensors.getCpuTemperature();
        int[] fanSpeeds = sensors.getFanSpeeds();
        double voltage = sensors.getCpuVoltage();

        System.out.println("CPU Temperature: " + cpuTemp + " °C");
        System.out.println("Fan Speeds: ");
        for (int i = 0; i < fanSpeeds.length; i++) {
            System.out.println(" - Fan " + (i+1) + ": " + fanSpeeds[i] + " RPM");
        }
        System.out.println("CPU Voltage: " + voltage + " V");
    }
}
