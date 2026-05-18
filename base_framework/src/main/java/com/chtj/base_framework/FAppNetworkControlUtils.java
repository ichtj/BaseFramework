package com.chtj.base_framework;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * App 网络访问控制工具类。
 *
 * <p>
 * 基于 Android Framework 隐藏接口：
 * {@code ConnectivityManager#setFirewallChainEnabled()}
 * 和 {@code ConnectivityManager#setUidFirewallRule()}
 * 实现指定 UID 的网络访问控制。
 * </p>
 *
 * <p>
 * 当前使用 {@code FIREWALL_CHAIN_OEM_DENY_1} 防火墙链，
 * 可用于实现：
 * </p>
 *
 * <ul>
 *     <li>禁用指定 App 网络</li>
 *     <li>恢复指定 App 网络</li>
 *     <li>查询禁网状态</li>
 *     <li>获取已禁网 App 列表</li>
 * </ul>
 *
 * <p>
 * 禁用状态会通过 {@link SharedPreferences} 持久化保存。
 * </p>
 */
public class FAppNetworkControlUtils {

    /**
     * 网络策略服务名称。
     */
    private static final String NETWORK_POLICY_SERVICE = "netpolicy";

    /**
     * SharedPreferences 文件名。
     */
    private static final String SP_NAME = "uid_network_firewall";

    /**
     * 已禁用网络 UID 集合 Key。
     */
    private static final String KEY_DISABLED_UIDS = "disabled_uids";

    /**
     * OEM 自定义 deny firewall chain。
     */
    private static final int FIREWALL_CHAIN_OEM_DENY_1 = 7;

    /**
     * 默认规则（允许访问网络）。
     */
    private static final int FIREWALL_RULE_DEFAULT = 0;

    /**
     * 拒绝规则（禁止访问网络）。
     */
    private static final int FIREWALL_RULE_DENY = 2;

    /**
     * 设置指定 UID 的网络访问状态。
     *
     * <p>
     * 当 {@code disabled=true} 时，
     * 将禁止该 UID 的网络访问。
     * </p>
     *
     * <p>
     * 当 {@code disabled=false} 时，
     * 恢复该 UID 的网络访问权限。
     * </p>
     *
     * @param context 上下文
     * @param uid 目标应用 UID
     * @param disabled
     * true：禁用网络
     * false：允许网络
     */
    public static void setUidNetworkDisabled(
            Context context,
            int uid,
            boolean disabled) {

        if (context == null) {
            return;
        }

        ensureOemDenyChainEnabled(context);

        if (disabled) {
            setUidFirewallRule(context, uid, FIREWALL_RULE_DENY);
            saveUidState(context, uid, true);
        } else {
            setUidFirewallRule(context, uid, FIREWALL_RULE_DEFAULT);
            saveUidState(context, uid, false);
        }
    }

    /**
     * 判断指定 UID 是否已被禁网。
     *
     * @param context 上下文
     * @param uid App UID
     * @return
     * true：已禁网
     * false：未禁网
     */
    public static boolean isUidNetworkDisabled(Context context, int uid) {
        Set<String> set = getDisabledUidStringSet(context);
        return set.contains(String.valueOf(uid));
    }

    /**
     * 获取所有已禁网 UID 列表。
     *
     * @param context 上下文
     * @return 已禁用网络的 UID 集合
     */
    public static List<Integer> getDisabledUidList(Context context) {
        List<Integer> result = new ArrayList<Integer>();
        Set<String> set = getDisabledUidStringSet(context);

        for (String item : set) {
            try {
                result.add(Integer.valueOf(Integer.parseInt(item)));
            } catch (Exception ignored) {
            }
        }

        return result;
    }

    /**
     * 获取所有已禁用网络的应用列表。
     *
     * @param context 上下文
     * @return 已禁网应用状态列表
     */
    public static List<AppNetworkState> getDisabledAppList(Context context) {
        return getAppListByState(context, true);
    }

    /**
     * 获取所有允许网络的应用列表。
     *
     * @param context 上下文
     * @return 已允许网络应用状态列表
     */
    public static List<AppNetworkState> getEnabledAppList(Context context) {
        return getAppListByState(context, false);
    }

    /**
     * 根据网络状态获取应用列表。
     *
     * @param context 上下文
     * @param disabled
     * true：获取已禁网应用
     * false：获取未禁网应用
     *
     * @return 应用网络状态列表
     */
    private static List<AppNetworkState> getAppListByState(
            Context context,
            boolean disabled) {

        List<AppNetworkState> result = new ArrayList<AppNetworkState>();

        if (context == null) {
            return result;
        }

        PackageManager pm = context.getPackageManager();
        List<ApplicationInfo> apps =
                pm.getInstalledApplications(0);

        for (int i = 0; i < apps.size(); i++) {
            ApplicationInfo app = apps.get(i);

            boolean uidDisabled =
                    isUidNetworkDisabled(context, app.uid);

            if (uidDisabled != disabled) {
                continue;
            }

            AppNetworkState state =
                    new AppNetworkState();

            state.uid = app.uid;
            state.packageName = app.packageName;

            CharSequence label =
                    app.loadLabel(pm);

            state.appName =
                    label == null
                            ? app.packageName
                            : label.toString();

            state.networkDisabled =
                    uidDisabled;

            result.add(state);
        }

        return result;
    }

    /**
     * 启用 OEM deny firewall chain。
     *
     * <p>
     * 如果 firewall chain 未启用，
     * 后续 UID 防火墙规则不会生效。
     * </p>
     *
     * @param context 上下文
     */
    private static void ensureOemDenyChainEnabled(
            Context context) {

        try {
            ConnectivityManager cm =
                    (ConnectivityManager)
                            context.getSystemService(
                                    Context.CONNECTIVITY_SERVICE);

            Method method =
                    ConnectivityManager.class.getMethod(
                            "setFirewallChainEnabled",
                            int.class,
                            boolean.class);

            method.invoke(
                    cm,
                    FIREWALL_CHAIN_OEM_DENY_1,
                    true);

        } catch (Exception e) {
            throw new RuntimeException(
                    "setFirewallChainEnabled failed",
                    e);
        }
    }

    /**
     * 设置 UID 的防火墙规则。
     *
     * @param context 上下文
     * @param uid 应用 UID
     * @param rule 防火墙规则
     */
    private static void setUidFirewallRule(
            Context context,
            int uid,
            int rule) {

        try {
            ConnectivityManager cm =
                    (ConnectivityManager)
                            context.getSystemService(
                                    Context.CONNECTIVITY_SERVICE);

            Method method =
                    ConnectivityManager.class.getMethod(
                            "setUidFirewallRule",
                            int.class,
                            int.class,
                            int.class);

            method.invoke(
                    cm,
                    FIREWALL_CHAIN_OEM_DENY_1,
                    uid,
                    rule);

        } catch (Exception e) {
            throw new RuntimeException(
                    "setUidFirewallRule failed, uid="
                            + uid
                            + ", rule="
                            + rule,
                    e);
        }
    }

    /**
     * 保存 UID 网络状态。
     *
     * @param context 上下文
     * @param uid 应用 UID
     * @param disabled 是否禁网
     */
    private static void saveUidState(
            Context context,
            int uid,
            boolean disabled) {

        SharedPreferences sp =
                context.getSharedPreferences(
                        SP_NAME,
                        Context.MODE_PRIVATE);

        Set<String> oldSet =
                sp.getStringSet(
                        KEY_DISABLED_UIDS,
                        null);

        Set<String> newSet =
                new HashSet<String>();

        if (oldSet != null) {
            newSet.addAll(oldSet);
        }

        String uidText =
                String.valueOf(uid);

        if (disabled) {
            newSet.add(uidText);
        } else {
            newSet.remove(uidText);
        }

        sp.edit()
                .putStringSet(
                        KEY_DISABLED_UIDS,
                        newSet)
                .apply();
    }

    /**
     * 获取已禁网 UID 字符串集合。
     *
     * @param context 上下文
     * @return UID 字符串集合
     */
    private static Set<String> getDisabledUidStringSet(
            Context context) {

        Set<String> result =
                new HashSet<String>();

        if (context == null) {
            return result;
        }

        SharedPreferences sp =
                context.getSharedPreferences(
                        SP_NAME,
                        Context.MODE_PRIVATE);

        Set<String> set =
                sp.getStringSet(
                        KEY_DISABLED_UIDS,
                        null);

        if (set != null) {
            result.addAll(set);
        }

        return result;
    }

    /**
     * App 网络状态实体类。
     */
    public static final class AppNetworkState {

        /**
         * App UID。
         */
        public int uid;

        /**
         * 包名。
         */
        public String packageName;

        /**
         * App 名称。
         */
        public String appName;

        /**
         * 是否已禁用网络。
         */
        public boolean networkDisabled;

        @Override
        public String toString() {
            return "AppNetworkState{"
                    + "uid=" + uid
                    + ", packageName='" + packageName + '\''
                    + ", appName='" + appName + '\''
                    + ", networkDisabled=" + networkDisabled
                    + '}';
        }
    }
}