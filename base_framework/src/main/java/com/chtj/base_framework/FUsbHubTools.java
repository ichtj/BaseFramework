package com.chtj.base_framework;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.text.TextUtils;
import android.view.InputDevice;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FUsbHubTools {
    private static final String TAG = FUsbHubTools.class.getSimpleName();
    public static List<UsbDeviceInfo> getAllConnectedUsbDevices(Context context) {
        List<UsbDeviceInfo> result = new ArrayList<UsbDeviceInfo>();

        addUsbManagerDevices(context, result);
        addInputDevices(result);
        addSysUsbDevices(result);

        return result;
    }

    private static void addUsbManagerDevices(Context context, List<UsbDeviceInfo> result) {
        if (context == null) {
            return;
        }

        UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        if (usbManager == null) {
            return;
        }

        HashMap<String, UsbDevice> deviceMap = usbManager.getDeviceList();
        if (deviceMap == null || deviceMap.isEmpty()) {
            return;
        }

        for (UsbDevice device : deviceMap.values()) {
            if (device == null) {
                continue;
            }

            UsbDeviceInfo info = findByVidPid(result, device.getVendorId(), device.getProductId());
            if (info == null) {
                info = new UsbDeviceInfo();
                result.add(info);
            }

            info.fromUsbManager = true;
            info.usbDeviceName = device.getDeviceName();
            info.deviceId = device.getDeviceId();
            info.vendorId = device.getVendorId();
            info.productId = device.getProductId();
            info.deviceClass = device.getDeviceClass();
            info.deviceSubclass = device.getDeviceSubclass();
            info.deviceProtocol = device.getDeviceProtocol();
            info.manufacturerName = choose(info.manufacturerName, device.getManufacturerName());
            info.productName = choose(info.productName, device.getProductName());
            info.version = choose(info.version, device.getVersion());
            info.configurationCount = device.getConfigurationCount();
            info.interfaceCount = device.getInterfaceCount();
        }
    }

    private static void addInputDevices(List<UsbDeviceInfo> result) {
        int[] ids = InputDevice.getDeviceIds();
        if (ids == null || ids.length == 0) {
            return;
        }

        for (int i = 0; i < ids.length; i++) {
            InputDevice device = InputDevice.getDevice(ids[i]);
            if (device == null) {
                continue;
            }

            if (!device.isExternal()) {
                continue;
            }

            UsbDeviceInfo info = findByVidPid(result, device.getVendorId(), device.getProductId());
            if (info == null) {
                info = new UsbDeviceInfo();
                result.add(info);
            }

            info.fromInputDevice = true;
            info.inputDeviceId = device.getId();
            info.inputName = choose(info.inputName, device.getName());
            info.descriptor = choose(info.descriptor, device.getDescriptor());
            info.vendorId = chooseInt(info.vendorId, device.getVendorId());
            info.productId = chooseInt(info.productId, device.getProductId());
            info.sources = device.getSources();
            info.inputType = getInputType(device);
        }
    }

    private static void addSysUsbDevices(List<UsbDeviceInfo> result) {
        File root = new File("/sys/bus/usb/devices");
        File[] files = root.listFiles();
        if (files == null || files.length == 0) {
            return;
        }

        for (int i = 0; i < files.length; i++) {
            File dir = files[i];
            if (dir == null || !dir.isDirectory()) {
                continue;
            }

            String dirName = dir.getName();

            if (dirName.indexOf(':') >= 0) {
                continue;
            }

            String idVendor = readTrim(new File(dir, "idVendor"));
            String idProduct = readTrim(new File(dir, "idProduct"));

            if (TextUtils.isEmpty(idVendor) || TextUtils.isEmpty(idProduct)) {
                continue;
            }

            int vendorId = parseHex(idVendor);
            int productId = parseHex(idProduct);

            UsbDeviceInfo info = findByVidPid(result, vendorId, productId);
            if (info == null) {
                info = new UsbDeviceInfo();
                result.add(info);
            }

            info.fromSysfs = true;
            info.sysfsPath = dir.getAbsolutePath();
            info.usbBusPath = dirName;
            info.vendorId = chooseInt(info.vendorId, vendorId);
            info.productId = chooseInt(info.productId, productId);
            info.manufacturerName = choose(info.manufacturerName, readTrim(new File(dir, "manufacturer")));
            info.productName = choose(info.productName, readTrim(new File(dir, "product")));
            info.serialNumber = choose(info.serialNumber, readTrim(new File(dir, "serial")));
            info.busNumber = parseDec(readTrim(new File(dir, "busnum")));
            info.deviceNumber = parseDec(readTrim(new File(dir, "devnum")));
            info.usbDeviceClass = readTrim(new File(dir, "bDeviceClass"));
            info.usbDeviceSubclass = readTrim(new File(dir, "bDeviceSubClass"));
            info.usbDeviceProtocol = readTrim(new File(dir, "bDeviceProtocol"));
        }
    }

    private static UsbDeviceInfo findByVidPid(List<UsbDeviceInfo> list, int vendorId, int productId) {
        if (vendorId == 0 && productId == 0) {
            return null;
        }

        for (int i = 0; i < list.size(); i++) {
            UsbDeviceInfo info = list.get(i);
            if (info.vendorId == vendorId && info.productId == productId) {
                return info;
            }
        }

        return null;
    }

    private static String getInputType(InputDevice device) {
        int sources = device.getSources();

        if ((sources & InputDevice.SOURCE_MOUSE) == InputDevice.SOURCE_MOUSE) {
            return "MOUSE";
        }

        if ((sources & InputDevice.SOURCE_KEYBOARD) == InputDevice.SOURCE_KEYBOARD) {
            return "KEYBOARD";
        }

        if ((sources & InputDevice.SOURCE_TOUCHPAD) == InputDevice.SOURCE_TOUCHPAD) {
            return "TOUCHPAD";
        }

        if ((sources & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK) {
            return "JOYSTICK";
        }

        if ((sources & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD) {
            return "GAMEPAD";
        }

        return "UNKNOWN";
    }

    private static String readTrim(File file) {
        BufferedReader reader = null;
        try {
            if (file == null || !file.exists()) {
                return null;
            }

            reader = new BufferedReader(new FileReader(file));
            String line = reader.readLine();
            if (line == null) {
                return null;
            }

            return line.trim();
        } catch (Exception e) {
            return null;
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (Exception ignored) {
            }
        }
    }

    private static int parseHex(String value) {
        try {
            return Integer.parseInt(value, 16);
        } catch (Exception e) {
            return 0;
        }
    }

    private static int parseDec(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return 0;
        }
    }

    private static String choose(String oldValue, String newValue) {
        if (!TextUtils.isEmpty(oldValue)) {
            return oldValue;
        }
        return newValue;
    }

    private static int chooseInt(int oldValue, int newValue) {
        if (oldValue != 0) {
            return oldValue;
        }
        return newValue;
    }

    public static final class UsbDeviceInfo {
        public boolean fromUsbManager;
        public boolean fromInputDevice;
        public boolean fromSysfs;

        public String usbDeviceName;
        public int deviceId;

        public int vendorId;
        public int productId;

        public int deviceClass;
        public int deviceSubclass;
        public int deviceProtocol;

        public String manufacturerName;
        public String productName;
        public String version;
        public String serialNumber;

        public int configurationCount;
        public int interfaceCount;

        public int inputDeviceId;
        public String inputName;
        public String descriptor;
        public int sources;
        public String inputType;

        public String sysfsPath;
        public String usbBusPath;
        public int busNumber;
        public int deviceNumber;
        public String usbDeviceClass;
        public String usbDeviceSubclass;
        public String usbDeviceProtocol;

        @Override
        public String toString() {
            return "UsbDeviceInfo{"
                    + "fromUsbManager=" + fromUsbManager
                    + ", fromInputDevice=" + fromInputDevice
                    + ", fromSysfs=" + fromSysfs
                    + ", usbDeviceName='" + usbDeviceName + '\''
                    + ", inputName='" + inputName + '\''
                    + ", manufacturerName='" + manufacturerName + '\''
                    + ", productName='" + productName + '\''
                    + ", vendorId=" + vendorId
                    + ", productId=" + productId
                    + ", vendorIdHex='" + Integer.toHexString(vendorId) + '\''
                    + ", productIdHex='" + Integer.toHexString(productId) + '\''
                    + ", inputType='" + inputType + '\''
                    + ", usbBusPath='" + usbBusPath + '\''
                    + ", sysfsPath='" + sysfsPath + '\''
                    + ", busNumber=" + busNumber
                    + ", deviceNumber=" + deviceNumber
                    + '}';
        }
    }
}
