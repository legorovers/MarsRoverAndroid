package Adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v4.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.liverpool.university.marsrover.R;

import java.util.ArrayList;

/**
 * Created by joecollenette on 17/07/2015.
 */
public class MarsListAdapter extends BaseAdapter
{
	private Activity activity;
	private ArrayList<Pair<String, Drawable>> items;
	private LayoutInflater inflater;

	public MarsListAdapter(Activity _activity, ArrayList<Pair<String, Drawable>> _items)
	{
		activity = _activity;
		items = _items;
		inflater = (LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	public int getCount()
	{
		return items.size();
	}

	@Override
	public Pair<String, Drawable> getItem(int position)
	{
		return items.get(position);
	}

	@Override
	public long getItemId(int position)
	{
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		View view;
		if (convertView == null)
		{
			view = inflater.inflate(R.layout.mars_list_item, null);
		}
		else
		{
			view = convertView;
		}

		((TextView)view.findViewById(R.id.txtItem)).setText(items.get(position).first);
		((ImageView)view.findViewById(R.id.imgIcon)).setImageDrawable(items.get(position).second);
		return view;
	}
}
