package lin.xposed.hook.view.main.itemview;

import android.content.Context;
import android.view.View;

import lin.xposed.common.config.SimpleConfig;
import lin.xposed.common.utils.ActivityTools;
import lin.xposed.hook.HookEnv;
import lin.xposed.hook.view.main.itemview.base.OtherViewItemInfo;

public class DeleteMethodInfo extends OtherViewItemInfo {

    public DeleteMethodInfo(Context context) {
        super(context);
    }

    @Override
    public String getLeftText() {
        return "清除方法信息";
    }

    @Override
    public String getTips() {
        return "清除方法信息并重启QQ开始查找方法";
    }

    @Override
    public View.OnClickListener getOnClick() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SimpleConfig simpleConfig = new SimpleConfig("BaseConfig");
                simpleConfig.remove("moduleVersionAndHostAppVersion");
                simpleConfig.submit();
                ActivityTools.killAppProcess(HookEnv.getHostAppContext());
            }
        };
    }
}
