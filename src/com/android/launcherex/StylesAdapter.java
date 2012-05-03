/* 
 * This program is free software. It comes without any warranty, to
 * the extent permitted by applicable law. You can redistribute it
 * and/or modify it under the terms of the Do What The Fuck You Want
 * To Public License, Version 2, as published by Sam Hocevar. See
 * http://sam.zoy.org/wtfpl/COPYING for more details.
 */

package com.android.launcherex;

import java.util.ArrayList;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class StylesAdapter extends BaseAdapter {
    private LayoutInflater inflater;
    private ArrayList<Drawable> imageDrawables = new ArrayList<Drawable>();
    private Integer[] titleIds;

    public StylesAdapter(Context context) {
        super();
        Resources resources = context.getResources();
        for (Integer id : getImageIds()) {
        	imageDrawables.add(resources.getDrawable(id));
        }
        titleIds = getTitleIds();
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }
    
    private Integer[] getImageIds() {
    	return new Integer[] {
                R.drawable.style_gingerbread,
                R.drawable.style_froyo,
                R.drawable.style_sense,
                R.drawable.style_slate,
                R.drawable.style_music,
                R.drawable.style_blackboard,
                R.drawable.style_espresso,
                R.drawable.style_blur,
                R.drawable.style_ninja,
                R.drawable.style_samurai
        };
    }
    
	private Integer[] getTitleIds() {
		return new Integer[] {
            R.string.gingerbread,
            R.string.froyo,
            R.string.sense,
            R.string.slate,
            R.string.music,
            R.string.blackboard,
            R.string.espresso,
            R.string.blur,
            R.string.ninja,
            R.string.samurai
		};
	}

    public int getCount() {
    	return imageDrawables.size();
    }

    public Object getItem(int position) {
        return position;
    }

    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
    	ViewHolder holder;
    	
    	if (convertView == null) {
            convertView = inflater.inflate(R.layout.theme_item, null);
    	
            holder = new ViewHolder();
            holder.previewTitle = (TextView) convertView.findViewById(R.id.theme_name);
            holder.previewImage = (ImageView) convertView.findViewById(R.id.theme_preview);
            
            convertView.setTag(holder);
    	} else {
        	holder = (ViewHolder) convertView.getTag();
        }

    	holder.previewTitle.setText(titleIds[position]);
    	holder.previewImage.setImageDrawable(imageDrawables.get(position));
    	
        return convertView;
    }
    
    private class ViewHolder {
    	TextView previewTitle;
		ImageView previewImage;
    }
}