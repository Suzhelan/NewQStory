package lin.xposed.hook.util.qq;

import lin.util.ReflectUtils.ClassUtils;
import lin.util.ReflectUtils.ConstructorUtils;
import lin.util.ReflectUtils.FieIdUtils;
import lin.util.ReflectUtils.MethodTool;
import lin.xposed.hook.QQVersion;
import lin.xposed.hook.item.api.SessionApi;

public class SessionUtils {
    /**
     * 获取当前聊天对象 已适配nt和非nt版本
     *
     * @return CurrentContact
     */
    public static Object getCurrentContact() {
        Object session = SessionApi.getSession();
        if (QQVersion.isQQNT()) {
            return aioContactToContact(session);
        }
        return session;
    }

    public static QSContact AIOContactToQSContact(Object aioContact) {
        try {
            String peerUid = FieIdUtils.getField(aioContact, "f", String.class);
            int type = FieIdUtils.getField(aioContact, "e", int.class);
            String guild = FieIdUtils.getField(aioContact, "g", String.class);
            String nick = FieIdUtils.getField(aioContact, "h", String.class);
            QSContact contact = new QSContact(type, peerUid);
            return contact;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * *         e = chatType;
     * *         f = peerUid;
     * *         g = guildId;
     * *         h = nick;
     */
    public static Object aioContactToContact(Object aioContact) {
        try {
            String peerUid = FieIdUtils.getField(aioContact, "f", String.class);
            int type = FieIdUtils.getField(aioContact, "e", int.class);
            String guild = FieIdUtils.getField(aioContact, "g", String.class);
            String nick = FieIdUtils.getField(aioContact, "h", String.class);
            Object contact = QQNT.getContact(type, peerUid, guild);
            return contact;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static class LegacyQQ {
        public static Object getAppInterface() {
            return SessionApi.getAppInterface();
        }
    }

    public static class QQNT {

        /**
         * 获取名称 未经测试
         *
         * @param peerUid groupUin length equals 0 / null ,get friendNickName
         * @param uin     friendUin
         * @return nickName
         */
        public static String getFriendNickName(String peerUid, String uin) {
            Class<?> contactUtilClass = ClassUtils.getClass("com.qwallet.temp.IContactUtils");
            Object ContactUtilsImpl = QQEnvTool.getQRouteApi(contactUtilClass);
            String targetNickName = MethodTool.find(contactUtilClass).name("getFriendNickName").params(String.class, String.class).returnType(String.class).call(ContactUtilsImpl, peerUid, uin);
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
         * 获取聊天对象
         *
         * @param type 联系人类型 2是群聊 1是好友
         * @param uin  正常的QQ号/群号
         */
        public static Object getContact(int type, String uin) {
            return getContact(type, uin, "");
        }

        /**
         * 获取聊天对象 包含可能是频道的情况
         *
         * @param type    type为4时创建频道聊天对象
         * @param guildId 频道id
         */
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
                return ConstructorUtils.newInstance(CommonQQMethodTools.getAIOContactClass(), new Class[]{int.class, String.class, String.class, String.class}, chatType, (chatType != 2 && chatType != 4 && isNumeric(peerUid)) ? QQEnvTool.getUidFromUin(peerUid) : peerUid, guildId, nick);
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

    public static class QSContact {
        public int chatType;
        public String peerUid;

        public QSContact(int chatType, String peerUid) {
            this.chatType = chatType;
            this.peerUid = peerUid;
        }

    }

}
