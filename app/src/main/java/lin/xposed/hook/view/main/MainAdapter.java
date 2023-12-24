package lin.xposed.hook.view.main;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import lin.xposed.R;
import lin.xposed.common.utils.ScreenParamUtils;
import lin.xposed.common.utils.ViewUtils;
import lin.xposed.hook.load.HookItemLoader;
import lin.xposed.hook.load.base.BaseSwitchFunctionHookItem;
import lin.xposed.hook.util.LogUtils;
import lin.xposed.hook.view.main.fragment.ItemFragment;
import lin.xposed.hook.view.main.fragment.SettingViewFragment;
import lin.xposed.hook.view.main.itemview.base.DefaultItemView;
import lin.xposed.hook.view.main.itemview.base.OtherViewItemInfo;
import lin.xposed.hook.view.main.itemview.info.DirectoryUiInfo;
import lin.xposed.hook.view.main.itemview.info.ItemUiInfo;
import lin.xposed.hook.view.main.itemview.info.ItemUiInfoGroupWrapper;

/*
 * 初始化流程
 * 循环所有项目
 * 将group信息先添加到item里 再循环group里面的元素添加到布局里
 */
public class MainAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public static final int MAIN_ITEM_VIEW = 0;//主页面视图
    public static final int FUNCTION_ITEM = 1;//功能标识
    public static final int DIRECTORY_ITEM = 2;//目录
    public static final int GROUP_NAME_ITEM = 3;//组
    private static List<Object> dataList;

    private Context context;


    public MainAdapter() {
        dataList = new ArrayList<>();
    }

    public static List<?> getDataList() {
        return dataList;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setDataList(List<Object> updateList) {
        dataList = updateList;
        notifyDataSetChanged();
    }
    //有几个创建几次

    /**
     * @param viewType this.getItemType(int)
     */
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        this.context = parent.getContext();
        //创建ViewHolder，返回每一项的布局类型
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view;
        RecyclerView.ViewHolder viewHolder = null;
        //判断布局类型
        switch (viewType) {
            //非功能类型
            case MAIN_ITEM_VIEW -> {
                view = inflater.inflate(R.layout.main_base_item_layout, parent, false);
                viewHolder = new DefaultItemView(view);
            }
            //功能类型
            case FUNCTION_ITEM -> {
                view = inflater.inflate(R.layout.item_withtips_layout, parent, false);
                viewHolder = new ItemViewHolder(view);
            }
            //目录类型
            case DIRECTORY_ITEM -> {
                view = inflater.inflate(R.layout.directory_item_root_layout, parent, false);
                viewHolder = new DirectoryItemViewHolder(view);
            }
            //组名类型
            case GROUP_NAME_ITEM -> {
                view = inflater.inflate(R.layout.group_name_layout, parent, false);
                viewHolder = new GroupItemViewHolder(view);
            }
        }
        return viewHolder;
    }


    //onBindViewHolder()方法用于对RecyclerView子项数据进行赋值，会在每个子项被滚动到屏幕内的时候执行
    //这里我们通过position参数的得到当前项的实例，然后将数据设置到ViewHolder的TextView即可
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        int radius = ScreenParamUtils.dpToPx(this.context, 20);
        //将数据和View绑定
        Object itemInfo = dataList.get(position);
        View view = holder.itemView;
        float[] backgroundRadius = {0, 0, 0, 0};//圆角度{左上角,右上角,左下角.右下角}
        boolean isGroupInfo = false;
        //设置上圆角
        if (itemInfo instanceof ItemUiInfoGroupWrapper groupWrapper) {
            isGroupInfo = true;
            backgroundRadius[0] = radius;
            backgroundRadius[1] = radius;
        }
        if (itemInfo instanceof ItemUiInfo itemUiInfo && position != 0) {
            if (MainAdapter.getDataList().get(position - 1) instanceof ItemUiInfo uiInfo) {
                String groupPath = itemUiInfo.getGroupPath();
                String previousGroupString = uiInfo.getGroupPath();
                if (previousGroupString != null && !previousGroupString.equals(groupPath)) {
                    backgroundRadius[0] = radius;
                    backgroundRadius[1] = radius;
                }
            }
        }

        if (position == 0) //判断是不是第一个
        {
            backgroundRadius[0] = radius;
            backgroundRadius[1] = radius;
        }
        if (itemInfo instanceof OtherViewItemInfo && !(dataList.get(position - 1) instanceof OtherViewItemInfo)) {
            backgroundRadius[0] = radius;
            backgroundRadius[1] = radius;
        }
        //设置下圆角
        if (position == dataList.size() - 1//判断是不是最后一个
                || dataList.get(position + 1) instanceof ItemUiInfoGroupWrapper || (dataList.get(position + 1) instanceof OtherViewItemInfo && !(itemInfo instanceof OtherViewItemInfo)))//判断下一个是不是groupInfo
        {
            backgroundRadius[2] = radius;
            backgroundRadius[3] = radius;
        }
        int color = isGroupInfo ? view.getContext().getColor(R.color.itemGroup_themeColors) : view.getContext().getColor(R.color.item_color);
        /*color -= 0xf * 16 ^ 7 + 0xf * 16 ^ 6;
        color += 5 * 16 ^ 7 + 9 * 16 ^ 6;*/

        //构造背景
        GradientDrawable background = ViewUtils.BackgroundBuilder.createRectangleDrawable(color, view.getContext().getColor(R.color.white), 0, backgroundRadius);
        background.setAlpha(170);
        //设置背景
        holder.itemView.setBackground(background);

        if (holder instanceof DefaultItemView defaultItemView) {
            OtherViewItemInfo info = (OtherViewItemInfo) itemInfo;

            defaultItemView.leftText.setText(info.getLeftText());
            defaultItemView.itemView.setOnClickListener(info.getOnClick());

            if (info.getTips() != null) {
                defaultItemView.tipsText.setText(info.getTips());
            } else {
                defaultItemView.tipsText.setVisibility(View.GONE);
            }

        }
        //普遍类型
        else if (holder instanceof ItemViewHolder itemViewHolder) {
            ItemUiInfo sourceUiInfo = (ItemUiInfo) itemInfo;
            itemViewHolder.itemUiInfo = sourceUiInfo;
            itemViewHolder.leftTextView.setText(sourceUiInfo.getItemName());

            BaseSwitchFunctionHookItem hookItem = (BaseSwitchFunctionHookItem) sourceUiInfo.item;

            if (hookItem.getTips() != null) {
                itemViewHolder.tipsText.setVisibility(View.VISIBLE);
                itemViewHolder.tipsText.setText(hookItem.getTips());
            }
            if (hookItem.getViewOnClickListener() != null) {
                itemViewHolder.itemView.setOnClickListener(hookItem.getViewOnClickListener());
            }
            if (hookItem.getExceptionCollectionToolInstance().hasException()) {
                itemViewHolder.leftTextView.setTextColor(context.getColor(R.color.蔷薇色));
            }

            itemViewHolder.itemSwitch.setChecked(hookItem.isEnabled());
            itemViewHolder.itemSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    hookItem.setEnabled(isChecked);
                    HookItemLoader.SettingLoader.saveData(hookItem);
                    if (isChecked) {
                        try {
                            hookItem.startLoadHook();
                        } catch (Exception e) {
                            hookItem.getExceptionCollectionToolInstance().addException(e);
                            itemViewHolder.leftTextView.setTextColor(context.getColor(R.color.蔷薇色));
                        }
                    }
                }
            });
        } else if (holder instanceof DirectoryItemViewHolder directoryItemViewHolder) {
            //目录类型
            DirectoryUiInfo directoryUiInfo = (DirectoryUiInfo) itemInfo;
            directoryItemViewHolder.directoryUIInfo = directoryUiInfo;
            directoryItemViewHolder.leftTextView.setText(directoryUiInfo.getItemName());
            directoryItemViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        //标题栏中央文字
                        MainSettingActivity.setTitleCenterText(directoryUiInfo.getItemName());
                        //
                        Bundle bundle = new Bundle();
                        bundle.putString("TAG", directoryUiInfo.getItemName());
                        ItemFragment fragment = new ItemFragment(directoryUiInfo);
                        fragment.setArguments(bundle);
                        FragmentManager fragmentManager = SettingViewFragment.firstFragment.getParentFragmentManager();
                        FragmentTransaction transaction = fragmentManager.beginTransaction();
                        transaction.setCustomAnimations(R.anim.slide_in_from_right, R.anim.slide_out_to_left, R.anim.fragment_pop_enter, R.anim.fragment_pop_exit);//动画
                        transaction.addToBackStack(null);//添加返回栈
                        transaction.replace(MainSettingActivity.ITEM_LIST_CONTAINER, fragment);//替换
                        transaction.commit();//提交更改
                    } catch (Exception e) {
                        LogUtils.addError(e);
                    }
                }
            });
        } else if (holder instanceof GroupItemViewHolder groupItemViewHolder) {
            //组名
            ItemUiInfoGroupWrapper groupWrapper = (ItemUiInfoGroupWrapper) itemInfo;
            groupItemViewHolder.groupWrapper = groupWrapper;
            groupItemViewHolder.groupNameTextView.setText(groupWrapper.getGroupName());
        }
    }

    /*
     * 返回数据源的长度(生成的空itemView数量)
     */
    @Override
    public int getItemCount() {
        return dataList.size();
    }

    /**
     * 根据位置获取类型返回给onCreateViewHolder(@NonNull ViewGroup parent, int viewType)的viewType
     */
    @Override
    public int getItemViewType(int position) {
        Object itemInfo = dataList.get(position);
        if (itemInfo instanceof ItemUiInfoGroupWrapper) {
            return GROUP_NAME_ITEM;
        } else if (itemInfo instanceof DirectoryUiInfo) {
            return DIRECTORY_ITEM;
        } else if (itemInfo instanceof ItemUiInfo) {
            return FUNCTION_ITEM;
        } else if (itemInfo instanceof OtherViewItemInfo) {
            return MAIN_ITEM_VIEW;
        }
        return 0;
    }


    //内部类，绑定控件
    static class ItemViewHolder extends RecyclerView.ViewHolder {
        public final TextView leftTextView;
        public final TextView tipsText;
        @SuppressLint("UseSwitchCompatOrMaterialCode")
        public final Switch itemSwitch;
        public ItemUiInfo itemUiInfo;

        public ItemViewHolder(View itemView) {//这个view参数就是recyclerview子项的最外层布局
            super(itemView);
            this.leftTextView = itemView.findViewById(R.id.item_left_text);
            this.tipsText = itemView.findViewById(R.id.item_left_tips_text);
            this.itemSwitch = itemView.findViewById(R.id.common_item_switch);
        }
    }

    static class DirectoryItemViewHolder extends RecyclerView.ViewHolder {

        public final TextView rightTextView;
        public final TextView leftTextView;
        public DirectoryUiInfo directoryUIInfo;

        public DirectoryItemViewHolder(View itemView) {
            super(itemView);
            //查找左文本控件
            leftTextView = itemView.findViewById(R.id.dir_item_left_text);
            //查找右文本控件
            rightTextView = itemView.findViewById(R.id.dir_item_right_text);
        }
    }

    static class GroupItemViewHolder extends RecyclerView.ViewHolder {
        public final TextView groupNameTextView;
        public ItemUiInfoGroupWrapper groupWrapper;

        public GroupItemViewHolder(View itemView) {
            super(itemView);
            groupNameTextView = itemView.findViewById(R.id.group_name_text);
        }
    }

}
