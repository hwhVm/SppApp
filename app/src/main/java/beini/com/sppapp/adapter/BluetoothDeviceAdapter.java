package beini.com.sppapp.adapter;

import android.bluetooth.BluetoothDevice;
import android.support.v7.widget.RecyclerView;

import java.util.List;

import beini.com.sppapp.R;

/**
 * Created by beini on 2018/1/3.
 */

public class BluetoothDeviceAdapter extends BaseAdapter {

    private List<BluetoothDevice> mBluetoothDevices;

    public BluetoothDeviceAdapter(BaseBean<BluetoothDevice> baseBean) {
        super(baseBean);
        mBluetoothDevices = baseBean.getBaseList();

    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        holder.itemView.setTag(position);
        getTextView((ViewHolder) holder, R.id.text_device_name).setText(mBluetoothDevices.get(position).getName());
    }
}
