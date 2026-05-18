package com.chtj.base_framework.network;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.ArrayList;

public class FEthTools {
    private static final String TAG = "FAllEthTools";

    /**
     * 获取静态
     * @param context
     * @return
     */
    public static boolean isEthernetStatic(Context context) {
        try {
            Object ethManager = context.getSystemService("ethernet");
            if (ethManager == null) {
                return false;
            }

            String[] ifaces = (String[]) ethManager.getClass()
                    .getMethod("getAvailableInterfaces")
                    .invoke(ethManager);

            if (ifaces == null || ifaces.length == 0) {
                return false;
            }

            Object ipConfig = ethManager.getClass()
                    .getMethod("getConfiguration", String.class)
                    .invoke(ethManager, ifaces[0]);

            String mode = getIpAssignmentMode(ipConfig);
            return "STATIC".equals(mode);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 获取dhcp
     * @param context
     * @return
     */
    public static boolean isEthernetDhcp(Context context) {
        try {
            Object ethManager = context.getSystemService("ethernet");
            if (ethManager == null) {
                return false;
            }

            String[] ifaces = (String[]) ethManager.getClass()
                    .getMethod("getAvailableInterfaces")
                    .invoke(ethManager);

            if (ifaces == null || ifaces.length == 0) {
                return false;
            }

            Object ipConfig = ethManager.getClass()
                    .getMethod("getConfiguration", String.class)
                    .invoke(ethManager, ifaces[0]);

            String mode = getIpAssignmentMode(ipConfig);
            return "DHCP".equals(mode) || "UNASSIGNED".equals(mode);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 设置静态IP
     * @param context
     * @param iface
     * @param ip
     * @param prefix
     * @param gateway
     * @param dns
     * @param dns2
     * @return
     */
    public static boolean setEthernetStatic(
            Context context,
            String iface,
            String ip,
            int prefix,
            String gateway,
            String dns,
            String dns2) {
        try {
            Object ethernetManager = context.getSystemService("ethernet");
            if (ethernetManager == null) {
                Log.e(TAG, "setEthernetStatic failed: ethernet service is null");
                return false;
            }

            Class<?> ethernetManagerClass = Class.forName("android.net.EthernetManager");
            Class<?> staticIpConfigClass = Class.forName("android.net.StaticIpConfiguration");
            Class<?> linkAddressClass = Class.forName("android.net.LinkAddress");
            Class<?> ipConfigClass = Class.forName("android.net.IpConfiguration");

            Constructor<?> linkAddressCtor;
            try {
                linkAddressCtor = linkAddressClass.getConstructor(InetAddress.class, int.class);
            } catch (NoSuchMethodException e) {
                linkAddressCtor = linkAddressClass.getDeclaredConstructor(InetAddress.class, int.class);
                linkAddressCtor.setAccessible(true);
            }

            Object linkAddress = linkAddressCtor.newInstance(InetAddress.getByName(ip), prefix);

            ArrayList<InetAddress> dnsList = new ArrayList<>();
            if (dns != null && dns.length() > 0) {
                dnsList.add(InetAddress.getByName(dns));
            }
            if (dns2 != null && dns2.length() > 0) {
                dnsList.add(InetAddress.getByName(dns2));
            }

            Object staticIpConfig;
            try {
                Class<?> staticBuilderClass =
                        Class.forName("android.net.StaticIpConfiguration$Builder");
                Object staticBuilder = staticBuilderClass.getConstructor().newInstance();

                staticBuilderClass.getMethod("setIpAddress", linkAddressClass)
                        .invoke(staticBuilder, linkAddress);
                staticBuilderClass.getMethod("setGateway", InetAddress.class)
                        .invoke(staticBuilder, InetAddress.getByName(gateway));
                staticBuilderClass.getMethod("setDnsServers", Iterable.class)
                        .invoke(staticBuilder, dnsList);

                staticIpConfig = staticBuilderClass.getMethod("build").invoke(staticBuilder);
            } catch (Throwable builderError) {
                staticIpConfig = staticIpConfigClass.getConstructor().newInstance();

                Field ipAddressField = staticIpConfigClass.getField("ipAddress");
                ipAddressField.set(staticIpConfig, linkAddress);

                Field gatewayField = staticIpConfigClass.getField("gateway");
                gatewayField.set(staticIpConfig, InetAddress.getByName(gateway));

                Field dnsServersField = staticIpConfigClass.getField("dnsServers");
                @SuppressWarnings("unchecked")
                ArrayList<InetAddress> oldDnsList =
                        (ArrayList<InetAddress>) dnsServersField.get(staticIpConfig);
                oldDnsList.clear();
                oldDnsList.addAll(dnsList);
            }

            Object ipConfig;
            try {
                Class<?> ipBuilderClass = Class.forName("android.net.IpConfiguration$Builder");
                Object ipBuilder = ipBuilderClass.getConstructor().newInstance();

                ipBuilderClass.getMethod("setStaticIpConfiguration", staticIpConfigClass)
                        .invoke(ipBuilder, staticIpConfig);

                ipConfig = ipBuilderClass.getMethod("build").invoke(ipBuilder);
            } catch (Throwable builderError) {
                Class<?> ipAssignmentEnum =
                        Class.forName("android.net.IpConfiguration$IpAssignment");
                Class<?> proxySettingsEnum =
                        Class.forName("android.net.IpConfiguration$ProxySettings");

                Object ipAssignmentStatic =
                        Enum.valueOf((Class<Enum>) ipAssignmentEnum, "STATIC");
                Object proxyNone =
                        Enum.valueOf((Class<Enum>) proxySettingsEnum, "NONE");

                try {
                    Constructor<?> ipConfigCtor = ipConfigClass.getConstructor(
                            ipAssignmentEnum,
                            proxySettingsEnum,
                            staticIpConfigClass,
                            Class.forName("android.net.ProxyInfo")
                    );
                    ipConfig = ipConfigCtor.newInstance(
                            ipAssignmentStatic,
                            proxyNone,
                            staticIpConfig,
                            null
                    );
                } catch (Throwable ctorError) {
                    ipConfig = ipConfigClass.getConstructor().newInstance();

                    try {
                        ipConfigClass.getMethod("setIpAssignment", ipAssignmentEnum)
                                .invoke(ipConfig, ipAssignmentStatic);
                    } catch (Throwable e) {
                        Field ipAssignmentField = ipConfigClass.getField("ipAssignment");
                        ipAssignmentField.set(ipConfig, ipAssignmentStatic);
                    }

                    try {
                        ipConfigClass.getMethod("setProxySettings", proxySettingsEnum)
                                .invoke(ipConfig, proxyNone);
                    } catch (Throwable e) {
                        Field proxySettingsField = ipConfigClass.getField("proxySettings");
                        proxySettingsField.set(ipConfig, proxyNone);
                    }

                    try {
                        ipConfigClass.getMethod("setStaticIpConfiguration", staticIpConfigClass)
                                .invoke(ipConfig, staticIpConfig);
                    } catch (Throwable e) {
                        Field staticIpConfigField = ipConfigClass.getField("staticIpConfiguration");
                        staticIpConfigField.set(ipConfig, staticIpConfig);
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= 33) {
                try {
                    Class<?> requestClass =
                            Class.forName("android.net.EthernetNetworkUpdateRequest");
                    Class<?> requestBuilderClass =
                            Class.forName("android.net.EthernetNetworkUpdateRequest$Builder");

                    Object requestBuilder = requestBuilderClass.getConstructor().newInstance();
                    requestBuilderClass.getMethod("setIpConfiguration", ipConfigClass)
                            .invoke(requestBuilder, ipConfig);

                    Object request = requestBuilderClass.getMethod("build").invoke(requestBuilder);

                    Method updateConfigurationMethod = ethernetManagerClass.getMethod(
                            "updateConfiguration",
                            String.class,
                            requestClass,
                            java.util.concurrent.Executor.class,
                            Class.forName("android.os.OutcomeReceiver")
                    );

                    updateConfigurationMethod.invoke(
                            ethernetManager,
                            iface,
                            request,
                            null,
                            null
                    );

                    Log.i(TAG, "Static IP set for " + iface + " by updateConfiguration");
                    return true;
                } catch (Throwable e) {
                    Log.w(TAG, "updateConfiguration failed, fallback to setConfiguration", e);
                }
            }

            Method setConfigMethod = ethernetManagerClass.getMethod(
                    "setConfiguration",
                    String.class,
                    ipConfigClass
            );

            setConfigMethod.invoke(ethernetManager, iface, ipConfig);

            Log.i(TAG, "Static IP set for " + iface + " by setConfiguration");
            return true;
        } catch (Throwable t) {
            Log.e(TAG, "setEthernetStatic failed", t);
            return false;
        }
    }

    /**
     * 设置动态IP
     * @param context
     * @param iface
     */
    public static boolean setEthDhcp(Context context, String iface) {
        try {
            Object ethernetManager = context.getSystemService("ethernet");
            if (ethernetManager == null) {
                Log.e(TAG, "setEthernetDhcp failed: ethernet service is null");
                return false;
            }

            Class<?> ethernetManagerClass = Class.forName("android.net.EthernetManager");
            Class<?> ipConfigClass = Class.forName("android.net.IpConfiguration");

            Object ipConfiguration;
            try {
                Class<?> builderClass = Class.forName("android.net.IpConfiguration$Builder");
                Object builder = builderClass.getConstructor().newInstance();
                Method buildMethod = builderClass.getMethod("build");
                ipConfiguration = buildMethod.invoke(builder);
            } catch (Throwable builderError) {
                Class<?> ipAssignmentClass =
                        Class.forName("android.net.IpConfiguration$IpAssignment");
                Class<?> proxySettingsClass =
                        Class.forName("android.net.IpConfiguration$ProxySettings");

                Object dhcp = Enum.valueOf((Class<Enum>) ipAssignmentClass, "DHCP");
                Object none = Enum.valueOf((Class<Enum>) proxySettingsClass, "NONE");

                ipConfiguration = ipConfigClass.getConstructor().newInstance();

                try {
                    Method setIpAssignmentMethod =
                            ipConfigClass.getMethod("setIpAssignment", ipAssignmentClass);
                    setIpAssignmentMethod.invoke(ipConfiguration, dhcp);
                } catch (Throwable e) {
                    Field ipAssignmentField = ipConfigClass.getField("ipAssignment");
                    ipAssignmentField.set(ipConfiguration, dhcp);
                }

                try {
                    Method setProxySettingsMethod =
                            ipConfigClass.getMethod("setProxySettings", proxySettingsClass);
                    setProxySettingsMethod.invoke(ipConfiguration, none);
                } catch (Throwable e) {
                    Field proxySettingsField = ipConfigClass.getField("proxySettings");
                    proxySettingsField.set(ipConfiguration, none);
                }
            }

            if (Build.VERSION.SDK_INT >= 33) {
                try {
                    Class<?> requestClass =
                            Class.forName("android.net.EthernetNetworkUpdateRequest");
                    Class<?> requestBuilderClass =
                            Class.forName("android.net.EthernetNetworkUpdateRequest$Builder");

                    Object requestBuilder = requestBuilderClass.getConstructor().newInstance();

                    Method setIpConfigurationMethod =
                            requestBuilderClass.getMethod("setIpConfiguration", ipConfigClass);
                    setIpConfigurationMethod.invoke(requestBuilder, ipConfiguration);

                    Method buildRequestMethod = requestBuilderClass.getMethod("build");
                    Object request = buildRequestMethod.invoke(requestBuilder);

                    Class<?> outcomeReceiverClass = Class.forName("android.os.OutcomeReceiver");

                    Method updateConfigurationMethod =
                            ethernetManagerClass.getMethod(
                                    "updateConfiguration",
                                    String.class,
                                    requestClass,
                                    java.util.concurrent.Executor.class,
                                    outcomeReceiverClass
                            );

                    updateConfigurationMethod.invoke(
                            ethernetManager,
                            iface,
                            request,
                            null,
                            null
                    );

                    Log.i(TAG, "DHCP enabled for " + iface + " by updateConfiguration");
                    return true;
                } catch (Throwable e) {
                    Log.w(TAG, "updateConfiguration failed, fallback to setConfiguration", e);
                }
            }

            Method setConfigMethod =
                    ethernetManagerClass.getMethod(
                            "setConfiguration",
                            String.class,
                            ipConfigClass
                    );

            setConfigMethod.invoke(ethernetManager, iface, ipConfiguration);

            Log.i(TAG, "DHCP enabled for " + iface + " by setConfiguration");
            return true;
        } catch (Throwable e) {
            Log.e(TAG, "setEthernetDhcp failed", e);
            return false;
        }
    }

    /**
     * 检测以太网是否连接
     */
    public static boolean isEthernetCablePlugged(Context context) {
        String iface = getFirstEthernetIface(context);
        if (iface == null || iface.length() == 0) {
            iface = "eth0";
        }

        File carrierFile = new File("/sys/class/net/" + iface + "/carrier");
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(carrierFile));
            String value = reader.readLine();
            return "1".equals(value);
        } catch (Exception e) {
            Log.e(TAG, "read carrier failed, iface=" + iface, e);
            return false;
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * 启用以太网
     */
    public static boolean enableEthernet(Context context) {
        return setEthernetEnabled(context, true);
    }

    /**
     * 停用以太网
     */
    public static boolean disableEthernet(Context context) {
        return setEthernetEnabled(context, false);
    }

    private static boolean setEthernetEnabled(Context context, boolean enabled) {
        try {
            Object ethernetManager = context.getSystemService("ethernet");
            if (ethernetManager != null) {
                try {
                    Method method = ethernetManager.getClass()
                            .getMethod("setEthernetEnabled", boolean.class);
                    method.invoke(ethernetManager, enabled);
                    return true;
                } catch (NoSuchMethodException e) {
                    Log.w(TAG, "setEthernetEnabled is not supported by EthernetManager, fallback to NetworkManagementService");
                }
            }

            String iface = getFirstEthernetIface(context);
            if (iface == null || iface.length() == 0) {
                iface = "eth0";
            }

            // Android 11 原生 EthernetManager 没有 setEthernetEnabled，使用 network_management 控制网口 up/down。
            Class<?> serviceManagerClass = Class.forName("android.os.ServiceManager");
            Method getServiceMethod = serviceManagerClass.getMethod("getService", String.class);
            Object binder = getServiceMethod.invoke(null, "network_management");
            if (binder == null) {
                Log.e(TAG, "setEthernetEnabled failed: network_management service is null");
                return false;
            }

            Class<?> iBinderClass = Class.forName("android.os.IBinder");
            Class<?> nmStubClass = Class.forName("android.os.INetworkManagementService$Stub");
            Method asInterfaceMethod = nmStubClass.getMethod("asInterface", iBinderClass);
            Object networkManagementService = asInterfaceMethod.invoke(null, binder);
            if (networkManagementService == null) {
                Log.e(TAG, "setEthernetEnabled failed: INetworkManagementService is null");
                return false;
            }

            Method method;
            if (enabled) {
                method = networkManagementService.getClass()
                        .getMethod("setInterfaceUp", String.class);
            } else {
                method = networkManagementService.getClass()
                        .getMethod("setInterfaceDown", String.class);
            }

            method.invoke(networkManagementService, iface);
            Log.i(TAG, "setEthernetEnabled success, enabled=" + enabled + ", iface=" + iface);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "setEthernetEnabled failed, enabled=" + enabled, e);
            return false;
        }
    }

    private static String getFirstEthernetIface(Context context) {
        try {
            Object ethernetManager = context.getSystemService("ethernet");
            if (ethernetManager == null) {
                return null;
            }

            Method method = ethernetManager.getClass()
                    .getMethod("getAvailableInterfaces");
            String[] ifaces = (String[]) method.invoke(ethernetManager);

            if (ifaces != null && ifaces.length > 0) {
                return ifaces[0];
            }
        } catch (Exception e) {
            Log.e(TAG, "getFirstEthernetIface failed", e);
        }
        return null;
    }

    public static String getIpAssignmentMode(Object ipConfig) throws Exception {
        if (ipConfig == null) {
            return null;
        }

        try {
            Object ipAssignment = ipConfig.getClass()
                    .getMethod("getIpAssignment")
                    .invoke(ipConfig);
            return String.valueOf(ipAssignment);
        } catch (NoSuchMethodException e) {
            // 兼容部分定制系统隐藏 getter 的情况，Android 11 源码中字段名为 ipAssignment。
            Field ipAssignmentField = ipConfig.getClass().getField("ipAssignment");
            Object ipAssignment = ipAssignmentField.get(ipConfig);
            return String.valueOf(ipAssignment);
        }
    }
}