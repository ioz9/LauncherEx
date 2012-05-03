/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.launcherex;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * GridView adapter to show the list of applications and shortcuts
 */
public class ApplicationsAdapter extends ArrayAdapter<ApplicationInfo> {
    private final LayoutInflater mInflater;

    public ApplicationsAdapter(Context context, ArrayList<ApplicationInfo> apps) {
        super(context, R.id.name, apps);
        mInflater = LayoutInflater.from(context);
        
        for (ApplicationInfo info : apps) {
        	info.icon = Utilities.createIconThumbnail(info.icon, getContext());
            info.filtered = true;
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ApplicationInfo info = getItem(position);
        ViewHolder holder;

        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.application_boxed, parent, false);
            
            holder = new ViewHolder();
            holder.name = (TextView) convertView.findViewById(R.id.name);
            
            convertView.setTag(holder);
        } else {
        	holder = (ViewHolder) convertView.getTag();
        }

        holder.name.setText(info.title);
        holder.name.setCompoundDrawablesWithIntrinsicBounds(null, info.icon, null, null);

        return convertView;
    }
    
    private class ViewHolder {
    	TextView name;
    }

	public void setChildDrawingCacheEnabled(boolean b) {}
	public void updateDataSet() {}
}