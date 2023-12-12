package lin.xposed.hook.util.qq;

import lin.util.ReflectUtils.ClassUtils;
import lin.util.ReflectUtils.ConstructorUtils;
import lin.util.ReflectUtils.MethodTool;
import lin.xposed.hook.item.api.SessionApi;

public class SessionUtils {
    /**
     * 获取当前聊天对象
     *
     * @return CurrentAIOContact
     */
    public static Object getCurrentAIOContact() {
        return SessionApi.getSession();
    }

    public static class QQNT {

        /**
         * 获取名称
         *
         * @param peerUid groupUin length equals 0 / null ,get friendNickName
         * @param uin     friendUin
         * @return nickName
         */
        public static String getFriendNickName(String peerUid, String uin) {
            Class<?> contactUtilClass = ClassUtils.getClass("com.qwallet.temp.IContactUtils");
            Object ContactUtilsImpl = QQEnvTool.getQRouteApi(contactUtilClass);
            String targetNickName = MethodTool.find(contactUtilClass)
                    .name("getFriendNickName")
                    .params(String.class, String.class)
                    .returnType(String.class)
                    .call(ContactUtilsImpl, peerUid, uin);
            return targetNickName;
        }

        /**
         * 获取好友聊天对象
         */
        public static Object getFriendContact(String uin) {
            return getContact(1, uin);
        }

        /**
         * 获取群聊聊天对象
         */
        public static Object getGroupContact(String troopUin) {
            return getContact(2, troopUin);
        }

        /**
         * @param type 联系人类型 2是群聊 1是好友
         * @param uin  正常的QQ号/群号
         */
        public static Object getContact(int type, String uin) {
            return getContact(type, uin, "");
        }

        public static Object getContact(int type, String uin, String guildId) {
            Class<?> aClass = ClassUtils.getClass("com.tencent.qqnt.kernel.nativeinterface.Contact");
            try {
                return ConstructorUtils.newInstance(aClass, new Class[]{int.class, String.class, String.class}, type, (type != 2 && type != 4 && isNumeric(uin)) ? QQEnvTool.getUidFromUin(uin) : uin, guildId);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public static Object getAIOContact(int chatType, String peerUid, String guildId, String nick) {
            try {
                return ConstructorUtils.newInstance(CommonClass.getAIOContactClass(), new Class[]{int.class, String.class, String.class, String.class}, chatType, (chatType != 2 && chatType != 4 && isNumeric(peerUid)) ? QQEnvTool.getUidFromUin(peerUid) : peerUid, guildId, nick);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private static boolean isNumeric(String str) {
            for (int i = str.length(); --i >= 0; ) {
                int chr = str.charAt(i);
                if (chr < 48 || chr > 57) return false;
            }
            return true;
        }
    }

}
