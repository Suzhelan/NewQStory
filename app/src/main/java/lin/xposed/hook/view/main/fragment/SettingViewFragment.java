package lin.xposed.hook.view.main.fragment;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import lin.xposed.R;
import lin.xposed.hook.util.LogUtils;
import lin.xposed.hook.view.main.LinearSpacingItemDecoration;
import lin.xposed.hook.view.main.MainAdapter;
import lin.xposed.hook.view.main.MainSettingActivity;
import lin.xposed.hook.view.main.itemview.base.OtherViewItemInfo;
import lin.xposed.hook.view.main.itemview.info.ItemUiInfoGroupWrapper;
import lin.xposed.hook.view.main.itemview.info.ItemUiInfoManager;


public class SettingViewFragment extends Fragment {
    public static Fragment firstFragment;
    private static List<Object> FirstItemViewInfoList;//{group,dirIno,dirInfo}
    private RecyclerView recyclerView;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //保存每次的实例
        firstFragment = this;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View RootView = inflater.inflate(R.layout.item_list_layout, container, false);
        recyclerView = RootView.findViewById(R.id.item_list_view);
        MainAdapter adapter = new MainAdapter();
        Bundle bundle = getArguments();
        {
            organizeAndSortData();
            adapter.setDataList(FirstItemViewInfoList);
        }
        recyclerView.setAdapter(adapter);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);
        LinearSpacingItemDecoration linearSpacingItemDecoration = new LinearSpacingItemDecoration();
        recyclerView.addItemDecoration(linearSpacingItemDecoration);

        //退回到QQ设置页面
        MainSettingActivity.leftText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().finish();
            }
        });

        return RootView;
    }

    //将数据整理并排序为新集合
    private void organizeAndSortData() {
        if (FirstItemViewInfoList == null) {
            FirstItemViewInfoList = new ArrayList<>();
            for (Object groupWrapper : ItemUiInfoManager.rootContainer) {
                if (groupWrapper instanceof ItemUiInfoGroupWrapper infoGroupWrapper) {
                    FirstItemViewInfoList.add(infoGroupWrapper);
                    FirstItemViewInfoList.addAll(infoGroupWrapper.getDirectoryUIInfoList());
                } else {
                    FirstItemViewInfoList.add(groupWrapper);
                }
            }
            for (Class<? extends OtherViewItemInfo> clz : OtherViewItemInfo.OTHER_VIEW_ITEM_INFO_LIST) {
                try {
                    FirstItemViewInfoList.add(clz.getConstructor(Context.class).newInstance(getActivity()));
                } catch (Exception e) {
                    LogUtils.addError(e);
                }
            }
        }
    }


}
